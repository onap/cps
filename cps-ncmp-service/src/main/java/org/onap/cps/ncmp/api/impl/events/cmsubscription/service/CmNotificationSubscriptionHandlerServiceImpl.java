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
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.CmNotificationSubscriptionNcmpOutEventProducer;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.DmiCmNotificationSubscriptionCacheHandler;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.mapper.CmNotificationSubscriptionNcmpOutEventMapper;
import org.onap.cps.ncmp.events.cmnotificationsubscription_merge1_0_0.client_to_ncmp.CmNotificationSubscriptionNcmpInEvent;
import org.onap.cps.ncmp.events.cmnotificationsubscription_merge1_0_0.client_to_ncmp.Predicate;
import org.onap.cps.ncmp.events.cmsubscription_merge1_0_0.ncmp_to_client.CmNotificationSubscriptionNcmpOutEvent;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CmNotificationSubscriptionHandlerServiceImpl implements CmNotificationSubscriptionHandlerService {

    private final CmNotificationSubscriptionPersistenceService cmNotificationSubscriptionPersistenceService;
    private final CmNotificationSubscriptionNcmpOutEventMapper cmNotificationSubscriptionNcmpOutEventMapper;
    private final CmNotificationSubscriptionNcmpOutEventProducer cmNotificationSubscriptionNcmpOutEventProducer;
    private final DmiCmNotificationSubscriptionCacheHandler dmiCmNotificationSubscriptionCacheHandler;

    @Override
    public void processSubscriptionCreateRequest(
        final CmNotificationSubscriptionNcmpInEvent cmNotificationSubscriptionNcmpInEvent) {
        final String subscriptionId = cmNotificationSubscriptionNcmpInEvent.getData().getSubscriptionId();
        final List<Predicate> predicates = cmNotificationSubscriptionNcmpInEvent.getData().getPredicates();

        if (cmNotificationSubscriptionPersistenceService.isUniqueSubscriptionId(subscriptionId)) {
            dmiCmNotificationSubscriptionCacheHandler.add(subscriptionId, predicates);
        } else {
            final Set<String> subscriptionTargetFilters = predicates.stream().flatMap(
                predicate -> predicate.getTargetFilter().stream()).collect(Collectors.toSet());
            rejectSubscriptionCreateRequest(subscriptionId, new ArrayList<>(subscriptionTargetFilters));
        }
    }

    private void rejectSubscriptionCreateRequest(final String subscriptionId,
                                                final List<String> subscriptionTargetFilters) {
        final CmNotificationSubscriptionNcmpOutEvent cmNotificationSubscriptionNcmpOutEvent =
            cmNotificationSubscriptionNcmpOutEventMapper
                .toCmNotificationSubscriptionNcmpOutEventForRejectedRequest(subscriptionId,
                    subscriptionTargetFilters);
        cmNotificationSubscriptionNcmpOutEventProducer.publishCmNotificationSubscriptionNcmpOutEvent(subscriptionId,
            "subscriptionCreateResponse", cmNotificationSubscriptionNcmpOutEvent, false);
    }
}
