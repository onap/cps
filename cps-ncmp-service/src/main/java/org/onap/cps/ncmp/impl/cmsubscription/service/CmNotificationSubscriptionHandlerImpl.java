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

package org.onap.cps.ncmp.impl.cmsubscription.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.events.cmnotificationsubscription_merge1_0_0.client_to_ncmp.Predicate;
import org.onap.cps.ncmp.events.cmnotificationsubscription_merge1_0_0.ncmp_to_dmi.CmNotificationSubscriptionDmiInEvent;
import org.onap.cps.ncmp.events.cmsubscription_merge1_0_0.ncmp_to_client.CmNotificationSubscriptionNcmpOutEvent;
import org.onap.cps.ncmp.impl.cmsubscription.CmNotificationSubscriptionEventsFacade;
import org.onap.cps.ncmp.impl.cmsubscription.CmNotificationSubscriptionMappersFacade;
import org.onap.cps.ncmp.impl.cmsubscription.cache.DmiCacheHandler;
import org.onap.cps.ncmp.impl.cmsubscription.models.CmNotificationSubscriptionStatus;
import org.onap.cps.ncmp.impl.cmsubscription.models.DmiCmNotificationSubscriptionDetails;
import org.onap.cps.ncmp.impl.cmsubscription.models.DmiCmNotificationSubscriptionPredicate;
import org.onap.cps.ncmp.impl.cmsubscription.ncmp.CmNotificationSubscriptionComparator;
import org.onap.cps.ncmp.impl.cmsubscription.utils.CmNotificationSubscriptionPersistenceService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CmNotificationSubscriptionHandlerImpl implements CmNotificationSubscriptionHandler {

    private final CmNotificationSubscriptionPersistenceService cmNotificationSubscriptionPersistenceService;
    private final CmNotificationSubscriptionComparator cmNotificationSubscriptionComparator;
    private final CmNotificationSubscriptionMappersFacade cmNotificationSubscriptionMappersFacade;
    private final CmNotificationSubscriptionEventsFacade cmNotificationSubscriptionEventsFacade;
    private final DmiCacheHandler dmiCacheHandler;

    @Override
    public void processSubscriptionCreateRequest(final String subscriptionId, final List<Predicate> predicates) {
        if (cmNotificationSubscriptionPersistenceService.isUniqueSubscriptionId(subscriptionId)) {
            dmiCacheHandler.add(subscriptionId, predicates);
            handleCmNotificationSubscriptionDelta(subscriptionId);
            scheduleCmNotificationSubscriptionNcmpOutEventResponse(subscriptionId,
                    "subscriptionCreateResponse");
        } else {
            rejectAndPublishCmNotificationSubscriptionCreateRequest(subscriptionId, predicates);
        }
    }

    @Override
    public void processSubscriptionDeleteRequest(final String subscriptionId, final List<Predicate> predicates) {
        dmiCacheHandler.add(subscriptionId, predicates);
        sendSubscriptionDeleteRequestToDmi(subscriptionId);
        scheduleCmNotificationSubscriptionNcmpOutEventResponse(subscriptionId, "subscriptionDeleteResponse");
    }

    private void scheduleCmNotificationSubscriptionNcmpOutEventResponse(final String subscriptionId,
                                                                        final String eventType) {
        cmNotificationSubscriptionEventsFacade.publishCmNotificationSubscriptionNcmpOutEvent(subscriptionId,
                eventType, null, true);
    }

    private void rejectAndPublishCmNotificationSubscriptionCreateRequest(final String subscriptionId,
                                                                         final List<Predicate> predicates) {
        final Set<String> subscriptionTargetFilters =
                predicates.stream().flatMap(predicate -> predicate.getTargetFilter().stream())
                        .collect(Collectors.toSet());
        final CmNotificationSubscriptionNcmpOutEvent cmNotificationSubscriptionNcmpOutEvent =
                cmNotificationSubscriptionMappersFacade.toCmNotificationSubscriptionNcmpOutEventForRejectedRequest(
                        subscriptionId, new ArrayList<>(subscriptionTargetFilters));
        cmNotificationSubscriptionEventsFacade.publishCmNotificationSubscriptionNcmpOutEvent(subscriptionId,
                "subscriptionCreateResponse", cmNotificationSubscriptionNcmpOutEvent, false);
    }

    private void handleCmNotificationSubscriptionDelta(final String subscriptionId) {
        final Map<String, DmiCmNotificationSubscriptionDetails> dmiCmNotificationSubscriptionDetailsMap =
                dmiCacheHandler.get(subscriptionId);
        dmiCmNotificationSubscriptionDetailsMap.forEach((dmiPluginName, dmiCmNotificationSubscriptionDetails) -> {
            final List<DmiCmNotificationSubscriptionPredicate> dmiCmNotificationSubscriptionPredicates =
                    cmNotificationSubscriptionComparator.getNewDmiCmNotificationSubscriptionPredicates(
                            dmiCmNotificationSubscriptionDetails.getDmiCmNotificationSubscriptionPredicates());

            if (dmiCmNotificationSubscriptionPredicates.isEmpty()) {
                acceptAndPublishCmNotificationSubscriptionNcmpOutEventPerDmi(subscriptionId, dmiPluginName);
            } else {
                publishCmNotificationSubscriptionDmiInEventPerDmi(subscriptionId, dmiPluginName,
                        dmiCmNotificationSubscriptionPredicates);
            }
        });
    }

    private void publishCmNotificationSubscriptionDmiInEventPerDmi(final String subscriptionId,
                                                                   final String dmiPluginName,
                                                                   final List<DmiCmNotificationSubscriptionPredicate>
                                                                           dmiCmNotificationSubscriptionPredicates) {
        final CmNotificationSubscriptionDmiInEvent cmNotificationSubscriptionDmiInEvent =
                cmNotificationSubscriptionMappersFacade.toCmNotificationSubscriptionDmiInEvent(
                        dmiCmNotificationSubscriptionPredicates);
        cmNotificationSubscriptionEventsFacade.publishCmNotificationSubscriptionDmiInEvent(subscriptionId,
                dmiPluginName, "subscriptionCreateRequest", cmNotificationSubscriptionDmiInEvent);
    }

    private void acceptAndPublishCmNotificationSubscriptionNcmpOutEventPerDmi(final String subscriptionId,
                                                                              final String dmiPluginName) {
        dmiCacheHandler.updateDmiCmNotificationSubscriptionStatusPerDmi(subscriptionId,
                dmiPluginName, CmNotificationSubscriptionStatus.ACCEPTED);
        dmiCacheHandler.persistIntoDatabasePerDmi(subscriptionId, dmiPluginName);
    }

    private void sendSubscriptionDeleteRequestToDmi(final String subscriptionId) {
        final Map<String, DmiCmNotificationSubscriptionDetails> dmiCmNotificationSubscriptionDetailsMap =
                dmiCacheHandler.get(subscriptionId);
        dmiCmNotificationSubscriptionDetailsMap.forEach((dmiPluginName, dmiCmNotificationSubscriptionDetails) -> {
            final CmNotificationSubscriptionDmiInEvent cmNotificationSubscriptionDmiInEvent =
                    cmNotificationSubscriptionMappersFacade.toCmNotificationSubscriptionDmiInEvent(
                            dmiCmNotificationSubscriptionDetails.getDmiCmNotificationSubscriptionPredicates());
            cmNotificationSubscriptionEventsFacade.publishCmNotificationSubscriptionDmiInEvent(subscriptionId,
                    dmiPluginName, "subscriptionDeleteRequest", cmNotificationSubscriptionDmiInEvent);
        });
    }
}