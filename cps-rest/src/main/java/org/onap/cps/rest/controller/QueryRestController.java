/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
 *  Modifications Copyright (C) 2022 Bell Canada.
 *  Modifications Copyright (C) 2022-2023 TechMahindra Ltd.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.rest.controller;

import io.micrometer.core.annotation.Timed;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.CpsQueryService;
import org.onap.cps.rest.api.CpsQueryApi;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.PaginationOption;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.utils.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

@RestController
@RequestMapping("${rest.api.cps-base-path}")
@RequiredArgsConstructor
public class QueryRestController implements CpsQueryApi {

    private final CpsQueryService cpsQueryService;
    private final JsonObjectMapper jsonObjectMapper;
    private final PrefixResolver prefixResolver;

    @Override
    @Timed(value = "cps.data.controller.datanode.query.v1",
            description = "Time taken to query data nodes")
    public ResponseEntity<Object> getNodesByDataspaceAndAnchorAndCpsPath(final String dataspaceName,
        final String anchorName, final String cpsPath, final Boolean includeDescendants) {
        final FetchDescendantsOption fetchDescendantsOption = Boolean.TRUE.equals(includeDescendants)
            ? FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS : FetchDescendantsOption.OMIT_DESCENDANTS;
        return executeNodesByDataspaceQueryAndCreateResponse(dataspaceName, anchorName, cpsPath,
                fetchDescendantsOption, ContentType.JSON);
    }

    @Override
    @Timed(value = "cps.data.controller.datanode.query.v2",
            description = "Time taken to query data nodes")
    public ResponseEntity<Object> getNodesByDataspaceAndAnchorAndCpsPathV2(final String dataspaceName,
                                                                           final String anchorName,
                                                                           @RequestHeader(value = "Content-Type")
                                                                               final String contentTypeInHeader,
                                                                           final String cpsPath,
                                                                           final String fetchDescendantsOptionAsString) {
        final ContentType contentType = contentTypeInHeader.contains(MediaType.APPLICATION_XML_VALUE) ? ContentType.XML
                : ContentType.JSON;
        final FetchDescendantsOption fetchDescendantsOption =
            FetchDescendantsOption.getFetchDescendantsOption(fetchDescendantsOptionAsString);
        return executeNodesByDataspaceQueryAndCreateResponse(dataspaceName, anchorName, cpsPath,
                fetchDescendantsOption, contentType);
    }

    @Override
    @Timed(value = "cps.data.controller.datanode.query.across.anchors",
            description = "Time taken to query data nodes across anchors")
    public ResponseEntity<Object> getNodesByDataspaceAndCpsPath(final String dataspaceName, final String cpsPath,
                                                                final String fetchDescendantsOptionAsString,
                                                                final Integer pageIndex, final Integer pageSize) {
        final FetchDescendantsOption fetchDescendantsOption =
                FetchDescendantsOption.getFetchDescendantsOption(fetchDescendantsOptionAsString);
        final PaginationOption paginationOption = (pageIndex == null || pageSize == null)
                ? PaginationOption.NO_PAGINATION : new PaginationOption(pageIndex, pageSize);
        final Collection<DataNode> dataNodes = cpsQueryService.queryDataNodesAcrossAnchors(dataspaceName,
                cpsPath, fetchDescendantsOption, paginationOption);
        final List<Map<String, Object>> dataNodesAsListOfMaps = new ArrayList<>(dataNodes.size());
        String prefix = null;
        final Map<String, List<DataNode>> anchorDataNodeListMap = prepareDataNodesForAnchor(dataNodes);
        for (final Map.Entry<String, List<DataNode>> anchorDataNodesMapEntry : anchorDataNodeListMap.entrySet()) {
            if (prefix == null) {
                prefix = prefixResolver.getPrefix(dataspaceName, anchorDataNodesMapEntry.getKey(),
                        anchorDataNodesMapEntry.getValue().get(0).getXpath());
            }
            final Map<String, Object> dataMap = DataMapUtils.toDataMapWithIdentifierAndAnchor(
                    anchorDataNodesMapEntry.getValue(), anchorDataNodesMapEntry.getKey(), prefix);
            dataNodesAsListOfMaps.add(dataMap);
        }
        final Integer totalPages = getTotalPages(dataspaceName, cpsPath, paginationOption);
        return ResponseEntity.ok().header("total-pages",
                totalPages.toString()).body(jsonObjectMapper.asJsonString(dataNodesAsListOfMaps));
    }

