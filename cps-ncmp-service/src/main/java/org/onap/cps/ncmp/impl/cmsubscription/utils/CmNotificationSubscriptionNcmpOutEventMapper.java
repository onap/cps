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

package org.onap.cps.ncmp.impl.cmsubscription.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.events.cmsubscription_merge1_0_0.ncmp_to_client.CmNotificationSubscriptionNcmpOutEvent;
import org.onap.cps.ncmp.events.cmsubscription_merge1_0_0.ncmp_to_client.Data;
import org.onap.cps.ncmp.impl.cmsubscription.models.CmNotificationSubscriptionStatus;
import org.onap.cps.ncmp.impl.cmsubscription.models.DmiCmNotificationSubscriptionDetails;
import org.onap.cps.ncmp.impl.cmsubscription.models.DmiCmNotificationSubscriptionPredicate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CmNotificationSubscriptionNcmpOutEventMapper {

    /**
     * Mapper to form a response for the client for the Cm Notification Subscription.
     *
     * @param subscriptionId                          Cm Notification Subscription Id
     * @param dmiCmNotificationSubscriptionDetailsMap contains CmNotificationSubscriptionDetails per dmi plugin
     * @return CmNotificationSubscriptionNcmpOutEvent to sent back to the client
     */
    public CmNotificationSubscriptionNcmpOutEvent toCmNotificationSubscriptionNcmpOutEvent(final String subscriptionId,
            final Map<String, DmiCmNotificationSubscriptionDetails> dmiCmNotificationSubscriptionDetailsMap) {

        final CmNotificationSubscriptionNcmpOutEvent cmNotificationSubscriptionNcmpOutEvent =
                new CmNotificationSubscriptionNcmpOutEvent();
        final Data cmSubscriptionData = new Data();
        cmSubscriptionData.setSubscriptionId(subscriptionId);
        populateCmNotificationSubscriptionNcmpOutEventWithCmHandleIds(dmiCmNotificationSubscriptionDetailsMap,
                cmSubscriptionData);
        cmNotificationSubscriptionNcmpOutEvent.setData(cmSubscriptionData);

        return cmNotificationSubscriptionNcmpOutEvent;
    }

    /**
     * Mapper to form a rejected response for the client for the Cm Notification Subscription Request.
     *
     * @param subscriptionId subscription id
     * @param rejectedTargetFilters list of rejected target filters for the subscription request
     * @return to sent back to the client
     */
    public CmNotificationSubscriptionNcmpOutEvent toCmNotificationSubscriptionNcmpOutEventForRejectedRequest(
            final String subscriptionId, final List<String> rejectedTargetFilters) {
        final CmNotificationSubscriptionNcmpOutEvent cmNotificationSubscriptionNcmpOutEvent =
                new CmNotificationSubscriptionNcmpOutEvent();
        final Data cmSubscriptionData = new Data();
        cmSubscriptionData.setSubscriptionId(subscriptionId);
        cmSubscriptionData.setRejectedTargets(rejectedTargetFilters);
        cmNotificationSubscriptionNcmpOutEvent.setData(cmSubscriptionData);
        return cmNotificationSubscriptionNcmpOutEvent;
    }

    private void populateCmNotificationSubscriptionNcmpOutEventWithCmHandleIds(
            final Map<String, DmiCmNotificationSubscriptionDetails> dmiCmNotificationSubscriptionDetailsMap,
            final Data cmSubscriptionData) {

        final List<String> acceptedCmHandleIds = new ArrayList<>();
        final List<String> pendingCmHandleIds = new ArrayList<>();
        final List<String> rejectedCmHandleIds = new ArrayList<>();

        dmiCmNotificationSubscriptionDetailsMap.forEach((dmiPluginName, dmiCmNotificationSubscriptionDetails) -> {
            final CmNotificationSubscriptionStatus cmNotificationSubscriptionStatus =
                    dmiCmNotificationSubscriptionDetails.getCmNotificationSubscriptionStatus();
            final List<DmiCmNotificationSubscriptionPredicate> dmiCmNotificationSubscriptionPredicates =
                    dmiCmNotificationSubscriptionDetails.getDmiCmNotificationSubscriptionPredicates();

            switch (cmNotificationSubscriptionStatus) {
                case ACCEPTED -> acceptedCmHandleIds.addAll(
                        extractCmHandleIds(dmiCmNotificationSubscriptionPredicates));
                case PENDING -> pendingCmHandleIds.addAll(extractCmHandleIds(dmiCmNotificationSubscriptionPredicates));
                default -> rejectedCmHandleIds.addAll(extractCmHandleIds(dmiCmNotificationSubscriptionPredicates));
            }
        });

        cmSubscriptionData.setAcceptedTargets(acceptedCmHandleIds);
        cmSubscriptionData.setPendingTargets(pendingCmHandleIds);
        cmSubscriptionData.setRejectedTargets(rejectedCmHandleIds);

    }

    private List<String> extractCmHandleIds(
            final List<DmiCmNotificationSubscriptionPredicate> dmiCmNotificationSubscriptionPredicates) {
        final List<String> cmHandleIds = new ArrayList<>();
        dmiCmNotificationSubscriptionPredicates.forEach(dmiCmNotificationSubscriptionPredicate -> cmHandleIds.addAll(
                dmiCmNotificationSubscriptionPredicate.getTargetCmHandleIds()));

        return cmHandleIds;
    }

}
