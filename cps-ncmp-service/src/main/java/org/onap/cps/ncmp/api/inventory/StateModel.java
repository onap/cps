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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * State Model which follows the Yang resource dmi-registry model.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StateModel {

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

    @Data
    @AllArgsConstructor
    public static class LockReason {

        @JsonProperty("reason")
        private String reason;

        @JsonProperty("details")
        private String details;

    }

    @Data
    @AllArgsConstructor
    public static class DataStores {

        @JsonProperty("operational")
        private Operational operationalDataStore;

        @JsonProperty("running")
        private Running runningDataStore;
    }

    @Data
    @AllArgsConstructor
    public static class Operational {

        @JsonProperty("operational")
        private DataStoreState dataStoreState;
    }

    @Data
    @AllArgsConstructor
    public static class Running {

        @JsonProperty("running")
        private DataStoreState dataStoreState;
    }

    @Data
    @AllArgsConstructor
    public static class DataStoreState {

        @JsonProperty("sync-state")
        private String syncState;

        @JsonProperty("last-sync-time")
        private String lastSyncTime;

    }

}
