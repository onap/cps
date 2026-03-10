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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.onap.cps.events.EventProducer;
import org.onap.cps.ncmp.events.avc1_0_0.AvcEvent;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.springframework.beans.factory.annotation.Value;

/**
 * Abstract base class for AVC event consumers.
 * Provides common functionality for processing and forwarding CM AVC events.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class CmAvcEventConsumer {

    private static final String CLOUD_EVENT_SOURCE_SYSTEM_HEADER_KEY = "ce_source";
    private static final String ONAP_DMI_PLUGIN_SOURCE = "ONAP-DMI-PLUGIN";

    @Value("${app.ncmp.avc.cm-events-topic}")
    protected String cmEventsTopicName;

    protected final EventProducer eventProducer;
    protected final CmAvcEventService cmAvcEventService;
    protected final InventoryPersistence inventoryPersistence;

    /**
     * Process CM AVC event changes if the event is from ONAP-DMI-PLUGIN and data sync is enabled.
     *
     * @param cmAvcEventAsConsumerRecord the consumer record containing the event
     */
    protected void processCmAvcEventChanges(final ConsumerRecord<String, CloudEvent> cmAvcEventAsConsumerRecord) {
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

    /**
     * Check if the event is from ONAP-DMI-PLUGIN based on the ce_source header.
     *
     * @param headers the Kafka headers
     * @return true if the event is from ONAP-DMI-PLUGIN, false otherwise
     */
    protected boolean isEventFromOnapDmiPlugin(final Headers headers) {
        final String sourceSystem = KafkaHeaders.getParsedKafkaHeader(headers, CLOUD_EVENT_SOURCE_SYSTEM_HEADER_KEY);
        return ONAP_DMI_PLUGIN_SOURCE.equals(sourceSystem);
    }
}
