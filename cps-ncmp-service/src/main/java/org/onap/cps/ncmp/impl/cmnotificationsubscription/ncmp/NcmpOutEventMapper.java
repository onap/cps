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

package org.onap.cps.ncmp.impl.cmnotificationsubscription.ncmp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.CmSubscriptionStatus;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionDetails;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionPredicate;
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.ncmp_to_client.Data;
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.ncmp_to_client.NcmpOutEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NcmpOutEventMapper {

    /**
     * Mapper to form a response for the client for the Cm Notification Subscription.
     *
     * @param subscriptionId                          Cm Notification Subscription Id
     * @param dmiSubscriptionsPerDmi contains CmNotificationSubscriptionDetails per dmi plugin
     * @return CmNotificationSubscriptionNcmpOutEvent to sent back to the client
     */
    public NcmpOutEvent toNcmpOutEvent(final String subscriptionId,
            final Map<String, DmiCmSubscriptionDetails> dmiSubscriptionsPerDmi) {

        final NcmpOutEvent ncmpOutEvent = new NcmpOutEvent();
        final Data cmSubscriptionData = new Data();
        cmSubscriptionData.setSubscriptionId(subscriptionId);
        populateNcmpOutEventWithCmHandleIds(dmiSubscriptionsPerDmi,
                cmSubscriptionData);
        ncmpOutEvent.setData(cmSubscriptionData);

        return ncmpOutEvent;
    }

    /**
     * Mapper to form a rejected response for the client for the Cm Notification Subscription Request.
     *
     * @param subscriptionId subscription id
     * @param rejectedTargetFilters list of rejected target filters for the subscription request
     * @return to sent back to the client
     */
    public NcmpOutEvent toNcmpOutEventForRejectedRequest(final String subscriptionId,
            final List<String> rejectedTargetFilters) {
        final NcmpOutEvent ncmpOutEvent = new NcmpOutEvent();
        final Data cmSubscriptionData = new Data();
        cmSubscriptionData.setSubscriptionId(subscriptionId);
        cmSubscriptionData.setRejectedTargets(rejectedTargetFilters);
        ncmpOutEvent.setData(cmSubscriptionData);
        return ncmpOutEvent;
    }

    private void populateNcmpOutEventWithCmHandleIds(
            final Map<String, DmiCmSubscriptionDetails> dmiSubscriptionsPerDmi,
            final Data cmSubscriptionData) {

        final Collection<String> acceptedCmHandleIds = new HashSet<>();
        final Collection<String> pendingCmHandleIds = new HashSet<>();
        final Collection<String> rejectedCmHandleIds = new HashSet<>();

        dmiSubscriptionsPerDmi.forEach((dmiPluginName, dmiSubscriptionDetails) -> {
            final CmSubscriptionStatus cmSubscriptionStatus =
                    dmiSubscriptionDetails.getCmSubscriptionStatus();
            final List<DmiCmSubscriptionPredicate> dmiCmSubscriptionPredicates =
                    dmiSubscriptionDetails.getDmiCmSubscriptionPredicates();

            switch (cmSubscriptionStatus) {
                case ACCEPTED -> acceptedCmHandleIds.addAll(
                        extractCmHandleIds(dmiCmSubscriptionPredicates));
                case PENDING -> pendingCmHandleIds.addAll(extractCmHandleIds(dmiCmSubscriptionPredicates));
                default -> rejectedCmHandleIds.addAll(extractCmHandleIds(dmiCmSubscriptionPredicates));
            }
        });

        cmSubscriptionData.setAcceptedTargets(acceptedCmHandleIds);
        cmSubscriptionData.setPendingTargets(pendingCmHandleIds);
        cmSubscriptionData.setRejectedTargets(rejectedCmHandleIds);

    }

    private List<String> extractCmHandleIds(
            final List<DmiCmSubscriptionPredicate> dmiCmSubscriptionPredicates) {
        final List<String> cmHandleIds = new ArrayList<>();
        dmiCmSubscriptionPredicates.forEach(dmiSubscriptionPredicate -> cmHandleIds.addAll(
                dmiSubscriptionPredicate.getTargetCmHandleIds()));

        return cmHandleIds;
    }

}
