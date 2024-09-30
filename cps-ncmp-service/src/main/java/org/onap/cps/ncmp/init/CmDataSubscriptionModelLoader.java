/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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
import static org.onap.cps.utils.ContentType.JSON;

import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsDataspaceService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.api.exceptions.AlreadyDefinedException;
import org.onap.cps.init.AbstractModelLoader;
import org.onap.cps.ncmp.api.data.models.DatastoreType;
import org.onap.cps.ncmp.exceptions.NcmpStartUpException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CmDataSubscriptionModelLoader extends AbstractModelLoader {

    private static final String MODEL_FILENAME = "cm-data-subscriptions@2024-02-12.yang";
    private static final String SCHEMASET_NAME = "cm-data-subscriptions";
    private static final String ANCHOR_NAME = "cm-data-subscriptions";
    private static final String REGISTRY_DATANODE_NAME = "datastores";

    public CmDataSubscriptionModelLoader(final CpsDataspaceService cpsDataspaceService,
            final CpsModuleService cpsModuleService, final CpsAnchorService cpsAnchorService,
            final CpsDataService cpsDataService) {
        super(cpsDataspaceService, cpsModuleService, cpsAnchorService, cpsDataService);
    }

    @Override
    public void onboardOrUpgradeModel() {
        onboardSubscriptionModels();
        log.info("Subscription Models onboarded successfully");
    }

    private void onboardSubscriptionModels() {
        createDataspace(NCMP_DATASPACE_NAME);
        createSchemaSet(NCMP_DATASPACE_NAME, SCHEMASET_NAME, MODEL_FILENAME);
        createAnchor(NCMP_DATASPACE_NAME, SCHEMASET_NAME, ANCHOR_NAME);
        createTopLevelDataNode(NCMP_DATASPACE_NAME, ANCHOR_NAME, REGISTRY_DATANODE_NAME);
        createDatastore(DatastoreType.PASSTHROUGH_OPERATIONAL.getDatastoreName(),
                DatastoreType.PASSTHROUGH_RUNNING.getDatastoreName());
    }

    private void createDatastore(final String... datastoreNames) {
        for (final String datastoreName : datastoreNames) {
            final String nodeData = "{\"datastore\":[{\"name\":\"" + datastoreName + "\",\"cm-handles\":{}}]}";
            try {
                cpsDataService.saveData(NCMP_DATASPACE_NAME, ANCHOR_NAME, "/" + REGISTRY_DATANODE_NAME, nodeData,
                        OffsetDateTime.now(), JSON);
            } catch (final AlreadyDefinedException exception) {
                log.info("Creating new child data node '{}' for data node '{}' failed as data node already exists",
                        datastoreName, REGISTRY_DATANODE_NAME);
            } catch (final Exception exception) {
                log.error("Creating data node failed: {}", exception.getMessage());
                throw new NcmpStartUpException("Creating data node failed", exception.getMessage());
            }
        }
    }

}
