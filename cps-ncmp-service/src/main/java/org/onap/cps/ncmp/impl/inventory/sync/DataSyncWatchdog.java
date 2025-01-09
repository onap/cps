/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2024 Nordix Foundation
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

package org.onap.cps.ncmp.impl.inventory.sync;

import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME;

import com.hazelcast.map.IMap;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.ncmp.api.inventory.DataStoreSyncState;
import org.onap.cps.ncmp.api.inventory.models.CompositeState;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class DataSyncWatchdog {

    private static final boolean DATA_SYNC_IN_PROGRESS = false;
    private static final boolean DATA_SYNC_DONE = true;

    private final InventoryPersistence inventoryPersistence;

    private final CpsDataService cpsDataService;

    private final ModuleOperationsUtils moduleOperationsUtils;

    private final IMap<String, Boolean> dataSyncSemaphores;

    /**
     * Execute Cm Handle poll which queries the cm handle state in 'READY' and Operational Datastore Sync State in
     * 'UNSYNCHRONIZED'.
     */
    @Scheduled(fixedDelayString = "${ncmp.timers.cm-handle-data-sync.sleep-time-ms:30000}")
    public void executeUnSynchronizedReadyCmHandlePoll() {
        moduleOperationsUtils.getUnsynchronizedReadyCmHandles().forEach(unSynchronizedReadyCmHandle -> {
            final String cmHandleId = unSynchronizedReadyCmHandle.getId();
            if (hasPushedIntoSemaphoreMap(cmHandleId)) {
                log.info("Executing data sync on {}", cmHandleId);
                final CompositeState compositeState = inventoryPersistence
                        .getCmHandleState(cmHandleId);
                final String resourceData = moduleOperationsUtils.getResourceData(cmHandleId);
                if (resourceData == null) {
                    log.error("Error retrieving resource data for Cm-Handle: {}", cmHandleId);
                } else {
                    cpsDataService.saveData(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandleId,
                            resourceData, OffsetDateTime.now());
                    setSyncStateToSynchronized().accept(compositeState);
                    inventoryPersistence.saveCmHandleState(cmHandleId, compositeState);
                    updateDataSyncSemaphoreMap(cmHandleId);
                    log.info("Data sync finished for {}", cmHandleId);
                }
            } else {
                log.info("{} already processed by another instance", cmHandleId);
            }
        });
        log.debug("No Cm-Handles currently found in READY State and Operational Sync State is UNSYNCHRONIZED");
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

    private void updateDataSyncSemaphoreMap(final String cmHandleId) {
        dataSyncSemaphores.replace(cmHandleId, DATA_SYNC_DONE);
    }

    private boolean hasPushedIntoSemaphoreMap(final String cmHandleId) {
        return dataSyncSemaphores.putIfAbsent(cmHandleId, DATA_SYNC_IN_PROGRESS,
                SynchronizationCacheConfig.DATA_SYNC_SEMAPHORE_TTL_SECS, TimeUnit.SECONDS) == null;
    }
}
