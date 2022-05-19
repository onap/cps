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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * State Model to store state corresponding to the Yang resource dmi-registry model.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompositeState {

    @JsonProperty("cm-handle-state")
    private CmHandleState cmhandleState;

    @JsonProperty("lock-reason")
    private LockReason lockReason;

    @JsonProperty("last-update-time")
    private String lastUpdateTime;

    @JsonProperty("data-sync-enabled")
    private Boolean dataSyncEnabled;

    @JsonProperty("datastores")
    private DataStores dataStores;


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

        @JsonProperty("running")
        private Running runningDataStore;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Operational {

        @JsonProperty("sync-state")
        private String syncState;

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

}
