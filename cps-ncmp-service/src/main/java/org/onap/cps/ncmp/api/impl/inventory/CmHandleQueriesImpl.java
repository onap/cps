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

package org.onap.cps.ncmp.api.impl.inventory;

import static org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence.NCMP_DATASPACE_NAME;
import static org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence.NCMP_DMI_REGISTRY_ANCHOR;
import static org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence.NCMP_DMI_REGISTRY_PARENT;
import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS;
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.impl.inventory.enums.PropertyType;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CmHandleQueriesImpl implements CmHandleQueries {

    private static final String DESCENDANT_PATH = "//";

    private final CpsDataPersistenceService cpsDataPersistenceService;
    private static final String ANCESTOR_CM_HANDLES = "/ancestor::cm-handles";

    @Override
    public Collection<String> queryCmHandleAdditionalProperties(final Map<String, String> privatePropertyQueryPairs) {
        return queryCmHandleAnyProperties(privatePropertyQueryPairs, PropertyType.ADDITIONAL);
    }

    @Override
    public Collection<String> queryCmHandlePublicProperties(final Map<String, String> publicPropertyQueryPairs) {
        return queryCmHandleAnyProperties(publicPropertyQueryPairs, PropertyType.PUBLIC);
    }

    @Override
    public List<DataNode> queryCmHandlesByState(final CmHandleState cmHandleState) {
        return queryCmHandleAncestorsByCpsPath("//state[@cm-handle-state=\"" + cmHandleState + "\"]",
            INCLUDE_ALL_DESCENDANTS);
    }

    @Override
    public List<DataNode> queryNcmpRegistryByCpsPath(final String cpsPath,
                                                     final FetchDescendantsOption fetchDescendantsOption) {
        return cpsDataPersistenceService.queryDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                cpsPath, fetchDescendantsOption);
    }

    @Override
    public List<DataNode> queryCmHandleAncestorsByCpsPath(final String cpsPath,
                                                          final FetchDescendantsOption fetchDescendantsOption) {
        return queryNcmpRegistryByCpsPath(cpsPath + ANCESTOR_CM_HANDLES, fetchDescendantsOption);
    }

    @Override
    public boolean cmHandleHasState(final String cmHandleId, final CmHandleState requiredCmHandleState) {
        final DataNode stateDataNode = getCmHandleState(cmHandleId);
        final String cmHandleStateAsString = (String) stateDataNode.getLeaves().get("cm-handle-state");
        return CmHandleState.valueOf(cmHandleStateAsString).equals(requiredCmHandleState);
    }

    @Override
    public List<DataNode> queryCmHandlesByOperationalSyncState(final DataStoreSyncState dataStoreSyncState) {
        return queryCmHandleAncestorsByCpsPath("//state/datastores" + "/operational[@sync-state=\""
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

            final Collection<DataNode> dataNodes = queryCmHandleAncestorsByCpsPath(cpsPath,
                    OMIT_DESCENDANTS);
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

    private List<DataNode> getCmHandlesByDmiPluginIdentifierAndDmiProperty(final String dmiPluginIdentifier,
                                                                           final String dmiProperty) {
        return cpsDataPersistenceService.queryDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                NCMP_DMI_REGISTRY_PARENT + "/cm-handles[@" + dmiProperty + "='" + dmiPluginIdentifier + "']",
                OMIT_DESCENDANTS);
    }

    private DataNode getCmHandleState(final String cmHandleId) {
        final String xpath = NCMP_DMI_REGISTRY_PARENT + "/cm-handles[@id='" + cmHandleId + "']/state";
        return cpsDataPersistenceService.getDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                xpath, OMIT_DESCENDANTS).iterator().next();
    }
}


