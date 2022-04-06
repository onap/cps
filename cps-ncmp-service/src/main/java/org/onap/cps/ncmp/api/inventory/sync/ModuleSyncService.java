/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.ncmp.api.impl.operations.DmiModelOperations;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.spi.model.ModuleReference;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModuleSyncService {

    private final DmiModelOperations dmiModelOperations;
    private final CpsModuleService cpsModuleService;

    /**
     * This method registers a cm handle and initiates modules sync.
     *
     * @param yangModelCmHandle the yang model of cm handle.
     * @return schemaSetName the name of the schema set (same as cm handle name).
     */
    public String syncAndCreateSchemaSet(final YangModelCmHandle yangModelCmHandle) {

        final Collection<ModuleReference> moduleReferencesFromCmHandle =
                dmiModelOperations.getModuleReferences(yangModelCmHandle);

        final Collection<ModuleReference> identifiedNewModuleReferencesFromCmHandle = cpsModuleService
                .identifyNewModuleReferences(moduleReferencesFromCmHandle);

        final Collection<ModuleReference> existingModuleReferencesFromCmHandle =
                moduleReferencesFromCmHandle.stream().filter(moduleReferenceFromCmHandle ->
                        !identifiedNewModuleReferencesFromCmHandle.contains(moduleReferenceFromCmHandle)
                ).collect(Collectors.toList());

        final Map<String, String> newModuleNameToContentMap;
        if (identifiedNewModuleReferencesFromCmHandle.isEmpty()) {
            newModuleNameToContentMap = new HashMap<>();
        } else {
            newModuleNameToContentMap = dmiModelOperations.getNewYangResourcesFromDmi(yangModelCmHandle,
                    identifiedNewModuleReferencesFromCmHandle);
        }
        return createSchemaSet(yangModelCmHandle, existingModuleReferencesFromCmHandle, newModuleNameToContentMap);
    }

    private String createSchemaSet(final YangModelCmHandle yangModelCmHandle,
                                 final Collection<ModuleReference> existingModuleReferencesFromCmHandle,
                                 final Map<String, String> newModuleNameToContentMap) {
        final String schemaSetName = yangModelCmHandle.getId();
        cpsModuleService
                .createSchemaSetFromModules(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, schemaSetName,
                        newModuleNameToContentMap, existingModuleReferencesFromCmHandle);
        return schemaSetName;
    }

}