    private Integer getTotalPages(final String dataspaceName, final String cpsPath,
                                                          final PaginationOption paginationOption) {
        if (paginationOption == PaginationOption.NO_PAGINATION) {
            return 1;
        }
        final int totalAnchors = cpsQueryService.countAnchorsForDataspaceAndCpsPath(dataspaceName, cpsPath);
        return totalAnchors <= paginationOption.getPageSize() ? 1
                : (int) Math.ceil((double) totalAnchors / paginationOption.getPageSize());
    }

    private Map<String, List<DataNode>> prepareDataNodesForAnchor(final Collection<DataNode> dataNodes) {
        final Map<String, List<DataNode>> dataNodesMapForAnchor = new HashMap<>();
        for (final DataNode dataNode : dataNodes) {
            List<DataNode> dataNodesInAnchor = dataNodesMapForAnchor.get(dataNode.getAnchorName());
            if (dataNodesInAnchor == null) {
                dataNodesInAnchor = new ArrayList<>();
                dataNodesMapForAnchor.put(dataNode.getAnchorName(), dataNodesInAnchor);
            }
            dataNodesInAnchor.add(dataNode);
        }
        return dataNodesMapForAnchor;
    }

    private ResponseEntity<Object> executeNodesByDataspaceQueryAndCreateResponse(final String dataspaceName,
                                                                                 final String anchorName, final String cpsPath, final FetchDescendantsOption fetchDescendantsOption,
                                                                                 final ContentType contentType) {
        final Collection<DataNode> dataNodes =
                cpsQueryService.queryDataNodes(dataspaceName, anchorName, cpsPath, fetchDescendantsOption, contentType);
        final List<Map<String, Object>> dataNodesAsListOfMaps = new ArrayList<>(dataNodes.size());
        String prefix = null;
        for (final DataNode dataNode : dataNodes) {
            if (prefix == null) {
                prefix = prefixResolver.getPrefix(dataspaceName, anchorName, dataNode.getXpath());
            }
            final Map<String, Object> dataMap = DataMapUtils.toDataMapWithIdentifier(dataNode, prefix);
            dataNodesAsListOfMaps.add(dataMap);
        }
        if (contentType == ContentType.XML) {
            try {
                final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                final DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                final Document doc = docBuilder.newDocument();
                Element rootElement = null;
                for (final Map<String, Object> dataMap : dataNodesAsListOfMaps) {
                    for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
                        if (rootElement == null) {
                            rootElement = doc.createElement(entry.getKey());
                            doc.appendChild(rootElement);
                            appendChildElements(doc, rootElement, entry.getValue());
                        } else {
                            appendChildElements(doc, rootElement, entry.getValue());
                        }
                    }
                }
                final TransformerFactory transformerFactory = TransformerFactory.newInstance();
                final Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                final StringWriter writer = new StringWriter();
                final StreamResult result = new StreamResult(writer);
                final DOMSource source = new DOMSource(doc);
                transformer.transform(source, result);
                return ResponseEntity.ok().contentType(MediaType.valueOf(MediaType.APPLICATION_XML_VALUE)).body(writer.toString());
            } catch (TransformerException | ParserConfigurationException e) {
                return new ResponseEntity<>("Error generating XML response", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>(jsonObjectMapper.asJsonString(dataNodesAsListOfMaps), HttpStatus.OK);
        }
    }

    private static void appendChildElements(final Document document, final Element parentElement,
                                            final Object xmlContent) {
        if (xmlContent instanceof Map) {
            final Map<String, Object> map = (Map<String, Object>) xmlContent;
            for (final Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    final String key = entry.getKey();
                    final Element element = document.createElement(key);
                    appendChildElements(document, element, entry.getValue());
                    parentElement.appendChild(element);
                }
            }
        } else if (xmlContent instanceof List) {
            final List<Object> list = (List<Object>) xmlContent;
            for (final Object element : list) {
                appendChildElements(document, parentElement, element);
            }
        } else {
            parentElement.appendChild(document.createTextNode(xmlContent.toString()));
        }
    }

}


