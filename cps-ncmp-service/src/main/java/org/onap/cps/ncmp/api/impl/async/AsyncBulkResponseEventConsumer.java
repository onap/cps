/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023 Nordix Foundation
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.events.EventsPublisher;
import org.onap.cps.ncmp.event.model.AsyncBulkResponseEvent;
import org.onap.cps.ncmp.event.model.BulkResponseEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listener for cps-ncmp async bulk response events.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.enabled", havingValue = "true", matchIfMissing = true)
public class AsyncBulkResponseEventConsumer {

    private final EventsPublisher<AsyncBulkResponseEvent> eventsPublisher;
    private final AsyncBulkResponseEventMapper asyncBulkResponseEventMapper;

    /**
     * Consume the specified event.
     *
     * @param bulkResponseEvent the event to be consumed and produced.
     */
    @KafkaListener(
            topics = "${app.ncmp.async-m2m.topic}",
            properties = {"spring.json.value.default.type=org.onap.cps.ncmp.event.model.BulkResponseEvent"})
    public void consumeAndForward(final BulkResponseEvent bulkResponseEvent) {
        log.debug("Consuming event {} ...", bulkResponseEvent);

        final AsyncBulkResponseEvent asyncBulkResponseEvent =
                asyncBulkResponseEventMapper.toNcmpAsyncBulkEvent(bulkResponseEvent);
        eventsPublisher.publishEvent(bulkResponseEvent.getEventTarget(),
                asyncBulkResponseEvent.getEventId(), asyncBulkResponseEvent);
    }
}
