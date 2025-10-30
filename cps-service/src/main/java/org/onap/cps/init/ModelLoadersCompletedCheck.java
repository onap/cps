/*
 * ============LICENSE_START========================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

import static org.springframework.core.Ordered.LOWEST_PRECEDENCE;

import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsDataspaceService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.init.actuator.ReadinessManager;
import org.onap.cps.utils.Sleeper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Order(LOWEST_PRECEDENCE)
public class ModelLoadersCompletedCheck extends AbstractModelLoader {

    final Sleeper sleeper;

    /**
     * Constructor.
     *
     * @param modelLoaderLock the modelLoaderCoordinatorLock
     * @param cpsDataspaceService the cpsDataspaceService
     * @param cpsModuleService the cpsModuleService
     * @param cpsAnchorService the cpsAnchorService
     * @param cpsDataService the cpsDataService
     * @param readinessManager the readinessManager
     * @param sleeper the sleeper
     */
    public ModelLoadersCompletedCheck(final ModelLoaderLock modelLoaderLock,
                                      final CpsDataspaceService cpsDataspaceService,
                                      final CpsModuleService cpsModuleService,
                                      final CpsAnchorService cpsAnchorService,
                                      final CpsDataService cpsDataService,
                                      final ReadinessManager readinessManager,
                                      final Sleeper sleeper) {
        super(modelLoaderLock, cpsDataspaceService, cpsModuleService, cpsAnchorService, cpsDataService,
            readinessManager);
        this.sleeper = sleeper;
    }

    @Override
    public void onboardOrUpgradeModel() {
        log.info("Model Loader #LAST Started: Completion Check");
        if (isMaster) {
            releaseLock();
            log.info("Model Loader #LAST: This instance is model loader master. Model loading completed");
        } else {
            log.info("Model Loader #LAST: Wait for model loading on master to finish");
            waitForMasterToFinish();
        }
        log.info("Model Loader #LAST Completed");
    }

    private void releaseLock() {
        modelLoaderLock.forceUnlock();
        log.info("Model Loader #LAST: Model loading (on master) finished");
    }

    private void waitForMasterToFinish() {
        while (modelLoaderLock.isLocked()) {
            log.info("Model Loader #LAST: Still waiting for model loading on master to finish");
            try {
                sleeper.haveALittleRest(100);
            } catch (final InterruptedException e) {
                log.warn("I cannot sleep (ignored)");
                Thread.currentThread().interrupt();
            }
        }
    }

}
