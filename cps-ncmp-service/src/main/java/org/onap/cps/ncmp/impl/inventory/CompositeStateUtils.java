/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2024 Nordix Foundation
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

package org.onap.cps.ncmp.impl.inventory;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.inventory.DataStoreSyncState;
import org.onap.cps.ncmp.api.inventory.models.CmHandleState;
import org.onap.cps.ncmp.api.inventory.models.CompositeState;

/**
 * It will have all the utility method responsible for handling the composite state.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CompositeStateUtils {

    /**
     * Sets the cmHandleState to the provided state and updates the timestamp.
     */
    public static void setCompositeState(final CmHandleState cmHandleState,
                                                   final CompositeState compositeState) {
        compositeState.setCmHandleState(cmHandleState);
        compositeState.setLastUpdateTimeNow();
    }

    /**
     * Set the Operational datastore sync state based on the global flag.
     */
    public static void setInitialDataStoreSyncState(final CompositeState compositeState) {
        compositeState.setDataSyncEnabled(false);
        final CompositeState.Operational operational =
            getInitialDataStoreSyncState(compositeState.getDataSyncEnabled());
        final CompositeState.DataStores dataStores =
            CompositeState.DataStores.builder().operationalDataStore(operational).build();
        compositeState.setDataStores(dataStores);
    }

    /**
     * Set the data sync enabled flag, along with the data store sync state based on this flag.
     *
     * @param dataSyncEnabled data sync enabled flag
     * @param compositeState  cm handle composite state
     */
    public static void setDataSyncEnabledFlagWithDataSyncState(final boolean dataSyncEnabled,
            final CompositeState compositeState) {
        compositeState.setDataSyncEnabled(dataSyncEnabled);
        compositeState.setLastUpdateTimeNow();
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
     */
    public static void setCompositeStateForRetry(final CompositeState compositeState) {
        compositeState.setCmHandleState(CmHandleState.ADVISED);
        compositeState.setLastUpdateTimeNow();
        final String oldLockReasonDetails = compositeState.getLockReason().getDetails();
        final CompositeState.LockReason lockReason =
            CompositeState.LockReason.builder()
                .lockReasonCategory(compositeState.getLockReason().getLockReasonCategory())
                .details(oldLockReasonDetails).build();
        compositeState.setLockReason(lockReason);
    }
}
