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

package org.onap.cps.ncmp.api.impl.event.lcm;

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
 * LcmEventsCreator to create LcmEvent based on relevant operation.
 */
@Slf4j
@Component
public class LcmEventsCreator {


    /**
     * Populate NcmpEvent.
     *
     * @param cmHandleId          Cm Handle Identifier
     * @param ncmpServiceCmHandle Ncmp CmHandle Data
     * @return Populated NcmpEvent
     */
    public NcmpEvent populateLcmEvent(final String cmHandleId, final NcmpServiceCmHandle ncmpServiceCmHandle) {
        return createLcmEvent(cmHandleId, ncmpServiceCmHandle);
    }

    /**
     * Populate NcmpEvent for delete state.
     *
     * @param cmHandleId          Deleted CmHandle ID
     * @return Populated NcmpEvent
     */
    public NcmpEvent populateLcmEventForDeleteState(final String cmHandleId) {
        final Event deleteEvent = new Event();
        final NcmpEvent ncmpEvent = lcmEventHeader(cmHandleId);
        deleteEvent.setCmHandleId(cmHandleId);
        ncmpEvent.setEvent(deleteEvent);
        return ncmpEvent;
    }

    private NcmpEvent createLcmEvent(final String cmHandleId, final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final NcmpEvent ncmpEvent = lcmEventHeader(cmHandleId);
        ncmpEvent.setEvent(lcmEventPayload(cmHandleId, ncmpServiceCmHandle));
        return ncmpEvent;
    }

    private Event lcmEventPayload(final String eventCorrelationId, final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final Event event = new Event();
        event.setCmHandleId(eventCorrelationId);
        event.setCmhandleState(
                Event.CmhandleState.fromValue(ncmpServiceCmHandle.getCompositeState().getCmHandleState().toString()));
        event.setCmhandleProperties(List.of(ncmpServiceCmHandle.getPublicProperties()));
        return event;
    }

    private NcmpEvent lcmEventHeader(final String eventCorrelationId) {
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
