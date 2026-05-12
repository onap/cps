/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2023-2026 OpenInfra Foundation Europe. All rights reserved.
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

import static org.onap.cps.ncmp.utils.events.CloudEventMapper.toTargetEvent;

import io.cloudevents.CloudEvent;
import io.cloudevents.kafka.impl.KafkaHeaders;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.header.Headers;
import org.onap.cps.events.EventProducer;
import org.onap.cps.ncmp.events.avc1_0_0.AvcEvent;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer for CM AVC (Attribute Value Change) events.
 * Supports two modes controlled by the batch-enabled property:
 * Single mode (default): processes one event at a time without transactions (max-poll-records=1).
 * Batch mode: processes multiple events in a single transaction with exactly-once semantics.
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "notification.enabled", matchIfMissing = true)
public class CmAvcEventConsumer {

    private static final String CLOUD_EVENT_SOURCE_SYSTEM_HEADER_KEY = "ce_source";
    private static final String ONAP_DMI_PLUGIN_SOURCE = "ONAP-DMI-PLUGIN";

    @Value("${app.ncmp.avc.cm-events-topic}")
    protected String cmEventsTopicName;

    protected final EventProducer eventProducer;
    private final CmAvcEventService cmAvcEventService;
    private final InventoryPersistence inventoryPersistence;
    private final Counter cmEventsForwardedCounter;

    /**
     * Constructor for CmAvcEventConsumer.
     *
     * @param eventProducer        the event producer for forwarding events
     * @param cmAvcEventService    the service for processing AVC events
     * @param inventoryPersistence the inventory persistence layer
     * @param meterRegistry        the meter registry for metrics
     */
    public CmAvcEventConsumer(final EventProducer eventProducer,
                              final CmAvcEventService cmAvcEventService,
                              final InventoryPersistence inventoryPersistence,
                              final MeterRegistry meterRegistry) {
        this.eventProducer = eventProducer;
        this.cmAvcEventService = cmAvcEventService;
        this.inventoryPersistence = inventoryPersistence;
        this.cmEventsForwardedCounter = Counter.builder("cps.ncmp.cm.avc.events.forwarded")
                .description("Total number of CM AVC events forwarded")
                .register(meterRegistry);
    }

    /**
     * Consume and forward CM AVC events.
     * In single mode (default), receives a batch of 1 event without transactions.
     * In batch mode, receives multiple events within a Kafka transaction (exactly-once semantics).
     * The container factory determines the mode based on the batch-enabled property.
     *
     * @param cmAvcEventBatch the incoming batch of consumer records
     */
    @KafkaListener(topics = "${app.dmi.cm-events.topic}",
            containerFactory = "cmAvcEventListenerContainerFactory")
    @Timed(value = "cps.ncmp.cm.notifications.consume.and.forward",
            description = "Time taken to forward CM AVC events")
    public void consumeAndForward(final ConsumerRecords<String, CloudEvent> cmAvcEventBatch) {
        log.debug("Processing {} AVC event(s)", cmAvcEventBatch.count());

        final List<Map.Entry<String, CloudEvent>> events = new ArrayList<>(cmAvcEventBatch.count());

        for (final ConsumerRecord<String, CloudEvent> cmAvcEventAsConsumerRecord : cmAvcEventBatch) {
            if (isEventFromOnapDmiPlugin(cmAvcEventAsConsumerRecord.headers())) {
                processCmAvcEventChanges(cmAvcEventAsConsumerRecord);
            }
            events.add(new AbstractMap.SimpleEntry<>(
                    cmAvcEventAsConsumerRecord.key(),
                    cmAvcEventAsConsumerRecord.value()
            ));
        }
        eventProducer.sendCloudEventBatch(cmEventsTopicName, events);
        cmEventsForwardedCounter.increment(events.size());
    }

    private void processCmAvcEventChanges(final ConsumerRecord<String, CloudEvent> cmAvcEventAsConsumerRecord) {
        final String cmHandleId = cmAvcEventAsConsumerRecord.key();
        final Boolean dataSyncEnabled = inventoryPersistence.getCmHandleState(cmHandleId).getDataSyncEnabled();
        if (Boolean.TRUE.equals(dataSyncEnabled)) {
            final AvcEvent cmAvcEvent = toTargetEvent(cmAvcEventAsConsumerRecord.value(), AvcEvent.class);
            log.debug("Event to be processed to update the cache with cmHandleId : {}", cmHandleId);
            if (cmAvcEvent != null) {
                cmAvcEventService.processCmAvcEvent(cmHandleId, cmAvcEvent);
            }
        }
    }

    private boolean isEventFromOnapDmiPlugin(final Headers headers) {
        final String sourceSystem = KafkaHeaders.getParsedKafkaHeader(headers, CLOUD_EVENT_SOURCE_SYSTEM_HEADER_KEY);
        return ONAP_DMI_PLUGIN_SOURCE.equals(sourceSystem);
    }
}
