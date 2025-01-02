/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2025 Nordix Foundation
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

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.api.exceptions.AlreadyDefinedException;
import org.onap.cps.api.exceptions.DuplicatedYangResourceException;
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
        Map<String, String> newModuleNameToContentMap;
    }

    /**
     * This method creates a cm handle and initiates modules sync.
     *
     * @param yangModelCmHandle the yang model of cm handle.
     */
    public void syncAndCreateSchemaSetAndAnchor(final YangModelCmHandle yangModelCmHandle) {
        final String cmHandleId = yangModelCmHandle.getId();
        final String moduleSetTag = yangModelCmHandle.getModuleSetTag();
        final String schemaSetName = getSchemaSetName(cmHandleId, moduleSetTag);
        syncAndCreateSchemaSet(yangModelCmHandle, schemaSetName);
        cpsAnchorService.createAnchor(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, schemaSetName, cmHandleId);
    }

    /**
     * This method upgrades a cm handle and initiates modules sync.
     *
     * @param yangModelCmHandle the yang model of cm handle.
     */
    public void syncAndUpgradeSchemaSet(final YangModelCmHandle yangModelCmHandle) {
        final String cmHandleId = yangModelCmHandle.getId();
        final String sourceModuleSetTag = yangModelCmHandle.getModuleSetTag();
        final String targetModuleSetTag = ModuleOperationsUtils.getUpgradedModuleSetTagFromLockReason(
                yangModelCmHandle.getCompositeState().getLockReason());
        if (sourceModuleSetTag.isEmpty() && targetModuleSetTag.isEmpty()) {
            final ModuleDelta moduleDelta = getModuleDelta(yangModelCmHandle);
            cpsModuleService.upgradeSchemaSetFromModules(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME,
                    cmHandleId, moduleDelta.newModuleNameToContentMap, moduleDelta.allModuleReferences);
        } else {
            final String targetSchemaSetName = getSchemaSetName(cmHandleId, targetModuleSetTag);
            syncAndCreateSchemaSet(yangModelCmHandle, targetSchemaSetName);
            cpsAnchorService.updateAnchorSchemaSet(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandleId,
                    targetSchemaSetName);
            setCmHandleModuleSetTag(yangModelCmHandle, targetModuleSetTag);
        }
    }

    private void syncAndCreateSchemaSet(final YangModelCmHandle yangModelCmHandle, final String schemaSetName) {
        if (isNewSchemaSet(schemaSetName)) {
            final ModuleDelta moduleDelta = getModuleDelta(yangModelCmHandle);
            try {
                log.info("Creating Schema Set {} for {}", schemaSetName, yangModelCmHandle.getId());
                cpsModuleService.createSchemaSetFromModules(
                        NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME,
                        schemaSetName,
                        moduleDelta.newModuleNameToContentMap,
                        moduleDelta.allModuleReferences
                );
                log.info("Successfully created Schema Set {} for {}", schemaSetName, yangModelCmHandle.getId());
            } catch (final AlreadyDefinedException | DuplicatedYangResourceException exception) {
                log.warn("Schema Set {} already exists, ignoring attempt to (re)create it for {}",
                    schemaSetName, yangModelCmHandle.getId());
            }
        }
    }

    private boolean isNewSchemaSet(final String schemaSetName) {
        return !cpsModuleService.schemaSetExists(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, schemaSetName);
    }

    private ModuleDelta getModuleDelta(final YangModelCmHandle yangModelCmHandle) {
        final Collection<ModuleReference> allModuleReferences =
            dmiModelOperations.getModuleReferences(yangModelCmHandle);
        final Collection<ModuleReference> newModuleReferences =
                cpsModuleService.identifyNewModuleReferences(allModuleReferences);
        final Map<String, String> newYangResources = dmiModelOperations.getNewYangResourcesFromDmi(yangModelCmHandle,
                newModuleReferences);
        return new ModuleDelta(allModuleReferences, newYangResources);
    }

    private void setCmHandleModuleSetTag(final YangModelCmHandle yangModelCmHandle, final String newModuleSetTag) {
        final String jsonForUpdate = jsonObjectMapper.asJsonString(Map.of(
                "cm-handles", Map.of("id", yangModelCmHandle.getId(), "module-set-tag", newModuleSetTag)));
        cpsDataService.updateNodeLeaves(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, NCMP_DMI_REGISTRY_PARENT,
                jsonForUpdate, OffsetDateTime.now(), ContentType.JSON);
    }

    private static String getSchemaSetName(final String cmHandleId, final String moduleSetTag) {
        return moduleSetTag.isEmpty() ? cmHandleId : moduleSetTag;
    }

}
