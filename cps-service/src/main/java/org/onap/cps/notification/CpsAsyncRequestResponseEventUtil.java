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

package org.onap.cps.notification;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.onap.cps.event.model.CpsAsyncRequestResponseEvent;
import org.onap.cps.event.model.CpsAsyncRequestResponseEventWithOrigin;
import org.onap.cps.event.model.EventOrigin;
import org.onap.cps.event.model.EventOriginContent;
import org.onap.cps.event.model.OriginResponseData;

/**
 * Util for converting CpsAsyncRequestResponseEvent to CpsAsyncRequestResponseEventWithOrigin.
 */
public class CpsAsyncRequestResponseEventUtil {

    public static final String DATA = "data";

    /**
     * Convert CpsAsyncRequestResponseEvent to CpsAsyncRequestResponseEventWithOrigin.
     *
     * @param receivedAsyncEvent the CpsAsyncRequestResponseEvent to convert
     * @return new CpsAsyncRequestResponseEventWithOrigin
     */
    public CpsAsyncRequestResponseEventWithOrigin toCpsAsyncRequestResponseEventWithOrigin(
        final CpsAsyncRequestResponseEvent receivedAsyncEvent) {
        final CpsAsyncRequestResponseEventWithOrigin clientAsyncRequest = new CpsAsyncRequestResponseEventWithOrigin();

        clientAsyncRequest.setEventId(UUID.randomUUID().toString());
        clientAsyncRequest.setEventCorrelationId(receivedAsyncEvent.getEventCorrelationId());

        clientAsyncRequest.setEventTime(getEventDateTime());
        clientAsyncRequest.setEventType(NcmpEvents.ASYNC_REQUEST.toString());
        clientAsyncRequest.setEventSchema(receivedAsyncEvent.getEventSchema());
        clientAsyncRequest.setEventSource(receivedAsyncEvent.getEventSource());
        clientAsyncRequest.setEventTarget(receivedAsyncEvent.getEventTarget());

        clientAsyncRequest.setEventOrigin(getEventOrigin(receivedAsyncEvent));
        clientAsyncRequest.setEvent(getEvent(receivedAsyncEvent));

        return clientAsyncRequest;
    }

    private EventOrigin getEventOrigin(final CpsAsyncRequestResponseEvent receivedAsyncEvent) {
        final EventOrigin eventOrigin = new EventOrigin();

        eventOrigin.setEventId(receivedAsyncEvent.getEventId());
        eventOrigin.setEventCorrelationId(receivedAsyncEvent.getEventCorrelationId());

        eventOrigin.setEventSchema(receivedAsyncEvent.getEventSchema());
        eventOrigin.setEventSource(receivedAsyncEvent.getEventSource());
        eventOrigin.setEventTarget(receivedAsyncEvent.getEventTarget());
        eventOrigin.setEventTime(receivedAsyncEvent.getEventTime());
        eventOrigin.setEventType(receivedAsyncEvent.getEventType());

        return eventOrigin;
    }

    private EventOriginContent getEvent(final CpsAsyncRequestResponseEvent receivedAsyncEvent) {
        final EventOriginContent eventOriginContent = new EventOriginContent();

        eventOriginContent.setResponseCode(receivedAsyncEvent.getEvent().getResponseCode());
        eventOriginContent.setResponseStatus(receivedAsyncEvent.getEvent().getResponseStatus());
        eventOriginContent.setResponseDataSchema(receivedAsyncEvent.getEvent().getResponseDataSchema());

        final OriginResponseData responseOriginData = new OriginResponseData();
        responseOriginData.setAdditionalProperty(DATA, getOriginData(receivedAsyncEvent));
        eventOriginContent.setResponseOriginData(responseOriginData);

        return eventOriginContent;
    }

    private String getEventDateTime() {
        return ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
    }

    private Object getOriginData(CpsAsyncRequestResponseEvent receivedAsyncEvent) {
        return receivedAsyncEvent.getEvent().getResponseData().getAdditionalProperties().get(DATA);
    }

}
