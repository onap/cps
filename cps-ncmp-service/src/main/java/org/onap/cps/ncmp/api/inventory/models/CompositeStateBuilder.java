/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Bell Canada
 * Modifications Copyright (C) 2022-2023 Nordix Foundation.
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

package org.onap.cps.ncmp.api.inventory.models;

import org.onap.cps.api.model.DataNode;
import org.onap.cps.ncmp.api.inventory.models.CompositeState.DataStores;
import org.onap.cps.ncmp.api.inventory.models.CompositeState.LockReason;
import org.onap.cps.ncmp.api.inventory.models.CompositeState.Operational;
import org.onap.cps.ncmp.impl.inventory.DataStoreSyncState;
import org.onap.cps.ncmp.impl.inventory.models.CmHandleState;
import org.onap.cps.ncmp.impl.inventory.models.LockReasonCategory;

public class CompositeStateBuilder {

    private CmHandleState cmHandleState;
    private LockReason lockReason;
    private DataStores datastores;
    private String lastUpdatedTime;
    private Boolean dataSyncEnabled;

    /**
     * To create the {@link CompositeState}.
     *
     * @return {@link DataNode}
     */
    public CompositeState build() {
        final CompositeState compositeState = new CompositeState();
        compositeState.setCmHandleState(cmHandleState);
        compositeState.setLockReason(lockReason);
        compositeState.setDataStores(datastores);
        compositeState.setLastUpdateTime(lastUpdatedTime);
        compositeState.setDataSyncEnabled(dataSyncEnabled);
        return compositeState;
    }

    /**
     * To use attributes for creating {@link CompositeState}.
     *
     * @param cmHandleState for the data node
     * @return CompositeStateBuilder
     */
    public CompositeStateBuilder withCmHandleState(final CmHandleState cmHandleState) {
        this.cmHandleState = cmHandleState;
        return this;
    }

    /**
     * To use attributes for creating {@link CompositeState}.
     *
     * @param reason for the locked state
     * @param details for the locked state
     * @return CompositeStateBuilder
     */
    public CompositeStateBuilder withLockReason(final LockReasonCategory reason, final String details) {
        this.lockReason = LockReason.builder().lockReasonCategory(reason).details(details).build();
        return this;
    }

    /**
     * To use attributes for creating {@link CompositeState}.
     *
     * @param time for the state change
     * @return CompositeStateBuilder
     */
    public CompositeStateBuilder withLastUpdatedTime(final String time) {
        this.lastUpdatedTime = time;
        return this;
    }

    /**
     * To use attributes for creating {@link CompositeState}.
     *
     * @return composite state builder
     */
    public CompositeStateBuilder withLastUpdatedTimeNow() {
        this.lastUpdatedTime = CompositeState.nowInSyncTimeFormat();
        return this;
    }

    /**
     * To use attributes for creating {@link CompositeState}.
     *
     * @param dataStoreSyncState for the locked state
     * @param lastSyncTime for the locked state
     * @return CompositeStateBuilder
     */
    public CompositeStateBuilder withOperationalDataStores(final DataStoreSyncState dataStoreSyncState,
                                                           final String lastSyncTime) {
        this.datastores = DataStores.builder().operationalDataStore(
            Operational.builder().dataStoreSyncState(dataStoreSyncState).lastSyncTime(lastSyncTime).build()).build();
        return this;
    }

    /**
     * To use dataNode for creating {@link CompositeState}.
     *
     * @param dataNode for the dataNode
     * @return CompositeState
     */
    public CompositeStateBuilder fromDataNode(final DataNode dataNode) {
        this.cmHandleState = CmHandleState.valueOf((String) dataNode.getLeaves()
            .get("cm-handle-state"));
        this.lastUpdatedTime = (String) dataNode.getLeaves().get("last-update-time");
        if (this.cmHandleState == CmHandleState.READY) {
            this.dataSyncEnabled = (Boolean) dataNode.getLeaves().get("data-sync-enabled");
        }
        for (final DataNode stateChildNode : dataNode.getChildDataNodes()) {
            if (stateChildNode.getXpath().endsWith("/lock-reason")) {
                this.lockReason = getLockReason(stateChildNode);
            }
            if (stateChildNode.getXpath().endsWith("/datastores")) {
                for (final DataNode dataStoreNodes : stateChildNode.getChildDataNodes()) {
                    Operational operationalDataStore = null;
                    if (dataStoreNodes.getXpath().contains("/operational")) {
                        operationalDataStore = getOperationalDataStore(dataStoreNodes);
                    }
                    this.datastores = DataStores.builder().operationalDataStore(operationalDataStore).build();
                }
            }
        }
        return this;
    }

    private Operational getOperationalDataStore(final DataNode dataStoreNodes) {
        return Operational.builder()
                .dataStoreSyncState(DataStoreSyncState.valueOf((String) dataStoreNodes.getLeaves().get("sync-state")))
                .lastSyncTime((String) dataStoreNodes.getLeaves().get("last-sync-time"))
                .build();
    }

    private LockReason getLockReason(final DataNode stateChildNode) {
        final boolean isLockReasonExists = stateChildNode.getLeaves().containsKey("reason");
        return new LockReason(isLockReasonExists
                ? LockReasonCategory.valueOf((String) stateChildNode.getLeaves().get("reason"))
                : null, (String) stateChildNode.getLeaves().get("details"));
    }

}
