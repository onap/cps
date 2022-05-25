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

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.inventory.CompositeState;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class DataSyncWatchdog {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private final SyncUtils syncUtils;

    /**
     * Execute Cm Handle poll which queries the cm handle state in 'READY' and Operational Datastore Sync State in
     * 'UNSYNCHRONIZED'.
     */
    @Scheduled(fixedDelayString = "${timers.ready-data-sync.sleep-time-ms}")
    public void executeUnSynchronizedReadyCmHandlePoll() {
        YangModelCmHandle cmHandle = syncUtils.getUnSynchronizedReadyCmHandle();
        while (cmHandle != null) {
            log.debug("Cm-Handles found in READY and UNSYNCHRONIZED state: {}", cmHandle.getId());
            //TODO:
            // Get the Data from RAN
            // Save the data
            // Temporarily set to SYNCHRONIZED
            cmHandle.getCompositeState().getDataStores()
                    .setOperationalDataStore(CompositeState.Operational.builder().syncState("SYNCHRONIZED")
                            .lastSyncTime(DATE_TIME_FORMATTER.format(OffsetDateTime.now())).build());
            syncUtils.updateCmHandleStateWithNodeLeaves(cmHandle);
            cmHandle = syncUtils.getUnSynchronizedReadyCmHandle();
        }
        log.debug("No Cm-Handles currently found in an READY State and Operational Sync State in UNSYNCHRONIZED");
    }

}
