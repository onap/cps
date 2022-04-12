/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Nordix Foundation
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

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.event.model.DmiAsyncRequestResponseEvent;
import org.onap.cps.event.model.EventContent;
import org.onap.cps.event.model.ForwardedEvent;
import org.onap.cps.event.model.NcmpAsyncRequestResponseEvent;

/**
 * Util for converting DmiAsyncRequestResponseEvent to NcmpAsyncRequestResponseEvent.
 */
@Slf4j
public class NcmpAsyncRequestResponseEventUtil {

    private static final String DATA = "response-data";

    private static final DateTimeFormatter dateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    /**
     * Convert DmiAsyncRequestResponseEvent to NcmpAsyncRequestResponseEvent.
     *
     * @param dmiAsyncRequestResponseEvent the DmiAsyncRequestResponseEvent to convert
     * @return new NcmpAsyncRequestResponseEvent
     */
    public static NcmpAsyncRequestResponseEvent toNcmpAsyncEvent(
        final DmiAsyncRequestResponseEvent dmiAsyncRequestResponseEvent) {
        final NcmpAsyncRequestResponseEvent ncmpAsyncRequestResponseEvent = new NcmpAsyncRequestResponseEvent();

        ncmpAsyncRequestResponseEvent.setEventId(UUID.randomUUID().toString());
        ncmpAsyncRequestResponseEvent.setEventCorrelationId(dmiAsyncRequestResponseEvent.getEventCorrelationId());
        ncmpAsyncRequestResponseEvent.setEventTime(getFormattedCurrentTime());
        ncmpAsyncRequestResponseEvent.setEventTarget(dmiAsyncRequestResponseEvent.getEventTarget());
        ncmpAsyncRequestResponseEvent.setEventType(NcmpAsyncRequestResponseEvent.class.getName());

        ncmpAsyncRequestResponseEvent.setForwardedEvent(getForwardedEvent(dmiAsyncRequestResponseEvent));

        return ncmpAsyncRequestResponseEvent;
    }

    private static ForwardedEvent getForwardedEvent(final DmiAsyncRequestResponseEvent dmiAsyncRequestResponseEvent) {
        final ForwardedEvent forwardedEvent = new ForwardedEvent();

        forwardedEvent.setEventId(dmiAsyncRequestResponseEvent.getEventId());
        forwardedEvent.setEventCorrelationId(dmiAsyncRequestResponseEvent.getEventCorrelationId());
        forwardedEvent.setEventSchema(dmiAsyncRequestResponseEvent.getEventSchema());
        forwardedEvent.setEventSource(dmiAsyncRequestResponseEvent.getEventSource());
        forwardedEvent.setEventTarget(dmiAsyncRequestResponseEvent.getEventTarget());
        forwardedEvent.setEventTime(dmiAsyncRequestResponseEvent.getEventTime());
        forwardedEvent.setEventType(dmiAsyncRequestResponseEvent.getEventType());

        final EventContent eventContent = dmiAsyncRequestResponseEvent.getEventContent();
        forwardedEvent.setResponseStatus(eventContent.getResponseStatus());
        forwardedEvent.setResponseCode(eventContent.getResponseCode());
        forwardedEvent.setResponseDataSchema(eventContent.getResponseDataSchema());
        forwardedEvent.setAdditionalProperty(DATA, eventContent.getResponseData().getAdditionalProperties());

        return forwardedEvent;
    }

    private static String getFormattedCurrentTime() {
        return ZonedDateTime.now().format(dateTimeFormatter);
    }

}
