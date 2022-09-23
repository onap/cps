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
import static org.onap.cps.spi.FetchDescendantsOption.FETCH_DIRECT_CHILDREN_ONLY;
import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS;
import static org.onap.cps.utils.CmHandleQueryRestParametersValidator.validateCpsPathConditionProperties;
import static org.onap.cps.utils.CmHandleQueryRestParametersValidator.validateModuleNameConditionProperties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.cpspath.parser.PathParsingException;
import org.onap.cps.ncmp.api.NetworkCmProxyCmHandlerQueryService;
import org.onap.cps.ncmp.api.impl.utils.YangDataConverter;
import org.onap.cps.ncmp.api.inventory.CmHandleQueries;
import org.onap.cps.ncmp.api.inventory.InventoryPersistence;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.onap.cps.spi.model.Anchor;
import org.onap.cps.spi.model.CmHandleQueryServiceParameters;
import org.onap.cps.spi.model.ConditionProperties;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.utils.ValidQueryProperties;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NetworkCmProxyCmHandlerQueryServiceImpl implements NetworkCmProxyCmHandlerQueryService {

    private static final Map<String, NcmpServiceCmHandle> NO_QUERY_TO_EXECUTE = null;
    private final CmHandleQueries cmHandleQueries;
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

        final Map<String, NcmpServiceCmHandle> combinedQueryResult = executeInventoryQueries(
                cmHandleQueryServiceParameters);

        return new HashSet<>(combineWithModuleNameQuery(cmHandleQueryServiceParameters, combinedQueryResult).values());
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

        final Map<String, NcmpServiceCmHandle> combinedQueryResult = executeInventoryQueries(
                cmHandleQueryServiceParameters);

        final Collection<String> moduleNamesForQuery =
                getModuleNamesForQuery(cmHandleQueryServiceParameters.getCmHandleQueryParameters());
        if (moduleNamesForQuery.isEmpty()) {
            return combinedQueryResult.keySet();
        }
        final Set<String> moduleNameQueryResult = getNamesOfAnchorsWithGivenModules(moduleNamesForQuery);

        if (combinedQueryResult == NO_QUERY_TO_EXECUTE) {
            return moduleNameQueryResult;
        }

        moduleNameQueryResult.retainAll(combinedQueryResult.keySet());
        return moduleNameQueryResult;
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
        if (previousQueryResult == NO_QUERY_TO_EXECUTE) {
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

    private Map<String, NcmpServiceCmHandle> executeInventoryQueries(
            final CmHandleQueryServiceParameters cmHandleQueryServiceParameters) {
        final Map<String, String> cpsPath = getCpsPath(cmHandleQueryServiceParameters.getCmHandleQueryParameters());
        if (!validateCpsPathConditionProperties(cpsPath)) {
            return Collections.emptyMap();
        }
        final Map<String, NcmpServiceCmHandle> cpsPathQueryResult;
        if (cpsPath.isEmpty()) {
            cpsPathQueryResult = NO_QUERY_TO_EXECUTE;
        } else {
            try {
                cpsPathQueryResult = cmHandleQueries.queryCmHandleDataNodesByCpsPath(
                                cpsPath.get("cpsPath"), INCLUDE_ALL_DESCENDANTS)
                        .stream().map(this::createNcmpServiceCmHandle)
                        .collect(Collectors.toMap(NcmpServiceCmHandle::getCmHandleId,
                                Function.identity()));
            } catch (final PathParsingException pathParsingException) {
                throw new DataValidationException(pathParsingException.getMessage(), pathParsingException.getDetails(),
                        pathParsingException);
            }
            if (cpsPathQueryResult.isEmpty()) {
                return Collections.emptyMap();
            }
        }

        final Map<String, String> publicPropertyQueryPairs =
                getPublicPropertyPairs(cmHandleQueryServiceParameters.getCmHandleQueryParameters());
        final Map<String, NcmpServiceCmHandle> propertiesQueryResult = publicPropertyQueryPairs.isEmpty()
                ? NO_QUERY_TO_EXECUTE : cmHandleQueries.queryCmHandlePublicProperties(publicPropertyQueryPairs);

        return cmHandleQueries.combineCmHandleQueries(cpsPathQueryResult, propertiesQueryResult);
    }

    private Set<String> getNamesOfAnchorsWithGivenModules(final Collection<String> moduleNamesForQuery) {
        final Collection<Anchor> anchors = inventoryPersistence.queryAnchors(moduleNamesForQuery);
        return anchors.parallelStream().map(Anchor::getName).collect(Collectors.toSet());
    }

    private Collection<String> getModuleNamesForQuery(final List<ConditionProperties> conditionProperties) {
        final List<String> result = new ArrayList<>();
        getConditions(conditionProperties, ValidQueryProperties.HAS_ALL_MODULES.getQueryProperty())
            .parallelStream().forEach(
                conditionProperty -> {
                    validateModuleNameConditionProperties(conditionProperty);
                    result.add(conditionProperty.get("moduleName"));
                }
            );
        return result;
    }

    private Map<String, String> getCpsPath(final List<ConditionProperties> conditionProperties) {
        final Map<String, String> result = new HashMap<>();
        getConditions(conditionProperties, ValidQueryProperties.WITH_CPS_PATH.getQueryProperty()).forEach(
                result::putAll);
        return result;
    }

    private Map<String, String> getPublicPropertyPairs(final List<ConditionProperties> conditionProperties) {
        final Map<String, String> result = new HashMap<>();
        getConditions(conditionProperties,
                ValidQueryProperties.HAS_ALL_PROPERTIES.getQueryProperty()).forEach(result::putAll);
        return result;
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

    private Set<NcmpServiceCmHandle> getAllCmHandles() {
        return inventoryPersistence.getDataNode("/dmi-registry")
                .getChildDataNodes().stream().map(this::createNcmpServiceCmHandle).collect(Collectors.toSet());
    }

    private Set<String> getAllCmHandleIds() {
        return inventoryPersistence.getDataNode("/dmi-registry", FETCH_DIRECT_CHILDREN_ONLY)
                .getChildDataNodes().stream().map(dataNode -> dataNode.getLeaves().get("id").toString())
                .collect(Collectors.toSet());
    }

    private NcmpServiceCmHandle createNcmpServiceCmHandle(final DataNode dataNode) {
        return convertYangModelCmHandleToNcmpServiceCmHandle(YangDataConverter
                .convertCmHandleToYangModel(dataNode, dataNode.getLeaves().get("id").toString()));
    }
}
