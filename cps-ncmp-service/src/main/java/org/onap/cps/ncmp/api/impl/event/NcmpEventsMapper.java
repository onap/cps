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

import static org.onap.ncmp.cmhandle.lcm.event.Event.CmhandleState.READY;
import static org.onap.ncmp.cmhandle.lcm.event.Event.Operation.CREATE;
import static org.onap.ncmp.cmhandle.lcm.event.Event.Operation.DELETE;
import static org.onap.ncmp.cmhandle.lcm.event.Event.Operation.UPDATE;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.ncmp.cmhandle.lcm.event.Event;
import org.onap.ncmp.cmhandle.lcm.event.Event.Operation;
import org.onap.ncmp.cmhandle.lcm.event.NcmpEvent;
import org.springframework.stereotype.Component;


/**
 * NcmpEventsMapper to create NcmpEvent based on relevant operation.
 */
@Slf4j
@Component
public class NcmpEventsMapper {


    /**
     * Convert to NcmpEvent.
     *
     * @param cmHandleId          Cm Handle Id
     * @param operation           Relevant Operation
     * @param ncmpServiceCmHandle Ncmp CmHandle Data
     * @return Populated NcmpEvent
     */
    public NcmpEvent toNcmpEvent(final String cmHandleId, final Operation operation,
            final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final NcmpEvent ncmpEvent = ncmpEventHeader(cmHandleId);
        switch (operation) {
            case CREATE:
                ncmpEvent.setEvent(createEvent(cmHandleId, ncmpServiceCmHandle));
                break;
            case UPDATE:
                ncmpEvent.setEvent(updateEvent(cmHandleId, ncmpServiceCmHandle));
                break;
            case DELETE:
                ncmpEvent.setEvent(deleteEvent(cmHandleId));
                break;
            default:
                log.error("{} operation not supported", operation);
        }
        return ncmpEvent;
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

    private Event createEvent(final String eventCorrelationId, final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final Event event = new Event();
        event.setOperation(CREATE);
        event.setCmHandleId(eventCorrelationId);
        event.setCmhandleState(READY);
        event.setCmhandleProperties(List.of(ncmpServiceCmHandle.getPublicProperties()));
        return event;
    }

    private Event updateEvent(final String eventCorrelationId, final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final Event event = new Event();
        event.setOperation(UPDATE);
        event.setCmHandleId(eventCorrelationId);
        event.setCmhandleState(READY);
        event.setCmhandleProperties(List.of(ncmpServiceCmHandle.getPublicProperties()));
        return event;
    }

    private Event deleteEvent(final String eventCorrelationId) {
        final Event event = new Event();
        event.setCmHandleId(eventCorrelationId);
        event.setOperation(DELETE);
        return event;
    }

}
