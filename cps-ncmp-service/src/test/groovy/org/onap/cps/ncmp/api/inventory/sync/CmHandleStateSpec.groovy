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
