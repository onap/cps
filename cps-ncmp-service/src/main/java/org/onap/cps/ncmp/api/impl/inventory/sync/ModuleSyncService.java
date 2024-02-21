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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.ncmp.api.impl.inventory.CmHandleQueries;
import org.onap.cps.ncmp.api.impl.inventory.CmHandleState;
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
    private static final Map<String, String> NO_NEW_MODULES = Collections.emptyMap();

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
        final ModuleDelta moduleDelta = getModuleDelta(yangModelCmHandle, yangModelCmHandle.getModuleSetTag());
        final String cmHandleId = yangModelCmHandle.getId();
        cpsModuleService.createSchemaSetFromModules(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandleId,
                moduleDelta.newModuleNameToContentMap, moduleDelta.allModuleReferences);
        cpsAnchorService.createAnchor(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandleId, cmHandleId);
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

    private ModuleDelta getModuleDelta(final YangModelCmHandle yangModelCmHandle, final String moduleSetTag) {
        final ModuleDelta moduleDelta = new ModuleDelta();
        final DataNode dataNodeWithSameModuleSetTag = getFirstReadyDataNodeByModuleSetTag(moduleSetTag);
        if (dataNodeWithSameModuleSetTag != null) {
            log.info("Found other cm handle having same module set tag: {}", moduleSetTag);
            final String otherAnchorWithSameModuleSetTag
                    = YangDataConverter.extractCmHandleIdFromXpath(dataNodeWithSameModuleSetTag.getXpath());
            moduleDelta.allModuleReferences = cpsModuleService.getYangResourcesModuleReferences(
                    NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, otherAnchorWithSameModuleSetTag);
            moduleDelta.newModuleNameToContentMap = NO_NEW_MODULES;
        } else {
            moduleDelta.allModuleReferences = dmiModelOperations.getModuleReferences(yangModelCmHandle);
            moduleDelta.newModuleNameToContentMap = getNewModuleNameToContentMap(yangModelCmHandle,
                    cpsModuleService.identifyNewModuleReferences(moduleDelta.allModuleReferences));
        }
        return moduleDelta;
    }

    private DataNode getFirstReadyDataNodeByModuleSetTag(final String moduleSetTag) {
        final List<DataNode> dataNodes = StringUtils.isNotBlank(moduleSetTag) ? cmHandleQueries
                .queryNcmpRegistryByCpsPath("//cm-handles[@module-set-tag='" + moduleSetTag + "']",
                        FetchDescendantsOption.OMIT_DESCENDANTS) : Collections.emptyList();
        return dataNodes.stream().filter(dataNode -> {
            final String cmHandleId = YangDataConverter.extractCmHandleIdFromXpath(dataNode.getXpath());
            return cmHandleQueries.cmHandleHasState(cmHandleId, CmHandleState.READY);
        }).findFirst().orElse(null);
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

    private Map<String, String> getNewModuleNameToContentMap(final YangModelCmHandle yangModelCmHandle,
                                                             final Collection<ModuleReference> moduleReferences) {
        final Collection<ModuleReference> identifiedNewModuleReferences = cpsModuleService
                .identifyNewModuleReferences(moduleReferences);
        final Map<String, String> newModuleNameToContentMap;
        if (identifiedNewModuleReferences.isEmpty()) {
            newModuleNameToContentMap = NO_NEW_MODULES;
        } else {
            newModuleNameToContentMap = dmiModelOperations.getNewYangResourcesFromDmi(yangModelCmHandle,
                    identifiedNewModuleReferences);
        }
        return newModuleNameToContentMap;
    }
}
