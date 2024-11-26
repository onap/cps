/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2024 Nordix Foundation
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

package org.onap.cps.ncmp.impl.inventory;

import static org.onap.cps.api.parameters.FetchDescendantsOption.DIRECT_CHILDREN_ONLY;
import static org.onap.cps.api.parameters.FetchDescendantsOption.OMIT_DESCENDANTS;
import static org.onap.cps.ncmp.impl.inventory.CmHandleQueryParametersValidator.validateCpsPathConditionProperties;
import static org.onap.cps.ncmp.impl.inventory.CmHandleQueryParametersValidator.validateModuleNameConditionProperties;
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DMI_REGISTRY_PARENT;
import static org.onap.cps.ncmp.impl.inventory.models.CmHandleQueryConditions.HAS_ALL_MODULES;
import static org.onap.cps.ncmp.impl.inventory.models.CmHandleQueryConditions.HAS_ALL_PROPERTIES;
import static org.onap.cps.ncmp.impl.inventory.models.CmHandleQueryConditions.WITH_CPS_PATH;
import static org.onap.cps.ncmp.impl.inventory.models.CmHandleQueryConditions.WITH_TRUST_LEVEL;
import static org.onap.cps.ncmp.impl.utils.YangDataConverter.toNcmpServiceCmHandle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.exceptions.DataValidationException;
import org.onap.cps.api.model.ConditionProperties;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.cpspath.parser.PathParsingException;
import org.onap.cps.ncmp.api.inventory.models.CmHandleQueryServiceParameters;
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle;
import org.onap.cps.ncmp.impl.inventory.models.InventoryQueryConditions;
import org.onap.cps.ncmp.impl.inventory.models.PropertyType;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.utils.YangDataConverter;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParameterizedCmHandleQueryServiceImpl implements ParameterizedCmHandleQueryService {

    private static final Collection<String> NO_QUERY_TO_EXECUTE = null;
    private final CmHandleQueryService cmHandleQueryService;
    private final InventoryPersistence inventoryPersistence;

    @Override
    public Collection<String> queryCmHandleReferenceIds(
        final CmHandleQueryServiceParameters cmHandleQueryServiceParameters,
        final boolean outputAlternateId) {
        return executeQueries(cmHandleQueryServiceParameters, outputAlternateId,
            this::executeCpsPathQuery,
            this::queryCmHandlesByPublicProperties,
            this::executeModuleNameQuery,
            this::queryCmHandlesByTrustLevel);
    }

    @Override
    public Collection<String> queryCmHandleIdsForInventory(
            final CmHandleQueryServiceParameters cmHandleQueryServiceParameters,
            final boolean outputAlternateId) {
        return executeQueries(cmHandleQueryServiceParameters, outputAlternateId,
                this::executeCpsPathQuery,
                this::queryCmHandlesByPublicProperties,
                this::queryCmHandlesByPrivateProperties,
                this::queryCmHandlesByDmiPlugin);
    }

    @Override
    public Collection<NcmpServiceCmHandle> queryCmHandles(
        final CmHandleQueryServiceParameters cmHandleQueryServiceParameters) {

        if (cmHandleQueryServiceParameters.getCmHandleQueryParameters().isEmpty()) {
            return getAllCmHandles();
        }

        final Collection<String> cmHandleIds = queryCmHandleReferenceIds(cmHandleQueryServiceParameters, false);

        return getNcmpServiceCmHandles(cmHandleIds);
    }

    @Override
    public Collection<NcmpServiceCmHandle> getAllCmHandles() {
        final DataNode dataNode = inventoryPersistence.getDataNode(NCMP_DMI_REGISTRY_PARENT).iterator().next();
        return dataNode.getChildDataNodes().stream().map(this::createNcmpServiceCmHandle).collect(Collectors.toSet());
    }

    private Collection<String> queryCmHandlesByDmiPlugin(
            final CmHandleQueryServiceParameters cmHandleQueryServiceParameters, final boolean outputAlternateId) {
        final Map<String, String> dmiPropertyQueryPairs =
                getPropertyPairs(cmHandleQueryServiceParameters.getCmHandleQueryParameters(),
                        InventoryQueryConditions.CM_HANDLE_WITH_DMI_PLUGIN.getName());
        if (dmiPropertyQueryPairs.isEmpty()) {
            return NO_QUERY_TO_EXECUTE;
        }

        final String dmiPluginIdentifierValue = dmiPropertyQueryPairs
            .get(PropertyType.DMI_PLUGIN.getYangContainerName());

        if (outputAlternateId) {
            return
                cmHandleQueryService.getCmHandleReferencesMapByDmiPluginIdentifier(dmiPluginIdentifierValue).values();
        } else {
            return cmHandleQueryService.getCmHandleReferencesByDmiPluginIdentifier(dmiPluginIdentifierValue,
                outputAlternateId);
        }
    }

    private Collection<String> queryCmHandlesByPrivateProperties(
            final CmHandleQueryServiceParameters cmHandleQueryServiceParameters, final boolean outputAlternateId) {

        final Map<String, String> privatePropertyQueryPairs =
                getPropertyPairs(cmHandleQueryServiceParameters.getCmHandleQueryParameters(),
                        InventoryQueryConditions.HAS_ALL_ADDITIONAL_PROPERTIES.getName());

        if (privatePropertyQueryPairs.isEmpty()) {
            return NO_QUERY_TO_EXECUTE;
        }
        return cmHandleQueryService.queryCmHandleAdditionalProperties(privatePropertyQueryPairs, outputAlternateId);
    }

    private Collection<String> queryCmHandlesByPublicProperties(
            final CmHandleQueryServiceParameters cmHandleQueryServiceParameters, final boolean outputAlternateId) {

        final Map<String, String> publicPropertyQueryPairs =
                getPropertyPairs(cmHandleQueryServiceParameters.getCmHandleQueryParameters(),
                        HAS_ALL_PROPERTIES.getConditionName());

        if (publicPropertyQueryPairs.isEmpty()) {
            return NO_QUERY_TO_EXECUTE;
        }
        return cmHandleQueryService.queryCmHandlePublicProperties(publicPropertyQueryPairs, outputAlternateId);
    }

    private Collection<String> queryCmHandlesByTrustLevel(final CmHandleQueryServiceParameters
                                                                  cmHandleQueryServiceParameters,
                                                          final boolean outputAlternateId) {

        final Map<String, String> trustLevelPropertyQueryPairs =
                getPropertyPairs(cmHandleQueryServiceParameters.getCmHandleQueryParameters(),
                        WITH_TRUST_LEVEL.getConditionName());

        if (trustLevelPropertyQueryPairs.isEmpty()) {
            return NO_QUERY_TO_EXECUTE;
        }
        return cmHandleQueryService.queryCmHandlesByTrustLevel(trustLevelPropertyQueryPairs, outputAlternateId);
    }

    private Collection<String> executeModuleNameQuery(
            final CmHandleQueryServiceParameters cmHandleQueryServiceParameters, final boolean outputAlternateId) {
        final Collection<String> moduleNamesForQuery =
                getModuleNamesForQuery(cmHandleQueryServiceParameters.getCmHandleQueryParameters());
        if (moduleNamesForQuery.isEmpty()) {
            return NO_QUERY_TO_EXECUTE;
        }
        return inventoryPersistence.getCmHandleReferencesWithGivenModules(moduleNamesForQuery, outputAlternateId);
    }

    private Collection<String> executeCpsPathQuery(
            final CmHandleQueryServiceParameters cmHandleQueryServiceParameters, final boolean outputAlternateId) {
        final Map<String, String> cpsPathCondition
            = getCpsPathCondition(cmHandleQueryServiceParameters.getCmHandleQueryParameters());
        if (!validateCpsPathConditionProperties(cpsPathCondition)) {
            return Collections.emptySet();
        }
        final Collection<String> cpsPathQueryResult;
        if (cpsPathCondition.isEmpty()) {
            return NO_QUERY_TO_EXECUTE;
        }
        try {
            cpsPathQueryResult = collectCmHandleReferencesFromDataNodes(
                cmHandleQueryService.queryCmHandleAncestorsByCpsPath(cpsPathCondition.get("cpsPath"), OMIT_DESCENDANTS),
                outputAlternateId);
        } catch (final PathParsingException pathParsingException) {
            throw new DataValidationException(pathParsingException.getMessage(), pathParsingException.getDetails(),
                    pathParsingException);
        }
        return cpsPathQueryResult;
    }

    private Collection<String> getModuleNamesForQuery(final List<ConditionProperties> conditionProperties) {
        final List<String> result = new ArrayList<>();
        getConditions(conditionProperties, HAS_ALL_MODULES.getConditionName()).forEach(
                conditionProperty -> {
                    validateModuleNameConditionProperties(conditionProperty);
                    result.add(conditionProperty.get("moduleName"));
                });
        return result;
    }

    private Map<String, String> getCpsPathCondition(final List<ConditionProperties> conditionProperties) {
        final Map<String, String> result = new HashMap<>();
        getConditions(conditionProperties, WITH_CPS_PATH.getConditionName()).forEach(result::putAll);
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

    private Collection<String> getAllCmHandleReferences(final boolean outputAlternateId) {
        final DataNode dataNode = inventoryPersistence.getDataNode(NCMP_DMI_REGISTRY_PARENT, DIRECT_CHILDREN_ONLY)
            .iterator().next();
        return collectCmHandleReferencesFromDataNodes(dataNode.getChildDataNodes(), outputAlternateId);
    }

    private Collection<NcmpServiceCmHandle> getNcmpServiceCmHandles(final Collection<String> cmHandleIds) {
        final Collection<YangModelCmHandle> yangModelcmHandles
            = inventoryPersistence.getYangModelCmHandles(cmHandleIds);

        final Collection<NcmpServiceCmHandle> ncmpServiceCmHandles = new ArrayList<>(yangModelcmHandles.size());

        yangModelcmHandles.forEach(yangModelcmHandle ->
            ncmpServiceCmHandles.add(YangDataConverter.toNcmpServiceCmHandle(yangModelcmHandle))
        );
        return ncmpServiceCmHandles;
    }

    private NcmpServiceCmHandle createNcmpServiceCmHandle(final DataNode dataNode) {
        return toNcmpServiceCmHandle(YangDataConverter.toYangModelCmHandle(dataNode));
    }

    private Collection<String> executeQueries(final CmHandleQueryServiceParameters cmHandleQueryServiceParameters,
                                              final boolean outputAlternateId,
                                              final BiFunction<CmHandleQueryServiceParameters, Boolean,
                                                  Collection<String>>... queryFunctions) {
        if (cmHandleQueryServiceParameters.getCmHandleQueryParameters().isEmpty()) {
            return getAllCmHandleReferences(outputAlternateId);
        }
        Collection<String> combinedQueryResult = NO_QUERY_TO_EXECUTE;
        for (final BiFunction<CmHandleQueryServiceParameters, Boolean,
            Collection<String>> queryFunction : queryFunctions) {
            final Collection<String> queryResult = queryFunction.apply(cmHandleQueryServiceParameters,
                outputAlternateId);
            if (noEntriesFoundCanStopQuerying(queryResult)) {
                return Collections.emptySet();
            }
            combinedQueryResult = combineCmHandleQueryResults(combinedQueryResult, queryResult);
        }
        return combinedQueryResult;
    }

    private boolean noEntriesFoundCanStopQuerying(final Collection<String> queryResult) {
        return queryResult != NO_QUERY_TO_EXECUTE && queryResult.isEmpty();
    }

    private Collection<String> combineCmHandleQueryResults(final Collection<String> firstQuery,
                                                           final Collection<String> secondQuery) {
        if (firstQuery == NO_QUERY_TO_EXECUTE && secondQuery == NO_QUERY_TO_EXECUTE) {
            return NO_QUERY_TO_EXECUTE;
        } else if (firstQuery == NO_QUERY_TO_EXECUTE) {
            return secondQuery;
        } else if (secondQuery == NO_QUERY_TO_EXECUTE) {
            return firstQuery;
        } else {
            firstQuery.retainAll(secondQuery);
            return firstQuery;
        }
    }

    private Collection<String> collectCmHandleReferencesFromDataNodes(final Collection<DataNode> dataNodes,
                                                               final boolean outputAlternateId) {
        if (outputAlternateId) {
            return dataNodes.stream().map(dataNode ->
                (String) dataNode.getLeaves().get("alternate-id")).collect(Collectors.toSet());
        } else {
            return dataNodes.stream().map(dataNode ->
                (String) dataNode.getLeaves().get("id")).collect(Collectors.toSet());
        }
    }
}
