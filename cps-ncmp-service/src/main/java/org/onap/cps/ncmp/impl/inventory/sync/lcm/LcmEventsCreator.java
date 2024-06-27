/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2024 Nordix Foundation
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

package org.onap.cps.ncmp.impl.inventory.sync.lcm;

import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.utils.EventDateTimeFormatter;
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle;
import org.onap.cps.ncmp.events.lcm.v1.Event;
import org.onap.cps.ncmp.events.lcm.v1.LcmEvent;
import org.onap.cps.ncmp.events.lcm.v1.LcmEventHeader;
import org.onap.cps.ncmp.events.lcm.v1.Values;
import org.springframework.stereotype.Component;


/**
 * LcmEventsCreator to create LcmEvent based on relevant operation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LcmEventsCreator {

    private final LcmEventHeaderMapper lcmEventHeaderMapper;

    /**
     * Populate Lifecycle Management Event.
     *
     * @param cmHandleId                  cm handle identifier
     * @param targetNcmpServiceCmHandle   target ncmp service cmhandle
     * @param existingNcmpServiceCmHandle existing ncmp service cmhandle
     * @return Populated LcmEvent
     */
    public LcmEvent populateLcmEvent(final String cmHandleId, final NcmpServiceCmHandle targetNcmpServiceCmHandle,
            final NcmpServiceCmHandle existingNcmpServiceCmHandle) {
        return createLcmEvent(cmHandleId, targetNcmpServiceCmHandle, existingNcmpServiceCmHandle);
    }

    /**
     * Populate Lifecycle Management Event Header.
     *
     * @param cmHandleId                  cm handle identifier
     * @param targetNcmpServiceCmHandle   target ncmp service cmhandle
     * @param existingNcmpServiceCmHandle existing ncmp service cmhandle
     * @return Populated LcmEventHeader
     */
    public LcmEventHeader populateLcmEventHeader(final String cmHandleId,
            final NcmpServiceCmHandle targetNcmpServiceCmHandle,
            final NcmpServiceCmHandle existingNcmpServiceCmHandle) {
        return createLcmEventHeader(cmHandleId, targetNcmpServiceCmHandle, existingNcmpServiceCmHandle);
    }

    private LcmEvent createLcmEvent(final String cmHandleId, final NcmpServiceCmHandle targetNcmpServiceCmHandle,
            final NcmpServiceCmHandle existingNcmpServiceCmHandle) {
        final LcmEventType lcmEventType =
                LcmEventsCreatorHelper.determineEventType(targetNcmpServiceCmHandle, existingNcmpServiceCmHandle);
        final LcmEvent lcmEvent = lcmEventHeader(cmHandleId, lcmEventType);
        lcmEvent.setEvent(
                lcmEventPayload(cmHandleId, targetNcmpServiceCmHandle, existingNcmpServiceCmHandle, lcmEventType));
        return lcmEvent;
    }

    private LcmEventHeader createLcmEventHeader(final String cmHandleId,
            final NcmpServiceCmHandle targetNcmpServiceCmHandle,
            final NcmpServiceCmHandle existingNcmpServiceCmHandle) {
        final LcmEventType lcmEventType =
                LcmEventsCreatorHelper.determineEventType(targetNcmpServiceCmHandle, existingNcmpServiceCmHandle);
        final LcmEvent lcmEventWithHeaderInformation = lcmEventHeader(cmHandleId, lcmEventType);
        return lcmEventHeaderMapper.toLcmEventHeader(lcmEventWithHeaderInformation);
    }

    private Event lcmEventPayload(final String eventCorrelationId, final NcmpServiceCmHandle targetNcmpServiceCmHandle,
            final NcmpServiceCmHandle existingNcmpServiceCmHandle, final LcmEventType lcmEventType) {
        final Event event = new Event();
        event.setCmHandleId(eventCorrelationId);
        event.setAlternateId(targetNcmpServiceCmHandle.getAlternateId());
        event.setModuleSetTag(targetNcmpServiceCmHandle.getModuleSetTag());
        event.setDataProducerIdentifier(targetNcmpServiceCmHandle.getDataProducerIdentifier());
        final CmHandleValuesHolder cmHandleValuesHolder =
                LcmEventsCreatorHelper.determineEventValues(targetNcmpServiceCmHandle, existingNcmpServiceCmHandle,
                        lcmEventType);
        event.setOldValues(cmHandleValuesHolder.getOldValues());
        event.setNewValues(cmHandleValuesHolder.getNewValues());

        return event;
    }

    private LcmEvent lcmEventHeader(final String eventCorrelationId, final LcmEventType lcmEventType) {
        final LcmEvent lcmEvent = new LcmEvent();
        lcmEvent.setEventId(UUID.randomUUID().toString());
        lcmEvent.setEventCorrelationId(eventCorrelationId);
        lcmEvent.setEventTime(EventDateTimeFormatter.getCurrentIsoFormattedDateTime());
        lcmEvent.setEventSource("org.onap.ncmp");
        lcmEvent.setEventType(lcmEventType.getEventType());
        lcmEvent.setEventSchema("org.onap.ncmp:cmhandle-lcm-event");
        lcmEvent.setEventSchemaVersion("1.0");
        return lcmEvent;
    }

    @NoArgsConstructor
    @Getter
    @Setter
    static class CmHandleValuesHolder {

        private Values oldValues;
        private Values newValues;
    }

}
