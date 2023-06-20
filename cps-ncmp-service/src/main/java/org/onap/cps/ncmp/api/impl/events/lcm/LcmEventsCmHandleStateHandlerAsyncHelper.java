/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023 Nordix Foundation
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

import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.utils.YangDataConverter;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.cps.ncmp.events.lcm.v1.LcmEvent;
import org.onap.cps.ncmp.events.lcm.v1.LcmEventHeader;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LcmEventsCmHandleStateHandlerAsyncHelper {

    private final LcmEventsCreator lcmEventsCreator;
    private final LcmEventsService lcmEventsService;

    /**
     * Publish LCM Event.
     *
     * @param targetNcmpServiceCmHandle  target NcmpServiceCmHandle
     * @param currentNcmpServiceCmHandle current NcmpServiceCmHandle
     */
    @Async("notificationExecutor")
    public void publishLcmEventAsynchronously(final NcmpServiceCmHandle targetNcmpServiceCmHandle,
                                              final NcmpServiceCmHandle currentNcmpServiceCmHandle) {
        log.info("publishLcmEventAsynchronously from thread: {}", Thread.currentThread().getName());
        publishLcmEvent(targetNcmpServiceCmHandle, currentNcmpServiceCmHandle);
    }

    /**
     * Publish LcmEvent in batches and in asynchronous manner.
     *
     * @param cmHandleTransitionPairs Pair of existing and modified cm handle represented as YangModelCmHandle
     */
    @Async("notificationExecutor")
    public void publishLcmEventBatchAsynchronously(
            final Collection<LcmEventsCmHandleStateHandlerImpl.CmHandleTransitionPair> cmHandleTransitionPairs) {
        log.info("publishLcmEventBatchAsynchronously from thread: {}", Thread.currentThread().getName());
        cmHandleTransitionPairs.forEach(cmHandleTransitionPair -> publishLcmEvent(
                toNcmpServiceCmHandle(cmHandleTransitionPair.getTargetYangModelCmHandle()),
                toNcmpServiceCmHandle(cmHandleTransitionPair.getCurrentYangModelCmHandle())));
    }

    private void publishLcmEvent(final NcmpServiceCmHandle targetNcmpServiceCmHandle,
                                 final NcmpServiceCmHandle existingNcmpServiceCmHandle) {
        final String cmHandleId = targetNcmpServiceCmHandle.getCmHandleId();
        final LcmEventHeader lcmEventHeader =
                lcmEventsCreator.populateLcmEventHeader(cmHandleId, targetNcmpServiceCmHandle,
                        existingNcmpServiceCmHandle);
        final LcmEvent lcmEvent =
                lcmEventsCreator.populateLcmEvent(cmHandleId, targetNcmpServiceCmHandle, existingNcmpServiceCmHandle);
        lcmEventsService.publishLcmEvent(cmHandleId, lcmEvent, lcmEventHeader);
    }

    private static NcmpServiceCmHandle toNcmpServiceCmHandle(final YangModelCmHandle yangModelCmHandle) {
        return YangDataConverter.convertYangModelCmHandleToNcmpServiceCmHandle(yangModelCmHandle);
    }
}
