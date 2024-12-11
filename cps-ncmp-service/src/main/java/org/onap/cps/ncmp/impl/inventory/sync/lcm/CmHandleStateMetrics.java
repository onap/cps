/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import static org.onap.cps.ncmp.impl.inventory.models.CmHandleState.ADVISED;
import static org.onap.cps.ncmp.impl.inventory.models.CmHandleState.DELETING;
import static org.onap.cps.ncmp.impl.inventory.models.CmHandleState.LOCKED;
import static org.onap.cps.ncmp.impl.inventory.models.CmHandleState.READY;

import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import org.onap.cps.ncmp.impl.inventory.models.CmHandleState;
import org.springframework.stereotype.Component;

@Component
@Getter
public class CmHandleStateMetrics {

    private final AtomicInteger advisedCmHandlesCount = new AtomicInteger(0);
    private final AtomicInteger readyCmHandlesCount = new AtomicInteger(0);
    private final AtomicInteger lockedCmHandlesCount = new AtomicInteger(0);
    private final AtomicInteger deletingCmHandlesCount = new AtomicInteger(0);

    /**
     * Update the Gauge values for cm handles by state.
     *
     * @param previousCmHandleState previous cm handle state
     * @param targetCmHandleState target cm handle state
     */
    public void updateMetricWithStateChange(final CmHandleState previousCmHandleState,
                                            final CmHandleState targetCmHandleState) {
        updatePreviousState(previousCmHandleState);
        updateTargetState(targetCmHandleState);
    }

    private void updatePreviousState(final CmHandleState previousCmHandleState) {
        if (previousCmHandleState.equals(ADVISED) && (advisedCmHandlesCount.get() != 0)) {
            advisedCmHandlesCount.decrementAndGet();
        }
        if (previousCmHandleState.equals(READY) && (readyCmHandlesCount.get() != 0)) {
            readyCmHandlesCount.decrementAndGet();
        }
        if (previousCmHandleState.equals(LOCKED) && (lockedCmHandlesCount.get() != 0)) {
            lockedCmHandlesCount.decrementAndGet();
        }

    }

    private void updateTargetState(final CmHandleState targetCmHandleState) {
        if (targetCmHandleState.equals(ADVISED)) {
            advisedCmHandlesCount.incrementAndGet();
        }
        if (targetCmHandleState.equals(READY)) {
            readyCmHandlesCount.incrementAndGet();
        }
        if (targetCmHandleState.equals(LOCKED)) {
            lockedCmHandlesCount.incrementAndGet();
        }
        if (targetCmHandleState.equals(DELETING)) {
            deletingCmHandlesCount.incrementAndGet();
        }
    }

}