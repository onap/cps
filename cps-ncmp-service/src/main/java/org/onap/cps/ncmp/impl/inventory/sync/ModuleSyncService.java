/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2026 OpenInfra Foundation Europe. All rights reserved.
 *  Modifications Copyright (C) 2024 Deutsche Telekom AG
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

package org.onap.cps.ncmp.impl.inventory.sync;

import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DATASPACE_NAME;
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DMI_REGISTRY_ANCHOR;
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DMI_REGISTRY_PARENT;
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.api.exceptions.AlreadyDefinedException;
import org.onap.cps.api.model.ModuleDefinition;
import org.onap.cps.api.model.ModuleReference;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.utils.ContentType;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModuleSyncService {

    private final DmiModelOperations dmiModelOperations;
    private final CpsModuleService cpsModuleService;
    private final CpsDataService cpsDataService;
    private final CpsAnchorService cpsAnchorService;
    private final JsonObjectMapper jsonObjectMapper;

    @AllArgsConstructor
    private static final class ModuleDelta {
        Collection<ModuleReference> allModuleReferences;
        Map<String, String> newYangResourceContentPerName;
    }

    /**
     * Creates a CM handle and initiates the synchronization of modules to create a schema set and anchor.
     *
     * @param yangModelCmHandle the yang model of cm handle.
     */
    public void syncAndCreateSchemaSetAndAnchor(final YangModelCmHandle yangModelCmHandle) {
        final String cmHandleId = yangModelCmHandle.getId();
        final String targetModuleSetTag = yangModelCmHandle.getModuleSetTag();
        final String schemaSetName = getSchemaSetNameForModuleSetTag(cmHandleId, targetModuleSetTag);
        syncAndCreateSchemaSet(yangModelCmHandle, schemaSetName, targetModuleSetTag);
        try {
            cpsAnchorService.createAnchor(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, schemaSetName, cmHandleId);
        } catch (final AlreadyDefinedException alreadyDefinedException) {
            log.warn("Ignoring (Anchor) already exists exception for {}. Exception details: {}", cmHandleId,
                    alreadyDefinedException.getDetails());
        }
    }

    /**
     * This method upgrades a cm handle and initiates modules sync.
     *
     * @param yangModelCmHandle the yang model of cm handle.
     */
    public void syncAndUpgradeSchemaSet(final YangModelCmHandle yangModelCmHandle) {
        final String cmHandleId = yangModelCmHandle.getId();
        final String sourceModuleSetTag = yangModelCmHandle.getModuleSetTag();
        final String targetModuleSetTag = ModuleOperationsUtils.getTargetModuleSetTagForUpgrade(yangModelCmHandle);
        final String schemaSetName = getSchemaSetNameForModuleSetTag(cmHandleId, targetModuleSetTag);
        if (sourceModuleSetTag.isEmpty() && targetModuleSetTag.isEmpty()) {
            final ModuleDelta moduleDelta = getModuleDelta(yangModelCmHandle, targetModuleSetTag);
            cpsModuleService.upgradeSchemaSetFromModules(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME,
                    schemaSetName, moduleDelta.newYangResourceContentPerName, moduleDelta.allModuleReferences);
        } else {
            syncAndCreateSchemaSet(yangModelCmHandle, schemaSetName, targetModuleSetTag);
            cpsAnchorService.updateAnchorSchemaSet(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandleId, schemaSetName);
            setCmHandleModuleSetTag(yangModelCmHandle, targetModuleSetTag);
            log.info("Upgrading schema set for CM handle ID: {}, Source Tag: {}, Target Tag: {}",
                cmHandleId, sourceModuleSetTag, targetModuleSetTag);
        }
    }

    /**
     * Force re-read of the module content from the node and update any changed YANG resource content in place.
     * The module set tag and revisions are not changed; only the stored content of existing modules is refreshed
     * when it differs from what the node now returns. This is used to pick up model regenerations where the
     * revision was (deliberately) not bumped.
     *
     * @param yangModelCmHandle the yang model of cm handle.
     */
    public void refreshModuleContent(final YangModelCmHandle yangModelCmHandle) {
        final String cmHandleId = yangModelCmHandle.getId();
        final String moduleSetTag = yangModelCmHandle.getModuleSetTag();
        final Collection<ModuleReference> allModuleReferences =
                dmiModelOperations.getModuleReferences(yangModelCmHandle, moduleSetTag);
        final Map<String, String> yangResourceContentPerModuleName =
                dmiModelOperations.getYangResourcesFromDmi(yangModelCmHandle, moduleSetTag, allModuleReferences);
        final Map<String, ModuleDefinition> storedModuleDefinitionPerModuleName =
                getStoredModuleDefinitionsPerModuleName(cmHandleId);

        int numberOfChangedModules = 0;
        for (final ModuleReference moduleReference : allModuleReferences) {
            final String moduleName = moduleReference.getModuleName();
            final String contentFromNode = yangResourceContentPerModuleName.get(moduleName);
            final ModuleDefinition storedModuleDefinition = storedModuleDefinitionPerModuleName.get(moduleName);
            if (contentFromNode != null && storedModuleDefinition != null
                    && !contentFromNode.equals(storedModuleDefinition.getContent())) {
                cpsModuleService.updateYangResourceContent(moduleName, moduleReference.getRevision(), contentFromNode);
                log.info("YANG module content change detected and refreshed: node (cm handle) '{}', "
                        + "module set tag '{}', module '{}@{}'",
                        cmHandleId, moduleSetTag, moduleName, moduleReference.getRevision());
                numberOfChangedModules++;
            }
        }
        if (numberOfChangedModules > 0) {
            final String schemaSetName = getSchemaSetNameForModuleSetTag(cmHandleId, moduleSetTag);
            cpsModuleService.removeSchemaSetFromModuleCache(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, schemaSetName);
        }
        log.info("Module refresh completed for CM handle '{}' (module set tag '{}'): {} module(s) changed",
                cmHandleId, moduleSetTag, numberOfChangedModules);
    }

    private Map<String, ModuleDefinition> getStoredModuleDefinitionsPerModuleName(final String cmHandleId) {
        final Collection<ModuleDefinition> storedModuleDefinitions =
                cpsModuleService.getModuleDefinitionsByAnchorName(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandleId);
        final Map<String, ModuleDefinition> storedModuleDefinitionPerModuleName =
                HashMap.newHashMap(storedModuleDefinitions.size());
        for (final ModuleDefinition storedModuleDefinition : storedModuleDefinitions) {
            storedModuleDefinitionPerModuleName.put(storedModuleDefinition.getModuleName(), storedModuleDefinition);
        }
        return storedModuleDefinitionPerModuleName;
    }

    private void syncAndCreateSchemaSet(final YangModelCmHandle yangModelCmHandle,
                                        final String schemaSetName,
                                        final String targetModuleSetTag) {
        if (isNewSchemaSet(schemaSetName)) {
            final String cmHandleId = yangModelCmHandle.getId();
            final ModuleDelta moduleDelta = getModuleDelta(yangModelCmHandle, targetModuleSetTag);
            try {
                log.info("Creating Schema Set {} for CM Handle {}", schemaSetName, cmHandleId);
                cpsModuleService.createSchemaSetFromModules(
                    NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME,
                    schemaSetName,
                    moduleDelta.newYangResourceContentPerName,
                    moduleDelta.allModuleReferences
                );
                log.info("Successfully created Schema Set {} for CM Handle {}", schemaSetName,
                    yangModelCmHandle.getId());
            } catch (final AlreadyDefinedException alreadyDefinedException) {
                log.warn("Ignoring (Schema Set) already exists exception for {}. Exception details: {}", cmHandleId,
                        alreadyDefinedException.getDetails());
            }
        }
    }

    private boolean isNewSchemaSet(final String schemaSetName) {
        return !cpsModuleService.schemaSetExists(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, schemaSetName);
    }

    private ModuleDelta getModuleDelta(final YangModelCmHandle yangModelCmHandle,
                                       final String targetModuleSetTag) {
        final Collection<ModuleReference> allModuleReferences =
            dmiModelOperations.getModuleReferences(yangModelCmHandle, targetModuleSetTag);
        final Collection<ModuleReference> newModuleReferences =
                cpsModuleService.identifyNewModuleReferences(allModuleReferences);
        final Map<String, String> newYangResourceContentPerName =
            dmiModelOperations.getYangResourcesFromDmi(yangModelCmHandle, targetModuleSetTag, newModuleReferences);
        log.debug("Module delta calculated for CM handle ID: {}. All references: {}. New modules: {}",
            yangModelCmHandle.getId(), allModuleReferences, newYangResourceContentPerName.keySet());
        return new ModuleDelta(allModuleReferences, newYangResourceContentPerName);
    }

    private void setCmHandleModuleSetTag(final YangModelCmHandle yangModelCmHandle, final String newModuleSetTag) {
        final String jsonForUpdate = jsonObjectMapper.asJsonString(Map.of(
                "cm-handles", Map.of("id", yangModelCmHandle.getId(), "module-set-tag", newModuleSetTag)));
        cpsDataService.updateNodeLeaves(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, NCMP_DMI_REGISTRY_PARENT,
                jsonForUpdate, OffsetDateTime.now(), ContentType.JSON);
    }

    private static String getSchemaSetNameForModuleSetTag(final String cmHandleId, final String moduleSetTag) {
        return moduleSetTag.isEmpty() ? cmHandleId : moduleSetTag;
    }

}
