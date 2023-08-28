/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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

import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsModuleService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InventoryModelLoader extends AbstractModelLoader {

    private static final String NEW_INVENTORY_MODEL_NAME = "dmi-registry@2023-08-23.yang";
    private static final String INVENTORY_DATASPACE_NAME = "NCMP-Admin";
    private static final String INVENTORY_ANCHOR_NAME = "ncmp-dmi-registry";

    public InventoryModelLoader(final CpsAdminService cpsAdminService,
                                final CpsModuleService cpsModuleService,
                                final CpsDataService cpsDataService) {
        super(cpsAdminService, cpsModuleService, cpsDataService);
    }

    @Override
    public void onboardOrUpgradeModel() {
        waitUntilDataspaceIsAvailable(INVENTORY_DATASPACE_NAME);
        updateInventoryModel();
    }

    private void updateInventoryModel() {
        createSchemaSet(INVENTORY_DATASPACE_NAME, NEW_INVENTORY_MODEL_NAME, NEW_INVENTORY_MODEL_NAME);
        updateAnchorSchemaSet(INVENTORY_DATASPACE_NAME, INVENTORY_ANCHOR_NAME, NEW_INVENTORY_MODEL_NAME);
        deleteOldButNotThePreviousSchemaSets();
    }

    private void deleteOldButNotThePreviousSchemaSets() {
        //Not schema sets passed in yet, but wil be required for future updates
        deleteUnusedSchemaSets(INVENTORY_DATASPACE_NAME);
    }

}
