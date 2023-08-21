/*
 * ============LICENSE_START=======================================================
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

package org.onap.cps.ncmp.api.impl.events.cmsubscription;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum CmSubscriptionType {

    SUBSCRIPTION_CREATED("subscriptionCreated", "subscriptionCreatedStatus"),
    SUBSCRIPTION_DELETED("subscriptionDeleted", "subscriptionDeletedStatus");

    private final String subscriptionType;
    private final String subscriptionStatus;

    CmSubscriptionType(final String subscriptionType, final String subscriptionStatus) {
        this.subscriptionType = subscriptionType;
        this.subscriptionStatus = subscriptionStatus;
    }

    public String getSubscriptionType() {
        return subscriptionType;
    }

    public static final Map<String, String> CM_SUBSCRIPTION_TYPE_TO_STATUS = Stream.of(values())
            .collect(Collectors.toMap(key -> key.subscriptionType, value -> value.subscriptionStatus));
}
