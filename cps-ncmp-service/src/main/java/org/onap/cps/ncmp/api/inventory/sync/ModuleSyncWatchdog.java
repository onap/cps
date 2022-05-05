/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.ncmp.api.inventory.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ModuleSyncWatchdog {

    private final SyncUtils syncUtils;

    private final ModuleSyncService moduleSyncService;

    /**
     * Execute Cm Handle poll which changes the cm handle state from 'ADVISED' to 'READY'.
     */
    @Scheduled(fixedDelay = 30000)
    public void executeAdvisedCmHandlePoll() {
        YangModelCmHandle newAdvisedCmHandle = syncUtils.getAnAdvisedCmHandle();
        while (newAdvisedCmHandle != null) {
            final CmHandleState cmHandleState = newAdvisedCmHandle.getCmHandleState();
            moduleSyncService.syncAndCreateSchemaSet(newAdvisedCmHandle);
            // ToDo Lock Cm Handle if cm handle discovery and sync is failed
            syncUtils.updateCmHandleState(newAdvisedCmHandle, cmHandleState.ready());
            log.info("{} is now in {} state", newAdvisedCmHandle.getId(), newAdvisedCmHandle.getCmHandleState());
            newAdvisedCmHandle = syncUtils.getAnAdvisedCmHandle();
        }
        log.debug("No Cm-Handles currently found in an ADVISED state");
    }

}
