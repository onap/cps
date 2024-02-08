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

package org.onap.cps.ncmp.api.impl.inventory.sync;

import static org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence.NCMP_DATASPACE_NAME;
import static org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence.NCMP_DMI_REGISTRY_ANCHOR;
import static org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence.NCMP_DMI_REGISTRY_PARENT;
import static org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence.NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.ncmp.api.impl.inventory.CmHandleQueries;
import org.onap.cps.ncmp.api.impl.inventory.CmHandleState;
import org.onap.cps.ncmp.api.impl.inventory.CompositeState;
import org.onap.cps.ncmp.api.impl.operations.DmiModelOperations;
import org.onap.cps.ncmp.api.impl.utils.YangDataConverter;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.spi.CascadeDeleteAllowed;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.exceptions.SchemaSetNotFoundException;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.ModuleReference;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModuleSyncService {

    private final DmiModelOperations dmiModelOperations;
    private final CpsModuleService cpsModuleService;
    private final CmHandleQueries cmHandleQueries;
    private final CpsDataService cpsDataService;
    private final CpsAnchorService cpsAnchorService;
    private final JsonObjectMapper jsonObjectMapper;
    private final Map<String, Collection<ModuleReference>> moduleSetTagCache;
    private static final Map<String, String> NO_NEW_MODULES = Collections.emptyMap();

    /**
     * This method registers a cm handle and initiates modules sync.
     *
     * @param yangModelCmHandle the yang model of cm handle.
     */
    public void syncAndCreateOrUpgradeSchemaSetAndAnchor(final YangModelCmHandle yangModelCmHandle) {

        final String cmHandleId = yangModelCmHandle.getId();
        final CompositeState compositeState = yangModelCmHandle.getCompositeState();
        final boolean inUpgrade = ModuleOperationsUtils.isInUpgradeOrUpgradeFailed(compositeState);
        final String moduleSetTag = getModuleSetTag(yangModelCmHandle, compositeState, inUpgrade);
        boolean isNewModuleSetTagForUpgradeOrCreate = false;

        final Collection<ModuleReference> moduleReferencesFromCache = moduleSetTagCache.get(moduleSetTag);

        if (moduleReferencesFromCache == null) {
            final Optional<DataNode> existingCmHandleWithSameModuleSetTag
                    = getFirstReadyDataNodeWithModuleSetTag(moduleSetTag);

            if (existingCmHandleWithSameModuleSetTag.isPresent()) {
                final String existingAnchorName = existingCmHandleWithSameModuleSetTag.get().getAnchorName();
                final Collection<ModuleReference> moduleReferencesFromExistingCmHandle =
                        upgradeOrCreateSchemaSetUsingModuleSetTag(yangModelCmHandle.getId(), moduleSetTag,
                                existingAnchorName, inUpgrade);
                updateModuleSetTagCache(moduleSetTag, moduleReferencesFromExistingCmHandle);
            } else {
                if (inUpgrade) {
                    deleteSchemaSetIfExists(cmHandleId);
                }
                final Collection<ModuleReference> allModuleReferencesFromCmHandle
                        = syncAndCreateSchemaSet(yangModelCmHandle);
                updateModuleSetTagCache(moduleSetTag, allModuleReferencesFromCmHandle);
                isNewModuleSetTagForUpgradeOrCreate = true;
            }
        } else {
            if (inUpgrade) {
                cpsModuleService.upgradeSchemaSetFromModules(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandleId,
                        NO_NEW_MODULES, moduleReferencesFromCache);
            } else {
                final Map<String, String> newModuleNameToContentMap
                        = getNewModuleNameToContentMap(yangModelCmHandle, moduleReferencesFromCache);
                cpsModuleService.createSchemaSetFromModules(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME,
                        cmHandleId, newModuleNameToContentMap, moduleReferencesFromCache);
                isNewModuleSetTagForUpgradeOrCreate = true;
            }
        }
        if (isNewModuleSetTagForUpgradeOrCreate) {
            cpsAnchorService.createAnchor(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandleId, cmHandleId);
        }
        setCmHandleModuleSetTag(yangModelCmHandle, moduleSetTag);
    }

    /**
     * Deletes the SchemaSet for schema set id if the SchemaSet Exists.
     *
     * @param schemaSetId the schema set id to be deleted
     */
    public void deleteSchemaSetIfExists(final String schemaSetId) {
        try {
            cpsModuleService.deleteSchemaSet(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, schemaSetId,
                CascadeDeleteAllowed.CASCADE_DELETE_ALLOWED);
            log.debug("SchemaSet for {} has been deleted. Ready to be recreated.", schemaSetId);
        } catch (final SchemaSetNotFoundException e) {
            log.debug("No SchemaSet for {}. Assuming CmHandle has not been previously Module Synced.", schemaSetId);
        }
    }

    private Optional<DataNode> getFirstReadyDataNodeWithModuleSetTag(final String moduleSetTag) {
        final List<DataNode> dataNodes = StringUtils.isNotBlank(moduleSetTag) ? cmHandleQueries
                .queryNcmpRegistryByCpsPath("//cm-handles[@module-set-tag='" + moduleSetTag + "']",
                        FetchDescendantsOption.OMIT_DESCENDANTS) : Collections.emptyList();
        return dataNodes.stream().filter(dataNode -> {
            final String cmHandleId = YangDataConverter.extractCmHandleIdFromXpath(dataNode.getXpath());
            return cmHandleQueries.cmHandleHasState(cmHandleId, CmHandleState.READY);
        }).findFirst();
    }

    private void setCmHandleModuleSetTag(final YangModelCmHandle upgradedCmHandle, final String moduleSetTag) {
        final Map<String, Map<String, String>> dmiRegistryProperties = new HashMap<>(1);
        final Map<String, String> cmHandleProperties = new HashMap<>(2);
        cmHandleProperties.put("id", upgradedCmHandle.getId());
        cmHandleProperties.put("module-set-tag", moduleSetTag);
        dmiRegistryProperties.put("cm-handles", cmHandleProperties);
        cpsDataService.updateNodeLeaves(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, NCMP_DMI_REGISTRY_PARENT,
                jsonObjectMapper.asJsonString(dmiRegistryProperties), OffsetDateTime.now());
    }

    private Collection<ModuleReference> syncAndCreateSchemaSet(final YangModelCmHandle yangModelCmHandle) {
        final Collection<ModuleReference> allModuleReferencesFromCmHandle =
                dmiModelOperations.getModuleReferences(yangModelCmHandle);
        final Map<String, String> newModuleNameToContentMap
                = getNewModuleNameToContentMap(yangModelCmHandle, allModuleReferencesFromCmHandle);
        cpsModuleService.createSchemaSetFromModules(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME,
                yangModelCmHandle.getId(), newModuleNameToContentMap, allModuleReferencesFromCmHandle);
        return allModuleReferencesFromCmHandle;
    }

    private Collection<ModuleReference> upgradeOrCreateSchemaSetUsingModuleSetTag(final String schemaSetName,
                                                                                  final String moduleSetTag,
                                                                                  final String existingAnchorName,
                                                                                  final boolean inUpgrade) {
        log.info("Found cm handle having module set tag: {}", moduleSetTag);
        final Collection<ModuleReference> moduleReferencesFromExistingCmHandle =
                cpsModuleService.getYangResourcesModuleReferences(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME,
                        existingAnchorName);
        if (inUpgrade) {
            cpsModuleService.upgradeSchemaSetFromModules(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, schemaSetName,
                    NO_NEW_MODULES, moduleReferencesFromExistingCmHandle);
        } else {
            cpsModuleService.createSchemaSetFromModules(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME,
                    schemaSetName, NO_NEW_MODULES, moduleReferencesFromExistingCmHandle);
        }
        return moduleReferencesFromExistingCmHandle;
    }

    private String getModuleSetTag(final YangModelCmHandle yangModelCmHandle,
                                   final CompositeState compositeState,
                                   final boolean inUpgrade) {
        if (inUpgrade) {
            return ModuleOperationsUtils.getLockedCompositeStateDetails(compositeState.getLockReason())
                    .get(ModuleOperationsUtils.MODULE_SET_TAG_KEY);
        }
        return yangModelCmHandle.getModuleSetTag();
    }

    private void updateModuleSetTagCache(final String moduleSetTag,
                                         final Collection<ModuleReference> allModuleReferencesFromCmHandle) {
        if (StringUtils.isNotBlank(moduleSetTag)) {
            moduleSetTagCache.putIfAbsent(moduleSetTag, allModuleReferencesFromCmHandle);
        }
    }

    private Map<String, String> getNewModuleNameToContentMap(final YangModelCmHandle yangModelCmHandle,
                                                             final Collection<ModuleReference> moduleReferences) {
        final Collection<ModuleReference> identifiedNewModuleReferencesFromCache = cpsModuleService
                .identifyNewModuleReferences(moduleReferences);
        final Map<String, String> newModuleNameToContentMap;
        if (identifiedNewModuleReferencesFromCache.isEmpty()) {
            newModuleNameToContentMap = NO_NEW_MODULES;
        } else {
            newModuleNameToContentMap = dmiModelOperations.getNewYangResourcesFromDmi(yangModelCmHandle,
                    identifiedNewModuleReferencesFromCache);
        }
        return newModuleNameToContentMap;
    }

}
