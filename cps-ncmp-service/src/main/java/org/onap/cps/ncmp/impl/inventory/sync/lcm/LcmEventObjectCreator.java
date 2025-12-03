/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2025 OpenInfra Foundation Europe. All rights reserved.
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

import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.DELETED;
import static org.onap.cps.ncmp.impl.inventory.sync.lcm.LcmEventType.CREATE;
import static org.onap.cps.ncmp.impl.inventory.sync.lcm.LcmEventType.DELETE;
import static org.onap.cps.ncmp.impl.inventory.sync.lcm.LcmEventType.UPDATE;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle;
import org.onap.cps.ncmp.events.lcm.v1.Event;
import org.onap.cps.ncmp.events.lcm.v1.LcmEvent;
import org.onap.cps.ncmp.impl.utils.EventDateTimeFormatter;
import org.springframework.stereotype.Service;

/**
 * LcmEventObjectCreator to create the events send by LcmEventProducer.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class LcmEventObjectCreator {

    /**
     * Create Lifecycle Management Event.
     *
     * @param currentNcmpServiceCmHandle  current ncmp service cmhandle
     * @param targetNcmpServiceCmHandle   target ncmp service cmhandle
     * @return Populated LcmEvent
     */
    public LcmEvent createLcmEvent(final NcmpServiceCmHandle currentNcmpServiceCmHandle,
                                    final NcmpServiceCmHandle targetNcmpServiceCmHandle) {
        final String cmHandleId = targetNcmpServiceCmHandle.getCmHandleId();
        final LcmEventType lcmEventType =
            determineEventType(currentNcmpServiceCmHandle, targetNcmpServiceCmHandle);
        final LcmEvent lcmEvent = createLcmEventWithHeaderDetails(cmHandleId, lcmEventType);
        final Event event = new Event();
        event.setCmHandleId(cmHandleId);
        event.setAlternateId(targetNcmpServiceCmHandle.getAlternateId());
        event.setModuleSetTag(targetNcmpServiceCmHandle.getModuleSetTag());
        event.setDataProducerIdentifier(targetNcmpServiceCmHandle.getDataProducerIdentifier());
        final CmHandlePropertyUpdates cmHandlePropertyUpdates =
            determineEventValues(lcmEventType, currentNcmpServiceCmHandle, targetNcmpServiceCmHandle);
        event.setOldValues(cmHandlePropertyUpdates.getOldValues());
        event.setNewValues(cmHandlePropertyUpdates.getNewValues());
        lcmEvent.setEvent(event);
        return lcmEvent;
    }

    private static LcmEventType determineEventType(final NcmpServiceCmHandle currentNcmpServiceCmHandle,
                                                   final NcmpServiceCmHandle targetNcmpServiceCmHandle) {

        if (currentNcmpServiceCmHandle.getCompositeState() == null) {
            return CREATE;
        } else if (targetNcmpServiceCmHandle.getCompositeState().getCmHandleState() == DELETED) {
            return DELETE;
        }
        return UPDATE;
    }

    private static CmHandlePropertyUpdates determineEventValues(final LcmEventType lcmEventType,
                                                                final NcmpServiceCmHandle currentNcmpServiceCmHandle,
                                                                final NcmpServiceCmHandle targetNcmpServiceCmHandle) {
        if (CREATE == lcmEventType) {
            return CmHandlePropertyChangeDetector.determineUpdatesForCreate(targetNcmpServiceCmHandle);
        }
        if (UPDATE == lcmEventType) {
            return CmHandlePropertyChangeDetector.determineUpdates(currentNcmpServiceCmHandle,
                targetNcmpServiceCmHandle);
        }
        return new CmHandlePropertyUpdates();
    }

    private LcmEvent createLcmEventWithHeaderDetails(final String eventCorrelationId, final LcmEventType lcmEventType) {
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

}
