/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2023 Nordix Foundation
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

package org.onap.cps.ncmp.rest.util;

import static org.onap.cps.ncmp.api.inventory.models.LockReasonCategory.LOCKED_MISBEHAVING;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.onap.cps.ncmp.api.inventory.models.CompositeState;
import org.onap.cps.ncmp.rest.model.CmHandleCompositeState;
import org.onap.cps.ncmp.rest.model.DataStores;
import org.onap.cps.ncmp.rest.model.LockReason;
import org.onap.cps.ncmp.rest.model.SyncState;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
public interface CmHandleStateMapper {

    @Mapping(target = "dataSyncState", source = "dataStores", qualifiedByName = "dataStoreToDataSyncState")
    @Mapping(target = "lockReason", source = "lockReason", qualifiedByName = "toExternalLockReason")
    CmHandleCompositeState toCmHandleCompositeStateExternalLockReason(CompositeState compositeState);

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

        if (compositeStateDataStore.getOperationalDataStore() != null) {
            final SyncState operationalSyncState = new SyncState();
            operationalSyncState.setSyncState(compositeStateDataStore.getOperationalDataStore()
                    .getDataStoreSyncState().name());
            operationalSyncState.setLastSyncTime(compositeStateDataStore.getOperationalDataStore().getLastSyncTime());
            dataStores.setOperational(operationalSyncState);
        }

        return dataStores;
    }

    /**
     * Convert Internal Lock Reason to External Lock Reason.
     *
     * @param internalLockReason Internal Lock Reason
     *
     * @return externalLockReason
     */
    @Named("toExternalLockReason")
    static LockReason toExternalLockReason(CompositeState.LockReason internalLockReason) {
        final LockReason externalLockReason = new LockReason();
        if (internalLockReason.getLockReasonCategory() == null) {
            externalLockReason.setReason(LOCKED_MISBEHAVING.name());
        } else {
            externalLockReason.setReason(internalLockReason.getLockReasonCategory().name());
        }
        externalLockReason.setDetails(internalLockReason.getDetails());
        return externalLockReason;
    }

}
