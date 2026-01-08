/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
 *  Modifications Copyright (C) 2022-2023 Deutsche Telekom AG
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

import io.micrometer.core.annotation.Timed;
import java.util.Collection;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.CpsQueryService;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.api.parameters.PaginationOption;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.utils.CpsValidator;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CpsQueryServiceImpl implements CpsQueryService {

    private final CpsDataPersistenceService cpsDataPersistenceService;
    private final CpsValidator cpsValidator;

    @Override
    @Timed(value = "cps.data.service.datanode.query",
            description = "Time taken to query data nodes")
    public Collection<DataNode> queryDataNodes(final String dataspaceName, final String anchorName,
                                               final String cpsPath,
                                               final FetchDescendantsOption fetchDescendantsOption) {
        return queryDataNodes(dataspaceName, anchorName, cpsPath, fetchDescendantsOption, NO_LIMIT);
    }

    @Override
    @Timed(value = "cps.data.service.datanode.query",
            description = "Time taken to query data nodes with a limit on results")
    public Collection<DataNode> queryDataNodes(final String dataspaceName, final String anchorName,
                                               final String cpsPath,
                                               final FetchDescendantsOption fetchDescendantsOption,
                                               final int queryResultLimit) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        return cpsDataPersistenceService.queryDataNodes(dataspaceName,
                                                        anchorName,
                                                        cpsPath,
                                                        fetchDescendantsOption,
                                                        queryResultLimit);
    }

    @Override
    public <T> Set<T> queryDataLeaf(final String dataspaceName, final String anchorName, final String cpsPath,
                                    final Class<T> targetClass) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        return cpsDataPersistenceService.queryDataLeaf(dataspaceName, anchorName, cpsPath, targetClass);
    }

    @Override
    public Collection<DataNode> queryDataNodesAcrossAnchors(final String dataspaceName, final String cpsPath,
                                                            final FetchDescendantsOption fetchDescendantsOption,
                                                            final PaginationOption paginationOption) {
        cpsValidator.validateNameCharacters(dataspaceName);
        cpsValidator.validatePaginationOption(paginationOption);
        return cpsDataPersistenceService.queryDataNodesAcrossAnchors(dataspaceName, cpsPath,
                fetchDescendantsOption, paginationOption);
    }

    @Override
    public Integer countAnchorsForDataspaceAndCpsPath(final String dataspaceName, final String cpsPath) {
        cpsValidator.validateNameCharacters(dataspaceName);
        return cpsDataPersistenceService.countAnchorsForDataspaceAndCpsPath(dataspaceName, cpsPath);
    }
}
