/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl;

import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NCMP_DATASPACE_NAME;
import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NCMP_DMI_REGISTRY_ANCHOR;
import static org.onap.cps.ncmp.api.impl.utils.YangDataConverter.convertYangModelCmHandleToNcmpServiceCmHandle;
import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS;
import static org.onap.cps.utils.CmHandleQueryRestParametersValidator.validateModuleNameConditionProperties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.NetworkCmProxyCmHandlerQueryService;
import org.onap.cps.ncmp.api.impl.utils.YangDataConverter;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.cps.spi.CpsAdminPersistenceService;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.model.Anchor;
import org.onap.cps.spi.model.CmHandleQueryServiceParameters;
import org.onap.cps.spi.model.ConditionProperties;
import org.onap.cps.spi.model.DataNode;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NetworkCmProxyCmHandlerQueryServiceImpl implements NetworkCmProxyCmHandlerQueryService {

    private static final String PROPERTY_QUERY_NAME = "hasAllProperties";
    private static final String MODULE_QUERY_NAME = "hasAllModules";
    private final CpsDataPersistenceService cpsDataPersistenceService;
    private final CpsAdminPersistenceService cpsAdminPersistenceService;

    /**
     * Query and return cm handles that match the given query parameters.
     *
     * @param cmHandleQueryServiceParameters the cm handle query parameters
     * @return collection of cm handles
     */
    @Override
    public Set<NcmpServiceCmHandle> queryCmHandles(
            final CmHandleQueryServiceParameters cmHandleQueryServiceParameters) {

        if (cmHandleQueryServiceParameters.getCmHandleQueryParameters().isEmpty()) {
            return getAllCmHandles();
        }

        final Set<String> cmHandleIds = new HashSet<>();
        final Map<String, NcmpServiceCmHandle> cmHandles = new HashMap<>();

        final boolean firstQuery = publicPropertyQuery(cmHandleQueryServiceParameters, cmHandleIds, cmHandles);
        moduleNameQuery(cmHandleQueryServiceParameters, cmHandleIds, cmHandles, firstQuery);

        final Set<NcmpServiceCmHandle> filteredNcmpServiceCmHandles = new HashSet<>();
        cmHandleIds.forEach(cmHandleId -> filteredNcmpServiceCmHandles.add(cmHandles.get(cmHandleId)));

        return filteredNcmpServiceCmHandles;
    }

    /**
     * Query and return cm handles that match the given query parameters.
     *
     * @param cmHandleQueryServiceParameters the cm handle query parameters
     * @return collection of cm handles
     */
    @Override
    public Set<String> queryCmHandleIds(
            final CmHandleQueryServiceParameters cmHandleQueryServiceParameters) {

        if (cmHandleQueryServiceParameters.getCmHandleQueryParameters().isEmpty()) {
            return getAllCmHandleIds();
        }

        final Set<String> cmHandleIds = new HashSet<>();

        final boolean firstQuery = publicPropertyQuery(cmHandleQueryServiceParameters, cmHandleIds, new HashMap<>());
        moduleNameIdQuery(cmHandleQueryServiceParameters, cmHandleIds, firstQuery);

        return cmHandleIds;
    }

    private boolean publicPropertyQuery(final CmHandleQueryServiceParameters cmHandleQueryServiceParameters,
                                     final Collection<String> cmHandleIds,
                                     final Map<String, NcmpServiceCmHandle> cmHandles) {

        boolean firstQuery = true;

        for (final Map.Entry<String, String> entry :
                getPublicPropertyPairs(cmHandleQueryServiceParameters.getCmHandleQueryParameters()).entrySet()) {
            final String cmHandlePath = "//public-properties[@name='" + entry.getKey() + "' and @value='"
                    + entry.getValue() + "']/ancestor::cm-handles";

            final Collection<DataNode> dataNodes = queryDataNodes(cmHandlePath);

            if (firstQuery) {
                firstQuery = false;
                collectDataNodesToNcmpServiceCmHandles(cmHandles, dataNodes, cmHandleIds);
            } else {
                final Collection<String> singleConditionQueryCmHandleIds = new ArrayList<>();
                collectDataNodesToNcmpServiceCmHandles(cmHandles, dataNodes, singleConditionQueryCmHandleIds);
                cmHandleIds.retainAll(singleConditionQueryCmHandleIds);
            }

            if (cmHandleIds.isEmpty()) {
                break;
            }
        }

        return firstQuery;
    }

    private void moduleNameQuery(final CmHandleQueryServiceParameters cmHandleQueryServiceParameters,
                                 final Set<String> cmHandleIds, final Map<String, NcmpServiceCmHandle> cmHandles,
                                 final boolean firstQuery) {
        if (!getModuleNames(cmHandleQueryServiceParameters.getCmHandleQueryParameters()).isEmpty()) {
            final Collection<String> cmHandleIdsByModuleName =
                    queryCmHandleIdsByModuleName(cmHandleQueryServiceParameters);
            if (firstQuery) {
                getAllCmHandles().forEach(ncmpServiceCmHandle -> {
                    if (cmHandleIdsByModuleName.contains(ncmpServiceCmHandle.getCmHandleId())) {
                        cmHandleIds.add(ncmpServiceCmHandle.getCmHandleId());
                        cmHandles.put(ncmpServiceCmHandle.getCmHandleId(), ncmpServiceCmHandle);
                    }
                });
            } else {
                cmHandleIds.retainAll(cmHandleIdsByModuleName);
            }
        }
    }

    private void moduleNameIdQuery(final CmHandleQueryServiceParameters cmHandleQueryServiceParameters,
                                   final Set<String> cmHandleIds, final boolean firstQuery) {
        if (!getModuleNames(cmHandleQueryServiceParameters.getCmHandleQueryParameters()).isEmpty()) {
            if (firstQuery) {
                cmHandleIds.addAll(queryCmHandleIdsByModuleName(cmHandleQueryServiceParameters));
            } else {
                cmHandleIds.retainAll(queryCmHandleIdsByModuleName(cmHandleQueryServiceParameters));
            }
        }
    }

    private Collection<String> queryCmHandleIdsByModuleName(
            final CmHandleQueryServiceParameters cmHandleQueryServiceParameters) {
        if (!getModuleNames(cmHandleQueryServiceParameters.getCmHandleQueryParameters()).isEmpty()) {
            return cpsAdminPersistenceService.queryAnchors("NFP-Operational",
                            getModuleNames(cmHandleQueryServiceParameters.getCmHandleQueryParameters()))
                    .parallelStream().map(Anchor::getName).collect(Collectors.toList());
        }
        return Collections.emptySet();
    }

    private void collectDataNodesToNcmpServiceCmHandles(final Map<String, NcmpServiceCmHandle> cmHandles,
                                                        final Collection<DataNode> dataNodes,
                                                        final Collection<String> cmHandleIds) {
        dataNodes.forEach(dataNode -> {
            final NcmpServiceCmHandle ncmpServiceCmHandle = createNcmpServiceCmHandle(dataNode);
            cmHandleIds.add(ncmpServiceCmHandle.getCmHandleId());
            cmHandles.put(ncmpServiceCmHandle.getCmHandleId(), ncmpServiceCmHandle);
        });
    }

    private List<Map<String, String>> getConditions(final List<ConditionProperties> conditionProperties,
                                                    final String name) {
        for (final ConditionProperties conditionProperty : conditionProperties) {
            if (conditionProperty.getConditionName().equals(name)) {
                return conditionProperty.getConditionParameters();
            }
        }
        return Collections.emptyList();
    }

    private List<String> getModuleNames(final List<ConditionProperties> conditionProperties) {
        final List<String> result = new ArrayList<>();
        getConditions(conditionProperties, MODULE_QUERY_NAME).parallelStream().forEach(
            conditionProperty -> {
                validateModuleNameConditionProperties(conditionProperty);
                result.add(conditionProperty.get("moduleName"));
            }
        );
        return result;
    }

    private Map<String, String> getPublicPropertyPairs(final List<ConditionProperties> conditionProperties) {
        final Map<String, String> result = new HashMap<>();
        getConditions(conditionProperties, PROPERTY_QUERY_NAME).forEach(result::putAll);
        return result;
    }

    private Set<NcmpServiceCmHandle> getAllCmHandles() {
        return queryDataNodes("/dmi-registry/cm-handles").stream()
                    .map(this::createNcmpServiceCmHandle).collect(Collectors.toSet());
    }

    private Set<String> getAllCmHandleIds() {
        return cpsAdminPersistenceService.getAnchors("NFP-Operational")
                    .parallelStream().map(Anchor::getName).collect(Collectors.toSet());
    }

    private List<DataNode> queryDataNodes(final String cmHandlePath) {
        return cpsDataPersistenceService.queryDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                cmHandlePath, INCLUDE_ALL_DESCENDANTS);
    }

    private NcmpServiceCmHandle createNcmpServiceCmHandle(final DataNode dataNode) {
        return convertYangModelCmHandleToNcmpServiceCmHandle(YangDataConverter
                .convertCmHandleToYangModel(dataNode, dataNode.getLeaves().get("id").toString()));
    }
}
