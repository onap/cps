/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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

import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle;
import org.onap.cps.ncmp.events.lcm.v1.LcmEvent;
import org.onap.cps.ncmp.events.lcm.v1.LcmEventHeader;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.utils.YangDataConverter;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LcmEventsHelper {

    private final LcmEventsProducerHelper lcmEventsProducerHelper;
    private final LcmEventsProducer lcmEventsProducer;

    /**
     * Send LcmEvent in batches and in asynchronous manner.
     *
     * @param cmHandleTransitionPairs Pair of existing and modified cm handle represented as YangModelCmHandle
     */
    @Async("notificationExecutor")
    public void sendLcmEventBatchAsynchronously(final Collection<CmHandleTransitionPair> cmHandleTransitionPairs) {
        cmHandleTransitionPairs.forEach(cmHandleTransitionPair -> sendLcmEvent(
            toNcmpServiceCmHandle(cmHandleTransitionPair.currentYangModelCmHandle()),
            toNcmpServiceCmHandle(cmHandleTransitionPair.targetYangModelCmHandle())
        ));
    }

    private void sendLcmEvent(final NcmpServiceCmHandle currentNcmpServiceCmHandle,
                              final NcmpServiceCmHandle targetNcmpServiceCmHandle) {
        final String cmHandleId = targetNcmpServiceCmHandle.getCmHandleId();
        final LcmEventHeader lcmEventHeader =
                lcmEventsProducerHelper.createLcmEventHeader(cmHandleId, currentNcmpServiceCmHandle,
                    targetNcmpServiceCmHandle
                );
        final LcmEvent lcmEvent =
                lcmEventsProducerHelper.createLcmEvent(cmHandleId, currentNcmpServiceCmHandle,
                    targetNcmpServiceCmHandle
                );
        lcmEventsProducer.sendLcmEvent(cmHandleId, lcmEvent, lcmEventHeader);
    }

    private static NcmpServiceCmHandle toNcmpServiceCmHandle(final YangModelCmHandle yangModelCmHandle) {
        return YangDataConverter.toNcmpServiceCmHandle(yangModelCmHandle);
    }
}
