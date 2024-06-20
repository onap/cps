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

package org.onap.cps.ncmp.api.impl.events.cmsubscription.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.CmNotificationSubscriptionDelta;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.CmNotificationSubscriptionEventsHandler;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.CmNotificationSubscriptionMappersHandler;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.DmiCmNotificationSubscriptionCacheHandler;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.CmNotificationSubscriptionStatus;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.DmiCmNotificationSubscriptionDetails;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.DmiCmNotificationSubscriptionPredicate;
import org.onap.cps.ncmp.events.cmnotificationsubscription_merge1_0_0.client_to_ncmp.Predicate;
import org.onap.cps.ncmp.events.cmnotificationsubscription_merge1_0_0.ncmp_to_dmi.CmNotificationSubscriptionDmiInEvent;
import org.onap.cps.ncmp.events.cmsubscription_merge1_0_0.ncmp_to_client.CmNotificationSubscriptionNcmpOutEvent;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CmNotificationSubscriptionHandlerServiceImpl implements CmNotificationSubscriptionHandlerService {

    private final CmNotificationSubscriptionPersistenceService cmNotificationSubscriptionPersistenceService;
    private final CmNotificationSubscriptionDelta cmNotificationSubscriptionDelta;
    private final CmNotificationSubscriptionMappersHandler cmNotificationSubscriptionMappersHandler;
    private final CmNotificationSubscriptionEventsHandler cmNotificationSubscriptionEventsHandler;
    private final DmiCmNotificationSubscriptionCacheHandler dmiCmNotificationSubscriptionCacheHandler;

    private static final String SUBSCRIPTION_CREATE_EVENT_TYPE = "subscriptionCreateRequest";
    private static final String SUBSCRIPTION_DELETE_EVENT_TYPE = "subscriptionDeleteRequest";

    @Override
    public void processSubscriptionCreateRequest(final String subscriptionId, final List<Predicate> predicates) {
        if (cmNotificationSubscriptionPersistenceService.isUniqueSubscriptionId(subscriptionId)) {
            dmiCmNotificationSubscriptionCacheHandler.add(subscriptionId, predicates);
            handleCmNotificationSubscriptionDelta(subscriptionId, SUBSCRIPTION_CREATE_EVENT_TYPE);
            scheduleCmNotificationSubscriptionNcmpOutEventResponse(subscriptionId,
                    "subscriptionCreateResponse");
        } else {
            rejectAndPublishCmNotificationSubscriptionCreateRequest(subscriptionId, predicates);
        }
    }

    @Override
    public void processSubscriptionDeleteRequest(final String subscriptionId) {
        dmiCmNotificationSubscriptionCacheHandler.add(subscriptionId);
        handleCmNotificationSubscriptionDelta(subscriptionId, SUBSCRIPTION_DELETE_EVENT_TYPE);
        scheduleCmNotificationSubscriptionNcmpOutEventResponse(subscriptionId, "subscriptionDeleteResponse");
    }

    private void scheduleCmNotificationSubscriptionNcmpOutEventResponse(final String subscriptionId,
                                                                        final String eventType) {
        cmNotificationSubscriptionEventsHandler.publishCmNotificationSubscriptionNcmpOutEvent(subscriptionId,
                eventType, null, true);
    }

    private void rejectAndPublishCmNotificationSubscriptionCreateRequest(final String subscriptionId,
                                                                         final List<Predicate> predicates) {
        final Set<String> subscriptionTargetFilters =
                predicates.stream().flatMap(predicate -> predicate.getTargetFilter().stream())
                        .collect(Collectors.toSet());
        final CmNotificationSubscriptionNcmpOutEvent cmNotificationSubscriptionNcmpOutEvent =
                cmNotificationSubscriptionMappersHandler.toCmNotificationSubscriptionNcmpOutEventForRejectedRequest(
                        subscriptionId, new ArrayList<>(subscriptionTargetFilters));
        cmNotificationSubscriptionEventsHandler.publishCmNotificationSubscriptionNcmpOutEvent(subscriptionId,
                "subscriptionCreateResponse", cmNotificationSubscriptionNcmpOutEvent, false);
    }

    private void handleCmNotificationSubscriptionDelta(final String subscriptionId, final String eventType) {
        final Map<String, DmiCmNotificationSubscriptionDetails> dmiCmNotificationSubscriptionDetailsMap =
                dmiCmNotificationSubscriptionCacheHandler.get(subscriptionId);
        dmiCmNotificationSubscriptionDetailsMap.forEach((dmiPluginName, dmiCmNotificationSubscriptionDetails) -> {
            final List<DmiCmNotificationSubscriptionPredicate> dmiCmNotificationSubscriptionPredicates =
                    getDmiCmNotificationSubscriptionPredicatesDelta(subscriptionId, eventType,
                            dmiCmNotificationSubscriptionDetails);

            if (dmiCmNotificationSubscriptionPredicates.isEmpty()) {
                acceptAndPublishCmNotificationSubscriptionNcmpOutEventPerDmi(subscriptionId, dmiPluginName, eventType);
            } else {
                publishCmNotificationSubscriptionDmiInEventPerDmi(subscriptionId, dmiPluginName,
                        dmiCmNotificationSubscriptionPredicates, eventType);
            }
        });
    }

    private List<DmiCmNotificationSubscriptionPredicate> getDmiCmNotificationSubscriptionPredicatesDelta(
            final String subscriptionId, final String eventType,
            final DmiCmNotificationSubscriptionDetails dmiCmNotificationSubscriptionDetails) {
        List<DmiCmNotificationSubscriptionPredicate> dmiCmNotificationSubscriptionPredicates = new ArrayList<>();
        if (eventType.equals(SUBSCRIPTION_CREATE_EVENT_TYPE)) {
            dmiCmNotificationSubscriptionPredicates = cmNotificationSubscriptionDelta.getDelta(
                    dmiCmNotificationSubscriptionDetails.getDmiCmNotificationSubscriptionPredicates());
        }
        if (eventType.equals(SUBSCRIPTION_DELETE_EVENT_TYPE)) {
            dmiCmNotificationSubscriptionPredicates = cmNotificationSubscriptionDelta
                    .getPredicatesUsedOnlyBySubscriptionId(subscriptionId,
                            dmiCmNotificationSubscriptionDetails.getDmiCmNotificationSubscriptionPredicates());
        }
        return dmiCmNotificationSubscriptionPredicates;
    }

    private void publishCmNotificationSubscriptionDmiInEventPerDmi(final String subscriptionId,
                                                                   final String dmiPluginName,
                                                                   final List<DmiCmNotificationSubscriptionPredicate>
                                                                           dmiCmNotificationSubscriptionPredicates,
                                                                   final String eventType) {
        final CmNotificationSubscriptionDmiInEvent cmNotificationSubscriptionDmiInEvent =
                cmNotificationSubscriptionMappersHandler.toCmNotificationSubscriptionDmiInEvent(
                        dmiCmNotificationSubscriptionPredicates);
        cmNotificationSubscriptionEventsHandler.publishCmNotificationSubscriptionDmiInEvent(subscriptionId,
                dmiPluginName, eventType, cmNotificationSubscriptionDmiInEvent);
    }

    private void acceptAndPublishCmNotificationSubscriptionNcmpOutEventPerDmi(final String subscriptionId,
                                                                              final String dmiPluginName,
                                                                              final String eventType) {
        dmiCmNotificationSubscriptionCacheHandler.updateDmiCmNotificationSubscriptionStatusPerDmi(subscriptionId,
                dmiPluginName, CmNotificationSubscriptionStatus.ACCEPTED);
        if (eventType.equals(SUBSCRIPTION_CREATE_EVENT_TYPE)) {
            dmiCmNotificationSubscriptionCacheHandler.persistIntoDatabasePerDmi(subscriptionId, dmiPluginName);
        }
        if (eventType.equals(SUBSCRIPTION_DELETE_EVENT_TYPE)) {
            dmiCmNotificationSubscriptionCacheHandler.removeFromDatabasePerDmi(subscriptionId, dmiPluginName);
        }
    }

}