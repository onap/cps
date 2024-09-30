/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 TechMahindra Ltd.
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

package org.onap.cps.init;

import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsDataspaceService;
import org.onap.cps.api.CpsModuleService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CpsNotificationSubscriptionModelLoader extends AbstractModelLoader {

    private static final String MODEL_FILENAME = "cps-notification-subscriptions@2024-07-03.yang";
    private static final String SCHEMASET_NAME = "cps-notification-subscriptions";
    private static final String ANCHOR_NAME = "cps-notification-subscriptions";
    private static final String CPS_DATASPACE_NAME = "CPS-Admin";
    private static final String REGISTRY_DATANODE_NAME = "dataspaces";

    public CpsNotificationSubscriptionModelLoader(final CpsDataspaceService cpsDataspaceService,
                                                  final CpsModuleService cpsModuleService,
                                                  final CpsAnchorService cpsAnchorService,
                                                  final CpsDataService cpsDataService) {
        super(cpsDataspaceService, cpsModuleService, cpsAnchorService, cpsDataService);
    }

    @Override
    public void onboardOrUpgradeModel() {
        onboardSubscriptionModels();
        log.info("Subscription models onboarded successfully");
    }

    private void onboardSubscriptionModels() {
        createDataspace(CPS_DATASPACE_NAME);
        createSchemaSet(CPS_DATASPACE_NAME, SCHEMASET_NAME, MODEL_FILENAME);
        createAnchor(CPS_DATASPACE_NAME, SCHEMASET_NAME, ANCHOR_NAME);
        createTopLevelDataNode(CPS_DATASPACE_NAME, ANCHOR_NAME, REGISTRY_DATANODE_NAME);
    }

}
