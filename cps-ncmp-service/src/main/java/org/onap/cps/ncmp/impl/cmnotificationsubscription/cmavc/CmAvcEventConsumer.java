/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2023-2024 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.impl.cmnotificationsubscription.cmavc;

import io.cloudevents.CloudEvent;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.onap.cps.events.EventsPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listener for AVC events based on Cm Subscriptions.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.enabled", havingValue = "true", matchIfMissing = true)
public class CmAvcEventConsumer {


    @Value("${app.ncmp.avc.cm-events-topic}")
    private String cmEventsTopicName;

    private final EventsPublisher<CloudEvent> eventsPublisher;

    /**
     * Incoming AvcEvent in the form of Consumer Record.
     *
     * @param cmAvcEventAsConsumerRecord Incoming raw consumer record
     */
    @KafkaListener(topics = "${app.dmi.cm-events.topic}",
            containerFactory = "cloudEventConcurrentKafkaListenerContainerFactory")
    public void consumeAndForward(
            final ConsumerRecord<String, CloudEvent> cmAvcEventAsConsumerRecord) {
        final CloudEvent outgoingAvcEvent = cmAvcEventAsConsumerRecord.value();
        log.debug("Consuming AVC event {} ...", outgoingAvcEvent);
        final String newEventId = UUID.randomUUID().toString();
        eventsPublisher.publishCloudEvent(cmEventsTopicName, newEventId, outgoingAvcEvent);
    }
}
