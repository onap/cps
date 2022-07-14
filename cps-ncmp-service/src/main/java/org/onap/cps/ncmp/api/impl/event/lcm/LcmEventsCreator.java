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

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.utils.DateBuilder;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.ncmp.cmhandle.event.lcm.Event;
import org.onap.ncmp.cmhandle.event.lcm.LcmEvent;
import org.springframework.stereotype.Component;


/**
 * LcmEventsCreator to create LcmEvent based on relevant operation.
 */
@Slf4j
@Component
public class LcmEventsCreator {

    /**
     * Populate LcmEvent.
     *
     * @param cmHandleId          Cm Handle Identifier
     * @param ncmpServiceCmHandle Ncmp CmHandle Data
     * @return Populated LcmEvent
     */
    public LcmEvent populateLcmEvent(final String cmHandleId, final NcmpServiceCmHandle ncmpServiceCmHandle) {
        return createLcmEvent(cmHandleId, ncmpServiceCmHandle);
    }

    private LcmEvent createLcmEvent(final String cmHandleId, final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final LcmEvent lcmEvent = lcmEventHeader(cmHandleId);
        lcmEvent.setEvent(lcmEventPayload(cmHandleId, ncmpServiceCmHandle));
        return lcmEvent;
    }

    private Event lcmEventPayload(final String eventCorrelationId, final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final Event event = new Event();
        event.setCmHandleId(eventCorrelationId);
        return event;
    }

    private LcmEvent lcmEventHeader(final String eventCorrelationId) {
        final LcmEvent lcmEvent = new LcmEvent();
        lcmEvent.setEventId(UUID.randomUUID().toString());
        lcmEvent.setEventCorrelationId(eventCorrelationId);
        lcmEvent.setEventTime(DateBuilder.getFormattedDate());
        lcmEvent.setEventSource("org.onap.ncmp");
        lcmEvent.setEventType("org.onap.ncmp.cmhandle-lcm-event");
        lcmEvent.setEventSchema("org.onap.ncmp:cmhandle-lcm-event");
        lcmEvent.setEventSchemaVersion("v1");
        return lcmEvent;
    }

}
