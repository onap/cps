/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2026 OpenInfra Foundation Europe. All rights reserved.
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
import org.onap.cps.ncmp.events.lcm.LcmEventBase;
import org.onap.cps.ncmp.events.lcm.LcmEventV1;
import org.onap.cps.ncmp.events.lcm.LcmEventV2;
import org.onap.cps.ncmp.events.lcm.PayloadV1;
import org.onap.cps.ncmp.events.lcm.PayloadV2;
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
    public LcmEventV1 createLcmEventV1(final NcmpServiceCmHandle currentNcmpServiceCmHandle,
                                       final NcmpServiceCmHandle targetNcmpServiceCmHandle) {
        final String cmHandleId = targetNcmpServiceCmHandle.getCmHandleId();
        final LcmEventType lcmEventType =
            determineEventType(currentNcmpServiceCmHandle, targetNcmpServiceCmHandle);
        final LcmEventV1 lcmEventV1 = new LcmEventV1();
        populateHeaderDetails(lcmEventV1, cmHandleId, lcmEventType);
        final PayloadV1 payloadV1 = PayloadFactory.createPayloadV1(lcmEventType, currentNcmpServiceCmHandle,
                                                                                 targetNcmpServiceCmHandle);
        lcmEventV1.setEvent(payloadV1);
        return lcmEventV1;
    }

    /**
     * Create Lifecycle Management Event Version 2.
     *
     * @param currentNcmpServiceCmHandle  current ncmp service cmhandle
     * @param targetNcmpServiceCmHandle   target ncmp service cmhandle
     * @return Populated LcmEvent Version 2
     */
    public LcmEventV2 createLcmEventV2(final NcmpServiceCmHandle currentNcmpServiceCmHandle,
                                       final NcmpServiceCmHandle targetNcmpServiceCmHandle) {
        final LcmEventV2 lcmEventV2 = new LcmEventV2();
        final String cmHandleId = targetNcmpServiceCmHandle.getCmHandleId();
        final LcmEventType lcmEventType =
            determineEventType(currentNcmpServiceCmHandle, targetNcmpServiceCmHandle);
        populateHeaderDetails(lcmEventV2, cmHandleId, lcmEventType);
        final PayloadV2 payloadV2 = PayloadFactory.createPayloadV2(lcmEventType, currentNcmpServiceCmHandle,
            targetNcmpServiceCmHandle);
        lcmEventV2.setEvent(payloadV2);
        return lcmEventV2;
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

    private void populateHeaderDetails(final LcmEventBase lcmEventBase,
                                       final String eventCorrelationId,
                                       final LcmEventType lcmEventType) {
        lcmEventBase.setEventId(UUID.randomUUID().toString());
        lcmEventBase.setEventCorrelationId(eventCorrelationId);
        lcmEventBase.setEventTime(EventDateTimeFormatter.getCurrentIsoFormattedDateTime());
        lcmEventBase.setEventSource("org.onap.ncmp");
        lcmEventBase.setEventType(lcmEventType.getEventType());
        lcmEventBase.setEventSchema("org.onap.ncmp:cmhandle-lcm-event");
        lcmEventBase.setEventSchemaVersion("1.0");
    }

}
