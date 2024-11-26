/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation
 *  Modifications Copyright (C) 2022 Bell Canada.
 *  Modifications Copyright (C) 2022-2024 TechMahindra Ltd.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsQueryService;
import org.onap.cps.rest.api.CpsQueryApi;
import org.onap.cps.spi.api.FetchDescendantsOption;
import org.onap.cps.spi.api.PaginationOption;
import org.onap.cps.spi.api.model.Anchor;
import org.onap.cps.spi.api.model.DataNode;
import org.onap.cps.utils.ContentType;
import org.onap.cps.utils.DataMapUtils;
import org.onap.cps.utils.JsonObjectMapper;
import org.onap.cps.utils.PrefixResolver;
import org.onap.cps.utils.XmlFileUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${rest.api.cps-base-path}")
@RequiredArgsConstructor
public class QueryRestController implements CpsQueryApi {

    private final CpsQueryService cpsQueryService;
    private final CpsAnchorService cpsAnchorService;
    private final JsonObjectMapper jsonObjectMapper;
    private final PrefixResolver prefixResolver;

    @Override
    @Timed(value = "cps.data.controller.datanode.query.v1",
            description = "Time taken to query data nodes")
    public ResponseEntity<Object> getNodesByDataspaceAndAnchorAndCpsPath(final String dataspaceName,
                                                                         final String anchorName,
                                                                         final String cpsPath,
                                                                         final Boolean includeDescendants) {
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
                                                                           final String cpsPath,
                                                                           final String fetchDescendantsOptionAsString,
                                                                           final String contentTypeInHeader) {
        final ContentType contentType = ContentType.fromString(contentTypeInHeader);
        final FetchDescendantsOption fetchDescendantsOption =
            FetchDescendantsOption.getFetchDescendantsOption(fetchDescendantsOptionAsString);
        return executeNodesByDataspaceQueryAndCreateResponse(dataspaceName, anchorName, cpsPath,
                fetchDescendantsOption, contentType);
    }

    @Override
    @Timed(value = "cps.data.controller.datanode.query.across.anchors",
            description = "Time taken to query data nodes across anchors")
    public ResponseEntity<Object> getNodesByDataspaceAndCpsPath(final String dataspaceName,
                                                                final String cpsPath,
                                                                final String fetchDescendantsOptionAsString,
                                                                final Integer pageIndex,
                                                                final Integer pageSize) {
        final FetchDescendantsOption fetchDescendantsOption =
                FetchDescendantsOption.getFetchDescendantsOption(fetchDescendantsOptionAsString);
        final PaginationOption paginationOption = (pageIndex == null || pageSize == null)
                ? PaginationOption.NO_PAGINATION : new PaginationOption(pageIndex, pageSize);
        final Collection<DataNode> dataNodes = cpsQueryService.queryDataNodesAcrossAnchors(dataspaceName,
                cpsPath, fetchDescendantsOption, paginationOption);
        final List<Map<String, Object>> dataNodesAsListOfMaps = new ArrayList<>(dataNodes.size());
        String prefix = null;
        final Map<String, List<DataNode>> dataNodesPerAnchor = groupDataNodesPerAnchor(dataNodes);
        for (final Map.Entry<String, List<DataNode>> dataNodesPerAnchorEntry : dataNodesPerAnchor.entrySet()) {
            final String anchorName = dataNodesPerAnchorEntry.getKey();
            if (prefix == null) {
                final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
                prefix = prefixResolver.getPrefix(anchor, dataNodesPerAnchorEntry.getValue().get(0).getXpath());
            }
            final Map<String, Object> dataMap = DataMapUtils.toDataMapWithIdentifierAndAnchor(
                    dataNodesPerAnchorEntry.getValue(), anchorName, prefix);
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
        final int totalAnchors =  cpsQueryService.countAnchorsForDataspaceAndCpsPath(dataspaceName, cpsPath);
        return totalAnchors <= paginationOption.getPageSize() ? 1
                : (int) Math.ceil((double) totalAnchors / paginationOption.getPageSize());
    }

    private Map<String, List<DataNode>> groupDataNodesPerAnchor(final Collection<DataNode> dataNodes) {
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
            cpsQueryService.queryDataNodes(dataspaceName, anchorName, cpsPath, fetchDescendantsOption);
        final List<Map<String, Object>> dataNodesAsListOfMaps = new ArrayList<>(dataNodes.size());
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        String prefix = null;
        for (final DataNode dataNode : dataNodes) {
            if (prefix == null) {
                prefix = prefixResolver.getPrefix(anchor, dataNode.getXpath());
            }
            final Map<String, Object> dataMap = DataMapUtils.toDataMapWithIdentifier(dataNode, prefix);
            dataNodesAsListOfMaps.add(dataMap);
        }
        return buildResponseEntity(dataNodesAsListOfMaps, contentType);
    }

    private ResponseEntity<Object> buildResponseEntity(final List<Map<String, Object>> dataNodesAsListOfMaps,
                                               final ContentType contentType) {
        final String responseData;
        if (contentType == ContentType.XML) {
            responseData = XmlFileUtils.convertDataMapsToXml(dataNodesAsListOfMaps);
        } else {
            responseData = jsonObjectMapper.asJsonString(dataNodesAsListOfMaps);
        }
        return new ResponseEntity<>(responseData, HttpStatus.OK);
    }
}
