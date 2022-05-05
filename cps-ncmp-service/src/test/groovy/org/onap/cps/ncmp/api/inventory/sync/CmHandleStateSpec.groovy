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

import spock.lang.Specification

class CmHandleStateSpec extends Specification{

    def 'Transition to READY state from ADVISED State'() {
        given: 'a cm handle with an ADVISED state'
            def cmHandleState = CmHandleState.ADVISED
        when: 'the state transitions to the next state'
            cmHandleState = cmHandleState.ready()
        then: 'the cm handle state changes to READY'
            assert CmHandleState.READY == cmHandleState
    }

    def 'Transition to READY state from READY State'() {
        given: 'a cm handle with a READY state'
            def cmHandleState = CmHandleState.READY
        when: 'the state transitions to next state'
            cmHandleState = cmHandleState.ready()
        then: 'the cm handle state remains as READY'
            assert CmHandleState.READY == cmHandleState
    }

}
