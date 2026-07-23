/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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
import org.onap.cps.impl.CpsServicesBundle;
import org.onap.cps.init.actuator.ReadinessManager;

@Slf4j
public abstract class AbstractSubscriptionModelLoader extends AbstractModelLoader {

    private final int modelLoaderSequenceNumber;
    private final String modelDescription;

    protected AbstractSubscriptionModelLoader(final ModelLoaderLock modelLoaderLock,
                                              final CpsServicesBundle cpsServicesBundle,
                                              final ReadinessManager readinessManager,
                                              final int modelLoaderSequenceNumber,
                                              final String modelDescription) {
        super(modelLoaderLock,
            cpsServicesBundle.getDataspaceService(),
            cpsServicesBundle.getModuleService(),
            cpsServicesBundle.getAnchorService(),
            cpsServicesBundle.getDataService(),
            readinessManager);
        this.modelLoaderSequenceNumber = modelLoaderSequenceNumber;
        this.modelDescription = modelDescription;
    }

    @Override
    public void onboardOrUpgradeModel() {
        if (isMaster) {
            log.info("Model Loader #{} Started: {}", modelLoaderSequenceNumber, modelDescription);
            onboardSubscriptionModels();
            log.info("Model Loader #{} Completed", modelLoaderSequenceNumber);
        } else {
            logMessageForNonMasterInstance();
        }
    }

    /**
     * Onboards the subscription models (dataspace, schema set, anchor and top level data node)
     * specific to the concrete loader.
     */
    protected abstract void onboardSubscriptionModels();

}
