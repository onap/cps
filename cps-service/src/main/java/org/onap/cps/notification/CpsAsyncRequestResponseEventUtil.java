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

    /**
     * Convert CpsAsyncRequestResponseEvent to CpsAsyncRequestResponseEventWithOrigin.
     *
     * @param cpsAsyncRequestResponseEvent the CpsAsyncRequestResponseEvent to convert
     * @return new CpsAsyncRequestResponseEventWithOrigin
     */
    public CpsAsyncRequestResponseEventWithOrigin toCpsAsyncRequestResponseEventWithOrigin(
        final CpsAsyncRequestResponseEvent cpsAsyncRequestResponseEvent) {
        final CpsAsyncRequestResponseEventWithOrigin cpsAsyncRequestResponseEventWithOrigin =
            new CpsAsyncRequestResponseEventWithOrigin();

        cpsAsyncRequestResponseEventWithOrigin.setEventId(UUID.randomUUID().toString());
        cpsAsyncRequestResponseEventWithOrigin.setEventCorrelationId(
            cpsAsyncRequestResponseEvent.getEventCorrelationId());
        cpsAsyncRequestResponseEventWithOrigin.setEventTime(getEventDateTime());
        cpsAsyncRequestResponseEventWithOrigin.setEventType(NcmpEvents.ASYNC_REQUEST.toString());

        cpsAsyncRequestResponseEventWithOrigin.setEventSchema(cpsAsyncRequestResponseEvent.getEventSchema());
        cpsAsyncRequestResponseEventWithOrigin.setEventSource(cpsAsyncRequestResponseEvent.getEventSource());
        cpsAsyncRequestResponseEventWithOrigin.setEventTarget(cpsAsyncRequestResponseEvent.getEventTarget());

        cpsAsyncRequestResponseEventWithOrigin.setEventOrigin(getEventOrigin(cpsAsyncRequestResponseEvent));
        cpsAsyncRequestResponseEventWithOrigin.setEvent(getEvent(cpsAsyncRequestResponseEvent));

        return cpsAsyncRequestResponseEventWithOrigin;
    }

    private EventOrigin getEventOrigin(final CpsAsyncRequestResponseEvent cpsAsyncRequestResponseEvent) {
        final EventOrigin eventOrigin = new EventOrigin();
        eventOrigin.setEventId(cpsAsyncRequestResponseEvent.getEventId());
        eventOrigin.setEventCorrelationId(cpsAsyncRequestResponseEvent.getEventCorrelationId());
        eventOrigin.setEventSchema(cpsAsyncRequestResponseEvent.getEventSchema());
        eventOrigin.setEventSource(cpsAsyncRequestResponseEvent.getEventSource());
        eventOrigin.setEventTarget(cpsAsyncRequestResponseEvent.getEventTarget());
        eventOrigin.setEventTime(cpsAsyncRequestResponseEvent.getEventTime());
        eventOrigin.setEventType(cpsAsyncRequestResponseEvent.getEventType());
        return eventOrigin;
    }

    private EventOriginContent getEvent(final CpsAsyncRequestResponseEvent cpsAsyncRequestResponseEvent) {
        final EventOriginContent eventOriginContent = new EventOriginContent();
        eventOriginContent.setResponseCode(cpsAsyncRequestResponseEvent.getEvent().getResponseCode());
        eventOriginContent.setResponseStatus(cpsAsyncRequestResponseEvent.getEvent().getResponseStatus());
        eventOriginContent.setResponseDataSchema(cpsAsyncRequestResponseEvent.getEvent().getResponseDataSchema());

        final OriginResponseData responseOriginData = new OriginResponseData();
        responseOriginData.setAdditionalProperty("data",
            cpsAsyncRequestResponseEvent.getEvent().getResponseData().getAdditionalProperties().get("data"));
        eventOriginContent.setResponseOriginData(responseOriginData);

        return eventOriginContent;
    }

    private String getEventDateTime() {
        return ZonedDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
    }
}
