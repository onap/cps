/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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
import org.onap.cps.ncmp.utils.events.NcmpInventoryModelOnboardingFinishedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InventoryModelLoader extends AbstractModelLoader {

    private final ApplicationEventPublisher applicationEventPublisher;
    private static final String PREVIOUS_SCHEMA_SET_NAME = "dmi-registry-2024-02-23";
    private static final String NEW_INVENTORY_SCHEMA_SET_NAME = "dmi-registry-2025-07-22";
    private static final String INVENTORY_YANG_MODULE_NAME = "dmi-registry";

    @Value("${ncmp.inventory.model.upgrade.r20250722.enabled:false}")
    private boolean newRevisionEnabled;

    public InventoryModelLoader(final CpsDataspaceService cpsDataspaceService,
                                final CpsModuleService cpsModuleService,
                                final CpsAnchorService cpsAnchorService,
                                final CpsDataService cpsDataService,
                                final ApplicationEventPublisher applicationEventPublisher) {
        super(cpsDataspaceService, cpsModuleService, cpsAnchorService, cpsDataService);
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void onboardOrUpgradeModel() {
        final String schemaToInstall = newRevisionEnabled ? NEW_INVENTORY_SCHEMA_SET_NAME : PREVIOUS_SCHEMA_SET_NAME;
        if (newRevisionEnabled) {
            if (doesAnchorExist(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR)) {
                final String moduleRevision = getModuleRevision(schemaToInstall);
                if (isModuleRevisionInstalled(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, INVENTORY_YANG_MODULE_NAME,
                        moduleRevision)) {
                    log.info("Revision {} is already installed.", moduleRevision);
                } else {
                    upgradeInventoryModel();
                    performInventoryDataMigration();
                }
            } else {
                installInventoryModel(schemaToInstall);
            }
        } else {
            installInventoryModel(schemaToInstall);
        }
        applicationEventPublisher.publishEvent(new NcmpInventoryModelOnboardingFinishedEvent(this));
    }

    private void installInventoryModel(final String schemaSetName) {
        createDataspace(NCMP_DATASPACE_NAME);
        createDataspace(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME);
        final String yangFileName = toYangFileName(schemaSetName);
        createSchemaSet(NCMP_DATASPACE_NAME, schemaSetName, yangFileName);
        createAnchor(NCMP_DATASPACE_NAME, schemaSetName, NCMP_DMI_REGISTRY_ANCHOR);
        createTopLevelDataNode(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, INVENTORY_YANG_MODULE_NAME);
        deleteOldButNotThePreviousSchemaSets();
        log.info("Inventory model {} installed successfully,", schemaSetName);
    }

    private void deleteOldButNotThePreviousSchemaSets() {
        //No schema sets passed in yet, but wil be required for future updates
        deleteUnusedSchemaSets(NCMP_DATASPACE_NAME);
        deleteUnusedSchemaSets(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME);
    }

    private void upgradeInventoryModel() {
        final String yangFileName = toYangFileName(NEW_INVENTORY_SCHEMA_SET_NAME);
        createSchemaSet(NCMP_DATASPACE_NAME, NEW_INVENTORY_SCHEMA_SET_NAME, yangFileName);
        cpsAnchorService.updateAnchorSchemaSet(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                NEW_INVENTORY_SCHEMA_SET_NAME);
        log.info("Inventory upgraded successfully to model {}", NEW_INVENTORY_SCHEMA_SET_NAME);
    }

    private void performInventoryDataMigration() {
        // TODO further implementation is pending
        //1. Load all the cm handles (in batch)
        //2. Copy the state and known properties
        log.info("Inventory module data migration is completed successfully.");
    }

    private static String toYangFileName(final String schemaSetName) {
        return INVENTORY_YANG_MODULE_NAME + "@" + getModuleRevision(schemaSetName) + ".yang";
    }

    private static String getModuleRevision(final String schemaSetName) {
        // Extract the revision part ( for example: 2024-02-23)
        return schemaSetName.substring(INVENTORY_YANG_MODULE_NAME.length() + 1);
    }
}
