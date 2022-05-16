/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
 *  Modifications Copyright (C) 2022 Bell Canada
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

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.inventory.CmHandleState;
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
    @Scheduled(fixedDelayString = "${ncmp.timers.advised-modules-sync.sleep-time-ms}")
    public void executeAdvisedCmHandlePoll() {
        YangModelCmHandle advisedCmHandle = syncUtils.getAnAdvisedCmHandle();
        while (advisedCmHandle != null) {
            final CmHandleState cmHandleState = advisedCmHandle.getCmHandleState();
            moduleSyncService.syncAndCreateSchemaSet(advisedCmHandle);
            // ToDo Lock Cm Handle if module sync fails
            syncUtils.updateCmHandleState(advisedCmHandle, cmHandleState.state());
            log.info("{} is now in {} state", advisedCmHandle.getId(), advisedCmHandle.getCmHandleState());
            advisedCmHandle = syncUtils.getAnAdvisedCmHandle();
        }
        log.debug("No Cm-Handles currently found in an ADVISED state");
    }

    /**
     * Execute Cm Handle poll which changes the cm handle state from 'LOCKED' to 'ADVISED'.
     */
    @Scheduled(fixedDelayString = "${ncmp.timers.locked-modules-sync.sleep-time-ms}")
    public void executeLockedCmHandlePoll() {
        final List<YangModelCmHandle> allLockedCmHandle = syncUtils.getLockedCmHandle();
        for (final YangModelCmHandle lockedModelCmHandle: allLockedCmHandle) {
            syncUtils.updateCmHandleState(lockedModelCmHandle, CmHandleState.ADVISED);
        }
        log.debug("No Cm-Handles currently found in an LOCKED state");
    }

}
