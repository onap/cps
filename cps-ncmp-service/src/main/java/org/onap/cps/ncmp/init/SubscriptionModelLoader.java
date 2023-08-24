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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component

public class SubscriptionModelLoader extends AbstractModelLoader {

    private static final String SUBSCRIPTION_MODEL_FILENAME = "subscription.yang";
    private static final String SUBSCRIPTION_DATASPACE_NAME = "NCMP-Admin";
    private static final String SUBSCRIPTION_ANCHOR_NAME = "AVC-Subscriptions";
    private static final String SUBSCRIPTION_SCHEMASET_NAME = "subscriptions";
    private static final String SUBSCRIPTION_REGISTRY_DATANODE_NAME = "subscription-registry";

    public SubscriptionModelLoader(final CpsAdminService cpsAdminService,
                                   final CpsModuleService cpsModuleService,
                                   final CpsDataService cpsDataService) {
        super(cpsAdminService, cpsModuleService, cpsDataService);
    }

    @Value("${ncmp.model-loader.maximum-attempt-count:20}")
    private int maximumAttemptCount;

    @Value("${ncmp.timers.model-loader.retry-time-ms:1000}")
    private long retryTimeMs;

    @Value("${ncmp.model-loader.subscription:true}")
    private boolean subscriptionModelLoaderEnabled;

    @Override
    public void onboardOrUpgradeModel() {
        if (subscriptionModelLoaderEnabled) {
            waitUntilDataspaceIsAvailable(SUBSCRIPTION_DATASPACE_NAME);
            onboardSubscriptionModel();
        } else {
            log.info("Subscription Model Loader is disabled");
        }
    }

    private void onboardSubscriptionModel() {
        createSchemaSet(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_SCHEMASET_NAME, SUBSCRIPTION_MODEL_FILENAME);
        createAnchor(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_SCHEMASET_NAME, SUBSCRIPTION_ANCHOR_NAME);
        createTopLevelDataNode(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_ANCHOR_NAME,
            SUBSCRIPTION_REGISTRY_DATANODE_NAME);
    }

}
