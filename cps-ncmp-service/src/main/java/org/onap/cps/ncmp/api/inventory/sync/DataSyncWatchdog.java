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
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.inventory.CompositeState;
import org.onap.cps.ncmp.api.inventory.InventoryPersistence;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class DataSyncWatchdog {

    private final InventoryPersistence inventoryPersistence;

    private final CpsDataService cpsDataService;

    private final SyncUtils syncUtils;

    /**
     * Execute Cm Handle poll which queries the cm handle state in 'READY' and Operational Datastore Sync State in
     * 'UNSYNCHRONIZED'.
     */
    @Scheduled(fixedDelayString = "${timers.cm-handle-data-sync.sleep-time-ms:30000}")
    public void executeUnSynchronizedReadyCmHandlePoll() {
        YangModelCmHandle unSynchronizedReadyCmHandle = syncUtils.getAnUnSynchronizedReadyCmHandle();
        while (unSynchronizedReadyCmHandle != null) {
            final String cmHadleId = unSynchronizedReadyCmHandle.getId();
            log.debug("Cm-Handles found in READY and UNSYNCHRONIZED state: {}", cmHadleId);
            final CompositeState compositeState = inventoryPersistence
                    .getCmHandleState(cmHadleId);
            final Optional<String> resourceData = syncUtils.getResourceData(cmHadleId);
            resourceData.ifPresentOrElse(resource -> cpsDataService.saveData("NFP-Operational", cmHadleId,
                            resource, OffsetDateTime.now()),
                    () -> log.debug("No data to persist in the DB for Cm-Handle: {}", cmHadleId));
            compositeState.setLastUpdateTimeNow();
            compositeState.getDataStores()
                    .setOperationalDataStore(CompositeState.Operational.builder()
                            .syncState("SYNCHRONIZED")
                            .lastSyncTime(CompositeState.nowInSyncTimeFormat()).build());
            inventoryPersistence.saveCmHandleState(cmHadleId, compositeState);
            unSynchronizedReadyCmHandle = syncUtils.getAnUnSynchronizedReadyCmHandle();
        }
        log.debug("No Cm-Handles currently found in an READY State and Operational Sync State in UNSYNCHRONIZED");
    }
}
