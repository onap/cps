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

package org.onap.cps.ncmp.api.impl.event.lcm;

import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.inventory.CmHandleState;

import java.util.Map;

/**
 * The implementation of it should handle the persisting of composite state and delegate the request to publish the
 * corresponding lcm event.
 */
public interface LcmEventsCmHandleStateHandler {

    /**
     * Updates the composite state of a single cmHandles with the given on cmHandleState.
     *
     * @param yangModelCmHandle cm handle represented as yang model
     * @param cmHandleState the new state
     */
    void updateCmHandleState(final YangModelCmHandle yangModelCmHandle, final CmHandleState cmHandleState);

    /**
     * Updates the composite state of a batch cmHandles based on cmHandleState.
     *
     * @param yangModelCmHandlesToNewCmHandleStates a map cm handles with the new state for each
     */
    void updateCmHandlesStateBatch(final Map<YangModelCmHandle, CmHandleState> yangModelCmHandlesToNewCmHandleStates);
}
