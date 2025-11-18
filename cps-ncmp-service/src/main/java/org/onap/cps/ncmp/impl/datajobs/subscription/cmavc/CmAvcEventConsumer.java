/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.onap.cps.events.EventsProducer;
import org.onap.cps.ncmp.events.avc1_0_0.AvcEvent;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listener for AVC events based on CM Subscriptions.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.enabled", havingValue = "true", matchIfMissing = true)
public class CmAvcEventConsumer {

    private static final String CLOUD_EVENT_SOURCE_SYSTEM_HEADER_KEY = "ce_source";

    @Value("${app.ncmp.avc.cm-events-topic}")
    private String cmEventsTopicName;

    private final EventsProducer eventsProducer;
    private final CmAvcEventService cmAvcEventService;
    private final InventoryPersistence inventoryPersistence;

    /**
     * Incoming Cm AvcEvent in the form of Consumer Record, it will be forwarded as is to a target topic.
     * The key from incoming record will be used as key for the target topic as well to preserve the message ordering.
     * If event is coming from ONAP-DMI-PLUGIN then the event will also be processed by NCMP and the cps cache/database
     * will be updated.
     *
     * @param cmAvcEventAsConsumerRecord Incoming raw consumer record
     */
    @Transactional
    @KafkaListener(topics = "${app.dmi.cm-events.topic}",
        containerFactory = "cloudEventConcurrentKafkaListenerContainerFactoryForEos")
    @Timed(value = "cps.ncmp.cm.notifications.consume.and.forward", description = "Time taken to forward CM AVC events")
    public void consumeAndForward(final ConsumerRecord<String, CloudEvent> cmAvcEventAsConsumerRecord) {
        if (isEventFromOnapDmiPlugin(cmAvcEventAsConsumerRecord.headers())) {
            processCmAvcEventChanges(cmAvcEventAsConsumerRecord);
        }
        final CloudEvent outgoingAvcEvent = cmAvcEventAsConsumerRecord.value();
        final String outgoingAvcEventKey = cmAvcEventAsConsumerRecord.key();

        log.debug("Consuming AVC event with key : {} and value : {}", outgoingAvcEventKey, outgoingAvcEvent);
        eventsProducer.sendCloudEventUsingEos(cmEventsTopicName, outgoingAvcEventKey, outgoingAvcEvent);
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
        return "ONAP-DMI-PLUGIN".equals(sourceSystem);
    }
}
