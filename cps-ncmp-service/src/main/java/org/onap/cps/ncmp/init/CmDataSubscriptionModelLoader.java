/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsDataspaceService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.init.AbstractModelLoader;
import org.onap.cps.init.ModelLoaderCoordinatorLock;
import org.onap.cps.init.actuator.ReadinessManager;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Order(3)
public class CmDataSubscriptionModelLoader extends AbstractModelLoader {

    private static final String MODEL_FILE_NAME = "cm-data-job-subscriptions@2025-09-03.yang";
    private static final String SCHEMA_SET_NAME = "cm-data-job-subscriptions";
    private static final String ANCHOR_NAME = "cm-data-job-subscriptions";
    private static final String REGISTRY_DATA_NODE_NAME = "dataJob";

    public CmDataSubscriptionModelLoader(final ModelLoaderCoordinatorLock modelLoaderCoordinatorLock,
                                         final CpsDataspaceService cpsDataspaceService,
                                         final CpsModuleService cpsModuleService,
                                         final CpsAnchorService cpsAnchorService,
                                         final CpsDataService cpsDataService,
                                         final ReadinessManager readinessManager) {
        super(modelLoaderCoordinatorLock, cpsDataspaceService, cpsModuleService, cpsAnchorService, cpsDataService,
            readinessManager);
    }

    @Override
    public void onboardOrUpgradeModel() {
        if (isMaster) {
            log.info("Model Loader #3 Started: NCMP CM Data Notification Subscription Models");
            onboardSubscriptionModels();
            log.info("Model Loader #3 Completed");
        } else {
            logMessageForNonMasterInstance();
        }
    }

    private void onboardSubscriptionModels() {
        createSchemaSet(NCMP_DATASPACE_NAME, SCHEMA_SET_NAME, MODEL_FILE_NAME);
        createAnchor(NCMP_DATASPACE_NAME, SCHEMA_SET_NAME, ANCHOR_NAME);
        createTopLevelDataNode(NCMP_DATASPACE_NAME, ANCHOR_NAME, REGISTRY_DATA_NODE_NAME);
        log.info("NCMP CM Data Notification Subscription Models onboarded successfully");
    }

}
