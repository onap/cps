/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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

package org.onap.cps.ncmp.impl.cmnotificationsubscription.dmi;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionDetails;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionKey;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DmiCmSubscriptionDetailsPerDmiMapper {

    /**
     * Map subscribers per DMI to DmiCmNotificationSubscriptionDetailsPerDmi.
     *
     * @param subscribersPerDmi subscribers per dmi
     * @return dmiCmNotificationSubscriptionDetailsPerDmi
     */
    public Map<String, DmiCmSubscriptionDetails> toDmiCmNotificationSubscriptionDetailsPerDmi(
            final Map<String, Collection<DmiCmSubscriptionKey>> subscribersPerDmi) {
        final Map<String, DmiCmSubscriptionDetails> dmiCmNotificationSubscriptionDetailsMap =
                new HashMap<>();
        return dmiCmNotificationSubscriptionDetailsMap;
    }
}