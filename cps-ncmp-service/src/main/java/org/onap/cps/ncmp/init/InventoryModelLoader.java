/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2026 OpenInfra Foundation Europe. All rights reserved.
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
import org.onap.cps.impl.CpsServicesBundle;
import org.onap.cps.init.AbstractModelLoader;
import org.onap.cps.init.ModelLoaderLock;
import org.onap.cps.init.actuator.ReadinessManager;
import org.onap.cps.ncmp.utils.events.NcmpInventoryModelOnboardingFinishedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Order(2)
public class InventoryModelLoader extends AbstractModelLoader {

    private final ApplicationEventPublisher applicationEventPublisher;

    private static final String PREVIOUS_SCHEMA_SET_NAME = "dmi-registry-2024-02-23";
    private static final String CURRENT_SCHEMA_SET_NAME = "dmi-registry-2026-01-28";
    private static final String INVENTORY_YANG_MODULE_NAME = "dmi-registry";

    /**
     * Creates a new {@code InventoryModelLoader} instance responsible for onboarding or upgrading
     * the NCMP inventory model schema sets and managing readiness state during migration.
     */
    public InventoryModelLoader(final ModelLoaderLock modelLoaderLock,
                                final CpsServicesBundle cpsServicesBundle,
                                final ApplicationEventPublisher applicationEventPublisher,
                                final ReadinessManager readinessManager) {
        super(modelLoaderLock,
                cpsServicesBundle.getDataspaceService(),
                cpsServicesBundle.getModuleService(),
                cpsServicesBundle.getAnchorService(),
                cpsServicesBundle.getDataService(),
            readinessManager);
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void onboardOrUpgradeModel() {
        if (isMaster) {
            log.info("Model Loader #2 Started: NCMP Inventory Models");
            if (isModuleRevisionInstalled(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, INVENTORY_YANG_MODULE_NAME,
                    CURRENT_SCHEMA_SET_NAME)) {
                log.info("Model Loader #2: Revision {} is already installed.", CURRENT_SCHEMA_SET_NAME);
            } else if (doesAnchorExist(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR)) {
                log.info("Model Loader #2: Upgrading already installed inventory to revision {}.",
                        CURRENT_SCHEMA_SET_NAME);
                upgradeInventoryModel();
            } else {
                log.info("Model Loader #2: New installation using inventory model revision {}.",
                        CURRENT_SCHEMA_SET_NAME);
                installInventoryModel();
            }
            applicationEventPublisher.publishEvent(new NcmpInventoryModelOnboardingFinishedEvent(this));
            log.info("Model Loader #2 Completed");
        } else {
            logMessageForNonMasterInstance();
        }
    }

    private void installInventoryModel() {
        createDataspace(NCMP_DATASPACE_NAME);
        createDataspace(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME);
        final String yangFileName = toYangFileName();
        createSchemaSet(NCMP_DATASPACE_NAME, CURRENT_SCHEMA_SET_NAME, yangFileName);
        createAnchor(NCMP_DATASPACE_NAME, CURRENT_SCHEMA_SET_NAME, NCMP_DMI_REGISTRY_ANCHOR);
        createTopLevelDataNode(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, INVENTORY_YANG_MODULE_NAME);
        deleteOldButNotThePreviousSchemaSets();
        log.info("Model Loader #2: Inventory model {} installed successfully,", CURRENT_SCHEMA_SET_NAME);
    }

    private void deleteOldButNotThePreviousSchemaSets() {
        deleteUnusedSchemaSets(NCMP_DATASPACE_NAME, CURRENT_SCHEMA_SET_NAME, PREVIOUS_SCHEMA_SET_NAME);
    }

    private void upgradeInventoryModel() {
        final String yangFileName = toYangFileName();
        createSchemaSet(NCMP_DATASPACE_NAME, CURRENT_SCHEMA_SET_NAME, yangFileName);
        cpsAnchorService.updateAnchorSchemaSet(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                CURRENT_SCHEMA_SET_NAME);
        log.info("Model Loader #2: Inventory upgraded successfully to model {}", CURRENT_SCHEMA_SET_NAME);
        deleteOldButNotThePreviousSchemaSets();
    }

    private static String toYangFileName() {
        return INVENTORY_YANG_MODULE_NAME + "@" + getModuleRevision() + ".yang";
    }

    private static String getModuleRevision() {
        // Extract the revision part ( for example: 2024-02-23)
        return CURRENT_SCHEMA_SET_NAME.substring(INVENTORY_YANG_MODULE_NAME.length() + 1);
    }

}
