/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation
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

package org.onap.cps.ncmp.init;

import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DATASPACE_NAME;
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DMI_REGISTRY_ANCHOR;
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME;

import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsDataspaceService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.init.AbstractModelLoader;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InventoryModelLoader extends AbstractModelLoader {
    private static final String NEW_MODEL_FILE_NAME = "dmi-registry@2024-02-23.yang";
    private static final String NEW_SCHEMA_SET_NAME = "dmi-registry-2024-02-23";
    private static final String REGISTRY_DATANODE_NAME = "dmi-registry";

    public InventoryModelLoader(final CpsDataspaceService cpsDataspaceService,
                                final CpsModuleService cpsModuleService,
                                final CpsAnchorService cpsAnchorService,
                                final CpsDataService cpsDataService) {
        super(cpsDataspaceService, cpsModuleService, cpsAnchorService, cpsDataService);
    }

    @Override
    public void onboardOrUpgradeModel() {
        updateInventoryModel();
        log.info("Inventory Model updated successfully");
    }

    private void updateInventoryModel() {
        createDataspace(NCMP_DATASPACE_NAME);
        createDataspace(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME);
        createSchemaSet(NCMP_DATASPACE_NAME, NEW_SCHEMA_SET_NAME, NEW_MODEL_FILE_NAME);
        createAnchor(NCMP_DATASPACE_NAME, NEW_SCHEMA_SET_NAME, NCMP_DMI_REGISTRY_ANCHOR);
        updateAnchorSchemaSet(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, NEW_SCHEMA_SET_NAME);
        createTopLevelDataNode(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, REGISTRY_DATANODE_NAME);
        deleteOldButNotThePreviousSchemaSets();
    }

    private void deleteOldButNotThePreviousSchemaSets() {
        //No schema sets passed in yet, but wil be required for future updates
        deleteUnusedSchemaSets(NCMP_DATASPACE_NAME);
        deleteUnusedSchemaSets(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME);
    }

}
