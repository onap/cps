/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2023 Nordix Foundation.
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

package org.onap.cps.ncmp.api.impl.events.avc;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.onap.cps.ncmp.api.impl.events.EventsPublisher;
import org.onap.cps.ncmp.events.avc.v1.AvcEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

/**
 * Listener for AVC events.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.enabled", havingValue = "true", matchIfMissing = true)
public class AvcEventConsumer {


    @Value("${app.ncmp.avc.cm-events-topic}")
    private String cmEventsTopicName;

    private final EventsPublisher<AvcEvent> eventsPublisher;
    private final AvcEventMapper avcEventMapper;


    /**
     * Incoming AvcEvent in the form of Consumer Record.
     *
     * @param avcEventConsumerRecord Incoming raw consumer record
     */
    @KafkaListener(topics = "${app.dmi.cm-events.topic}",
            properties = {"spring.json.value.default.type=org.onap.cps.ncmp.events.avc.v1.AvcEvent"})
    public void consumeAndForward(final ConsumerRecord<String, AvcEvent> avcEventConsumerRecord) {
        log.debug("Consuming AVC event {} ...", avcEventConsumerRecord.value());
        final String mutatedEventId = UUID.randomUUID().toString();
        mutateEventHeaderWithEventId(avcEventConsumerRecord.headers(), mutatedEventId);
        final AvcEvent outgoingAvcEvent = avcEventMapper.toOutgoingAvcEvent(avcEventConsumerRecord.value());
        eventsPublisher.publishEvent(cmEventsTopicName, mutatedEventId, avcEventConsumerRecord.headers(),
                outgoingAvcEvent);
    }

    private void mutateEventHeaderWithEventId(final Headers eventHeaders, final String mutatedEventId) {
        final Headers existingEventId = eventHeaders.remove("eventId");
        log.info("Removing existing eventId from header : {} and updating with id : {}", existingEventId.toString(),
                mutatedEventId);
        eventHeaders.add(new RecordHeader("eventId", SerializationUtils.serialize(mutatedEventId)));

    }
}
