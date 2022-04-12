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

    public static final String DATA = "data";

    /**
     * Convert CpsAsyncRequestResponseEvent to CpsAsyncRequestResponseEventWithOrigin.
     *
     * @param receivedAsyncEvent the CpsAsyncRequestResponseEvent to convert
     * @return new CpsAsyncRequestResponseEventWithOrigin
     */
    public CpsAsyncRequestResponseEventWithOrigin toCpsAsyncRequestResponseEventWithOrigin(
        final CpsAsyncRequestResponseEvent receivedAsyncEvent) {
        final CpsAsyncRequestResponseEventWithOrigin clientAsyncRequest =
            mapper.convertValue(receivedAsyncEvent, CpsAsyncRequestResponseEventWithOrigin.class);
        clientAsyncRequest.setEventId(UUID.randomUUID().toString());
        clientAsyncRequest.setEventTime(getEventDateTime());
        clientAsyncRequest.setEventOrigin(mapper.convertValue(receivedAsyncEvent, EventOrigin.class));
        clientAsyncRequest.setEvent(mapper.convertValue(receivedAsyncEvent, EventOriginContent.class));
        return clientAsyncRequest;
    }

    private String getEventDateTime() {
        return ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
    }

    private Object getOriginData(final CpsAsyncRequestResponseEvent receivedAsyncEvent) {
        return receivedAsyncEvent.getEvent().getResponseData().getAdditionalProperties().get(DATA);
    }

}
