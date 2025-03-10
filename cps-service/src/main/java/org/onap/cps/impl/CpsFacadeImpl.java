/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Nordix Foundation
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

package org.onap.cps.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsFacade;
import org.onap.cps.api.CpsQueryService;
import org.onap.cps.api.model.Anchor;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.api.parameters.PaginationOption;
import org.onap.cps.utils.DataMapUtils;
import org.onap.cps.utils.PrefixResolver;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CpsFacadeImpl implements CpsFacade {

    private final CpsAnchorService cpsAnchorService;
    private final CpsDataService cpsDataService;
    private final CpsQueryService cpsQueryService;
    private final PrefixResolver prefixResolver;

    @Override
    public Map<String, Object> getFirstDataNodeByAnchor(final String dataspaceName,
                                                        final String anchorName,
                                                        final String xpath,
                                                        final FetchDescendantsOption fetchDescendantsOption) {
        final DataNode dataNode = cpsDataService.getDataNodes(dataspaceName, anchorName, xpath,
            fetchDescendantsOption).iterator().next();
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        final String prefix = prefixResolver.getPrefix(anchor, dataNode.getXpath());
        return DataMapUtils.toDataMapWithIdentifier(dataNode, prefix);
    }

    @Override
    public List<Map<String, Object>> getDataNodesByAnchor(final String dataspaceName,
                                                          final String anchorName,
                                                          final String xpath,
                                                          final FetchDescendantsOption fetchDescendantsOption) {
        final Collection<DataNode> dataNodes = cpsDataService.getDataNodes(dataspaceName, anchorName, xpath,
            fetchDescendantsOption);
        return convertDataNodesToMaps(dataspaceName, anchorName, dataNodes);
    }

    @Override
    public List<Map<String, Object>> executeAnchorQuery(final String dataspaceName,
                                                        final String anchorName,
                                                        final String cpsPath,
                                                        final FetchDescendantsOption fetchDescendantsOption) {
        final Collection<DataNode> dataNodes =
            cpsQueryService.queryDataNodes(dataspaceName, anchorName, cpsPath, fetchDescendantsOption);
        return convertDataNodesToMaps(dataspaceName, anchorName, dataNodes);
    }

    @Override
    public List<Map<String, Object>> executeDataspaceQuery(final String dataspaceName,
                                                           final String cpsPath,
                                                           final FetchDescendantsOption fetchDescendantsOption,
                                                           final PaginationOption paginationOption) {
        final Collection<DataNode> dataNodes = cpsQueryService.queryDataNodesAcrossAnchors(dataspaceName,
            cpsPath, fetchDescendantsOption, paginationOption);
        final List<Map<String, Object>> dataNodesAsMaps = new ArrayList<>(dataNodes.size());
        String prefix = null;
        final Map<String, List<DataNode>> dataNodesPerAnchor = groupDataNodesPerAnchor(dataNodes);
        for (final Map.Entry<String, List<DataNode>> dataNodesPerAnchorEntry : dataNodesPerAnchor.entrySet()) {
            final String anchorName = dataNodesPerAnchorEntry.getKey();
            if (prefix == null) {  //Note how this method assume same prefix for ALL data nodes (technical debt issue!)
                final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
                prefix = prefixResolver.getPrefix(anchor, dataNodesPerAnchorEntry.getValue().get(0).getXpath());
            }
            final Map<String, Object> dataMap = DataMapUtils.toDataMapWithIdentifierAndAnchor(
                dataNodesPerAnchorEntry.getValue(), anchorName, prefix);
            dataNodesAsMaps.add(dataMap);
        }
        return dataNodesAsMaps;
    }

    @Override
    public int countAnchorsInDataspaceQuery(final String dataspaceName,
                                            final String cpsPath,
                                            final PaginationOption paginationOption) {
        if (paginationOption == PaginationOption.NO_PAGINATION) {
            return 1;
        }
        final int totalAnchors =  cpsQueryService.countAnchorsForDataspaceAndCpsPath(dataspaceName, cpsPath);
        return totalAnchors <= paginationOption.getPageSize() ? 1
            : (int) Math.ceil((double) totalAnchors / paginationOption.getPageSize());
    }

    private List<Map<String, Object>> convertDataNodesToMaps(final String dataspaceName,
                                                             final String anchorName,
                                                             final Collection<DataNode> dataNodes) {
        final List<Map<String, Object>> dataNodesAsMaps = new ArrayList<>(dataNodes.size());
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        for (final DataNode dataNode : dataNodes) {
            final String prefix = prefixResolver.getPrefix(anchor, dataNode.getXpath());
            final Map<String, Object> dataMap = DataMapUtils.toDataMapWithIdentifier(dataNode, prefix);
            dataNodesAsMaps.add(dataMap);
        }
        return dataNodesAsMaps;
    }

    private static Map<String, List<DataNode>> groupDataNodesPerAnchor(final Collection<DataNode> dataNodes) {
        return dataNodes.stream().collect(Collectors.groupingBy(DataNode::getAnchorName));
    }

}

