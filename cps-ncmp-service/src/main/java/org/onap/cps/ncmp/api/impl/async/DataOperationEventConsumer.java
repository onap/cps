/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023-2024 Nordix Foundation
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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

package org.onap.cps.ncmp.api.impl.async;

import io.cloudevents.CloudEvent;
import io.cloudevents.kafka.impl.KafkaHeaders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.onap.cps.events.EventsPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listener for cps-ncmp async data operation events.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.enabled", havingValue = "true", matchIfMissing = true)
public class DataOperationEventConsumer {

    private final EventsPublisher<CloudEvent> eventsPublisher;

    /**
     * Consume the DataOperation cloud event published by producer to topic 'async-m2m.topic'
     * and publish the same to client specified topic.
     *
     * @param dataOperationEventConsumerRecord consuming event as a ConsumerRecord.
     */
    @KafkaListener(
            topics = "${app.ncmp.async-m2m.topic}",
            filter = "includeDataOperationEventsOnly",
            groupId = "ncmp-data-operation-event-group",
            containerFactory = "cloudEventConcurrentKafkaListenerContainerFactory")
    public void consumeAndPublish(final ConsumerRecord<String, CloudEvent> dataOperationEventConsumerRecord) {
        log.debug("Consuming event payload {} ...", dataOperationEventConsumerRecord.value());
        final String eventTarget = KafkaHeaders.getParsedKafkaHeader(
                dataOperationEventConsumerRecord.headers(), "ce_destination");
        final String correlationId = KafkaHeaders.getParsedKafkaHeader(
                dataOperationEventConsumerRecord.headers(), "ce_correlationid");
        eventsPublisher.publishCloudEvent(eventTarget, correlationId, dataOperationEventConsumerRecord.value());
    }
}
