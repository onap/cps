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

import java.util.Collection;
import java.util.Map;
import org.onap.cps.ncmp.api.inventory.models.CmHandleState;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;

/**
 * The implementation of it should handle the persisting of composite state and delegate the request to publish the
 * corresponding lcm event.
 */
public interface LcmEventsCmHandleStateHandler {

    /**
     * Updates the composite state of cmHandle based on cmHandleState in batch.
     *
     * @param cmHandleStatePerCmHandle Map of Yang Model Cm Handle and corresponding cm handle state.
     */
    void updateCmHandleStateBatch(final Map<YangModelCmHandle, CmHandleState> cmHandleStatePerCmHandle);

    /**
     * Sets the initial state of cmHandles to ADVISED.
     *
     * @param yangModelCmHandles List of Yang Model Cm Handle.
     */
    void initiateStateAdvised(Collection<YangModelCmHandle> yangModelCmHandles);
}
