/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2021 Bell Canada.
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

import java.net.URI;
import java.net.URISyntaxException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.event.model.CpsAsyncRequestResponseEvent;
import org.onap.cps.event.model.CpsAsyncRequestResponseEventWithOrigin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listener for data updated events.
 */
@Component
@Slf4j
@AllArgsConstructor
public class cpsasyncrequestresponseeventConsumer {

    private static final String EVENT_SCHEMA_URN_PREFIX = "urn:cps:org.onap.cps:async-request-response-event-schema:v1";
    private static URI EVENT_SOURCE = null;

    @Autowired
    private CpsAsyncRequestResponseEventPublisherService cpsAsyncRequestResponseEventPublisherService;

    static {
        try {
            EVENT_SOURCE = new URI("urn:cps:org.onap.cps");
        } catch (final URISyntaxException exc) {
            exc.printStackTrace();
        }

    }

    private static final String EVENT_TYPE = "org.onap.cps.async-request-response-event";

    /**
     * Constructor for CpsAsyncRequestResponseEventListener.
     */
    public cpsasyncrequestresponseeventConsumer() {}

    /**
     * Consume the specified event.
     *
     * @param cpsAsyncRequestResponseEvent the event to be consumed and persisted.
     */
    @KafkaListener(topics = "${app.listener.ncmp.async-m2m.topic}",
        errorHandler = "cpsAsyncRequestResponseEventErrorHandler")
    public void consume(final CpsAsyncRequestResponseEvent cpsAsyncRequestResponseEvent) {
        log.info("Consuming {} ...", cpsAsyncRequestResponseEvent);

        final CpsAsyncRequestResponseEventUtil cpsAsyncRequestResponseEventUtil =
            new CpsAsyncRequestResponseEventUtil();

        final CpsAsyncRequestResponseEventWithOrigin cpsAsyncRequestResponseEventWithOrigin =
            cpsAsyncRequestResponseEventUtil.toCpsAsyncRequestResponseEventWithOrigin(cpsAsyncRequestResponseEvent);
        cpsAsyncRequestResponseEventPublisherService.publishEvent(
            cpsAsyncRequestResponseEventWithOrigin.getEventId(),
            cpsAsyncRequestResponseEventWithOrigin);
    }

}
