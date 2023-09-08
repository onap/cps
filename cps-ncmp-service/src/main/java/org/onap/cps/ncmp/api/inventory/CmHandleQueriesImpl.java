/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation
 *  Modifications Copyright (C) 2023 TechMahindra Ltd.
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

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS;
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS;

import com.google.common.collect.Sets;
import com.hazelcast.collection.ISet;
import com.hazelcast.map.IMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.impl.trustlevel.TrustLevel;
import org.onap.cps.ncmp.api.impl.utils.DmiServiceNameOrganizer;
import org.onap.cps.ncmp.api.impl.utils.YangDataConverter;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.inventory.enums.PropertyType;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CmHandleQueriesImpl implements CmHandleQueries {

    private static final String NCMP_DATASPACE_NAME = "NCMP-Admin";
    private static final String NCMP_DMI_REGISTRY_ANCHOR = "ncmp-dmi-registry";
    private static final String DESCENDANT_PATH = "//";
    private static final String ANCESTOR_CM_HANDLES = "/ancestor::cm-handles";

    private final CpsDataPersistenceService cpsDataPersistenceService;
    private final InventoryPersistence inventoryPersistence;
    private final ISet<String> untrustworthyCmHandlesSet;
    private final IMap<String, TrustLevel> trustLevelPerDmiPlugin;

    @Override
    public Collection<String> queryCmHandleAdditionalProperties(final Map<String, String> privatePropertyQueryPairs) {
        return queryCmHandleAnyProperties(privatePropertyQueryPairs, PropertyType.ADDITIONAL);
    }

    @Override
    public Collection<String> queryCmHandlePublicProperties(final Map<String, String> publicPropertyQueryPairs) {
        return queryCmHandleAnyProperties(publicPropertyQueryPairs, PropertyType.PUBLIC);
    }

    @Override
    public Collection<String> queryCmHandlesByTrustLevel(final Map<String, String> trustLevelPropertyQueryPairs) {
        return getCmHandlesByTrustLevel(trustLevelPropertyQueryPairs);
    }

    @Override
    public List<DataNode> queryCmHandlesByState(final CmHandleState cmHandleState) {
        return queryCmHandleDataNodesByCpsPath("//state[@cm-handle-state=\"" + cmHandleState + "\"]",
            INCLUDE_ALL_DESCENDANTS);
    }

    @Override
    public List<DataNode> queryCmHandleDataNodesByCpsPath(final String cpsPath,
            final FetchDescendantsOption fetchDescendantsOption) {
        return cpsDataPersistenceService.queryDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
            cpsPath + ANCESTOR_CM_HANDLES, fetchDescendantsOption);
    }

    @Override
    public boolean cmHandleHasState(final String cmHandleId, final CmHandleState requiredCmHandleState) {
        final DataNode stateDataNode = getCmHandleState(cmHandleId);
        final String cmHandleStateAsString = (String) stateDataNode.getLeaves().get("cm-handle-state");
        return CmHandleState.valueOf(cmHandleStateAsString).equals(requiredCmHandleState);
    }

    @Override
    public List<DataNode> queryCmHandlesByOperationalSyncState(final DataStoreSyncState dataStoreSyncState) {
        return queryCmHandleDataNodesByCpsPath("//state/datastores" + "/operational[@sync-state=\""
                + dataStoreSyncState + "\"]", FetchDescendantsOption.OMIT_DESCENDANTS);
    }

    @Override
    public Collection<String> getCmHandleIdsByDmiPluginIdentifier(final String dmiPluginIdentifier) {
        final Collection<String> cmHandleIds = new HashSet<>();
        for (final ModelledDmiServiceLeaves modelledDmiServiceLeaf : ModelledDmiServiceLeaves.values()) {
            for (final DataNode cmHandleAsDataNode: getCmHandlesByDmiPluginIdentifierAndDmiProperty(
                    dmiPluginIdentifier,
                    modelledDmiServiceLeaf.getLeafName())) {
                cmHandleIds.add(cmHandleAsDataNode.getLeaves().get("id").toString());
            }
        }
        return cmHandleIds;
    }

    private Collection<String> collectCmHandleIdsFromDataNodes(final Collection<DataNode> dataNodes) {
        return dataNodes.stream().map(dataNode -> (String) dataNode.getLeaves().get("id")).collect(Collectors.toSet());
    }

    private Collection<String> queryCmHandleAnyProperties(
        final Map<String, String> propertyQueryPairs,
        final PropertyType propertyType) {
        if (propertyQueryPairs.isEmpty()) {
            return Collections.emptySet();
        }
        Collection<String> cmHandleIds = null;
        for (final Map.Entry<String, String> publicPropertyQueryPair : propertyQueryPairs.entrySet()) {
            final String cpsPath = DESCENDANT_PATH + propertyType.getYangContainerName() + "[@name=\""
                + publicPropertyQueryPair.getKey()
                + "\" and @value=\"" + publicPropertyQueryPair.getValue() + "\"]";

            final Collection<DataNode> dataNodes = queryCmHandleDataNodesByCpsPath(cpsPath, OMIT_DESCENDANTS);
            if (cmHandleIds == null) {
                cmHandleIds = collectCmHandleIdsFromDataNodes(dataNodes);
            } else {
                final Collection<String> cmHandleIdsToRetain = collectCmHandleIdsFromDataNodes(dataNodes);
                cmHandleIds.retainAll(cmHandleIdsToRetain);
            }
            if (cmHandleIds.isEmpty()) {
                break;
            }
        }
        return cmHandleIds;
    }

    private Collection<String> getCmHandlesByTrustLevel(final Map<String, String> trustLevelPropertyQueryPairs) {
        final String targetTrustLevel = trustLevelPropertyQueryPairs.get("trustLevel");

        if (targetTrustLevel.equals("NONE")) {
            if (!untrustworthyCmHandlesSet.isEmpty()) {
                return untrustworthyCmHandlesSet.stream().collect(Collectors.toSet());
            }
        } else if (targetTrustLevel.equals("COMPLETE")) {
            final Collection<String> trustedCmHandles = Sets.newHashSet();
            final Map<String, Map<String, Map<String, String>>> dmiPropertiesPerCmHandleIdPerServiceName
                    = getDmiPropertiesPerCmHandleIdPerServiceName();
            final Collection<String> allExistingDmis = new HashSet<>(dmiPropertiesPerCmHandleIdPerServiceName.keySet());

            allExistingDmis.forEach(dmi -> {
                final TrustLevel trustLevel = trustLevelPerDmiPlugin.get(dmi);
                if (trustLevel != null && trustLevel.equals(TrustLevel.COMPLETE)) {
                    final Set<String> cmHandleIds =
                            dmiPropertiesPerCmHandleIdPerServiceName.get(dmi).entrySet().stream()
                                    .map(Map.Entry::getKey).collect(Collectors.toSet());
                    trustedCmHandles.addAll(cmHandleIds);
                }
            });
            return trustedCmHandles;
        }
        return Collections.emptySet();
    }

    private Map<String, Map<String, Map<String, String>>> getDmiPropertiesPerCmHandleIdPerServiceName() {
        return DmiServiceNameOrganizer.getDmiPropertiesPerCmHandleIdPerServiceName(getAllYangModelCmHandles());
    }

    private List<DataNode> getCmHandlesByDmiPluginIdentifierAndDmiProperty(final String dmiPluginIdentifier,
                                                             final String dmiProperty) {
        return cpsDataPersistenceService.queryDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                "/dmi-registry/cm-handles[@" + dmiProperty + "='" + dmiPluginIdentifier + "']",
                OMIT_DESCENDANTS);
    }

    private DataNode getCmHandleState(final String cmHandleId) {
        final String xpath = "/dmi-registry/cm-handles[@id='" + cmHandleId + "']/state";
        return cpsDataPersistenceService.getDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                xpath, OMIT_DESCENDANTS).iterator().next();
    }

    private Collection<YangModelCmHandle> getAllYangModelCmHandles() {
        final DataNode dataNode = inventoryPersistence.getDataNode("/dmi-registry").iterator().next();
        return dataNode.getChildDataNodes().stream().map(this::createYangModelCmHandle).collect(Collectors.toSet());
    }

    private YangModelCmHandle createYangModelCmHandle(final DataNode dataNode) {
        return YangDataConverter.convertCmHandleToYangModel(dataNode, dataNode.getLeaves().get("id").toString());
    }
}


