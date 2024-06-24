/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2022 Bell Canada.
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021-2023 Nordix Foundation
 *  Modifications Copyright (C) 2022-2024 TechMahindra Ltd.
 *  Modifications Copyright (C) 2022 Deutsche Telekom AG
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

import static org.onap.cps.rest.utils.MultipartFileUtil.extractYangResourcesMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.ValidationException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.rest.api.CpsDataApi;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.DeltaReport;
import org.onap.cps.utils.ContentType;
import org.onap.cps.utils.DataMapUtils;
import org.onap.cps.utils.JsonObjectMapper;
import org.onap.cps.utils.PrefixResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("${rest.api.cps-base-path}")
@RequiredArgsConstructor
public class DataRestController implements CpsDataApi {

    private static final String ROOT_XPATH = "/";
    private static final String ISO_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final DateTimeFormatter ISO_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern(ISO_TIMESTAMP_FORMAT);

    private final CpsDataService cpsDataService;
    private final JsonObjectMapper jsonObjectMapper;
    private final PrefixResolver prefixResolver;

    @Override
    public ResponseEntity<String> createNode(final String apiVersion,
                                             final String dataspaceName, final String anchorName,
                                             @RequestHeader(value = "Content-Type") final String contentTypeHeader,
                                             final String nodeData, final String parentNodeXpath,
                                             final String observedTimestamp) {
        final ContentType contentType = contentTypeHeader.contains(MediaType.APPLICATION_XML_VALUE) ? ContentType.XML
                : ContentType.JSON;
        if (isRootXpath(parentNodeXpath)) {
            cpsDataService.saveData(dataspaceName, anchorName, nodeData,
                    toOffsetDateTime(observedTimestamp), contentType);
        } else {
            cpsDataService.saveData(dataspaceName, anchorName, parentNodeXpath,
                    nodeData, toOffsetDateTime(observedTimestamp), contentType);
        }
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Void> deleteDataNode(final String apiVersion,
        final String dataspaceName, final String anchorName,
        final String xpath, final String observedTimestamp) {
        cpsDataService.deleteDataNode(dataspaceName, anchorName, xpath,
            toOffsetDateTime(observedTimestamp));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<String> addListElements(final String apiVersion, final String dataspaceName,
                                                  final String anchorName, final String parentNodeXpath,
                                                  final Object jsonData, final String observedTimestamp) {
        cpsDataService.saveListElements(dataspaceName, anchorName, parentNodeXpath,
                jsonObjectMapper.asJsonString(jsonData), toOffsetDateTime(observedTimestamp));
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Override
    @Timed(value = "cps.data.controller.datanode.get.v1",
            description = "Time taken to get data node")
    public ResponseEntity<Object> getNodeByDataspaceAndAnchor(final String dataspaceName,
        final String anchorName, final String xpath, final Boolean includeDescendants) {
        final FetchDescendantsOption fetchDescendantsOption = Boolean.TRUE.equals(includeDescendants)
            ? FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS : FetchDescendantsOption.OMIT_DESCENDANTS;
        final DataNode dataNode = cpsDataService.getDataNodes(dataspaceName, anchorName, xpath,
            fetchDescendantsOption, ContentType.JSON).iterator().next();
        final String prefix = prefixResolver.getPrefix(dataspaceName, anchorName, dataNode.getXpath());
        return new ResponseEntity<>(DataMapUtils.toDataMapWithIdentifier(dataNode, prefix), HttpStatus.OK);
    }

    @Override
    @Timed(value = "cps.data.controller.datanode.get.v2",
            description = "Time taken to get data node")
    public ResponseEntity<Object> getNodeByDataspaceAndAnchorV2(final String dataspaceName, final String anchorName,
                                                                final String contentTypeHeader, final String xpath,
                                                                final String fetchDescendantsOptionAsString) {
        final ContentType contentType = contentTypeHeader.contains(MediaType.APPLICATION_XML_VALUE) ? ContentType.XML
                : ContentType.JSON;
        final FetchDescendantsOption fetchDescendantsOption =
                FetchDescendantsOption.getFetchDescendantsOption(fetchDescendantsOptionAsString);
        final Collection<DataNode> dataNodes = cpsDataService.getDataNodes(dataspaceName, anchorName, xpath,
                fetchDescendantsOption, contentType);
        final List<Map<String, Object>> dataMaps = new ArrayList<>(dataNodes.size());
        for (final DataNode dataNode: dataNodes) {
            final String prefix = prefixResolver.getPrefix(dataspaceName, anchorName, dataNode.getXpath());
            final Map<String, Object> dataMap = DataMapUtils.toDataMapWithIdentifier(dataNode, prefix);
            dataMaps.add(dataMap);
        }
        if (contentType == ContentType.XML) {
            try {
                final XmlMapper xmlMapper = new XmlMapper();
                final StringBuilder xmlContentBuilder = new StringBuilder();
                for (final Map<String, Object> dataMap : dataMaps) {
                    final String xmlString = xmlMapper.writeValueAsString(dataMap);
                    xmlContentBuilder.append(xmlData(xmlString));
                }
                final String xmlContent = xmlContentBuilder.toString();
                return new ResponseEntity<>(xmlContent, HttpStatus.OK);
            } catch (final JsonProcessingException e) {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>(jsonObjectMapper.asJsonString(dataMaps), HttpStatus.OK);
        }
    }

    @Override
    public ResponseEntity<Object> updateNodeLeaves(final String apiVersion, final String dataspaceName,
                                                   final String anchorName, final String contentTypeHeader,
                                                   final String nodeData, final String parentNodeXpath,
                                                   final String observedTimestamp) {
        final ContentType contentType = contentTypeHeader.contains(MediaType.APPLICATION_XML_VALUE) ? ContentType.XML
                : ContentType.JSON;
        cpsDataService.updateNodeLeaves(dataspaceName, anchorName, parentNodeXpath,
                nodeData, toOffsetDateTime(observedTimestamp), contentType);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Object> replaceNode(final String apiVersion,
        final String dataspaceName, final String anchorName,
        final Object jsonData, final String parentNodeXpath, final String observedTimestamp) {
        cpsDataService
                .updateDataNodeAndDescendants(dataspaceName, anchorName, parentNodeXpath,
                        jsonObjectMapper.asJsonString(jsonData), toOffsetDateTime(observedTimestamp));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Object> replaceListContent(final String apiVersion,
                                                     final String dataspaceName, final String anchorName,
                                                     final String parentNodeXpath, final Object jsonData,
        final String observedTimestamp) {
        cpsDataService.replaceListContent(dataspaceName, anchorName, parentNodeXpath,
                jsonObjectMapper.asJsonString(jsonData), toOffsetDateTime(observedTimestamp));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> deleteListOrListElement(final String dataspaceName, final String anchorName,
        final String listElementXpath, final String observedTimestamp) {
        cpsDataService
            .deleteListOrListElement(dataspaceName, anchorName, listElementXpath, toOffsetDateTime(observedTimestamp));
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<Object> getDeltaByDataspaceAnchorAndPayload(final String dataspaceName,
                                                                      final String sourceAnchorName,
                                                                      final Object jsonPayload,
                                                                      final String xpath,
                                                                      final MultipartFile multipartFile) {
        final FetchDescendantsOption fetchDescendantsOption = FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS;

        final Map<String, String> yangResourceMap;
        if (multipartFile == null) {
            yangResourceMap = Collections.emptyMap();
        } else {
            yangResourceMap = extractYangResourcesMap(multipartFile);
        }
        final Collection<DeltaReport> deltaReports = Collections.unmodifiableList(
                cpsDataService.getDeltaByDataspaceAnchorAndPayload(dataspaceName, sourceAnchorName,
                xpath, yangResourceMap, jsonPayload.toString(), fetchDescendantsOption));

        return new ResponseEntity<>(jsonObjectMapper.asJsonString(deltaReports), HttpStatus.OK);
    }

    @Override
    @Timed(value = "cps.data.controller.get.delta",
            description = "Time taken to get delta between anchors")
    public ResponseEntity<Object> getDeltaByDataspaceAndAnchors(final String dataspaceName,
                                                                           final String sourceAnchorName,
                                                                           final String targetAnchorName,
                                                                           final String xpath,
                                                                           final String descendants) {
        final FetchDescendantsOption fetchDescendantsOption =
                FetchDescendantsOption.getFetchDescendantsOption(descendants);

        final List<DeltaReport> deltaBetweenAnchors =
                cpsDataService.getDeltaByDataspaceAndAnchors(dataspaceName, sourceAnchorName,
                targetAnchorName, xpath, fetchDescendantsOption);
        return new ResponseEntity<>(jsonObjectMapper.asJsonString(deltaBetweenAnchors), HttpStatus.OK);
    }

    private static boolean isRootXpath(final String xpath) {
        return ROOT_XPATH.equals(xpath);
    }

    private String xmlData(final String xmlString) {
        final int start = xmlString.indexOf(">") + 1;
        final int end = xmlString.lastIndexOf("<");
        if (start < end) {
            return xmlString.substring(start, end);
        }
        return xmlString;
    }

    private static OffsetDateTime toOffsetDateTime(final String datetTimestamp) {
        try {
            return StringUtils.isEmpty(datetTimestamp)
                ? null : OffsetDateTime.parse(datetTimestamp, ISO_TIMESTAMP_FORMATTER);
        } catch (final Exception exception) {
            throw new ValidationException(
                String.format("observed-timestamp must be in '%s' format", ISO_TIMESTAMP_FORMAT));
        }
    }
}
