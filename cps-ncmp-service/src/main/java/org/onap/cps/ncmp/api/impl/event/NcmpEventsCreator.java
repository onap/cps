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

package org.onap.cps.ncmp.api.impl.event;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.ncmp.cmhandle.lcm.event.Event;
import org.onap.ncmp.cmhandle.lcm.event.NcmpEvent;
import org.springframework.stereotype.Component;


/**
 * NcmpEventsCreator to create NcmpEvent based on relevant operation.
 */
@Slf4j
@Component
public class NcmpEventsCreator {


    /**
     * Populate NcmpEvent.
     *
     * @param cmHandleId          Cm Handle Identifier
     * @param ncmpServiceCmHandle Ncmp CmHandle Data
     * @return Populated NcmpEvent
     */
    public NcmpEvent populateNcmpEvent(final String cmHandleId, final NcmpServiceCmHandle ncmpServiceCmHandle) {
        return createNcmpEvent(cmHandleId, ncmpServiceCmHandle);
    }

    private NcmpEvent createNcmpEvent(final String cmHandleId, final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final NcmpEvent ncmpEvent = ncmpEventHeader(cmHandleId);
        ncmpEvent.setEvent(ncmpEventPayload(cmHandleId, ncmpServiceCmHandle));
        return ncmpEvent;
    }

    private Event ncmpEventPayload(final String eventCorrelationId, final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final Event event = new Event();
        event.setCmHandleId(eventCorrelationId);
        event.setCmhandleState(
                Event.CmhandleState.fromValue(ncmpServiceCmHandle.getCompositeState().getCmHandleState().toString()));
        event.setCmhandleProperties(List.of(ncmpServiceCmHandle.getPublicProperties()));
        return event;
    }

    private NcmpEvent ncmpEventHeader(final String eventCorrelationId) {
        final NcmpEvent ncmpEvent = new NcmpEvent();
        ncmpEvent.setEventId(UUID.randomUUID().toString());
        ncmpEvent.setEventCorrelationId(eventCorrelationId);
        ncmpEvent.setEventTime(ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")));
        ncmpEvent.setEventSource("org.onap.ncmp");
        ncmpEvent.setEventType("org.onap.ncmp.cmhandle-lcm-event");
        ncmpEvent.setEventSchema("org.onap.ncmp:cmhandle-lcm-event:v1");
        return ncmpEvent;
    }

}
