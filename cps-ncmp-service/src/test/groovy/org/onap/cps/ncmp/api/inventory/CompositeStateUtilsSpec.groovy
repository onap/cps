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

package org.onap.cps.ncmp.api.inventory

import spock.lang.Specification

import static org.onap.cps.ncmp.api.inventory.CmHandleState.ADVISED
import static org.onap.cps.ncmp.api.inventory.LockReasonCategory.LOCKED_MODULE_SYNC_FAILED

class CompositeStateUtilsSpec extends Specification {

    def 'Composite State cmHandleState updated to ADVISED with retaining old Lock Details'() {
        given: 'a composite state and lock details'
            def lockDetails = 'some lock details'
            def compositeState = new CompositeState()
            compositeState.lockReason = new CompositeState.LockReason(LOCKED_MODULE_SYNC_FAILED, lockDetails)
        when: 'utility method is called to set state to READY'
            CompositeStateUtils.setCompositeStateToAdvisedAndRetainOldLockReasonDetails().accept(compositeState)
        then: 'composite state is updated'
            assert compositeState.cmHandleState == ADVISED
        and: 'old lock details are retained'
            assert compositeState.lockReason.details == lockDetails

    }

}
