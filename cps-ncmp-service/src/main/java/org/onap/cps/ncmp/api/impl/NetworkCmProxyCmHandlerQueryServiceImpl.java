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

import static org.onap.cps.ncmp.api.impl.utils.RestQueryParametersValidator.validateCpsPathConditionProperties;
import static org.onap.cps.ncmp.api.impl.utils.RestQueryParametersValidator.validateModuleNameConditionProperties;
import static org.onap.cps.ncmp.api.impl.utils.YangDataConverter.convertYangModelCmHandleToNcmpServiceCmHandle;
import static org.onap.cps.spi.FetchDescendantsOption.FETCH_DIRECT_CHILDREN_ONLY;
import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS;

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
import org.onap.cps.ncmp.api.impl.utils.CmHandleQueryConditions;
import org.onap.cps.ncmp.api.impl.utils.InventoryQueryConditions;
import org.onap.cps.ncmp.api.impl.utils.YangDataConverter;
import org.onap.cps.ncmp.api.inventory.CmHandleQueries;
import org.onap.cps.ncmp.api.inventory.InventoryPersistence;
import org.onap.cps.ncmp.api.inventory.enums.PropertyType;
import org.onap.cps.ncmp.api.models.CmHandleQueryServiceParameters;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.onap.cps.spi.model.ConditionProperties;
import org.onap.cps.spi.model.DataNode;
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

    @Override
    public Set<String> queryCmHandleIdsForInventory(
            final CmHandleQueryServiceParameters cmHandleQueryServiceParameters) {

        if (cmHandleQueryServiceParameters.getCmHandleQueryParameters().isEmpty()) {
            return getAllCmHandleIds();
        }

        final Map<String, NcmpServiceCmHandle> publicPropertiesQueryResult = queryCmHandlesByPublicProperties(
                cmHandleQueryServiceParameters);
        if (publicPropertiesQueryResult != null && publicPropertiesQueryResult.isEmpty()) {
            return Collections.emptySet();
        }

        final Map<String, NcmpServiceCmHandle> privatePropertiesQueryResult = queryCmHandlesByPrivateProperties(
                cmHandleQueryServiceParameters);
        if (privatePropertiesQueryResult != null && privatePropertiesQueryResult.isEmpty()) {
            return Collections.emptySet();
        }

        final Map<String, NcmpServiceCmHandle> dmiPropertiesQueryResult = queryCmHandlesByDmiPlugin(
                cmHandleQueryServiceParameters);
        if (dmiPropertiesQueryResult != null && dmiPropertiesQueryResult.isEmpty()) {
            return Collections.emptySet();
        }

        final Map<String, NcmpServiceCmHandle> combinedResult =
              combineQueryResults(publicPropertiesQueryResult, privatePropertiesQueryResult, dmiPropertiesQueryResult);

        return combinedResult.keySet();
    }

    private Map<String, NcmpServiceCmHandle> queryCmHandlesByDmiPlugin(
            final CmHandleQueryServiceParameters cmHandleQueryServiceParameters) {
        final Map<String, String> dmiPropertyQueryPairs =
                getPropertyPairs(cmHandleQueryServiceParameters.getCmHandleQueryParameters(),
                        InventoryQueryConditions.CM_HANDLE_WITH_DMI_PLUGIN.getName());
        if (dmiPropertyQueryPairs.isEmpty()) {
            return NO_QUERY_TO_EXECUTE;
        }

        final String dmiPluginIdentifierValue = dmiPropertyQueryPairs.get(
                PropertyType.DMI_PLUGIN.getYangContainerName());

        final Set<NcmpServiceCmHandle> cmHandlesByDmiPluginIdentifier = cmHandleQueries
                .getCmHandlesByDmiPluginIdentifier(dmiPluginIdentifierValue);

        return cmHandlesByDmiPluginIdentifier.stream()
                .collect(Collectors.toMap(NcmpServiceCmHandle::getCmHandleId, cmH -> cmH));
    }

    private Map<String, NcmpServiceCmHandle> queryCmHandlesByPrivateProperties(
            final CmHandleQueryServiceParameters cmHandleQueryServiceParameters) {

        final Map<String, String> privatePropertyQueryPairs =
                getPropertyPairs(cmHandleQueryServiceParameters.getCmHandleQueryParameters(),
                        InventoryQueryConditions.HAS_ALL_ADDITIONAL_PROPERTIES.getName());

        return privatePropertyQueryPairs.isEmpty()
                ? NO_QUERY_TO_EXECUTE
                : cmHandleQueries.queryCmHandleAdditionalProperties(privatePropertyQueryPairs);
    }

    private Map<String, NcmpServiceCmHandle> queryCmHandlesByPublicProperties(
            final CmHandleQueryServiceParameters cmHandleQueryServiceParameters) {

        final Map<String, String> publicPropertyQueryPairs =
                getPropertyPairs(cmHandleQueryServiceParameters.getCmHandleQueryParameters(),
                        CmHandleQueryConditions.HAS_ALL_PROPERTIES.getConditionName());

        return publicPropertyQueryPairs.isEmpty()
                ? NO_QUERY_TO_EXECUTE
                : cmHandleQueries.queryCmHandlePublicProperties(publicPropertyQueryPairs);
    }

    private Map<String, NcmpServiceCmHandle> combineQueryResults(
            final Map<String, NcmpServiceCmHandle> publicPropertiesQueryResult,
            final Map<String, NcmpServiceCmHandle> privatePropertiesQueryResult,
            final Map<String, NcmpServiceCmHandle> dmiPropertiesQueryResult) {

        final Map<String, NcmpServiceCmHandle> propertiesCombinedResult = cmHandleQueries
                .combineCmHandleQueries(publicPropertiesQueryResult, privatePropertiesQueryResult);
        return cmHandleQueries
                .combineCmHandleQueries(propertiesCombinedResult, dmiPropertiesQueryResult);
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
                getPropertyPairs(cmHandleQueryServiceParameters.getCmHandleQueryParameters(),
                        CmHandleQueryConditions.HAS_ALL_PROPERTIES.getConditionName());
        final Map<String, NcmpServiceCmHandle> propertiesQueryResult = publicPropertyQueryPairs.isEmpty()
                ? NO_QUERY_TO_EXECUTE : cmHandleQueries.queryCmHandlePublicProperties(publicPropertyQueryPairs);

        return cmHandleQueries.combineCmHandleQueries(cpsPathQueryResult, propertiesQueryResult);
    }

    private Set<String> getNamesOfAnchorsWithGivenModules(final Collection<String> moduleNamesForQuery) {
        return new HashSet<>(inventoryPersistence.queryAnchors(moduleNamesForQuery));
    }

    private Collection<String> getModuleNamesForQuery(final List<ConditionProperties> conditionProperties) {
        final List<String> result = new ArrayList<>();
        getConditions(conditionProperties, CmHandleQueryConditions.HAS_ALL_MODULES.getConditionName())
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
        getConditions(conditionProperties, CmHandleQueryConditions.WITH_CPS_PATH.getConditionName()).forEach(
                result::putAll);
        return result;
    }

    private Map<String, String> getPropertyPairs(final List<ConditionProperties> conditionProperties,
                                                       final String queryProperty) {
        final Map<String, String> result = new HashMap<>();
        getConditions(conditionProperties, queryProperty).forEach(result::putAll);
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
