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

package org.onap.cps.ncmp.impl.cmnotificationsubscription.ncmp;

import static org.onap.cps.ncmp.impl.cmnotificationsubscription.ncmp.NcmpOutEventProducer.buildAndGetNcmpOutEventAsCloudEvent;

import io.cloudevents.CloudEvent;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.events.EventsPublisher;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.cache.DmiCacheHandler;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionDetails;
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.ncmp_to_client.NcmpOutEvent;

@Slf4j
@RequiredArgsConstructor
public class NcmpOutEventPublishingTask implements Runnable {

    private final String topicName;
    private final String subscriptionId;
    private final String eventType;
    private final EventsPublisher<CloudEvent> eventsPublisher;
    private final NcmpOutEventMapper ncmpOutEventMapper;
    private final DmiCacheHandler dmiCacheHandler;

    /**
     * Delegating the responsibility of publishing NcmpOutEvent as a separate task which will
     * be called after a specified delay.
     */
    @Override
    public void run() {
        final Map<String, DmiCmSubscriptionDetails> dmiSubscriptionsPerDmi =
                dmiCacheHandler.get(subscriptionId);
        final NcmpOutEvent ncmpOutEvent = ncmpOutEventMapper.toNcmpOutEvent(subscriptionId,
                dmiSubscriptionsPerDmi);
        eventsPublisher.publishCloudEvent(topicName, subscriptionId,
                buildAndGetNcmpOutEventAsCloudEvent(subscriptionId, eventType,
                        ncmpOutEvent));
        dmiCacheHandler.removeAcceptedAndRejectedDmiSubscriptionEntries(subscriptionId);
    }
}
