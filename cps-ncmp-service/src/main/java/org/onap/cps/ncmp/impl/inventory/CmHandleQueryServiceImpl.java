/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2025 Nordix Foundation
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
    private static final String ALTERNATE_ID = "alternate-id";
    private final CpsDataService cpsDataService;
    private final CpsQueryService cpsQueryService;

    @Qualifier(TrustLevelCacheConfig.TRUST_LEVEL_PER_DMI_PLUGIN)
    private final Map<String, TrustLevel> trustLevelPerDmiPlugin;

    @Qualifier(TrustLevelCacheConfig.TRUST_LEVEL_PER_CM_HANDLE)
    private final IMap<String, TrustLevel> trustLevelPerCmHandleId;

    private final CpsValidator cpsValidator;

    @Override
    public Collection<String> queryCmHandleAdditionalProperties(final Map<String, String> privatePropertyQueryPairs,
                                                                final boolean outputAlternateId) {
        return queryCmHandleAnyProperties(privatePropertyQueryPairs, PropertyType.ADDITIONAL, outputAlternateId);
    }

    @Override
    public Collection<String> queryCmHandlePublicProperties(final Map<String, String> publicPropertyQueryPairs,
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
        return cpsQueryService.queryDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, cpsPath,
                fetchDescendantsOption);
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

    private Collection<String> getCmHandleReferencesByTrustLevel(final TrustLevel targetTrustLevel,
                                                                 final boolean outputAlternateId) {
        final Collection<String> selectedCmHandleReferences = new HashSet<>();
        for (final Map.Entry<String, TrustLevel> mapEntry : trustLevelPerDmiPlugin.entrySet()) {
            final String dmiPluginIdentifier = mapEntry.getKey();
            final TrustLevel dmiTrustLevel = mapEntry.getValue();
            final Collection<String> candidateCmHandleIds = getCmHandleReferencesByDmiPluginIdentifier(
                    dmiPluginIdentifier, false);
            final Set<String> candidateCmHandleIdsSet = new HashSet<>(candidateCmHandleIds);
            final Map<String, TrustLevel> trustLevelPerCmHandleIdInBatch =
                trustLevelPerCmHandleId.getAll(candidateCmHandleIdsSet);
            for (final Map.Entry<String, TrustLevel> canidateCmHandleId : trustLevelPerCmHandleIdInBatch.entrySet()) {
                final TrustLevel effectiveTrustlevel =
                    canidateCmHandleId.getValue().getEffectiveTrustLevel(dmiTrustLevel);
                if (targetTrustLevel.equals(effectiveTrustlevel)) {
                    selectedCmHandleReferences.add(canidateCmHandleId.getKey());
                }
            }
        }
        if (outputAlternateId) {
            return getAlternateIdsByCmHandleIds(selectedCmHandleReferences);
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
        final String attributeName = outputAlternateId ? ALTERNATE_ID : "id";
        final String cpsPath = String.format("%s/cm-handles[@%s='%s']/@%s",
                NCMP_DMI_REGISTRY_PARENT, dmiProperty, dmiPluginIdentifier, attributeName);
        return cpsQueryService.queryDataLeaf(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, cpsPath, String.class);
    }

    private Collection<String> getAlternateIdsByCmHandleIds(final Collection<String> cmHandleIds) {

        final String cpsPath = NCMP_DMI_REGISTRY_PARENT + "/cm-handles["
                + createFormattedQueryString(cmHandleIds) + "]/@alternate-id";

        return cpsQueryService.queryDataLeaf(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, cpsPath, String.class);
    }

    private Collection<String> getCmHandleReferencesByProperties(final PropertyType propertyType,
                                                                 final String propertyName,
                                                                 final String propertyValue,
                                                                 final boolean outputAlternateId) {
        final String attributeName = outputAlternateId ? ALTERNATE_ID : "id";
        final String cpsPath = String.format("//%s[@name='%s' and @value='%s']%s/@%s",
                propertyType.getYangContainerName(), propertyName, propertyValue, ANCESTOR_CM_HANDLES, attributeName);
        return cpsQueryService.queryDataLeaf(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, cpsPath, String.class);
    }

    private String createFormattedQueryString(final Collection<String> cmHandleIds) {
        return cmHandleIds.stream()
                .map(cmHandleId -> "@id='" + cmHandleId + "'")
                .collect(Collectors.joining(" or "));
    }


    private DataNode getCmHandleState(final String cmHandleId) {
        cpsValidator.validateNameCharacters(cmHandleId);
        final String xpath = NCMP_DMI_REGISTRY_PARENT + "/cm-handles[@id='" + cmHandleId + "']/state";
        return cpsDataService.getDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                xpath, OMIT_DESCENDANTS).iterator().next();
    }
}