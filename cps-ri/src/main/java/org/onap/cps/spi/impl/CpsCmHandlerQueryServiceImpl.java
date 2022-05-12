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

package org.onap.cps.spi.impl;

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS;
import static org.onap.cps.utils.CmHandleQueryRestParametersValidator.validateModuleNameConditionProperties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.spi.CpsAdminPersistenceService;
import org.onap.cps.spi.CpsCmHandlerQueryService;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.model.Anchor;
import org.onap.cps.spi.model.CmHandleQueryParameters;
import org.onap.cps.spi.model.ConditionProperties;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.DataNodeIdentifier;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CpsCmHandlerQueryServiceImpl implements CpsCmHandlerQueryService {

    private static final String PROPERTY_QUERY_NAME = "hasAllProperties";
    private static final String MODULE_QUERY_NAME = "hasAllModules";
    private final CpsDataPersistenceService cpsDataPersistenceService;
    private final CpsAdminPersistenceService cpsAdminPersistenceService;

    private final JsonObjectMapper jsonObjectMapper;

    /**
     * Query and return cm handles that match the given query parameters.
     *
     * @param cmHandleQueryParameters the cm handle query parameters
     * @return collection of cm handles
     */
    @Override
    public Collection<DataNode> queryCmHandles(final CmHandleQueryParameters cmHandleQueryParameters) {

        if (cmHandleQueryParameters.getCmHandleQueryParameters().isEmpty()) {
            return getAllCmHandles();
        }

        final Collection<DataNodeIdentifier> amalgamatedQueryResultIdentifiers = new ArrayList<>();
        final Map<DataNodeIdentifier, DataNode> amalgamatedQueryResults = new HashMap<>();

        final boolean firstQuery = moduleNameQuery(cmHandleQueryParameters,
                amalgamatedQueryResultIdentifiers, amalgamatedQueryResults);

        publicPropertyQuery(cmHandleQueryParameters, amalgamatedQueryResultIdentifiers,
                amalgamatedQueryResults, firstQuery);

        final Collection<DataNode> filteredDataNodes = new ArrayList<>();
        amalgamatedQueryResultIdentifiers.forEach(amalgamatedQueryResultIdentifier ->
            filteredDataNodes.add(amalgamatedQueryResults.get(amalgamatedQueryResultIdentifier))
        );

        return filteredDataNodes;
    }

    private void publicPropertyQuery(final CmHandleQueryParameters cmHandleQueryParameters,
                                     final Collection<DataNodeIdentifier> amalgamatedQueryResultIdentifiers,
                                     final Map<DataNodeIdentifier, DataNode> amalgamatedQueryResults,
                                     boolean firstQuery) {
        for (final Map.Entry<String, String> entry :
                getPublicPropertyPairs(cmHandleQueryParameters.getCmHandleQueryParameters()).entrySet()) {
            final String cmHandlePath = "//public-properties[@name='" + entry.getKey() + "' " + "and @value='"
                    + entry.getValue() + "']" + "/ancestor::cm-handles";

            final Collection<DataNode> dataNodes = getDataNodes(cmHandlePath);

            if (firstQuery) {
                firstQuery = false;
                dataNodes.forEach(dataNode -> {
                    final DataNodeIdentifier dataNodeIdentifier =
                            jsonObjectMapper.convertToValueType(dataNode, DataNodeIdentifier.class);
                    amalgamatedQueryResultIdentifiers.add(dataNodeIdentifier);
                    amalgamatedQueryResults.put(dataNodeIdentifier, dataNode);
                });
            } else {
                final Collection<DataNodeIdentifier> singleConditionQueryDataNodeIdentifiers = new ArrayList<>();
                dataNodes.forEach(dataNode -> {
                    final DataNodeIdentifier dataNodeIdentifier =
                            jsonObjectMapper.convertToValueType(dataNode, DataNodeIdentifier.class);
                    singleConditionQueryDataNodeIdentifiers.add(dataNodeIdentifier);
                    amalgamatedQueryResults.put(dataNodeIdentifier, dataNode);
                });
                amalgamatedQueryResultIdentifiers.retainAll(singleConditionQueryDataNodeIdentifiers);
            }

            if (amalgamatedQueryResultIdentifiers.isEmpty()) {
                break;
            }
        }
    }

    private boolean moduleNameQuery(final CmHandleQueryParameters cmHandleQueryParameters,
                                    final Collection<DataNodeIdentifier> amalgamatedQueryResultIdentifiers,
                                    final Map<DataNodeIdentifier, DataNode> amalgamatedQueryResults) {
        boolean firstQuery = true;
        if (!getModuleNames(cmHandleQueryParameters.getCmHandleQueryParameters()).isEmpty()) {
            final Collection<Anchor> anchors = cpsAdminPersistenceService.queryAnchors("NFP-Operational",
                    getModuleNames(cmHandleQueryParameters.getCmHandleQueryParameters()));
            anchors.forEach(anchor -> {
                final List<DataNode> dataNodes = getDataNodes("//cm-handles[@id='" + anchor.getName() + "']");
                dataNodes.parallelStream().forEach(dataNode -> {
                    final DataNodeIdentifier dataNodeIdentifier =
                            jsonObjectMapper.convertToValueType(dataNode, DataNodeIdentifier.class);
                    amalgamatedQueryResultIdentifiers.add(dataNodeIdentifier);
                    amalgamatedQueryResults.put(dataNodeIdentifier, dataNode);
                });
            });
            firstQuery = false;
        }
        return firstQuery;
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

    private Collection<DataNode> getAllCmHandles() {
        return getDataNodes("//public-properties/ancestor::cm-handles");
    }

    private List<DataNode> getDataNodes(final String cmHandlePath) {
        return cpsDataPersistenceService.queryDataNodes(
                "NCMP-Admin", "ncmp-dmi-registry", cmHandlePath, INCLUDE_ALL_DESCENDANTS);
    }
}
