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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
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

    private final NetworkCmProxyDataService networkCmProxyDataService;

    /**
     * Execute Cm Handle poll which queries the cm handle state in 'READY' and Operational Datastore Sync State in
     * 'UNSYNCHRONIZED'.
     */
    @Scheduled(fixedDelayString = "${timers.ready-data-sync.sleep-time-ms}")
    public void executeUnSynchronizedReadyCmHandlePoll() {
        YangModelCmHandle unSynchronizedReadyCmHandle = syncUtils.getUnSynchronizedReadyCmHandle();
        while (unSynchronizedReadyCmHandle != null) {
            log.debug("Cm-Handles found in READY and UNSYNCHRONIZED state: {}", unSynchronizedReadyCmHandle.getId());
            // Get the Data from RAN
            final Object resourceData = networkCmProxyDataService.getResourceDataPassThroughRunningForCmHandle(
                    unSynchronizedReadyCmHandle.getId(), "/", null,
                    null, UUID.randomUUID().toString());
            log.trace("Cm Handle Id: {} with Resource Data: {}", unSynchronizedReadyCmHandle.getId(), resourceData);
            //TODO:
            // Save the data
            // Temporarily set to SYNCHRONIZED to prevent it from picking it in next cycle.
            unSynchronizedReadyCmHandle.getCompositeState().getDataStores()
                    .setOperationalDataStore(CompositeState.Operational.builder()
                            .syncState("SYNCHRONIZED")
                            .lastSyncTime(DATE_TIME_FORMATTER.format(OffsetDateTime.now())).build());
            syncUtils.updateCmHandleStateWithNodeLeaves(unSynchronizedReadyCmHandle);
            unSynchronizedReadyCmHandle = syncUtils.getUnSynchronizedReadyCmHandle();
        }
        log.debug("No Cm-Handles currently found in an READY State and Operational Sync State in UNSYNCHRONIZED");
    }

}
