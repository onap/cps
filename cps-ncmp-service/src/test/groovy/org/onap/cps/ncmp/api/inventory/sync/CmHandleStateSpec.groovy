/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.inventory.sync

import org.onap.cps.ncmp.api.inventory.CmHandleState
import spock.lang.Specification

class CmHandleStateSpec extends Specification{

    def 'Transition to READY state from ADVISED state'() {
        given: 'a cm handle with an ADVISED state'
            def cmHandleState = CmHandleState.ADVISED
        when: 'the state transitions to the READY state'
            cmHandleState = cmHandleState.ready()
        then: 'the cm handle state changes to READY'
            assert CmHandleState.READY == cmHandleState
    }

    def 'Transition to READY state from READY state'() {
        given: 'a cm handle with a READY state'
            def cmHandleState = CmHandleState.READY
        when: 'the state transitions to READY state'
            cmHandleState = cmHandleState.ready()
        then: 'the cm handle state remains as READY'
            assert CmHandleState.READY == cmHandleState
    }

    def 'Transition to LOCK state from ADVISED state'() {
        given: 'a cm handle with an ADVISED state'
            def cmHandleState = CmHandleState.ADVISED
        when: 'the state transitions to the lock'
            cmHandleState = cmHandleState.lock(CmHandleState.LockReasonEnum.LOCKED_MISBEHAVING, 'some error message')
        then: 'the cm handle state changes to LOCKED'
            assert CmHandleState.LOCKED == cmHandleState
    }

    def 'Transition to READY state from LOCKED state'() {
        given: 'a cm handle with a LOCKED state'
            def cmHandleState = CmHandleState.LOCKED
        when: 'the state transitions to the next state'
            cmHandleState = cmHandleState.ready()
        then: 'the cm handle state changes to READY'
            assert CmHandleState.READY == cmHandleState
    }

    def 'Transition to LOCK state from LOCKED State'() {
        given: 'a cm handle with a LOCKED state'
            def cmHandleState = CmHandleState.LOCKED
        when: 'the state transitions to lock'
            cmHandleState = cmHandleState.lock(CmHandleState.LockReasonEnum.LOCKED_MISBEHAVING, 'some error message')
        then: 'the cm handle state remains to LOCKED'
            assert CmHandleState.LOCKED == cmHandleState
    }

}
