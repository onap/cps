/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2022 Nordix Foundation.
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

package org.onap.cps.notification.async;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.event.model.DmiAsyncRequestResponseEvent;
import org.onap.cps.event.model.NcmpAsyncRequestResponseEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listener for cps-ncmp async request response events.
 */
@Component
@Slf4j
@AllArgsConstructor
public class NcmpAsyncRequestResponseEventConsumer {

    @Autowired
    private NcmpAsyncRequestResponseEventProducer ncmpAsyncRequestResponseEventProducer;

    /**
     * Consume the specified event.
     *
     * @param dmiAsyncRequestResponseEvent the event to be consumed and produced.
     */
    @KafkaListener(topics = "${app.ncmp.async-m2m.topic}")
    public void consume(final DmiAsyncRequestResponseEvent dmiAsyncRequestResponseEvent) {
        log.debug("Consuming event {} ...", dmiAsyncRequestResponseEvent);

        final NcmpAsyncRequestResponseEvent ncmpAsyncRequestResponseEvent =
            NcmpAsyncRequestResponseEventUtil.toNcmpAsyncEvent(dmiAsyncRequestResponseEvent);

        ncmpAsyncRequestResponseEventProducer.sendMessage(
                ncmpAsyncRequestResponseEvent.getEventId(), ncmpAsyncRequestResponseEvent);
    }
}
