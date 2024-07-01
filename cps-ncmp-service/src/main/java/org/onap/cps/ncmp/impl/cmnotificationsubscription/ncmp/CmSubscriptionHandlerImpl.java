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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.EventsFacade;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.MappersFacade;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.cache.DmiCacheHandler;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.CmSubscriptionStatus;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionDetails;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionPredicate;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.utils.CmSubscriptionPersistenceService;
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.client_to_ncmp.Predicate;
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.ncmp_to_client.NcmpOutEvent;
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.ncmp_to_dmi.DmiInEvent;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CmSubscriptionHandlerImpl implements CmSubscriptionHandler {

    private final CmSubscriptionPersistenceService cmSubscriptionPersistenceService;
    private final CmSubscriptionComparator cmSubscriptionComparator;
    private final MappersFacade mappersFacade;
    private final EventsFacade eventsFacade;
    private final DmiCacheHandler dmiCacheHandler;

    @Override
    public void processSubscriptionCreateRequest(final String subscriptionId, final List<Predicate> predicates) {
        if (cmSubscriptionPersistenceService.isUniqueSubscriptionId(subscriptionId)) {
            dmiCacheHandler.add(subscriptionId, predicates);
            handleNewCmSubscription(subscriptionId);
            scheduleNcmpOutEventResponse(subscriptionId, "subscriptionCreateResponse");
        } else {
            rejectAndPublishCreateRequest(subscriptionId, predicates);
        }
    }

    @Override
    public void processSubscriptionDeleteRequest(final String subscriptionId, final List<Predicate> predicates) {
        dmiCacheHandler.add(subscriptionId, predicates);
        sendSubscriptionDeleteRequestToDmi(subscriptionId);
        scheduleNcmpOutEventResponse(subscriptionId, "subscriptionDeleteResponse");
    }

    private void scheduleNcmpOutEventResponse(final String subscriptionId, final String eventType) {
        eventsFacade.publishNcmpOutEvent(subscriptionId, eventType, null, true);
    }

    private void rejectAndPublishCreateRequest(final String subscriptionId, final List<Predicate> predicates) {
        final Set<String> subscriptionTargetFilters =
                predicates.stream().flatMap(predicate -> predicate.getTargetFilter().stream())
                        .collect(Collectors.toSet());
        final NcmpOutEvent ncmpOutEvent = mappersFacade.toNcmpOutEventForRejectedRequest(subscriptionId,
                new ArrayList<>(subscriptionTargetFilters));
        eventsFacade.publishNcmpOutEvent(subscriptionId, "subscriptionCreateResponse", ncmpOutEvent, false);
    }

    private void handleNewCmSubscription(final String subscriptionId) {
        final Map<String, DmiCmSubscriptionDetails> dmiSubscriptionDetailsMap =
                dmiCacheHandler.get(subscriptionId);
        dmiSubscriptionDetailsMap.forEach((dmiPluginName, dmiSubscriptionDetails) -> {
            final List<DmiCmSubscriptionPredicate> dmiCmSubscriptionPredicates =
                    cmSubscriptionComparator.getNewDmiSubscriptionPredicates(
                            dmiSubscriptionDetails.getDmiCmSubscriptionPredicates());

            if (dmiCmSubscriptionPredicates.isEmpty()) {
                acceptAndPublishNcmpOutEventPerDmi(subscriptionId, dmiPluginName);
            } else {
                publishDmiInEventPerDmi(subscriptionId, dmiPluginName, dmiCmSubscriptionPredicates);
            }
        });
    }

    private void publishDmiInEventPerDmi(final String subscriptionId, final String dmiPluginName,
            final List<DmiCmSubscriptionPredicate> dmiCmSubscriptionPredicates) {
        final DmiInEvent dmiInEvent = mappersFacade.toDmiInEvent(dmiCmSubscriptionPredicates);
        eventsFacade.publishDmiInEvent(subscriptionId, dmiPluginName,
                "subscriptionCreateRequest", dmiInEvent);
    }

    private void acceptAndPublishNcmpOutEventPerDmi(final String subscriptionId, final String dmiPluginName) {
        dmiCacheHandler.updateDmiSubscriptionStatusPerDmi(subscriptionId, dmiPluginName,
                CmSubscriptionStatus.ACCEPTED);
        dmiCacheHandler.persistIntoDatabasePerDmi(subscriptionId, dmiPluginName);
    }

    private void sendSubscriptionDeleteRequestToDmi(final String subscriptionId) {
        final Map<String, DmiCmSubscriptionDetails> dmiSubscriptionDetailsMap =
                dmiCacheHandler.get(subscriptionId);
        dmiSubscriptionDetailsMap.forEach((dmiPluginName, dmiSubscriptionDetails) -> {
            final DmiInEvent dmiInEvent = mappersFacade.toDmiInEvent(
                    dmiSubscriptionDetails.getDmiCmSubscriptionPredicates());
            eventsFacade.publishDmiInEvent(subscriptionId, dmiPluginName,
                    "subscriptionDeleteRequest", dmiInEvent);
        });
    }
}