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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.onap.cps.events.EventProducer;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Batch consumer for AVC events (enabled via feature toggle).
 * Processes multiple events in a single transaction for improved throughput.
 * Active when batch processing is enabled.
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "notification.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(name = "ncmp.notifications.avc-event-consumer.batch-enabled")
public class CmAvcEventBatchConsumer extends CmAvcEventConsumer {

    public CmAvcEventBatchConsumer(final EventProducer eventProducer,
                                   final CmAvcEventService cmAvcEventService,
                                   final InventoryPersistence inventoryPersistence) {
        super(eventProducer, cmAvcEventService, inventoryPersistence);
    }

    /**
     * Consume and forward a batch of CM AVC events.
     * Events from ONAP-DMI-PLUGIN are processed to update the cache/database.
     * All events are then forwarded to the target topic in a single transaction.
     * Uses Kafka transactions to ensure exactly-once semantics.
     *
     * @param cmAvcEventBatch the incoming batch of consumer records
     */
    @KafkaListener(topics = "${app.dmi.cm-events.topic}",
            containerFactory = "cloudEventConcurrentKafkaListenerContainerFactoryForExactlyOnceSemantics")
    @Timed(value = "cps.ncmp.cm.notifications.consume.and.forward.batch",
            description = "Time taken to forward batch of CM AVC events")
    @Transactional(transactionManager = "kafkaExactlyOnceSemanticsTransactionManager")
    public void consumeAndForwardBatch(final ConsumerRecords<String, CloudEvent> cmAvcEventBatch) {
        log.debug("Processing batch of {} AVC events", cmAvcEventBatch.count());

        final List<Map.Entry<String, CloudEvent>> eventsToForward = new ArrayList<>(cmAvcEventBatch.count());

        for (final ConsumerRecord<String, CloudEvent> cmAvcEventAsConsumerRecord : cmAvcEventBatch) {
            if (isEventFromOnapDmiPlugin(cmAvcEventAsConsumerRecord.headers())) {
                processCmAvcEventChanges(cmAvcEventAsConsumerRecord);
            }
            eventsToForward.add(new AbstractMap.SimpleEntry<>(
                    cmAvcEventAsConsumerRecord.key(),
                    cmAvcEventAsConsumerRecord.value()
            ));
        }

        eventProducer.sendCloudEventBatch(cmEventsTopicName, eventsToForward);
    }
}
