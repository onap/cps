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

package org.onap.cps.notification;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.event.model.CpsAsyncRequestResponseEvent;
import org.onap.cps.event.model.CpsAsyncRequestResponseEventWithOrigin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listener for cps async request response events.
 */
@Component
@Slf4j
@AllArgsConstructor
public class CpsAsyncRequestResponseEventConsumer {

    @Autowired
    private CpsAsyncRequestResponseEventProducer cpsAsyncRequestResponseEventProducer;

    /**
     * Consume the specified event.
     *
     * @param cpsAsyncRequestResponseEvent the event to be consumed and persisted.
     */
    @KafkaListener(topics = "${app.ncmp.async-m2m.topic}", errorHandler = "cpsAsyncRequestResponseEventErrorHandler")
    public void consume(final CpsAsyncRequestResponseEvent cpsAsyncRequestResponseEvent) {
        log.debug("Consuming event {} ...", cpsAsyncRequestResponseEvent);

        final CpsAsyncRequestResponseEventUtil cpsAsyncRequestResponseEventUtil =
            new CpsAsyncRequestResponseEventUtil();

        final CpsAsyncRequestResponseEventWithOrigin cpsAsyncRequestResponseEventWithOrigin =
            cpsAsyncRequestResponseEventUtil.toCpsAsyncRequestResponseEventWithOrigin(cpsAsyncRequestResponseEvent);

        // In the future it may be ip: for example so we may want to handle that differently
        if (cpsAsyncRequestResponseEventWithOrigin.getEventTarget().contains("topic:")) {
            cpsAsyncRequestResponseEventProducer.sendMessage(
                cpsAsyncRequestResponseEventWithOrigin.getEventId(), cpsAsyncRequestResponseEventWithOrigin);
        }
    }
}
