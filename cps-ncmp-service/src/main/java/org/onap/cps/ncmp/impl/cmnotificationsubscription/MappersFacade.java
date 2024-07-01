/*
 *  ============LICENSE_START=======================================================
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

package org.onap.cps.ncmp.impl.cmnotificationsubscription;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.dmi.DmiInEventMapper;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionDetails;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionPredicate;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.ncmp.NcmpOutEventMapper;
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.ncmp_to_client.NcmpOutEvent;
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.ncmp_to_dmi.DmiInEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MappersFacade {

    private final DmiInEventMapper dmiInEventMapper;
    private final NcmpOutEventMapper ncmpOutEventMapper;

    /**
     * Mapper to form a request for the DMI Plugin for the Cm Notification Subscription.
     *
     * @param dmiCmSubscriptionPredicates Collection of Cm Notification Subscription predicates
     * @return cm notification subscription dmi in event
     */
    public DmiInEvent toDmiInEvent(
            final List<DmiCmSubscriptionPredicate> dmiCmSubscriptionPredicates) {
        return dmiInEventMapper.toDmiInEvent(dmiCmSubscriptionPredicates);
    }

    /**
     * Mapper to form a response for the client for the Cm Notification Subscription.
     *
     * @param subscriptionId                          Cm Notification Subscription id
     * @param dmiCmNotificationSubscriptionDetailsMap contains CmNotificationSubscriptionDetails per dmi plugin
     * @return CmNotificationSubscriptionNcmpOutEvent to sent back to the client
     */
    public NcmpOutEvent toNcmpOutEvent(final String subscriptionId,
         final Map<String, DmiCmSubscriptionDetails> dmiCmNotificationSubscriptionDetailsMap) {
        return ncmpOutEventMapper.toNcmpOutEvent(subscriptionId,
                dmiCmNotificationSubscriptionDetailsMap);
    }

    /**
     * Mapper to form a rejected response for the client for the Cm Notification Subscription Request.
     *
     * @param subscriptionId subscription id
     * @param rejectedTargetFilters list of rejected target filters for the subscription request
     * @return to sent back to the client
     */
    public NcmpOutEvent toNcmpOutEventForRejectedRequest(
            final String subscriptionId, final List<String> rejectedTargetFilters) {
        return ncmpOutEventMapper.toNcmpOutEventForRejectedRequest(
                subscriptionId, rejectedTargetFilters);
    }
}
