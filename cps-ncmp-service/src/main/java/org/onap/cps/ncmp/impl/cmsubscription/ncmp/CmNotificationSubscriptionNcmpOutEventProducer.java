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

package org.onap.cps.ncmp.impl.cmsubscription.ncmp;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.events.EventsPublisher;
import org.onap.cps.ncmp.events.cmsubscription_merge1_0_0.ncmp_to_client.CmNotificationSubscriptionNcmpOutEvent;
import org.onap.cps.ncmp.impl.cmsubscription.CmNotificationSubscriptionMappersFacade;
import org.onap.cps.ncmp.impl.cmsubscription.cache.DmiCacheHandler;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.enabled", havingValue = "true", matchIfMissing = true)
public class CmNotificationSubscriptionNcmpOutEventProducer {

    @Value("${app.ncmp.avc.cm-subscription-ncmp-out}")
    private String cmNotificationSubscriptionNcmpOutEventTopic;

    @Value("${ncmp.timers.subscription-forwarding.dmi-response-timeout-ms}")
    private Integer cmNotificationSubscriptionDmiOutEventTimeoutInMs;

    private final EventsPublisher<CloudEvent> eventsPublisher;
    private final JsonObjectMapper jsonObjectMapper;
    private final CmNotificationSubscriptionMappersFacade cmNotificationSubscriptionMappersFacade;
    private final DmiCacheHandler dmiCacheHandler;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private static final Map<String, ScheduledFuture<?>> scheduledTasksPerSubscriptionId = new ConcurrentHashMap<>();

    /**
     * Publish the event to the client who requested the subscription with key as subscription id and event is Cloud
     * Event compliant.
     *
     * @param subscriptionId                         Cm Subscription Id
     * @param eventType                              Type of event
     * @param cmNotificationSubscriptionNcmpOutEvent Cm Notification Subscription Event for the
     *                                               client
     * @param isScheduledEvent                       Determines if the event is to be scheduled
     *                                               or published now
     */
    public void publishCmNotificationSubscriptionNcmpOutEvent(final String subscriptionId, final String eventType,
                                                              final CmNotificationSubscriptionNcmpOutEvent
                                                                      cmNotificationSubscriptionNcmpOutEvent,
                                                              final boolean isScheduledEvent) {

        if (isScheduledEvent && !scheduledTasksPerSubscriptionId.containsKey(subscriptionId)) {
            final ScheduledFuture<?> scheduledFuture =
                    scheduleAndPublishCmNotificationSubscriptionNcmpOutEvent(subscriptionId, eventType);
            scheduledTasksPerSubscriptionId.putIfAbsent(subscriptionId, scheduledFuture);
            log.debug("Scheduled the CmNotificationSubscriptionEvent for subscriptionId : {}", subscriptionId);
        } else {
            cancelScheduledTaskForSubscriptionId(subscriptionId);
            publishCmNotificationSubscriptionNcmpOutEventNow(subscriptionId, eventType,
                    cmNotificationSubscriptionNcmpOutEvent);
            log.info("Published CmNotificationSubscriptionEvent on demand for subscriptionId : {}", subscriptionId);
        }
    }

    private ScheduledFuture<?> scheduleAndPublishCmNotificationSubscriptionNcmpOutEvent(final String subscriptionId,
                                                                                        final String eventType) {
        final CmNotificationSubscriptionNcmpOutEventPublishingTask
                cmNotificationSubscriptionNcmpOutEventPublishingTask =
                new CmNotificationSubscriptionNcmpOutEventPublishingTask(cmNotificationSubscriptionNcmpOutEventTopic,
                        subscriptionId, eventType, eventsPublisher, jsonObjectMapper,
                        cmNotificationSubscriptionMappersFacade, dmiCacheHandler);
        return scheduledExecutorService.schedule(cmNotificationSubscriptionNcmpOutEventPublishingTask,
                cmNotificationSubscriptionDmiOutEventTimeoutInMs, TimeUnit.MILLISECONDS);
    }

    private void cancelScheduledTaskForSubscriptionId(final String subscriptionId) {

        final ScheduledFuture<?> scheduledFuture = scheduledTasksPerSubscriptionId.get(subscriptionId);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            scheduledTasksPerSubscriptionId.remove(subscriptionId);
        }

    }


    private void publishCmNotificationSubscriptionNcmpOutEventNow(final String subscriptionId, final String eventType,
                                                                  final CmNotificationSubscriptionNcmpOutEvent
                                                                          cmNotificationSubscriptionNcmpOutEvent) {
        final CloudEvent cmNotificationSubscriptionNcmpOutEventAsCloudEvent =
                buildAndGetCmNotificationNcmpOutEventAsCloudEvent(jsonObjectMapper, subscriptionId, eventType,
                        cmNotificationSubscriptionNcmpOutEvent);
        eventsPublisher.publishCloudEvent(cmNotificationSubscriptionNcmpOutEventTopic, subscriptionId,
                cmNotificationSubscriptionNcmpOutEventAsCloudEvent);
        dmiCacheHandler
                .removeAcceptedAndRejectedDmiCmNotificationSubscriptionEntries(subscriptionId);
    }

    /**
     * Get an NCMP out event as cloud event.
     *
     * @param jsonObjectMapper  JSON object mapper
     * @param subscriptionId subscription id
     * @param eventType event type
     * @param cmNotificationSubscriptionNcmpOutEvent cm notification subscription NCMP out event
     * @return cm notification subscription NCMP out event as cloud event
     */
    public static CloudEvent buildAndGetCmNotificationNcmpOutEventAsCloudEvent(
            final JsonObjectMapper jsonObjectMapper, final String subscriptionId, final String eventType,
            final CmNotificationSubscriptionNcmpOutEvent cmNotificationSubscriptionNcmpOutEvent) {

        return CloudEventBuilder.v1().withId(UUID.randomUUID().toString()).withType(eventType)
                .withSource(URI.create("NCMP")).withDataSchema(URI.create("org.onap.ncmp.cm.subscription:1.0.0"))
                .withExtension("correlationid", subscriptionId)
                .withData(jsonObjectMapper.asJsonBytes(cmNotificationSubscriptionNcmpOutEvent)).build();
    }

}
