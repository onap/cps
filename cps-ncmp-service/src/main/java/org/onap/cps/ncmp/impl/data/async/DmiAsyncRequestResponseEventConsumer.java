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

package org.onap.cps.ncmp.impl.data.async;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.events.EventsProducer;
import org.onap.cps.ncmp.event.model.DmiAsyncRequestResponseEvent;
import org.onap.cps.ncmp.event.model.NcmpAsyncRequestResponseEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listener for cps-ncmp async request response events.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.enabled", havingValue = "true", matchIfMissing = true)
public class DmiAsyncRequestResponseEventConsumer {

    private final EventsProducer<NcmpAsyncRequestResponseEvent> eventsProducer;
    private final NcmpAsyncRequestResponseEventMapper ncmpAsyncRequestResponseEventMapper;

    /**
     * Consume the specified event.
     *
     * @param dmiAsyncRequestResponseEvent the event to be consumed and produced.
     */
    @KafkaListener(
            topics = "${app.ncmp.async-m2m.topic}",
            filter = "includeNonCloudEventsOnly",
            groupId = "ncmp-async-rest-request-event-group",
            containerFactory = "legacyEventConcurrentKafkaListenerContainerFactory",
            properties = {"spring.json.value.default.type=org.onap.cps.ncmp.event.model.DmiAsyncRequestResponseEvent"})
    public void consumeAndForward(final DmiAsyncRequestResponseEvent dmiAsyncRequestResponseEvent) {
        log.debug("Consuming event {} ...", dmiAsyncRequestResponseEvent);
        final NcmpAsyncRequestResponseEvent ncmpAsyncRequestResponseEvent =
                ncmpAsyncRequestResponseEventMapper.toNcmpAsyncEvent(dmiAsyncRequestResponseEvent);
        eventsProducer.sendEvent(ncmpAsyncRequestResponseEvent.getEventTarget(),
                                     ncmpAsyncRequestResponseEvent.getEventId(),
                                     ncmpAsyncRequestResponseEvent);
    }
}
