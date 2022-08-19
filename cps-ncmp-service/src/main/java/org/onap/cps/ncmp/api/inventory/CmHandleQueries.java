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

package org.onap.cps.ncmp.api.inventory;

import static org.onap.cps.ncmp.api.impl.utils.YangDataConverter.convertYangModelCmHandleToNcmpServiceCmHandle;
import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS;
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS;

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
import org.onap.cps.ncmp.api.impl.utils.YangDataConverter;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CmHandleQueries {

    private static final String NCMP_DATASPACE_NAME = "NCMP-Admin";
    private static final String NCMP_DMI_REGISTRY_ANCHOR = "ncmp-dmi-registry";

    private final CpsDataPersistenceService cpsDataPersistenceService;
    private static final Map<String, NcmpServiceCmHandle> NO_QUERY_TO_EXECUTE = null;
    private static final String ANCESTOR_CM_HANDLES = "/ancestor::cm-handles";


    /**
     * Query CmHandles based on PublicProperties.
     *
     * @param publicPropertyQueryPairs public properties for query
     * @return CmHandles which have these public properties
     */
    public Map<String, NcmpServiceCmHandle> queryCmHandlePublicProperties(
            final Map<String, String> publicPropertyQueryPairs) {
        if (publicPropertyQueryPairs.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, NcmpServiceCmHandle> cmHandleIdToNcmpServiceCmHandles = null;
        for (final Map.Entry<String, String> publicPropertyQueryPair : publicPropertyQueryPairs.entrySet()) {
            final String cpsPath = "//public-properties[@name=\"" + publicPropertyQueryPair.getKey()
                    + "\" and @value=\"" + publicPropertyQueryPair.getValue() + "\"]";

            final Collection<DataNode> dataNodes = queryCmHandleDataNodesByCpsPath(cpsPath, INCLUDE_ALL_DESCENDANTS);
            if (cmHandleIdToNcmpServiceCmHandles == null) {
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

    /**
     * Combine Maps of CmHandles.
     *
     * @param firstQuery first CmHandles Map
     * @param secondQuery second CmHandles Map
     * @return combined Map of CmHandles
     */
    public Map<String, NcmpServiceCmHandle> combineCmHandleQueries(
            final Map<String, NcmpServiceCmHandle> firstQuery,
            final Map<String, NcmpServiceCmHandle> secondQuery) {
        if (firstQuery == NO_QUERY_TO_EXECUTE && secondQuery == NO_QUERY_TO_EXECUTE) {
            return NO_QUERY_TO_EXECUTE;
        } else if (firstQuery == NO_QUERY_TO_EXECUTE) {
            return secondQuery;
        } else if (secondQuery == NO_QUERY_TO_EXECUTE) {
            return firstQuery;
        } else {
            firstQuery.keySet().retainAll(secondQuery.keySet());
            return firstQuery;
        }
    }

    /**
     * Method which returns cm handles by the cm handles state.
     *
     * @param cmHandleState cm handle state
     * @return a list of cm handles
     */
    public List<DataNode> queryCmHandlesByState(final CmHandleState cmHandleState) {
        return queryCmHandleDataNodesByCpsPath("//state[@cm-handle-state=\"" + cmHandleState + "\"]",
            INCLUDE_ALL_DESCENDANTS);
    }

    /**
     * Method to return data nodes representing the cm handles.
     *
     * @param cpsPath cps path for which the cmHandle is requested
     * @return a list of data nodes representing the cm handles.
     */
    public List<DataNode> queryCmHandleDataNodesByCpsPath(final String cpsPath,
                                                          final FetchDescendantsOption fetchDescendantsOption) {
        return cpsDataPersistenceService.queryDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
            cpsPath + ANCESTOR_CM_HANDLES, fetchDescendantsOption);
    }

    /**
     * Method to check the state of a cm handle with given id.
     *
     * @param cmHandleId cm handle id
     * @param requiredCmHandleState the required state of the cm handle
     * @return a boolean, true if the state is equal to the required state
     */
    public boolean cmHandleHasState(final String cmHandleId, final CmHandleState requiredCmHandleState) {
        final DataNode stateDataNode = getCmHandleState(cmHandleId);
        final String cmHandleStateAsString = (String) stateDataNode.getLeaves().get("cm-handle-state");
        return CmHandleState.valueOf(cmHandleStateAsString).equals(requiredCmHandleState);
    }

    /**
     * Method which returns cm handles by the operational sync state of cm handle.
     * @param dataStoreSyncState sync state
     * @return a list of cm handles
     */
    public List<DataNode> queryCmHandlesByOperationalSyncState(final DataStoreSyncState dataStoreSyncState) {
        return queryCmHandleDataNodesByCpsPath("//state/datastores" + "/operational[@sync-state=\""
                + dataStoreSyncState + "\"]", FetchDescendantsOption.OMIT_DESCENDANTS);
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

    private NcmpServiceCmHandle createNcmpServiceCmHandle(final DataNode dataNode) {
        return convertYangModelCmHandleToNcmpServiceCmHandle(YangDataConverter
                .convertCmHandleToYangModel(dataNode, dataNode.getLeaves().get("id").toString()));
    }

    /**
     * Get all cm handle IDs by DMI plugin identifier.
     *
     * @param dmiPluginIdentifier DMI plugin identifier
     * @return collection of cm handles
     */
    public Set<NcmpServiceCmHandle> getCmHandlesByDmiPluginIdentifier(final String dmiPluginIdentifier) {
        final List<DataNode> dataNodes = new ArrayList<>();
        final Set<NcmpServiceCmHandle> ncmpServiceCmHandles = new HashSet<>();
        for (final DmiProperty dmiProperty : DmiProperty.values()) {
            dataNodes.addAll(getCmHandlesByDmiPluginIdentifierAndDmiProperty(
                    dmiPluginIdentifier,
                    dmiProperty.getDmiPropertyKey()));
        }
        final Set<String> cmHandleIds = getCmHandleIdsByDataNodes(dataNodes);
        cmHandleIds.forEach(cmHandleId -> {
            final NcmpServiceCmHandle ncmpServiceCmHandle = new NcmpServiceCmHandle();
            ncmpServiceCmHandle.setCmHandleId(cmHandleId);
            ncmpServiceCmHandles.add(ncmpServiceCmHandle);
        });
        return ncmpServiceCmHandles;
    }

    private List<DataNode> getCmHandlesByDmiPluginIdentifierAndDmiProperty(final String dmiPluginIdentifier,
                                                             final String dmiProperty) {
        return cpsDataPersistenceService.queryDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                "/dmi-registry/cm-handles[@" + dmiProperty + "='" + dmiPluginIdentifier + "']",
                OMIT_DESCENDANTS);
    }

    private Set<String> getCmHandleIdsByDataNodes(final List<DataNode> dataNodes) {
        final Set<String> cmHandleIds = new HashSet<>();
        dataNodes.forEach(item -> {
            cmHandleIds.add((item.getLeaves().get("id").toString()));
        });
        return cmHandleIds;
    }

    private DataNode getCmHandleState(final String cmHandleId) {
        final String xpath = "/dmi-registry/cm-handles[@id='" + cmHandleId + "']/state";
        return cpsDataPersistenceService.getDataNode(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                xpath, OMIT_DESCENDANTS);
    }
}


