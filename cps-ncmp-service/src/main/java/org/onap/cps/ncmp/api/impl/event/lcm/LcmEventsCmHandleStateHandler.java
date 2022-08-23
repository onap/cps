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
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;

/**
 * The implementation of it should handle the persisting of composite state and delegate the request to publish the
 * corresponding lcm event.
 */
public interface LcmEventsCmHandleStateHandler {

    /**
     * Updates the composite state of cmHandle based on cmHandleState.
     *
     * @param yangModelCmHandle   cm handle represented as yang model
     * @param targetCmHandleState target cm handle state
     */
    void updateCmHandleState(final YangModelCmHandle yangModelCmHandle, final CmHandleState targetCmHandleState);

    /**
     * Publish LCM Event.
     *
     * @param targetNcmpServiceCmHandle   target NcmpServiceCmHandle
     * @param existingNcmpServiceCmHandle existing NcmpServiceCmHandle
     */
    void publishLcmEvent(final NcmpServiceCmHandle targetNcmpServiceCmHandle,
            final NcmpServiceCmHandle existingNcmpServiceCmHandle);
}
