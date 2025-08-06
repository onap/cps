/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2025 OpenInfra Foundation Europe. All rights reserved.
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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.ncmp.api.inventory.DataStoreSyncState;
import org.onap.cps.ncmp.api.inventory.models.CompositeState;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
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
    private final CpsModuleService cpsModuleService;

    /**
     * Execute Cm Handle poll which queries the cm handle state in 'READY' and Operational Datastore Sync State in
     * 'UNSYNCHRONIZED'.
     */
    @Scheduled(initialDelayString = "${ncmp.timers.cm-handle-data-sync.initial-delay-ms:40000}",
            fixedDelayString = "${ncmp.timers.cm-handle-data-sync.sleep-time-ms:30000}")
    public void executeUnsynchronizedReadyCmHandleForInitialDataSync() {
        final List<YangModelCmHandle> unsynchronizedReadyCmHandles =
                moduleOperationsUtils.getUnsynchronizedReadyCmHandles();
        unsynchronizedReadyCmHandles.forEach(this::processCmHandle);
    }

    private void processCmHandle(final YangModelCmHandle unsynchronizedReadyCmHandle) {
        final String cmHandleId = unsynchronizedReadyCmHandle.getId();

        if (!hasPushedIntoSemaphoreMap(cmHandleId)) {
            log.debug("{} already processed by another instance", cmHandleId);
            return;
        }

        log.info("Executing data sync on {}", cmHandleId);

        try {
            performDataSyncForCmHandle(cmHandleId);
            log.info("Data sync finished for {}", cmHandleId);
        } catch (final Exception exception) {
            log.error("Failed to complete data sync for CM handle: {}", cmHandleId, exception);
        }
    }

    private void performDataSyncForCmHandle(final String cmHandleId) {
        final CompositeState compositeState = inventoryPersistence.getCmHandleState(cmHandleId);
        final Collection<String> rootNodeReferences =
                cpsModuleService.getRootNodeReferences(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandleId);

        for (final String rootNodeReference : rootNodeReferences) {
            syncRootNodeReferences(cmHandleId, rootNodeReference);
        }

        setSyncStateToSynchronized().accept(compositeState);
        inventoryPersistence.saveCmHandleState(cmHandleId, compositeState);
        updateDataSyncSemaphoreMap(cmHandleId);
    }

    private void syncRootNodeReferences(final String cmHandleId, final String rootNodeReference) {
        final String options = String.format("(fields=%s)", rootNodeReference);

        try {
            final String resourceData = moduleOperationsUtils.getResourceData(cmHandleId, options);
            if (resourceData == null) {
                log.warn("No resource data found for CM handle: {} with options: {}", cmHandleId, options);
                return;
            }
            cpsDataService.saveData(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandleId, resourceData,
                    OffsetDateTime.now());
        } catch (final Exception exception) {
            log.error("Failed to sync module and root node for CM handle: {} with options: {}", cmHandleId, options,
                    exception);
        }
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
