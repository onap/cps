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

import java.util.HashMap;
import java.util.Map;
import org.onap.cps.spi.exceptions.SubscriptionOutcomeTypeNotFoundException;

public enum SubscriptionOutcomeType {

    SUCCESS(600),
    PARTIAL_SUCCESS(601),
    PENDING(602),
    TARGETS_NOT_FOUND(603),
    SUBSCRIPTION_NOT_APPLICABLE(604);

    private final Integer outcomeCode;

    private static final Map<Integer, SubscriptionOutcomeType> CONSTANTS = new HashMap<>();

    static {
        for (final SubscriptionOutcomeType outcomeType: values()) {
            CONSTANTS.put(outcomeType.outcomeCode, outcomeType);
        }
    }

    SubscriptionOutcomeType(final Integer outcomeCode) {
        this.outcomeCode = outcomeCode;
    }

    public int outcomeCode() {
        return this.outcomeCode;
    }

    /**
     * Finds the outcome type of the given outcome code.
     *
     * @param outcomeCode value of the enum
     * @return a SubscriptionOutcomeType
     */
    public static SubscriptionOutcomeType fromCode(final Integer outcomeCode) {
        final SubscriptionOutcomeType subscriptionOutcomeType = CONSTANTS.get(outcomeCode);
        if (subscriptionOutcomeType == null) {
            throw new SubscriptionOutcomeTypeNotFoundException("Invalid outcome code", "Subscription outcome type not"
                    + " found");
        } else {
            return subscriptionOutcomeType;
        }
    }
}
