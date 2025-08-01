/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.inventory;

import static org.onap.cps.api.parameters.FetchDescendantsOption.OMIT_DESCENDANTS;
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DATASPACE_NAME;
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DMI_REGISTRY_ANCHOR;
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DMI_REGISTRY_PARENT;

import com.hazelcast.map.IMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsQueryService;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.cpspath.parser.CpsPathUtil;
import org.onap.cps.ncmp.api.inventory.DataStoreSyncState;
import org.onap.cps.ncmp.api.inventory.models.CmHandleState;
import org.onap.cps.ncmp.api.inventory.models.TrustLevel;
import org.onap.cps.ncmp.impl.inventory.models.ModelledDmiServiceLeaves;
import org.onap.cps.ncmp.impl.inventory.models.PropertyType;
import org.onap.cps.ncmp.impl.inventory.trustlevel.TrustLevelCacheConfig;
import org.onap.cps.ncmp.impl.utils.YangDataConverter;
import org.onap.cps.utils.CpsValidator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CmHandleQueryServiceImpl implements CmHandleQueryService {
    private static final String ANCESTOR_CM_HANDLES = "/ancestor::cm-handles";
    public static final String CM_HANDLE_ID = "id";
    private static final String ALTERNATE_ID = "alternate-id";
    private static final Integer NO_LIMIT = 0;
    private final CpsDataService cpsDataService;
    private final CpsQueryService cpsQueryService;

    @Qualifier(TrustLevelCacheConfig.TRUST_LEVEL_PER_DMI_PLUGIN)
    private final Map<String, TrustLevel> trustLevelPerDmiPlugin;

    @Qualifier(TrustLevelCacheConfig.TRUST_LEVEL_PER_CM_HANDLE)
    private final IMap<String, TrustLevel> trustLevelPerCmHandleId;

    private final CpsValidator cpsValidator;

    @Override
    public Collection<String> queryCmHandleAdditionalProperties(final Map<String, String> additionalPropertyQueryPairs,
                                                                final boolean outputAlternateId) {
        return queryCmHandleAnyProperties(additionalPropertyQueryPairs, PropertyType.ADDITIONAL, outputAlternateId);
    }

    @Override
    public Collection<String> queryPublicCmHandleProperties(final Map<String, String> publicPropertyQueryPairs,
                                                            final boolean outputAlternateId) {
        return queryCmHandleAnyProperties(publicPropertyQueryPairs, PropertyType.PUBLIC, outputAlternateId);
    }

    @Override
    public Collection<String> queryCmHandlesByTrustLevel(final Map<String, String> trustLevelPropertyQueryPairs,
                                                         final boolean outputAlternateId) {
        final String trustLevelProperty = trustLevelPropertyQueryPairs.values().iterator().next();
        final TrustLevel targetTrustLevel = TrustLevel.valueOf(trustLevelProperty);
        return getCmHandleReferencesByTrustLevel(targetTrustLevel, outputAlternateId);
    }

    @Override
    public Collection<String> queryCmHandleIdsByState(final CmHandleState cmHandleState) {
        final Collection<DataNode> cmHandlesAsDataNodes =
                queryNcmpRegistryByCpsPath("//state[@cm-handle-state='" + cmHandleState + "']",
                        OMIT_DESCENDANTS);
        return cmHandlesAsDataNodes.stream()
                .map(DataNode::getXpath)
                .map(YangDataConverter::extractCmHandleIdFromXpath)
                .toList();
    }

    @Override
    public Collection<DataNode> queryNcmpRegistryByCpsPath(final String cpsPath,
                                                           final FetchDescendantsOption fetchDescendantsOption) {
        return queryNcmpRegistryByCpsPath(cpsPath, fetchDescendantsOption, NO_LIMIT);
    }

    @Override
    public Collection<DataNode> queryNcmpRegistryByCpsPath(final String cpsPath,
                                                           final FetchDescendantsOption fetchDescendantsOption,
                                                           final int queryResultLimit) {
        return cpsQueryService.queryDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, cpsPath,
                fetchDescendantsOption, queryResultLimit);
    }

    @Override
    public Collection<DataNode> queryCmHandleAncestorsByCpsPath(final String cpsPath,
                                                                final FetchDescendantsOption fetchDescendantsOption) {
        if (CpsPathUtil.getCpsPathQuery(cpsPath).getXpathPrefix().endsWith("/cm-handles")) {
            return queryNcmpRegistryByCpsPath(cpsPath, fetchDescendantsOption);
        }
        return queryNcmpRegistryByCpsPath(cpsPath + ANCESTOR_CM_HANDLES, fetchDescendantsOption);
    }

    @Override
    public boolean cmHandleHasState(final String cmHandleId, final CmHandleState requiredCmHandleState) {
        final DataNode stateDataNode = getCmHandleState(cmHandleId);
        final String cmHandleStateAsString = (String) stateDataNode.getLeaves().get("cm-handle-state");
        return CmHandleState.valueOf(cmHandleStateAsString).equals(requiredCmHandleState);
    }

    @Override
    public Collection<DataNode> queryCmHandlesByOperationalSyncState(final DataStoreSyncState dataStoreSyncState) {
        return queryCmHandleAncestorsByCpsPath("//state/datastores" + "/operational[@sync-state=\""
                + dataStoreSyncState + "\"]", FetchDescendantsOption.OMIT_DESCENDANTS);
    }

    @Override
    public Collection<String> getCmHandleReferencesByDmiPluginIdentifier(final String dmiPluginIdentifier,
                                                                         final boolean outputAlternateId) {
        final Collection<String> cmHandleReferences = new HashSet<>();
        for (final ModelledDmiServiceLeaves modelledDmiServiceLeaf : ModelledDmiServiceLeaves.values()) {
            cmHandleReferences.addAll(getIdsByDmiPluginIdentifierAndDmiProperty(
                        dmiPluginIdentifier, modelledDmiServiceLeaf.getLeafName(), outputAlternateId));
        }
        return cmHandleReferences;
    }

    @Override
    public Collection<String> getAllCmHandleReferences(final boolean outputAlternateId) {
        final String attributeName = outputAlternateId ? ALTERNATE_ID : CM_HANDLE_ID;
        final String cpsPath = String.format("%s/cm-handles/@%s", NCMP_DMI_REGISTRY_PARENT, attributeName);
        return cpsQueryService.queryDataLeaf(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, cpsPath, String.class);
    }

    @Override
    public Collection<String> getCmHandleReferencesByCpsPath(final String cpsPath, final boolean outputAlternateId) {
        final String cpsPathInQuery;
        final String cpsPathInQueryWithAttribute;
        if (CpsPathUtil.getCpsPathQuery(cpsPath).getXpathPrefix().endsWith("/cm-handles")) {
            cpsPathInQuery = cpsPath;
        } else {
            cpsPathInQuery = cpsPath + ANCESTOR_CM_HANDLES;
        }

        if (outputAlternateId) {
            cpsPathInQueryWithAttribute = cpsPathInQuery + "/@alternate-id";
        } else {
            cpsPathInQueryWithAttribute = cpsPathInQuery + "/@id";
        }
        return cpsQueryService.queryDataLeaf(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                cpsPathInQueryWithAttribute, String.class);
    }

    private Collection<String> getCmHandleReferencesByTrustLevel(final TrustLevel targetTrustLevel,
                                                                 final boolean outputAlternateId) {
        final Collection<String> selectedCmHandleReferences = new HashSet<>();
        for (final Map.Entry<String, TrustLevel> mapEntry : trustLevelPerDmiPlugin.entrySet()) {
            final String dmiPluginIdentifier = mapEntry.getKey();
            final TrustLevel dmiTrustLevel = mapEntry.getValue();
            final Map<String, String> candidateCmHandleReferences =
                    getCmHandleReferencesMapByDmiPluginIdentifier(dmiPluginIdentifier);
            final Map<String, TrustLevel> trustLevelPerCmHandleIdInBatch =
                    trustLevelPerCmHandleId.getAll(candidateCmHandleReferences.keySet());
            for (final Map.Entry<String, String> candidateCmHandleReference : candidateCmHandleReferences.entrySet()) {
                final TrustLevel candidateCmHandleTrustLevel =
                        trustLevelPerCmHandleIdInBatch.get(candidateCmHandleReference.getKey());
                final TrustLevel effectiveTrustlevel =
                        candidateCmHandleTrustLevel.getEffectiveTrustLevel(dmiTrustLevel);
                if (targetTrustLevel.equals(effectiveTrustlevel)) {
                    if (outputAlternateId) {
                        selectedCmHandleReferences.add(candidateCmHandleReference.getValue());
                    } else {
                        selectedCmHandleReferences.add(candidateCmHandleReference.getKey());
                    }
                }
            }
        }
        return selectedCmHandleReferences;
    }

    private Collection<String> queryCmHandleAnyProperties(
            final Map<String, String> propertyQueryPairs,
            final PropertyType propertyType, final boolean outputAlternateId) {
        if (propertyQueryPairs.isEmpty()) {
            return Collections.emptySet();
        }
        Collection<String> cmHandleReferences = null;
        for (final Map.Entry<String, String> publicPropertyQueryPair : propertyQueryPairs.entrySet()) {
            final Collection<String> cmHandleReferencesToRetain = getCmHandleReferencesByProperties(propertyType,
                    publicPropertyQueryPair.getKey(), publicPropertyQueryPair.getValue(), outputAlternateId);
            if (cmHandleReferences == null) {
                cmHandleReferences = cmHandleReferencesToRetain;
            } else {
                cmHandleReferences.retainAll(cmHandleReferencesToRetain);
            }
            if (cmHandleReferences.isEmpty()) {
                break;
            }
        }
        return cmHandleReferences;
    }

    private Set<String> getIdsByDmiPluginIdentifierAndDmiProperty(final String dmiPluginIdentifier,
                                                                  final String dmiProperty,
                                                                  final boolean outputAlternateId) {
        final String attributeName = outputAlternateId ? ALTERNATE_ID : CM_HANDLE_ID;
        final String cpsPath = String.format("%s/cm-handles[@%s='%s']/@%s",
                NCMP_DMI_REGISTRY_PARENT, dmiProperty, dmiPluginIdentifier, attributeName);
        return cpsQueryService.queryDataLeaf(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, cpsPath, String.class);
    }

    private Collection<DataNode> getDataNodesByDmiPluginIdentifierAndDmiProperty(final String dmiPluginIdentifier,
                                                                                 final String dmiProperty) {
        return cpsQueryService.queryDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                NCMP_DMI_REGISTRY_PARENT + "/cm-handles[@" + dmiProperty + "='" + dmiPluginIdentifier + "']",
                OMIT_DESCENDANTS);
    }

    private Map<String, String> getCmHandleReferencesMapByDmiPluginIdentifier(final String dmiPluginIdentifier) {
        final Map<String, String> cmHandleReferencesMap = new HashMap<>();
        for (final ModelledDmiServiceLeaves modelledDmiServiceLeaf : ModelledDmiServiceLeaves.values()) {
            final Collection<DataNode> cmHandlesAsDataNodes = getDataNodesByDmiPluginIdentifierAndDmiProperty(
                    dmiPluginIdentifier, modelledDmiServiceLeaf.getLeafName());
            for (final DataNode cmHandleAsDataNode : cmHandlesAsDataNodes) {
                final String cmHandleId = cmHandleAsDataNode.getLeaves().get(CM_HANDLE_ID).toString();
                final String alternateId = cmHandleAsDataNode.getLeaves().get(ALTERNATE_ID).toString();
                cmHandleReferencesMap.put(cmHandleId, alternateId);
            }
        }
        return cmHandleReferencesMap;
    }

    private Collection<String> getCmHandleReferencesByProperties(final PropertyType propertyType,
                                                                 final String propertyName,
                                                                 final String propertyValue,
                                                                 final boolean outputAlternateId) {
        final String attributeName = outputAlternateId ? ALTERNATE_ID : CM_HANDLE_ID;
        final String cpsPath = String.format("//%s[@name='%s' and @value='%s']%s/@%s",
                propertyType.getYangContainerName(), propertyName, propertyValue, ANCESTOR_CM_HANDLES, attributeName);
        return cpsQueryService.queryDataLeaf(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, cpsPath, String.class);
    }

    private DataNode getCmHandleState(final String cmHandleId) {
        cpsValidator.validateNameCharacters(cmHandleId);
        final String xpath = NCMP_DMI_REGISTRY_PARENT + "/cm-handles[@id='" + cmHandleId + "']/state";
        return cpsDataService.getDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                xpath, OMIT_DESCENDANTS).iterator().next();
    }
}
