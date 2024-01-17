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
import static org.onap.cps.utils.ContentType.JSON;

import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsDataspaceService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.ncmp.api.impl.exception.NcmpStartUpException;
import org.onap.cps.spi.exceptions.AlreadyDefinedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CmDataSubscriptionModelLoader extends AbstractModelLoader {

    private static final String MODEL_FILENAME = "cm-data-subscriptions@2023-11-13.yang";
    private static final String SCHEMASET_NAME = "cm-data-subscriptions";
    private static final String ANCHOR_NAME = "cm-data-subscriptions";
    private static final String REGISTRY_DATANODE_NAME = "datastores";
    private static final String DATASTORE_PASSTHROUGH_OPERATIONAL = "ncmp-datastores:passthrough-operational";
    private static final String DATASTORE_PASSTHROUGH_RUNNING = "ncmp-datastores:passthrough-running";

    private static final String DEPRECATED_MODEL_FILENAME = "subscription.yang";
    private static final String DEPRECATED_ANCHOR_NAME = "AVC-Subscriptions";
    private static final String DEPRECATED_SCHEMASET_NAME = "subscriptions";
    private static final String DEPRECATED_REGISTRY_DATANODE_NAME = "subscription-registry";



    public CmDataSubscriptionModelLoader(final CpsDataspaceService cpsDataspaceService,
                                         final CpsModuleService cpsModuleService,
                                         final CpsAnchorService cpsAnchorService,
                                         final CpsDataService cpsDataService) {
        super(cpsDataspaceService, cpsModuleService, cpsAnchorService, cpsDataService);
    }

    @Value("${ncmp.model-loader.subscription:true}")
    private boolean subscriptionModelLoaderEnabled;

    @Override
    public void onboardOrUpgradeModel() {
        if (subscriptionModelLoaderEnabled) {
            waitUntilDataspaceIsAvailable(NCMP_DATASPACE_NAME);
            onboardSubscriptionModels();
            log.info("Subscription Models onboarded successfully");
        } else {
            log.info("Subscription Model Loader is disabled");
        }
    }

    private void onboardSubscriptionModels() {
        createSchemaSet(NCMP_DATASPACE_NAME, DEPRECATED_SCHEMASET_NAME, DEPRECATED_MODEL_FILENAME);
        createAnchor(NCMP_DATASPACE_NAME, DEPRECATED_SCHEMASET_NAME, DEPRECATED_ANCHOR_NAME);
        createTopLevelDataNode(NCMP_DATASPACE_NAME, DEPRECATED_ANCHOR_NAME, DEPRECATED_REGISTRY_DATANODE_NAME);

        createSchemaSet(NCMP_DATASPACE_NAME, SCHEMASET_NAME, MODEL_FILENAME);
        createAnchor(NCMP_DATASPACE_NAME, SCHEMASET_NAME, ANCHOR_NAME);
        createTopLevelDataNode(NCMP_DATASPACE_NAME, ANCHOR_NAME, REGISTRY_DATANODE_NAME);
        createDatastore(DATASTORE_PASSTHROUGH_OPERATIONAL, DATASTORE_PASSTHROUGH_RUNNING);
    }

    private void createDatastore(final String... datastoreNames) {
        for (final String datastoreName : datastoreNames) {
            final String nodeData = "{\"datastore\":[{\"name\":\"" + datastoreName + "\",\"cm-handles\":{}}]}";
            try {
                cpsDataService.saveData(NCMP_DATASPACE_NAME, ANCHOR_NAME, "/" + REGISTRY_DATANODE_NAME, nodeData,
                        OffsetDateTime.now(), JSON);
            } catch (final AlreadyDefinedException exception) {
                log.warn("Creating new child data node '{}' for data node '{}' failed as data node already exists",
                        datastoreName, REGISTRY_DATANODE_NAME);
            } catch (final Exception exception) {
                log.error("Creating data node failed: {}", exception.getMessage());
                throw new NcmpStartUpException("Creating data node failed", exception.getMessage());
            }
        }
    }

}
