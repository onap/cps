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

package org.onap.cps.notification.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.onap.cps.event.model.CpsAsyncRequestResponseEvent;
import org.onap.cps.event.model.CpsAsyncRequestResponseEventWithOrigin;
import org.onap.cps.event.model.EventOrigin;
import org.onap.cps.event.model.EventOriginContent;

/**
 * Util for converting CpsAsyncRequestResponseEvent to CpsAsyncRequestResponseEventWithOrigin.
 */
public class CpsAsyncRequestResponseEventUtil {

    private ObjectMapper mapper =  new ObjectMapper();

    public static final String DATA = "response-data";

    /**
     * Convert CpsAsyncRequestResponseEvent to CpsAsyncRequestResponseEventWithOrigin.
     *
     * @param receivedAsyncEvent the CpsAsyncRequestResponseEvent to convert
     * @return new CpsAsyncRequestResponseEventWithOrigin
     */
    public CpsAsyncRequestResponseEventWithOrigin toCpsAsyncRequestResponseEventWithOrigin(
        final CpsAsyncRequestResponseEvent receivedAsyncEvent) {
        final CpsAsyncRequestResponseEventWithOrigin outgoingAsyncRequest =
            mapper.convertValue(receivedAsyncEvent, CpsAsyncRequestResponseEventWithOrigin.class);

        outgoingAsyncRequest.setEventId(UUID.randomUUID().toString());
        outgoingAsyncRequest.setEventTime(getEventDateTime());

        outgoingAsyncRequest.setEventOrigin(getReceivedEventOrigin(receivedAsyncEvent));
        outgoingAsyncRequest.setEvent(getReceivedEvent(receivedAsyncEvent));

        return outgoingAsyncRequest;
    }

    private EventOrigin getReceivedEventOrigin(final CpsAsyncRequestResponseEvent receivedAsyncEvent) {
        final EventOrigin outgoingAsyncRequestEventOrigin = new EventOrigin();

        outgoingAsyncRequestEventOrigin.setEventId(receivedAsyncEvent.getEventId());
        outgoingAsyncRequestEventOrigin.setEventCorrelationId(receivedAsyncEvent.getEventCorrelationId());
        outgoingAsyncRequestEventOrigin.setEventSchema(receivedAsyncEvent.getEventSchema());
        outgoingAsyncRequestEventOrigin.setEventSource(receivedAsyncEvent.getEventSource());
        outgoingAsyncRequestEventOrigin.setEventTarget(receivedAsyncEvent.getEventTarget());
        outgoingAsyncRequestEventOrigin.setEventTime(receivedAsyncEvent.getEventTime());
        outgoingAsyncRequestEventOrigin.setEventType(receivedAsyncEvent.getEventType());

        return outgoingAsyncRequestEventOrigin;
    }

    private EventOriginContent getReceivedEvent(final CpsAsyncRequestResponseEvent receivedAsyncEvent) {
        final EventOriginContent outgoingAsyncRequestEventOriginContent = new EventOriginContent();

        outgoingAsyncRequestEventOriginContent.setResponseCode(receivedAsyncEvent.getEvent().getResponseCode());
        outgoingAsyncRequestEventOriginContent.setResponseStatus(receivedAsyncEvent.getEvent().getResponseStatus());
        outgoingAsyncRequestEventOriginContent.setResponseDataSchema(
            receivedAsyncEvent.getEvent().getResponseDataSchema());
        outgoingAsyncRequestEventOriginContent.setAdditionalProperty(
            DATA, receivedAsyncEvent.getEvent().getAdditionalProperties().get(DATA));
        return outgoingAsyncRequestEventOriginContent;
    }

    private String getEventDateTime() {
        return ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
    }

}
