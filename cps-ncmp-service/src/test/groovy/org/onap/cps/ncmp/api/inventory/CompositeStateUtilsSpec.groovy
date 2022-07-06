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
import static org.onap.cps.ncmp.api.inventory.CmHandleState.LOCKED
import static org.onap.cps.ncmp.api.inventory.CmHandleState.READY
import static org.onap.cps.ncmp.api.inventory.CompositeStateUtils.setCompositeStateToAdvised
import static org.onap.cps.ncmp.api.inventory.CompositeStateUtils.setCompositeStateToAdvisedAndRetainOldLockReasonDetails
import static org.onap.cps.ncmp.api.inventory.CompositeStateUtils.setCompositeStateToLocked
import static org.onap.cps.ncmp.api.inventory.CompositeStateUtils.setCompositeStateToReadyWithInitialDataStoreSyncState
import static org.onap.cps.ncmp.api.inventory.DataStoreSyncState.UNSYNCHRONIZED
import static org.onap.cps.ncmp.api.inventory.LockReasonCategory.LOCKED_MODULE_SYNC_FAILED

class CompositeStateUtilsSpec extends Specification {

    def 'Composite State cmHandleState updated to ADVISED'() {
        given: 'a composite state'
            def compositeState = new CompositeState()
        when: 'utility method is called to set state to ADVISED'
            setCompositeStateToAdvised().accept(compositeState)
        then: 'composite state is updated'
            assert compositeState.cmHandleState == ADVISED
    }

    def 'Composite State cmHandleState updated to LOCKED'() {
        given: 'a composite state'
            def compositeState = new CompositeState()
        when: 'utility method is called to set state to LOCKED'
            setCompositeStateToLocked().accept(compositeState)
        then: 'composite state is updated'
            assert compositeState.cmHandleState == LOCKED
    }

    def 'Composite State cmHandleState updated to READY'() {
        given: 'a composite state'
            def compositeState = new CompositeState()
        when: 'utility method is called to set state to READY'
            setCompositeStateToReadyWithInitialDataStoreSyncState().accept(compositeState)
        then: 'composite state is updated'
            assert compositeState.cmHandleState == READY
            assert compositeState.dataStores.operationalDataStore.dataStoreSyncState == UNSYNCHRONIZED
    }

    def 'Composite State cmHandleState updated to ADVISED with retaining old Lock Details'() {
        given: 'a composite state and lock details'
            def lockDetails = 'some lock details'
            def compositeState = new CompositeState()
            compositeState.lockReason = new CompositeState.LockReason(LOCKED_MODULE_SYNC_FAILED, lockDetails)
        when: 'utility method is called to set state to READY'
            setCompositeStateToAdvisedAndRetainOldLockReasonDetails().accept(compositeState)
        then: 'composite state is updated'
            assert compositeState.cmHandleState == ADVISED
            assert compositeState.lockReason.details == lockDetails
    }

}
