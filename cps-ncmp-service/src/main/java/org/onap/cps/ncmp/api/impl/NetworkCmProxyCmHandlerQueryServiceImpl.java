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

import static org.onap.cps.ncmp.api.impl.utils.YangDataConverter.convertYangModelCmHandleToNcmpServiceCmHandle;
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
import org.onap.cps.ncmp.api.inventory.InventoryPersistence;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
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
    private static final Map<String, NcmpServiceCmHandle> NO_QUERY_EXECUTED = null;
    private final InventoryPersistence inventoryPersistence;

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

        final Map<String, NcmpServiceCmHandle> publicPropertyQueryResult
            = executePublicPropertyQueries(cmHandleQueryServiceParameters);

        final Map<String, NcmpServiceCmHandle> combinedQueryResult =
            combineWithModuleNameQuery(cmHandleQueryServiceParameters, publicPropertyQueryResult);

        return combinedQueryResult == NO_QUERY_EXECUTED
            ? Collections.emptySet() : new HashSet<>(combinedQueryResult.values());
    }

    /**
     * Query and return cm handles that match the given query parameters.
     *
     * @param cmHandleQueryServiceParameters the cm handle query parameters
     * @return collection of cm handle ids
     */
    @Override
    public Set<String> queryCmHandleIds(
        final CmHandleQueryServiceParameters cmHandleQueryServiceParameters) {

        if (cmHandleQueryServiceParameters.getCmHandleQueryParameters().isEmpty()) {
            return getAllCmHandleIds();
        }

        final Map<String, NcmpServiceCmHandle> publicPropertyQueryResult
            = executePublicPropertyQueries(cmHandleQueryServiceParameters);

        final Collection<String> moduleNamesForQuery =
            getModuleNamesForQuery(cmHandleQueryServiceParameters.getCmHandleQueryParameters());
        if (moduleNamesForQuery.isEmpty()) {
            return publicPropertyQueryResult == NO_QUERY_EXECUTED
                ? Collections.emptySet() : publicPropertyQueryResult.keySet();
        }
        final Set<String> moduleNameQueryResult = getNamesOfAnchorsWithGivenModules(moduleNamesForQuery);

        if (publicPropertyQueryResult == NO_QUERY_EXECUTED) {
            return moduleNameQueryResult;
        }

        moduleNameQueryResult.retainAll(publicPropertyQueryResult.keySet());
        return moduleNameQueryResult;
    }

    private Map<String, NcmpServiceCmHandle> executePublicPropertyQueries(
        final CmHandleQueryServiceParameters cmHandleQueryServiceParameters) {
        final Map<String, String> publicPropertyQueryPairs =
            getPublicPropertyPairs(cmHandleQueryServiceParameters.getCmHandleQueryParameters());
        if (publicPropertyQueryPairs.isEmpty()) {
            return NO_QUERY_EXECUTED;
        }
        Map<String, NcmpServiceCmHandle> cmHandleIdToNcmpServiceCmHandles = null;
        for (final Map.Entry<String, String> entry : publicPropertyQueryPairs.entrySet()) {
            final String cpsPath = "//public-properties[@name='" + entry.getKey() + "' and @value='"
                + entry.getValue() + "']/ancestor::cm-handles";

            final Collection<DataNode> dataNodes = inventoryPersistence.queryDataNodes(cpsPath);
            if (cmHandleIdToNcmpServiceCmHandles == NO_QUERY_EXECUTED) {
                cmHandleIdToNcmpServiceCmHandles = collectDataNodesToNcmpServiceCmHandles(dataNodes);
            } else {
                final Collection<String> cmHandleIdsToRetain = dataNodes.parallelStream()
                    .map(dataNode -> dataNode.getLeaves().get("id").toString()).collect(Collectors.toSet());
                cmHandleIdToNcmpServiceCmHandles.keySet().retainAll(cmHandleIdsToRetain);
            }
            if (cmHandleIdToNcmpServiceCmHandles.isEmpty()) {
                break;
            }
        }
        return cmHandleIdToNcmpServiceCmHandles;
    }

    private Map<String, NcmpServiceCmHandle> combineWithModuleNameQuery(
        final CmHandleQueryServiceParameters cmHandleQueryServiceParameters,
        final Map<String, NcmpServiceCmHandle> previousQueryResult) {
        final Collection<String> moduleNamesForQuery =
            getModuleNamesForQuery(cmHandleQueryServiceParameters.getCmHandleQueryParameters());
        if (moduleNamesForQuery.isEmpty()) {
            return previousQueryResult;
        }
        final Collection<String> cmHandleIdsByModuleName = getNamesOfAnchorsWithGivenModules(moduleNamesForQuery);
        if (cmHandleIdsByModuleName.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<String, NcmpServiceCmHandle> queryResult = new HashMap<>(cmHandleIdsByModuleName.size());
        if (previousQueryResult == NO_QUERY_EXECUTED) {
            cmHandleIdsByModuleName.forEach(cmHandleId ->
                    queryResult.put(cmHandleId, createNcmpServiceCmHandle(
                            inventoryPersistence.getDataNode("/dmi-registry/cm-handles[@id='" + cmHandleId + "']")))
            );
            return queryResult;
        }
        previousQueryResult.keySet().retainAll(cmHandleIdsByModuleName);
        queryResult.putAll(previousQueryResult);
        return queryResult;
    }

    private Set<String> getNamesOfAnchorsWithGivenModules(final Collection<String> moduleNamesForQuery) {
        final Collection<Anchor> anchors = inventoryPersistence.queryAnchors(moduleNamesForQuery);
        return anchors.parallelStream().map(Anchor::getName).collect(Collectors.toSet());
    }

    private Map<String, NcmpServiceCmHandle> collectDataNodesToNcmpServiceCmHandles(
        final Collection<DataNode> dataNodes) {
        final Map<String, NcmpServiceCmHandle> cmHandleIdToNcmpServiceCmHandle = new HashMap<>();
        dataNodes.forEach(dataNode -> {
            final NcmpServiceCmHandle ncmpServiceCmHandle = createNcmpServiceCmHandle(dataNode);
            cmHandleIdToNcmpServiceCmHandle.put(ncmpServiceCmHandle.getCmHandleId(), ncmpServiceCmHandle);
        });
        return cmHandleIdToNcmpServiceCmHandle;
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

    private Collection<String> getModuleNamesForQuery(final List<ConditionProperties> conditionProperties) {
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
        return inventoryPersistence.getDataNode("/dmi-registry")
            .getChildDataNodes().stream().map(this::createNcmpServiceCmHandle).collect(Collectors.toSet());
    }

    private Set<String> getAllCmHandleIds() {
        return inventoryPersistence.getAnchors().parallelStream().map(Anchor::getName).collect(Collectors.toSet());
    }

    private NcmpServiceCmHandle createNcmpServiceCmHandle(final DataNode dataNode) {
        return convertYangModelCmHandleToNcmpServiceCmHandle(YangDataConverter
            .convertCmHandleToYangModel(dataNode, dataNode.getLeaves().get("id").toString()));
    }
}
