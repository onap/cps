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

package org.onap.cps.ncmp.api.inventory.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.onap.cps.ncmp.api.inventory.DataStoreSyncState;

/**
 * State Model to store state corresponding to the Yang resource dmi-registry model.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompositeState {

    @JsonProperty("cm-handle-state")
    private CmHandleState cmHandleState;

    @JsonProperty("lock-reason")
    private LockReason lockReason;

    @JsonProperty("last-update-time")
    private String lastUpdateTime;

    @JsonProperty("data-sync-enabled")
    private Boolean dataSyncEnabled;

    @JsonProperty("datastores")
    private DataStores dataStores;

    /**
     * Date and Time in the format of yyyy-MM-dd'T'HH:mm:ss.SSSZ
     */
    private static final DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    /**
     * Composite State copy constructor.
     *
     * @param compositeState Composite State
     */
    public CompositeState(final CompositeState compositeState) {
        this.cmHandleState = compositeState.getCmHandleState();
        this.lockReason = compositeState.getLockReason();
        this.lastUpdateTime = compositeState.getLastUpdateTime();
        this.dataSyncEnabled = compositeState.getDataSyncEnabled();
        this.dataStores = compositeState.getDataStores();
    }


    /**
     * This will specify the latest lock reason for a specific cm handle. If a cm handle is in a state other than LOCKED
     * it specifies the last lock reason.
     * This can be used to track retry attempts as part of the lock details.
     */
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LockReason {

        @JsonProperty("reason")
        private LockReasonCategory lockReasonCategory;

        @JsonProperty("details")
        private String details;

    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DataStores {

        @JsonProperty("operational")
        private Operational operationalDataStore;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Operational {

        @JsonProperty("sync-state")
        private DataStoreSyncState dataStoreSyncState;

        @JsonProperty("last-sync-time")
        private String lastSyncTime;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Running {

        @JsonProperty("sync-state")
        private String syncState;

        @JsonProperty("last-sync-time")
        private String lastSyncTime;
    }

    /**
     * The date and time format used for the cm handle sync state.
     *
     * @return the date and time in the format of yyyy-MM-dd'T'HH:mm:ss.SSSZ
     */
    public static String nowInSyncTimeFormat() {
        return dateTimeFormatter.format(OffsetDateTime.now());
    }

    /**
     * Sets the last updated date and time for the cm handle sync state.
     */
    public void setLastUpdateTimeNow() {
        lastUpdateTime = CompositeState.nowInSyncTimeFormat();
    }

}
