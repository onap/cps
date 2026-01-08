/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Nordix Foundation
 *  Modifications Copyright (C) 2025 Deutsche Telekom AG
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsFacade;
import org.onap.cps.api.CpsQueryService;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.api.parameters.PaginationOption;
import org.onap.cps.cpspath.parser.CpsPathQuery;
import org.onap.cps.cpspath.parser.CpsPathUtil;
import org.onap.cps.utils.DataMapper;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CpsFacadeImpl implements CpsFacade {

    private final CpsDataService cpsDataService;
    private final CpsQueryService cpsQueryService;
    private final DataMapper dataMapper;

    @Override
    public Map<String, Object> getFirstDataNodeByAnchor(final String dataspaceName,
                                                        final String anchorName,
                                                        final String xpath,
                                                        final FetchDescendantsOption fetchDescendantsOption) {
        final DataNode dataNode = cpsDataService.getDataNodes(dataspaceName, anchorName, xpath,
            fetchDescendantsOption).iterator().next();
        return dataMapper.toDataMap(dataspaceName, anchorName, dataNode);
    }

    @Override
    public List<Map<String, Object>> getDataNodesByAnchor(final String dataspaceName,
                                                          final String anchorName,
                                                          final String xpath,
                                                          final FetchDescendantsOption fetchDescendantsOption) {
        final Collection<DataNode> dataNodes = cpsDataService.getDataNodes(dataspaceName, anchorName, xpath,
            fetchDescendantsOption);
        return dataMapper.toDataMaps(dataspaceName, anchorName, dataNodes);
    }

    @Override
    public Map<String, Object> getDataNodesByAnchorV3(final String dataspaceName,
                                                      final String anchorName,
                                                      final String xpath,
                                                      final FetchDescendantsOption fetchDescendantsOption) {
        final Collection<DataNode> dataNodes = cpsDataService.getDataNodes(dataspaceName, anchorName, xpath,
            fetchDescendantsOption);
        return dataMapper.toDataMapForApiV3(dataspaceName, anchorName, dataNodes);
    }

    @Override
    public List<Map<String, Object>> executeAnchorQuery(final String dataspaceName,
                                                        final String anchorName,
                                                        final String cpsPath,
                                                        final FetchDescendantsOption fetchDescendantsOption) {
        final CpsPathQuery cpsPathQuery = CpsPathUtil.getCpsPathQuery(cpsPath);
        if (cpsPathQuery.hasAttributeAxis()) {
            final String attributeName = cpsPathQuery.getAttributeAxisAttributeName();
            final Set<Object> attributeValues =
                    cpsQueryService.queryDataLeaf(dataspaceName, anchorName, cpsPath, Object.class);
            return dataMapper.toAttributeMaps(attributeName, attributeValues);
        }
        final Collection<DataNode> dataNodes =
            cpsQueryService.queryDataNodes(dataspaceName, anchorName, cpsPath, fetchDescendantsOption);
        return dataMapper.toDataMaps(dataspaceName, anchorName, dataNodes);
    }

    @Override
    public List<Map<String, Object>> executeDataspaceQuery(final String dataspaceName,
                                                           final String cpsPath,
                                                           final FetchDescendantsOption fetchDescendantsOption,
                                                           final PaginationOption paginationOption) {
        final Collection<DataNode> dataNodes = cpsQueryService.queryDataNodesAcrossAnchors(dataspaceName,
            cpsPath, fetchDescendantsOption, paginationOption);
        return dataMapper.toDataMaps(dataspaceName, dataNodes);
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

}

