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

import static org.onap.ncmp.cmhandle.lcm.event.Event.Operation.CREATE;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.event.NcmpEventsService;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.inventory.CmHandleState;
import org.onap.cps.ncmp.api.inventory.CompositeState;
import org.onap.cps.ncmp.api.inventory.InventoryPersistence;
import org.onap.cps.ncmp.api.inventory.LockReasonCategory;
import org.onap.cps.ncmp.api.inventory.SyncState;
import org.onap.cps.spi.exceptions.AlreadyDefinedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ModuleSyncWatchdog {

    private final InventoryPersistence inventoryPersistence;

    private final SyncUtils syncUtils;

    private final ModuleSyncService moduleSyncService;

    private final NcmpEventsService ncmpEventsService;

    /**
     * Execute Cm Handle poll which changes the cm handle state from 'ADVISED' to 'READY'.
     * Also publish the LCM Create Event when cm handle state is moved to 'READY'.
     */
    @Scheduled(fixedDelayString = "${timers.advised-modules-sync.sleep-time-ms:30000}")
    public void executeAdvisedCmHandlePoll() {
        YangModelCmHandle advisedCmHandle = syncUtils.getAnAdvisedCmHandle();
        while (advisedCmHandle != null) {
            final String cmHandleId = advisedCmHandle.getId();
            final CompositeState compositeState = inventoryPersistence.getCmHandleState(cmHandleId);
            try {
                moduleSyncService.syncAndCreateSchemaSetAndAnchor(advisedCmHandle);
                setCompositeStateToReadyAndUnsynchronized().accept(compositeState);
            } catch (final AlreadyDefinedException alreadyDefinedException) {
                log.info("AlreadyDefinedException : Setting composite state to ready and Un-synchronized "
                        + "for cmHandleId : {}", alreadyDefinedException.getMessage());
                setCompositeStateToReadyAndUnsynchronized().accept(compositeState);
            } catch (final Exception exception) {
                setCompositeStateToLocked().accept(compositeState, exception);
            }
            inventoryPersistence.saveCmHandleState(cmHandleId, compositeState);
            log.info("{} is now in {} state", cmHandleId, compositeState.getCmHandleState().name());
            if (compositeState.getCmHandleState() == CmHandleState.READY) {
                log.debug("Publishing LCM Create Event for cmHandleId : {}", cmHandleId);
                ncmpEventsService.publishNcmpEvent(cmHandleId, CREATE);
            }
            advisedCmHandle = syncUtils.getAnAdvisedCmHandle();
        }
        log.debug("No Cm-Handles currently found in an ADVISED state");
    }

    private BiConsumer<CompositeState, Exception> setCompositeStateToLocked() {
        return (compositeState, exception) -> {
            compositeState.setCmHandleState(CmHandleState.LOCKED);
            compositeState.setLastUpdateTimeNow();
            syncUtils.updateLockReasonDetailsAndAttempts(compositeState,
                    LockReasonCategory.LOCKED_MISBEHAVING, exception.getMessage());
        };
    }

    private Consumer<CompositeState> setCompositeStateToReadyAndUnsynchronized() {
        return compositeState -> {
            compositeState.setCmHandleState(CmHandleState.READY);
            compositeState.setDataStores(CompositeState.DataStores.builder()
                    .operationalDataStore(CompositeState.Operational.builder()
                            .syncState(SyncState.UNSYNCHRONIZED)
                            .lastSyncTime(CompositeState.nowInSyncTimeFormat())
                            .build())
                    .build());
        };
    }

    /**
     * Execute Cm Handle poll which changes the cm handle state from 'LOCKED' to 'ADVISED'.
     */
    @Scheduled(fixedDelayString = "${timers.locked-modules-sync.sleep-time-ms:120000}")
    public void executeLockedMisbehavingCmHandlePoll() {
        final List<YangModelCmHandle> lockedMisbehavingCmHandles = syncUtils.getLockedMisbehavingYangModelCmHandles();
        for (final YangModelCmHandle lockedMisbehavingModelCmHandle: lockedMisbehavingCmHandles) {
            final CompositeState updatedCompositeState = lockedMisbehavingModelCmHandle.getCompositeState();
            updatedCompositeState.setCmHandleState(CmHandleState.ADVISED);
            updatedCompositeState.setLastUpdateTimeNow();
            updatedCompositeState.setLockReason(CompositeState.LockReason.builder()
                .details(updatedCompositeState.getLockReason().getDetails()).build());
            log.info("Locked misbehaving cm handle {} is being recycled", lockedMisbehavingModelCmHandle.getId());
            inventoryPersistence.saveCmHandleState(lockedMisbehavingModelCmHandle.getId(), updatedCompositeState);
        }
    }
}
