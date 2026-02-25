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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Order(2)
public class InventoryModelLoader extends AbstractModelLoader {

    private final DataMigration dataMigration;
    private final ApplicationEventPublisher applicationEventPublisher;

    private static final String CURRENT_SCHEMA_SET_NAME = "dmi-registry-2026-01-28";
    private static final String FUTURE_SCHEMA_SET_NAME = "dmi-registry-2025-07-22";
    private static final String INVENTORY_YANG_MODULE_NAME = "dmi-registry";
    private static final int MIGRATION_BATCH_SIZE = 300;

    @Value("${ignore.r20250722.model:true}")
    private boolean ignoreModelR20250722;

    /**
     * Creates a new {@code InventoryModelLoader} instance responsible for onboarding or upgrading
     * the NCMP inventory model schema sets and managing readiness state during migration.
     */
    public InventoryModelLoader(final ModelLoaderLock modelLoaderLock,
                                final CpsServicesBundle cpsServicesBundle,
                                final ApplicationEventPublisher applicationEventPublisher,
                                final ReadinessManager readinessManager,
                                final DataMigration dataMigration) {
        super(modelLoaderLock,
                cpsServicesBundle.getDataspaceService(),
                cpsServicesBundle.getModuleService(),
                cpsServicesBundle.getAnchorService(),
                cpsServicesBundle.getDataService(),
            readinessManager);
        this.applicationEventPublisher = applicationEventPublisher;
        this.dataMigration = dataMigration;
    }

    @Override
    public void onboardOrUpgradeModel() {
        if (isMaster) {
            log.info("Model Loader #2 Started: NCMP Inventory Models");
            final String schemaToInstall = ignoreModelR20250722 ? CURRENT_SCHEMA_SET_NAME : FUTURE_SCHEMA_SET_NAME;
            final String moduleRevision = getModuleRevision(schemaToInstall);
            log.info("Model Loader #2 Schema Set: {}", schemaToInstall);

            if (isModuleRevisionInstalled(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, INVENTORY_YANG_MODULE_NAME,
                moduleRevision)) {
                log.info("Model Loader #2: Revision {} is already installed.", moduleRevision);
            } else if (!ignoreModelR20250722 && doesAnchorExist(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR)) {
                log.info("Model Loader #2: Upgrading to revision {} and migrating data", moduleRevision);
                upgradeAndMigrateInventoryModel();
            } else if (doesAnchorExist(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR)) {
                log.info("Model Loader #2: Upgrading already installed inventory to revision {}.", moduleRevision);
                upgradeInventoryModel(schemaToInstall);
            } else {
                log.info("Model Loader #2: New installation using inventory model revision {}.", moduleRevision);
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
        final String yangFileName = toYangFileName(CURRENT_SCHEMA_SET_NAME);
        createSchemaSet(NCMP_DATASPACE_NAME, CURRENT_SCHEMA_SET_NAME, yangFileName);
        createAnchor(NCMP_DATASPACE_NAME, CURRENT_SCHEMA_SET_NAME, NCMP_DMI_REGISTRY_ANCHOR);
        createTopLevelDataNode(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, INVENTORY_YANG_MODULE_NAME);
        log.info("Model Loader #2: Inventory model {} installed successfully,", CURRENT_SCHEMA_SET_NAME);
    }

    private void deleteOldButNotThePreviousSchemaSets(final String currentSchemaSetName,
                                                              final String previousSchemaSetName) {
        deleteUnusedSchemaSets(NCMP_DATASPACE_NAME, currentSchemaSetName, previousSchemaSetName);
    }

    private void upgradeAndMigrateInventoryModel() {
        upgradeInventoryModel(FUTURE_SCHEMA_SET_NAME);
        dataMigration.migrateInventoryToModelRelease20250722(MIGRATION_BATCH_SIZE);
    }

    private void upgradeInventoryModel(final String schemaSetName) {
        final String previousSchemaSetName = cpsAnchorService.getAnchor(NCMP_DATASPACE_NAME,
                NCMP_DMI_REGISTRY_ANCHOR).getSchemaSetName();
        if (previousSchemaSetName.equals(schemaSetName)) {
            log.info("Model Loader #2: Anchor already points to {}, skipping upgrade", schemaSetName);
            return;
        }
        final String yangFileName = toYangFileName(schemaSetName);
        createSchemaSet(NCMP_DATASPACE_NAME, schemaSetName, yangFileName);
        cpsAnchorService.updateAnchorSchemaSet(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                schemaSetName);
        log.info("Model Loader #2: Inventory upgraded successfully to model {}", schemaSetName);
        deleteOldButNotThePreviousSchemaSets(schemaSetName, previousSchemaSetName);
    }

    private static String toYangFileName(final String schemaSetName) {
        return INVENTORY_YANG_MODULE_NAME + "@" + getModuleRevision(schemaSetName) + ".yang";
    }

    private static String getModuleRevision(final String schemaSetName) {
        // Extract the revision part ( for example: 2024-02-23)
        return schemaSetName.substring(INVENTORY_YANG_MODULE_NAME.length() + 1);
    }

}
