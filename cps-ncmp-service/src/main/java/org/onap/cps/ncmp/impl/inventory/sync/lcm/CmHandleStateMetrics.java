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

import lombok.Getter;
import org.onap.cps.ncmp.impl.inventory.models.CmHandleState;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;

import static org.onap.cps.ncmp.impl.inventory.models.CmHandleState.*;


@Component
@Getter
public class CmHandleStateMetrics {

    private final AtomicInteger advisedCmHandlesCount = new AtomicInteger(0);
    private final AtomicInteger readyCmHandlesCount = new AtomicInteger(0);
    private final AtomicInteger lockedCmHandlesCount = new AtomicInteger(0);
    private final AtomicInteger deletingCmHandlesCount = new AtomicInteger(0);

    public void updateMetricWithStateChange(final CmHandleState previousState,
                                            final CmHandleState targetState) {
        updatePreviousState(previousState);
        updateTargetState(targetState);
    }

    private void updatePreviousState(final CmHandleState cmHandleState) {
        if(cmHandleState.equals(ADVISED) && (advisedCmHandlesCount.get() > 0)) {
                advisedCmHandlesCount.decrementAndGet();
        }
        if(cmHandleState.equals(READY) && (readyCmHandlesCount.get() > 0)) {
                readyCmHandlesCount.decrementAndGet();
        }
        if(cmHandleState.equals(LOCKED) && (lockedCmHandlesCount.get() > 0)) {
                lockedCmHandlesCount.decrementAndGet();
        }

    }

    private void updateTargetState(final CmHandleState cmHandleState) {
        if(cmHandleState.equals(ADVISED)) {
             advisedCmHandlesCount.incrementAndGet();
        }
        if(cmHandleState.equals(READY)) {
            readyCmHandlesCount.incrementAndGet();
        }
        if(cmHandleState.equals(LOCKED)) {
            lockedCmHandlesCount.incrementAndGet();
        }
        if(cmHandleState.equals(DELETING)) {
            deletingCmHandlesCount.incrementAndGet();
        }
    }

}
