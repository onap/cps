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

import static org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence.NCMP_DATASPACE_NAME;
import static org.onap.cps.ncmp.init.inventory.SubscriptionModels.CURRENT_SUBSCRIPTION_MODEL;
import static org.onap.cps.ncmp.init.inventory.SubscriptionModels.PREVIOUS_SUBSCRIPTION_MODEL;

import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.ncmp.init.inventory.SubscriptionModels;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SubscriptionModelLoader extends AbstractModelLoader {

    private static final String ANCHOR_NAME = "AVC-Subscriptions";
    private static final String SCHEMASET_NAME = "subscriptions";

    public SubscriptionModelLoader(final CpsAdminService cpsAdminService,
                                   final CpsModuleService cpsModuleService,
                                   final CpsDataService cpsDataService) {
        super(cpsAdminService, cpsModuleService, cpsDataService);
    }

    @Value("${ncmp.model-loader.subscription:true}")
    private boolean subscriptionModelLoaderEnabled;

    @Override
    public void onboardOrUpgradeModel() {
        if (subscriptionModelLoaderEnabled) {
            waitUntilDataspaceIsAvailable(NCMP_DATASPACE_NAME);
            onboardSubscriptionModels(CURRENT_SUBSCRIPTION_MODEL, PREVIOUS_SUBSCRIPTION_MODEL);
            log.info("Subscription Models onboarded successfully");
        } else {
            log.info("Subscription Model Loader is disabled");
        }
    }

    private void onboardSubscriptionModels(final SubscriptionModels... subscriptionModels) {
        for (final SubscriptionModels subscriptionModel : subscriptionModels) {
            final String modelFileName = subscriptionModel.getModelFileName();
            createSchemaSet(NCMP_DATASPACE_NAME, SCHEMASET_NAME, modelFileName);
        }
        createAnchor(NCMP_DATASPACE_NAME, SCHEMASET_NAME, ANCHOR_NAME);
        for (final SubscriptionModels subscriptionModel : subscriptionModels) {
            final String modelTopRegistryDataNodeName = subscriptionModel.getTopLevelRegistryDatanodeName();
            createTopLevelDataNode(NCMP_DATASPACE_NAME, ANCHOR_NAME, modelTopRegistryDataNodeName);
        }
    }
}
