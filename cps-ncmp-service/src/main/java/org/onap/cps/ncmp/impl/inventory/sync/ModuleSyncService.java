/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2024 Nordix Foundation
 *  Modifications Copyright (C) 2024 TechMahindra Ltd.
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

import com.hazelcast.collection.ISet;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.ncmp.api.exceptions.NcmpException;
import org.onap.cps.ncmp.impl.inventory.models.CmHandleState;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.spi.CascadeDeleteAllowed;
import org.onap.cps.spi.exceptions.SchemaSetNotFoundException;
import org.onap.cps.spi.model.ModuleReference;
import org.onap.cps.utils.ContentType;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModuleSyncService {

    private static final Map<String, String> NO_NEW_MODULES = Collections.emptyMap();

    private final DmiModelOperations dmiModelOperations;
    private final CpsModuleService cpsModuleService;
    private final CpsDataService cpsDataService;
    private final CpsAnchorService cpsAnchorService;
    private final JsonObjectMapper jsonObjectMapper;
    private final ISet<String> moduleSetTagsBeingProcessed;
    private final Map<String, ModuleDelta> privateModuleSetCache = new HashMap<>();

    @AllArgsConstructor
    private static final class ModuleDelta {
        Collection<ModuleReference> allModuleReferences;
        Map<String, String> newModuleNameToContentMap;
    }

    /**
     * This method creates a cm handle and initiates modules sync.
     *
     * @param yangModelCmHandle the yang model of cm handle.
     */
    public void syncAndCreateSchemaSetAndAnchor(final YangModelCmHandle yangModelCmHandle) {
        final String moduleSetTag = yangModelCmHandle.getModuleSetTag();
        final ModuleDelta moduleDelta;
        boolean isNewModuleSetTag = Strings.isNotBlank(moduleSetTag);
        try {
            if (privateModuleSetCache.containsKey(moduleSetTag)) {
                moduleDelta = privateModuleSetCache.get(moduleSetTag);
            } else {
                if (isNewModuleSetTag) {
                    if (moduleSetTagsBeingProcessed.add(moduleSetTag)) {
                        log.info("Processing new module set tag {}", moduleSetTag);
                    } else {
                        isNewModuleSetTag = false;
                        throw new NcmpException("Concurrent processing of module set tag " + moduleSetTag,
                            moduleSetTag + " already being processed for cm handle " + yangModelCmHandle.getId());
                    }
                }
                moduleDelta = getModuleDelta(yangModelCmHandle, moduleSetTag);
            }
            final String cmHandleId = yangModelCmHandle.getId();
            cpsModuleService.createSchemaSetFromModules(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandleId,
                moduleDelta.newModuleNameToContentMap, moduleDelta.allModuleReferences);
            cpsAnchorService.createAnchor(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandleId, cmHandleId);
            if (isNewModuleSetTag) {
                final ModuleDelta noModuleDelta = new ModuleDelta(moduleDelta.allModuleReferences, NO_NEW_MODULES);
                privateModuleSetCache.put(moduleSetTag, noModuleDelta);
            }
        } finally {
            if (isNewModuleSetTag) {
                moduleSetTagsBeingProcessed.remove(moduleSetTag);
            }
        }
    }

    /**
     * This method upgrades a cm handle and initiates modules sync.
     *
     * @param yangModelCmHandle the yang model of cm handle.
     */
    public void syncAndUpgradeSchemaSet(final YangModelCmHandle yangModelCmHandle) {
        final String upgradedModuleSetTag = ModuleOperationsUtils.getUpgradedModuleSetTagFromLockReason(
                yangModelCmHandle.getCompositeState().getLockReason());
        final ModuleDelta moduleDelta = getModuleDelta(yangModelCmHandle, upgradedModuleSetTag);
        cpsModuleService.upgradeSchemaSetFromModules(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME,
                yangModelCmHandle.getId(), moduleDelta.newModuleNameToContentMap, moduleDelta.allModuleReferences);
        setCmHandleModuleSetTag(yangModelCmHandle, upgradedModuleSetTag);
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

    public void clearPrivateModuleSetCache() {
        privateModuleSetCache.clear();
    }

    private ModuleDelta getModuleDelta(final YangModelCmHandle yangModelCmHandle, final String targetModuleSetTag) {
        final Map<String, String> newYangResources;
        Collection<ModuleReference> allModuleReferences = getModuleReferencesByModuleSetTag(targetModuleSetTag);
        if (allModuleReferences.isEmpty()) {
            allModuleReferences = dmiModelOperations.getModuleReferences(yangModelCmHandle);
            newYangResources = dmiModelOperations.getNewYangResourcesFromDmi(yangModelCmHandle,
                    cpsModuleService.identifyNewModuleReferences(allModuleReferences));
        } else {
            log.info("Found other cm handle having same module set tag: {}", targetModuleSetTag);
            newYangResources = NO_NEW_MODULES;
        }
        return new ModuleDelta(allModuleReferences, newYangResources);
    }

    private Collection<ModuleReference> getModuleReferencesByModuleSetTag(final String moduleSetTag) {
        if (Strings.isBlank(moduleSetTag)) {
            return Collections.emptyList();
        }
        return cpsModuleService.getModuleReferencesByAttribute(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                Map.of("module-set-tag", moduleSetTag), Map.of("cm-handle-state", CmHandleState.READY.name()));
    }

    private void setCmHandleModuleSetTag(final YangModelCmHandle yangModelCmHandle, final String newModuleSetTag) {
        final String jsonForUpdate = jsonObjectMapper.asJsonString(Map.of(
                "cm-handles", Map.of("id", yangModelCmHandle.getId(), "module-set-tag", newModuleSetTag)));
        cpsDataService.updateNodeLeaves(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, NCMP_DMI_REGISTRY_PARENT,
                jsonForUpdate, OffsetDateTime.now(), ContentType.JSON);
    }

}
