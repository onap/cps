/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.datajobs.subscription.cmavc;

import io.cloudevents.CloudEvent;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.onap.cps.events.EventProducer;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Single-record consumer for AVC events (default mode).
 * Processes one event at a time without transactions.
 * Active when batch processing is disabled (default).
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "notification.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(name = "ncmp.notifications.avc-event-consumer.batch-enabled", havingValue = "false",
        matchIfMissing = true)
public class CmAvcEventSingleConsumer extends CmAvcEventConsumer {

    public CmAvcEventSingleConsumer(final EventProducer eventProducer,
                                    final CmAvcEventService cmAvcEventService,
                                    final InventoryPersistence inventoryPersistence) {
        super(eventProducer, cmAvcEventService, inventoryPersistence);
    }

    /**
     * Consume and forward a single CM AVC event.
     * If the event is from ONAP-DMI-PLUGIN, it will be processed to update the cache/database.
     * The event is then forwarded to the target topic with the same key to preserve message ordering.
     *
     * @param cmAvcEventAsConsumerRecord the incoming consumer record
     */
    @KafkaListener(topics = "${app.dmi.cm-events.topic}",
            containerFactory = "cloudEventConcurrentKafkaListenerContainerFactory")
    @Timed(value = "cps.ncmp.cm.notifications.consume.and.forward",
            description = "Time taken to forward CM AVC events")
    public void consumeAndForward(final ConsumerRecord<String, CloudEvent> cmAvcEventAsConsumerRecord) {
        if (isEventFromOnapDmiPlugin(cmAvcEventAsConsumerRecord.headers())) {
            processCmAvcEventChanges(cmAvcEventAsConsumerRecord);
        }
        log.debug("Consuming AVC event with key : {} and value : {}",
                cmAvcEventAsConsumerRecord.key(), cmAvcEventAsConsumerRecord.value());
        eventProducer.sendCloudEvent(cmEventsTopicName,
                cmAvcEventAsConsumerRecord.key(),
                cmAvcEventAsConsumerRecord.value());
    }
}

