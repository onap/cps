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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.utils.YangDataConverter;
import org.onap.cps.ncmp.api.inventory.CmHandleState;
import org.onap.cps.ncmp.api.inventory.CompositeStateBuilder;
import org.onap.cps.ncmp.api.inventory.InventoryPersistence;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.ncmp.cmhandle.lcm.event.Event;
import org.onap.ncmp.cmhandle.lcm.event.NcmpEvent;
import org.springframework.stereotype.Component;


/**
 * NcmpEventsCreator to create NcmpEvent based on relevant state transition.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NcmpEventsCreator {

    private final InventoryPersistence inventoryPersistence;

    /**
     * Populate NcmpEvent.
     *
     * @param cmHandleId                  Cm Handle Identifier
     * @param ncmpCmHandleStateTransition Ncmp CmHandle State Transition details
     * @return Populated NcmpEvent
     */
    public NcmpEvent populateNcmpEvent(final String cmHandleId,
            final NcmpCmHandleStateTransition ncmpCmHandleStateTransition) {
        return createNcmpEvent(cmHandleId, ncmpCmHandleStateTransition);
    }

    private NcmpEvent createNcmpEvent(final String cmHandleId,
            final NcmpCmHandleStateTransition ncmpCmHandleStateTransition) {
        final NcmpEvent ncmpEvent = ncmpEventHeader(cmHandleId);
        final NcmpServiceCmHandle ncmpServiceCmHandle =
                ncmpServiceCmHandleForIdAndCmHandleStateTransition(cmHandleId, ncmpCmHandleStateTransition);
        ncmpEvent.setEvent(ncmpEventPayload(cmHandleId, ncmpServiceCmHandle));
        return ncmpEvent;
    }

    private Event ncmpEventPayload(final String eventCorrelationId, final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final Event event = new Event();
        event.setCmHandleId(eventCorrelationId);
        event.setCmhandleState(
                Event.CmhandleState.fromValue(ncmpServiceCmHandle.getCompositeState().getCmHandleState().toString()));
        if (!ncmpServiceCmHandle.getPublicProperties().isEmpty()) {
            event.setCmhandleProperties(List.of(ncmpServiceCmHandle.getPublicProperties()));
        }
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

    private NcmpServiceCmHandle ncmpServiceCmHandleForIdAndCmHandleStateTransition(final String cmHandleId,
            final NcmpCmHandleStateTransition ncmpCmHandleStateTransition) {

        if (ncmpCmHandleStateTransition == NcmpCmHandleStateTransition.NOTHING_TO_ADVISED) {
            return getNothingToAdvisedCmHandle(cmHandleId);
        } else if (ncmpCmHandleStateTransition == NcmpCmHandleStateTransition.ANY_TO_DELETING) {
            return getDeletedOrDeletingCmHandle(cmHandleId, CmHandleState.DELETING);
        } else if (ncmpCmHandleStateTransition == NcmpCmHandleStateTransition.DELETING_TO_DELETED) {
            return getDeletedOrDeletingCmHandle(cmHandleId, CmHandleState.DELETED);
        } else {
            return getNcmpServiceCmHandleFromPersistenceLayer(cmHandleId);
        }
    }

    private NcmpServiceCmHandle getDeletedOrDeletingCmHandle(final String cmHandleId,
            final CmHandleState cmHandleState) {
        final NcmpServiceCmHandle ncmpServiceCmHandle = new NcmpServiceCmHandle();
        ncmpServiceCmHandle.setCmHandleId(cmHandleId);
        ncmpServiceCmHandle.setCompositeState(
                new CompositeStateBuilder().withLastUpdatedTimeNow().withCmHandleState(cmHandleState).build());
        return ncmpServiceCmHandle;
    }

    private NcmpServiceCmHandle getNothingToAdvisedCmHandle(final String cmHandleId) {
        final NcmpServiceCmHandle ncmpServiceCmHandle = getNcmpServiceCmHandleFromPersistenceLayer(cmHandleId);
        ncmpServiceCmHandle.getCompositeState().setCmHandleState(CmHandleState.ADVISED);
        return ncmpServiceCmHandle;
    }

    private NcmpServiceCmHandle getNcmpServiceCmHandleFromPersistenceLayer(final String cmHandleId) {
        return YangDataConverter.convertYangModelCmHandleToNcmpServiceCmHandle(
                inventoryPersistence.getYangModelCmHandle(cmHandleId));
    }

}
