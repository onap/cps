/*
 *  ============LICENSE_START=======================================================
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
import org.onap.cps.ncmp.api.inventory.CmHandleState;
import org.onap.cps.ncmp.api.inventory.LockReasonEnum;
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
    @Scheduled(fixedDelayString = "${timers.advised-modules-sync.sleep-time-ms}")
    public void executeAdvisedCmHandlePoll() {
        YangModelCmHandle advisedCmHandle = syncUtils.getAnAdvisedCmHandle();
        try {
            while (advisedCmHandle != null) {
                moduleSyncService.syncAndCreateSchemaSet(advisedCmHandle);
                syncUtils.updateCmHandleState(advisedCmHandle, CmHandleState.READY);
                log.info("{} is now in {} state", advisedCmHandle.getId(),
                    advisedCmHandle.getCompositeState().getCmhandleState());
                advisedCmHandle = syncUtils.getAnAdvisedCmHandle();
            }
        } catch (final Exception e) {
            syncUtils.lockCmHandleState(advisedCmHandle, CmHandleState.LOCKED,
                LockReasonEnum.LOCKED_MISBEHAVING,  e.getMessage());
        }
        log.debug("No Cm-Handles currently found in an ADVISED state");
    }

}
