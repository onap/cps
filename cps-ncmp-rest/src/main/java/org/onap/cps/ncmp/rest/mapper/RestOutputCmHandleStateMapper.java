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

package org.onap.cps.ncmp.rest.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.onap.cps.ncmp.api.inventory.CmHandleState;
import org.onap.cps.ncmp.api.inventory.CompositeState;
import org.onap.cps.ncmp.rest.model.DataStores;
import org.onap.cps.ncmp.rest.model.RestOutputCmHandleState;
import org.onap.cps.ncmp.rest.model.SyncState;

@Mapper(componentModel = "spring")
public interface RestOutputCmHandleStateMapper {

    @Mapping(target = "dataSyncState", source = "dataStores", qualifiedByName = "dataStoreToDataSyncState")
    @Mapping(target = "cmHandleState", source = "cmhandleState", qualifiedByName = "cmHandleStateEnumToString")
    RestOutputCmHandleState toRestOutputCmHandleState(CompositeState compositeState);

    /**
     * Convert from CompositeState datastore to RestOutput Datastores.
     *
     * @param compositeStateDataStore Composite State data stores
     * @return DataStores
     */
    @Named("dataStoreToDataSyncState")
    static DataStores toDataStores(CompositeState.DataStores compositeStateDataStore) {

        if (compositeStateDataStore == null) {
            return null;
        }

        final DataStores dataStores = new DataStores();

        if (compositeStateDataStore.getRunningDataStore() != null) {
            final SyncState runningSyncState = new SyncState();
            runningSyncState.setState(compositeStateDataStore.getRunningDataStore().getSyncState());
            runningSyncState.setLastSyncTime(compositeStateDataStore.getRunningDataStore().getLastSyncTime());
            dataStores.setRunning(runningSyncState);
        }

        if (compositeStateDataStore.getOperationalDataStore() != null) {
            final SyncState operationalSyncState = new SyncState();
            operationalSyncState.setState(compositeStateDataStore.getOperationalDataStore().getSyncState());
            operationalSyncState.setLastSyncTime(compositeStateDataStore.getOperationalDataStore().getLastSyncTime());
            dataStores.setOperational(operationalSyncState);
        }


        return dataStores;

    }

    /**
     * Converts cmHandleState enum value to equivalent string.
     *
     * @param cmHandleState cm handle state enum
     * @return cm handle state as string
     */
    @Named("cmHandleStateEnumToString")
    static String toCmHandleState(final CmHandleState cmHandleState) {
        return cmHandleState.name();
    }

}
