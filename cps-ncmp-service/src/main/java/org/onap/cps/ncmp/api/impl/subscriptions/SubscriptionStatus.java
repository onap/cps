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

package org.onap.cps.ncmp.api.impl.subscriptions;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

public enum SubscriptionStatus {
    ACCEPTED("ACCEPTED"),
    REJECTED("REJECTED"),
    PENDING("PENDING");

    private final String subscriptionStatusValue;

    SubscriptionStatus(final String subscriptionStatusValue) {
        this.subscriptionStatusValue = subscriptionStatusValue;
    }

    /**
     * Finds the value of the given enum.
     *
     * @param statusValue value of the enum
     * @return a SubscriptionStatus
     */
    public static SubscriptionStatus fromString(final String statusValue) {
        for (final SubscriptionStatus subscriptionStatusType : SubscriptionStatus.values()) {
            if (subscriptionStatusType.subscriptionStatusValue.equalsIgnoreCase(statusValue)) {
                return subscriptionStatusType;
            }
        }
        return null;
    }

    /**
     * Populates a map with a key of cm handle id and a value of subscription status.
     *
     * @param resultMap the map is being populated
     * @param bucketIterator to iterate over the collection
     */
    public static void populateCmHandleToSubscriptionStatusMap(final Map<String, SubscriptionStatus> resultMap,
                                                          final Iterator<Serializable> bucketIterator) {
        final String item = (String) bucketIterator.next();
        if ("PENDING".equals(item)) {
            resultMap.put((String) bucketIterator.next(),
                    SubscriptionStatus.PENDING);
        }
        if ("REJECTED".equals(item)) {
            resultMap.put((String) bucketIterator.next(),
                    SubscriptionStatus.REJECTED);
        }
        if ("ACCEPTED".equals(item)) {
            resultMap.put((String) bucketIterator.next(),
                    SubscriptionStatus.ACCEPTED);
        }
    }
}
