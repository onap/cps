/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.events.lcm;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.cps.ncmp.events.lcm.v1.Data;
import org.onap.cps.ncmp.events.lcm.v1.LcmEvent;
import org.onap.cps.ncmp.events.lcm.v1.Values;
import org.springframework.stereotype.Component;


/**
 * LcmEventsCreator to create LcmEvent based on relevant operation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LcmEventsCreator {

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

    private LcmEvent createLcmEvent(final String cmHandleId, final NcmpServiceCmHandle targetNcmpServiceCmHandle,
            final NcmpServiceCmHandle existingNcmpServiceCmHandle) {
        final LcmEventType lcmEventType =
                LcmEventsCreatorHelper.determineEventType(targetNcmpServiceCmHandle, existingNcmpServiceCmHandle);
        return buildLcmEvent(
                cmHandleId, targetNcmpServiceCmHandle, existingNcmpServiceCmHandle, lcmEventType);
    }

    private LcmEvent buildLcmEvent(final String cmHandleId,
                                   final NcmpServiceCmHandle targetNcmpServiceCmHandle,
                                   final NcmpServiceCmHandle existingNcmpServiceCmHandle,
                                   final LcmEventType lcmEventType) {
        final Data payload = new Data();
        payload.setCmHandleId(cmHandleId);
        // waiting for alternateId cache to be merged
        payload.setAlternateId(cmHandleId);
        final CmHandleValuesHolder cmHandleValuesHolder =
                LcmEventsCreatorHelper.determineEventValues(targetNcmpServiceCmHandle, existingNcmpServiceCmHandle,
                        lcmEventType);
        payload.setOldValues(cmHandleValuesHolder.getOldValues());
        payload.setNewValues(cmHandleValuesHolder.getNewValues());
        final LcmEvent lcmEvent = new LcmEvent();
        lcmEvent.setData(payload);
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