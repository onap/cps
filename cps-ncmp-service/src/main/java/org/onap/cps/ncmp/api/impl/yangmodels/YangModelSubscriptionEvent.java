/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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


package org.onap.cps.ncmp.api.impl.yangmodels;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.onap.cps.ncmp.api.impl.deprecated_subscriptions.SubscriptionStatus;

/**
 * Subscription event model to persist data into DB.
 * Yang model subscription event
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class YangModelSubscriptionEvent {

    @EqualsAndHashCode.Include
    @JsonProperty("clientID")
    private String clientId;

    @EqualsAndHashCode.Include
    @JsonProperty("subscriptionName")
    private String subscriptionName;

    private String topic;

    @JsonProperty("isTagged")
    private boolean isTagged;

    private Predicates predicates;


    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Predicates {

        private String datastore;

        private List<TargetCmHandle> targetCmHandles;

    }

    @AllArgsConstructor
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TargetCmHandle {

        @JsonProperty()
        private final String cmHandleId;

        @JsonProperty()
        private final SubscriptionStatus status;

        @JsonProperty()
        private final String details;

        /**
         * Constructor with single parameter for TargetCmHandle.
         *
         * @param cmHandleId as cm handle id
         */
        public TargetCmHandle(final String cmHandleId) {
            this.cmHandleId = cmHandleId;
            this.status = SubscriptionStatus.PENDING;
            this.details = "Subscription forwarded to dmi plugin";
        }
    }
}


