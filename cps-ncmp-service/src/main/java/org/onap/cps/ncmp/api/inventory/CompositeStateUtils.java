/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Nordix Foundation
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.inventory;

import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * It will have all the utility method responsible for handling the composite state.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CompositeStateUtils {

    /**
     * Sets the cmHandleState to the provided state and updates the timestamp.
     *
     * @return Updated CompositeState
     */
    public static Consumer<CompositeState> setCompositeState(final CmHandleState cmHandleState) {
        return compositeState -> {
            compositeState.setCmHandleState(cmHandleState);
            compositeState.setLastUpdateTimeNow();
        };
    }

    /**
     * Sets the cmHandleState to READY and operational datastore sync state based on the global flag.
     *
     * @return Updated CompositeState
     */
    public static Consumer<CompositeState> setCompositeStateToReadyWithInitialDataStoreSyncState() {
        return compositeState -> {
            compositeState.setDataSyncEnabled(false);
            compositeState.setCmHandleState(CmHandleState.READY);
            final CompositeState.Operational operational =
                    getInitialDataStoreSyncState(compositeState.getDataSyncEnabled());
            final CompositeState.DataStores dataStores =
                    CompositeState.DataStores.builder().operationalDataStore(operational).build();
            compositeState.setDataStores(dataStores);
        };
    }

    /**
     * Set the data sync enabled flag, along with the data store sync state based on this flag.
     *
     * @param dataSyncEnabled data sync enabled flag
     * @param compositeState cm handle composite state
     */
    public static void setDataSyncEnabledFlagWithDataSyncState(final boolean dataSyncEnabled,
                                                               final CompositeState compositeState) {
        compositeState.setDataSyncEnabled(dataSyncEnabled);
        final CompositeState.Operational operational = getInitialDataStoreSyncState(dataSyncEnabled);
        final CompositeState.DataStores dataStores =
            CompositeState.DataStores.builder().operationalDataStore(operational).build();
        compositeState.setDataStores(dataStores);
    }

    /**
     * Get initial data sync state based on data sync enabled boolean flag.
     *
     * @param dataSyncEnabled data sync enabled boolean flag
     * @return the data store sync state
     */
    private static CompositeState.Operational getInitialDataStoreSyncState(final boolean dataSyncEnabled) {
        final DataStoreSyncState dataStoreSyncState =
                dataSyncEnabled ? DataStoreSyncState.UNSYNCHRONIZED : DataStoreSyncState.NONE_REQUESTED;
        return CompositeState.Operational.builder().dataStoreSyncState(dataStoreSyncState).build();
    }

    /**
     * Sets the cmHandleState to ADVISED and retain the lock details. Used in retry scenarios.
     *
     * @return Updated CompositeState
     */
    public static Consumer<CompositeState> setCompositeStateForRetry() {
        return compositeState -> {
            compositeState.setCmHandleState(CmHandleState.ADVISED);
            compositeState.setLastUpdateTimeNow();
            final String oldLockReasonDetails = compositeState.getLockReason().getDetails();
            final CompositeState.LockReason lockReason =
                    CompositeState.LockReason.builder().details(oldLockReasonDetails).build();
            compositeState.setLockReason(lockReason);
        };
    }
}
