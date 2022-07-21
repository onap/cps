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
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.ncmp.api.inventory.CompositeState;
import org.onap.cps.ncmp.api.inventory.DataStoreSyncState;
import org.onap.cps.ncmp.api.inventory.InventoryPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class DataSyncWatchdog {

    private final InventoryPersistence inventoryPersistence;

    private final CpsDataService cpsDataService;

    private final SyncUtils syncUtils;

    @Autowired
    private ConcurrentMap<String, Boolean> dataSyncSemaphore;

    /**
     * Execute Cm Handle poll which queries the cm handle state in 'READY' and Operational Datastore Sync State in
     * 'UNSYNCHRONIZED'.
     */
    @Scheduled(fixedDelayString = "${timers.cm-handle-data-sync.sleep-time-ms:30000}")
    public void executeUnSynchronizedReadyCmHandlePoll() {
        syncUtils.getAnUnSynchronizedReadyCmHandle().forEach(unSynchronizedReadyCmHandle -> {
            final String cmHandleId = unSynchronizedReadyCmHandle.getId();
            if (dataSyncSemaphore.putIfAbsent(cmHandleId, false) == null) {
                log.debug("executing data sync on {}", cmHandleId);
                log.debug("Cm-Handles found in READY and UNSYNCHRONIZED state: {}", cmHandleId);
                final CompositeState compositeState = inventoryPersistence
                        .getCmHandleState(cmHandleId);
                final String resourceData = syncUtils.getResourceData(cmHandleId);
                if (resourceData == null) {
                    log.debug("Error accessing the node for Cm-Handle: {}", cmHandleId);
                } else {
                    cpsDataService.saveData("NFP-Operational", cmHandleId,
                            resourceData, OffsetDateTime.now());
                    setSyncStateToSynchronized().accept(compositeState);
                    inventoryPersistence.saveCmHandleState(cmHandleId, compositeState);
                    dataSyncSemaphore.put(cmHandleId, true);
                }
            } else {
                log.debug("{} already processed by other instance", cmHandleId);
            }
        });
        log.debug("No Cm-Handles currently found in an READY State and Operational Sync State is UNSYNCHRONIZED");
    }

    private Consumer<CompositeState> setSyncStateToSynchronized() {
        return compositeState -> {
            compositeState.setLastUpdateTimeNow();
            compositeState.getDataStores()
                    .setOperationalDataStore(CompositeState.Operational.builder()
                            .dataStoreSyncState(DataStoreSyncState.SYNCHRONIZED)
                            .lastSyncTime(CompositeState.nowInSyncTimeFormat()).build());
        };
    }
}
