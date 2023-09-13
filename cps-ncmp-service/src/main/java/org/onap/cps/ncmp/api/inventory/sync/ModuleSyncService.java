/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.inventory.sync;

import static org.onap.cps.constants.DmiRegistryConstants.NCMP_DATASPACE_NAME;
import static org.onap.cps.constants.DmiRegistryConstants.NCMP_DMI_REGISTRY_ANCHOR;
import static org.onap.cps.constants.DmiRegistryConstants.NCMP_DMI_REGISTRY_PARENT;
import static org.onap.cps.constants.DmiRegistryConstants.NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME;

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
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.ncmp.api.impl.operations.DmiModelOperations;
import org.onap.cps.ncmp.api.impl.utils.YangDataConverter;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.inventory.CmHandleQueries;
import org.onap.cps.ncmp.api.inventory.CmHandleState;
import org.onap.cps.ncmp.api.inventory.CompositeState;
import org.onap.cps.ncmp.api.inventory.LockReasonCategory;
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
    private final CpsAdminService cpsAdminService;
    private final CmHandleQueries cmHandleQueries;
    private final CpsDataService cpsDataService;
    private final JsonObjectMapper jsonObjectMapper;

    /**
     * This method registers a cm handle and initiates modules sync.
     *
     * @param upgradedCmHandle the yang model of cm handle.
     */
    public void syncAndCreateOrUpgradeSchemaSetAndAnchor(final YangModelCmHandle upgradedCmHandle) {

        final CompositeState compositeState = upgradedCmHandle.getCompositeState();
        final String moduleSetTag =
                (compositeState.getLockReason() != null && compositeState.getLockReason().getLockReasonCategory()
                        == LockReasonCategory.MODULE_UPGRADE) ? compositeState.getLockReason().getDetails()
                        : StringUtils.EMPTY;
        final Optional<DataNode> existingCmHandleWithSameModuleSetTag
                = getFirstReadyDataNodeWithModuleSeTag(moduleSetTag);
        if (existingCmHandleWithSameModuleSetTag.isPresent()) {
            log.info("Found cm handle having module set tag: {}", moduleSetTag);
            final Collection<ModuleReference> moduleReferencesFromExistingCmHandle =
                    cpsModuleService.getYangResourcesModuleReferences(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR);
            final String upgradedSchemaSetAndAnchorName = upgradedCmHandle.getId();
            final Map<String, String> noNewModules = Collections.emptyMap();
            cpsModuleService.createOrUpgradeSchemaSetFromModules(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME,
                    upgradedSchemaSetAndAnchorName, noNewModules, moduleReferencesFromExistingCmHandle);
        } else {
            syncAndCreateSchemaSetAndAnchor(upgradedCmHandle);
        }
        updateCmHandleModuleSetTag(upgradedCmHandle);
    }

    private void syncAndCreateSchemaSetAndAnchor(final YangModelCmHandle yangModelCmHandle) {
        final Collection<ModuleReference> allModuleReferencesFromCmHandle =
                dmiModelOperations.getModuleReferences(yangModelCmHandle);

        final Collection<ModuleReference> identifiedNewModuleReferencesFromCmHandle = cpsModuleService
                .identifyNewModuleReferences(allModuleReferencesFromCmHandle);

        final Map<String, String> newModuleNameToContentMap;
        if (identifiedNewModuleReferencesFromCmHandle.isEmpty()) {
            newModuleNameToContentMap = Collections.emptyMap();
        } else {
            newModuleNameToContentMap = dmiModelOperations.getNewYangResourcesFromDmi(yangModelCmHandle,
                    identifiedNewModuleReferencesFromCmHandle);
        }
        createSchemaSetAndAnchor(yangModelCmHandle, newModuleNameToContentMap, allModuleReferencesFromCmHandle);
    }

    private void createSchemaSetAndAnchor(final YangModelCmHandle yangModelCmHandle,
                                          final Map<String, String> newModuleNameToContentMap,
                                          final Collection<ModuleReference> allModuleReferencesFromCmHandle) {
        final String schemaSetAndAnchorName = yangModelCmHandle.getId();
        cpsModuleService.createOrUpgradeSchemaSetFromModules(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME,
                schemaSetAndAnchorName, newModuleNameToContentMap, allModuleReferencesFromCmHandle);
        cpsAdminService.createAnchor(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, schemaSetAndAnchorName,
            schemaSetAndAnchorName);
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

    private Optional<DataNode> getFirstReadyDataNodeWithModuleSeTag(final String moduleSetTag) {
        final List<DataNode> dataNodes = StringUtils.isNotBlank(moduleSetTag) ? cmHandleQueries
                .queryCmHandleDataNodesByCpsPath("//cm-handles[@module-set-tag='" + moduleSetTag + "']",
                        FetchDescendantsOption.OMIT_DESCENDANTS) : Collections.emptyList();
        return dataNodes.stream().filter(dataNode -> {
            final String cmHandleId = YangDataConverter.extractCmHandleIdFromXpath(dataNode.getXpath());
            return cmHandleQueries.cmHandleHasState(cmHandleId, CmHandleState.READY);
        }).findFirst();
    }

    private void updateCmHandleModuleSetTag(final YangModelCmHandle upgradedCmHandle) {
        final CompositeState.LockReason lockReason = upgradedCmHandle.getCompositeState().getLockReason();
        if (lockReason != null && lockReason.getLockReasonCategory() == LockReasonCategory.MODULE_UPGRADE) {
            final Map<String, Map<String, String>> dmiRegistryProperties = new HashMap<>(2);
            final Map<String, String> cmHandleProperties = new HashMap<>();
            cmHandleProperties.put("id", upgradedCmHandle.getId());
            cmHandleProperties.put("module-set-tag", lockReason.getDetails());
            dmiRegistryProperties.put("cm-handles", cmHandleProperties);
            cpsDataService.updateNodeLeaves(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, NCMP_DMI_REGISTRY_PARENT,
                    jsonObjectMapper.asJsonString(dmiRegistryProperties), OffsetDateTime.now());
        }
    }

}
