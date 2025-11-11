/*
 *  ===========LICENSE_START========================================================
 * Copyright (c) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.impl.datajobs.subscription.ncmp;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.impl.datajobs.subscription.client_to_ncmp.DataJob;
import org.onap.cps.ncmp.impl.datajobs.subscription.client_to_ncmp.DataJobSubscriptionOperationInEvent;
import org.onap.cps.ncmp.impl.datajobs.subscription.client_to_ncmp.DataSelector;
import org.onap.cps.ncmp.impl.utils.JexParser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.enabled", havingValue = "true", matchIfMissing = true)
public class NcmpInEventConsumer {

    private final CmSubscriptionHandler cmSubscriptionHandler;

    /**
     * Consume the specified event.
     *
     * @param dataJobSubscriptionOperationInEvent the event to be consumed
     */
    @KafkaListener(topics = "${app.ncmp.avc.cm-subscription-ncmp-in}",
        containerFactory = "legacyEventConcurrentKafkaListenerContainerFactory",
        properties = {"spring.json.value.default.type="
                + "org.onap.cps.ncmp.impl.datajobs.subscription.client_to_ncmp.DataJobSubscriptionOperationInEvent"})
    public void consumeSubscriptionEvent(
        final DataJobSubscriptionOperationInEvent dataJobSubscriptionOperationInEvent) {
        final String eventType = dataJobSubscriptionOperationInEvent.getEventType();
        final DataJob dataJob = dataJobSubscriptionOperationInEvent.getEvent().getDataJob();
        final String dataJobId = dataJob.getId();

        log.info("Consumed subscription event with details: | dataJobId={} | eventType={}", dataJobId, eventType);

        try {
            switch (eventType) {
                case "dataJobCreated" -> handleCreate(dataJobId, dataJob);
                case "dataJobDeleted" -> cmSubscriptionHandler.deleteSubscription(dataJobId);
                default -> log.warn("Unknown eventType={} for dataJobId={}", eventType, dataJobId);
            }
        } finally {
            log.info("NCMP In Event with eventType={} has been Processed for dataJobId={}", eventType, dataJobId);
        }
    }

    private void handleCreate(final String dataJobId, final DataJob dataJob) {
        final String dataNodeSelector =
                dataJob.getProductionJobDefinition().getTargetSelector().getDataNodeSelector();
        final List<String> dataNodeSelectors = JexParser.toXpaths(dataNodeSelector);
        final DataSelector dataSelector = dataJob.getProductionJobDefinition().getDataSelector();
        cmSubscriptionHandler.createSubscription(dataSelector, dataJobId, dataNodeSelectors);
    }
}
