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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.CpsQueryService;
import org.onap.cps.rest.api.CpsQueryApi;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.utils.DataMapUtils;
import org.onap.cps.utils.JsonObjectMapper;
import org.onap.cps.utils.PrefixResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
                fetchDescendantsOption);
    }

    @Override
    @Timed(value = "cps.data.controller.datanode.query.v2",
            description = "Time taken to query data nodes")
    public ResponseEntity<Object> getNodesByDataspaceAndAnchorAndCpsPathV2(final String dataspaceName,
        final String anchorName, final String cpsPath, final String fetchDescendantsOptionAsString) {
        final FetchDescendantsOption fetchDescendantsOption =
            FetchDescendantsOption.getFetchDescendantsOption(fetchDescendantsOptionAsString);
        return executeNodesByDataspaceQueryAndCreateResponse(dataspaceName, anchorName, cpsPath,
                fetchDescendantsOption);
    }

    @Override
    @Timed(value = "cps.data.controller.datanode.query.across.anchors",
            description = "Time taken to query data nodes across anchors")
    public ResponseEntity<Object> getNodesByDataspaceAndCpsPath(final String dataspaceName, final String cpsPath,
                                                                final String fetchDescendantsOptionAsString,
                                                                final Integer pageIndex, final Integer pageSize) {
        final FetchDescendantsOption fetchDescendantsOption =
                FetchDescendantsOption.getFetchDescendantsOption(fetchDescendantsOptionAsString);
        final Collection<DataNode> dataNodes = cpsQueryService.queryDataNodesAcrossAnchors(dataspaceName,
                cpsPath, fetchDescendantsOption);
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
        if (pageIndex > 0 && pageSize > 0) {
            final Set<String> anchorNameList = new TreeSet<>();
            dataNodesAsListOfMaps.stream().forEach(dataNodesPerAnchor -> anchorNameList
                    .add(dataNodesPerAnchor.get("anchorName").toString()));
            final List<String> anchorNamesForPagination = fetchAnchorNamesForResponse(
                    anchorNameList, pageIndex, pageSize);
            final List<Map<String, Object>> pagedDataNodes = dataNodesAsListOfMaps.stream()
                    .filter(dataNodesPerAnchor -> anchorNamesForPagination.contains(dataNodesPerAnchor
                            .get("anchorName").toString())).collect(Collectors.toList());
            return ResponseEntity.ok().header("TotalResults",
                    String.valueOf(dataNodesAsListOfMaps.size())).body(jsonObjectMapper.asJsonString(pagedDataNodes));
        }
        return new ResponseEntity<>(jsonObjectMapper.asJsonString(dataNodesAsListOfMaps), HttpStatus.OK);
    }

    private List<String> fetchAnchorNamesForResponse(final Set<String> anchorNameList,
                                                     final long pageIndex, final long pageSize) {
        return anchorNameList.stream().skip((pageIndex - 1) * pageSize).limit(pageSize).collect(Collectors.toList());
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
             final String anchorName, final String cpsPath, final FetchDescendantsOption fetchDescendantsOption) {
        final Collection<DataNode> dataNodes =
            cpsQueryService.queryDataNodes(dataspaceName, anchorName, cpsPath, fetchDescendantsOption);
        final List<Map<String, Object>> dataNodesAsListOfMaps = new ArrayList<>(dataNodes.size());
        String prefix = null;
        for (final DataNode dataNode : dataNodes) {
            if (prefix == null) {
                prefix = prefixResolver.getPrefix(dataspaceName, anchorName, dataNode.getXpath());
            }
            final Map<String, Object> dataMap = DataMapUtils.toDataMapWithIdentifier(dataNode, prefix);
            dataNodesAsListOfMaps.add(dataMap);
        }
        return new ResponseEntity<>(jsonObjectMapper.asJsonString(dataNodesAsListOfMaps), HttpStatus.OK);
    }
}
