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
import org.onap.cps.spi.exceptions.AlreadyDefinedException;
import org.onap.cps.spi.exceptions.CpsStartupException;
import org.onap.cps.spi.model.Dataspace;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Collection;

import static org.onap.cps.utils.ContentType.JSON;

@Slf4j
@Service
public class CpsNotificationSubscriptionModuleLoader extends AbstractCpsModuleLoader {

    private static final String MODEL_FILENAME = "cps-notification-subscriptions@2024-07-03.yang";
    private static final String SCHEMASET_NAME = "cps-notification-subscriptions";
    private static final String ANCHOR_NAME = "cps-notification-subscriptions";
    private static final String CPS_DATASPACE_NAME = "CPS-Admin";

    private static final String REGISTRY_DATANODE_NAME = "dataspaces";

    public CpsNotificationSubscriptionModuleLoader(CpsDataspaceService cpsDataspaceService,
                                                   CpsModuleService cpsModuleService, CpsAnchorService cpsAnchorService,
                                                   CpsDataService cpsDataService) {
        super(cpsDataspaceService, cpsModuleService, cpsAnchorService, cpsDataService);
    }

    @Override
    public void onboardOrUpgradeModel() {

        onboardSubscriptionModels();
        log.info("Subscription Models onboarded successfully");
    }

    private void onboardSubscriptionModels() {
        createDataspace(CPS_DATASPACE_NAME);
        createSchemaSet(CPS_DATASPACE_NAME, SCHEMASET_NAME, MODEL_FILENAME);
        createAnchor(CPS_DATASPACE_NAME, SCHEMASET_NAME, ANCHOR_NAME);
        createTopLevelDataNode(CPS_DATASPACE_NAME, ANCHOR_NAME, REGISTRY_DATANODE_NAME);
        createInitialSubscription(CPS_DATASPACE_NAME, ANCHOR_NAME);
    }

    private void createInitialSubscription(final String dataspaceName, final String anchorName) {
        Collection<Dataspace> dataspaces = cpsDataspaceService.getAllDataspaces();
        for (Dataspace ds : dataspaces) {
            subscribeNotificationForDataspace(ds);
        }
    }

    private void subscribeNotificationForDataspace(Dataspace ds) {
        final String nodeData = "{\"dataspace\":[{\"name\":\""+ds.getName()+"\"}]}";
        try {
            cpsDataService.saveData(CPS_DATASPACE_NAME, ANCHOR_NAME, "/" + REGISTRY_DATANODE_NAME, nodeData,
                    OffsetDateTime.now(), JSON);
        } catch (final AlreadyDefinedException exception) {
            log.info("Creating new child data node '{}' for data node '{}' failed as data node already exists",
                    ds.getName(), REGISTRY_DATANODE_NAME);
        } catch (final Exception exception) {
            log.error("Creating data node failed: {}", exception.getMessage());
            throw new CpsStartupException("Creating data node failed", exception.getMessage());
        }
    }
}
