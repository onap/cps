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
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CmDataSubscriptionModelLoader extends AbstractModelLoader {

    private static final String MODEL_FILENAME = "cm-data-job-subscriptions@2025-07-16.yang";
    private static final String SCHEMASET_NAME = "cm-data-job-subscriptions";
    private static final String ANCHOR_NAME = "cm-data-job-subscriptions";
    private static final String REGISTRY_DATANODE_NAME = "dataJob";

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
    }

}
