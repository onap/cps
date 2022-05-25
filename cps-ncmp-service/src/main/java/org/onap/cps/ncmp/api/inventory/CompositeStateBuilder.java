/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Bell Canada
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

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import org.onap.cps.ncmp.api.inventory.CompositeState.DataStores;
import org.onap.cps.ncmp.api.inventory.CompositeState.LockReason;
import org.onap.cps.ncmp.api.inventory.CompositeState.Operational;
import org.onap.cps.ncmp.api.inventory.CompositeState.Running;
import org.onap.cps.spi.model.DataNode;

public class CompositeStateBuilder {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private CmHandleState cmHandleState;
    private LockReason lockReason;
    private DataStores datastores;
    private String lastUpdatedTime;

    /**
     * To create the {@link CompositeState}.
     *
     * @return {@link DataNode}
     */
    public CompositeState build() {
        final CompositeState compositeState = new CompositeState();
        compositeState.setCmhandleState(cmHandleState);
        compositeState.setLockReason(lockReason);
        compositeState.setDataStores(datastores);
        compositeState.setLastUpdateTime(lastUpdatedTime);
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
    public CompositeStateBuilder withLockReason(final String reason, final String details) {
        this.lockReason = LockReason.builder().reason(reason).details(details).build();
        return this;
    }

    /**
     * To use attributes for creating {@link CompositeState}.
     *
     * @return CompositeStateBuilder
     */
    public CompositeStateBuilder withLastUpdatedTimeNow() {
        this.lastUpdatedTime = DATE_TIME_FORMATTER.format(OffsetDateTime.now());
        return this;
    }

    /**
     * To use attributes for creating {@link CompositeState}.
     *
     * @param syncState for the locked state
     * @param lastSyncTime for the locked state
     * @return CompositeStateBuilder
     */
    public CompositeStateBuilder withOperationalDataStores(final String syncState, final String lastSyncTime) {
        this.datastores = DataStores.builder().operationalDataStore(
                Operational.builder().syncState(syncState).lastSyncTime(lastSyncTime).build()).build();
        return this;
    }

    /**
     * To use attributes for creating {@link CompositeState}.
     *
     * @param syncState for the locked state
     * @param lastSyncTime for the locked state
     * @return CompositeStateBuilder
     */
    public CompositeStateBuilder withRunningDataStores(final String syncState, final String lastSyncTime) {
        this.datastores = DataStores.builder().runningDataStore(
                Running.builder().syncState(syncState).lastSyncTime(lastSyncTime).build()).build();
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
        for (final DataNode stateChildNode : dataNode.getChildDataNodes()) {
            if (stateChildNode.getXpath().endsWith("/lock-reason")) {
                this.lockReason = new LockReason((String) stateChildNode.getLeaves().get("reason"),
                        (String) stateChildNode.getLeaves().get("details"));
            }
            if (stateChildNode.getXpath().endsWith("/datastores")) {
                for (final DataNode dataStoreNodes : stateChildNode.getChildDataNodes()) {
                    Operational operationalDataStore = null;
                    Running runningDataStore = null;
                    if (dataStoreNodes.getXpath().contains("/operational")) {
                        operationalDataStore = Operational.builder()
                                .syncState((String) dataStoreNodes.getLeaves().get("sync-state"))
                                .lastSyncTime((String) dataStoreNodes.getLeaves().get("last-sync-time"))
                                .build();
                    } else {
                        runningDataStore = Running.builder()
                                .syncState((String) dataStoreNodes.getLeaves().get("sync-state"))
                                .lastSyncTime((String) dataStoreNodes.getLeaves().get("last-sync-time"))
                                .build();
                    }
                    this.datastores = DataStores.builder().operationalDataStore(operationalDataStore)
                            .runningDataStore(runningDataStore).build();
                }
            }
        }
        return this;
    }

}