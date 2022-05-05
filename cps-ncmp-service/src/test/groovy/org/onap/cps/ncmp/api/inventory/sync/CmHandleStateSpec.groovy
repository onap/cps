package org.onap.cps.ncmp.api.inventory.sync

import spock.lang.Specification

class CmHandleStateSpec extends Specification{

    def 'Transition to next state from ADVISED State'() {
        given: 'a cm handle with an ADVISED state'
            def cmHandleState = CmHandleState.ADVISED
        when: 'the state transitions to the next state'
            cmHandleState = cmHandleState.nextState()
        then: 'the cm handle state changes to READY'
            assert CmHandleState.READY == cmHandleState
    }

    def 'Transition to lock from ADVISED State'() {
        given: 'a cm handle with an ADVISED state'
            def cmHandleState = CmHandleState.ADVISED
        when: 'the state transitions to the lock'
            cmHandleState = cmHandleState.lock(CmHandleState.LockReasonEnum.LOCKED_MISBEHAVING, 'some error message')
        then: 'the cm handle state changes to LOCKED'
            assert CmHandleState.LOCKED == cmHandleState
    }

    def 'Transition to delete from ADVISED State'() {
        given: 'a cm handle with an ADVISED state'
            def cmHandleState = CmHandleState.ADVISED
        when: 'the state transitions to DELETING'
            cmHandleState = cmHandleState.delete()
        then: 'the cm handle state changes to DELETING'
            assert CmHandleState.DELETING == cmHandleState
    }

    def 'Transition to next state from LOCKED State'() {
        given: 'a cm handle with a LOCKED state'
            def cmHandleState = CmHandleState.LOCKED
        when: 'the state transitions to the next state'
            cmHandleState = cmHandleState.nextState()
        then: 'the cm handle state changes to READY'
            assert CmHandleState.READY == cmHandleState
    }

    def 'Transition to lock from LOCKED State'() {
        given: 'a cm handle with a LOCKED state'
            def cmHandleState = CmHandleState.LOCKED
        when: 'the state transitions to lock'
            cmHandleState = cmHandleState.lock(CmHandleState.LockReasonEnum.LOCKED_MISBEHAVING, 'some error message')
        then: 'the cm handle state remains to LOCKED'
            assert CmHandleState.LOCKED == cmHandleState
    }

    def 'Transition to delete from LOCKED State'() {
        given: 'a cm handle with a LOCKED state'
            def cmHandleState = CmHandleState.LOCKED
        when: 'the state transitions to delete'
            cmHandleState = cmHandleState.delete()
        then: 'the cm handle state changes to DELETING'
            assert CmHandleState.DELETING == cmHandleState
    }

    def 'Transition to next state from READY State'() {
        given: 'a cm handle with a READY state'
            def cmHandleState = CmHandleState.READY
        when: 'the state transitions to next state'
            cmHandleState = cmHandleState.nextState()
        then: 'the cm handle state remains as READY'
            assert CmHandleState.READY == cmHandleState
    }

    def 'Transition to lock from READY State'() {
        given: 'a cm handle with a READY state'
            def cmHandleState = CmHandleState.READY
        when: 'the state transitions to the lock'
            cmHandleState = cmHandleState.lock(CmHandleState.LockReasonEnum.LOCKED_MISBEHAVING, 'some error message')
        then: 'the cm handle state changes to LOCKED'
            assert CmHandleState.LOCKED == cmHandleState
    }

    def 'Transition to delete from READY State'() {
        given: 'a cm handle with a READY state'
            def cmHandleState = CmHandleState.READY
        when: 'the state transitions to delete'
            cmHandleState = cmHandleState.delete()
        then: 'the cm handle state changes to DELETING'
            assert CmHandleState.DELETING == cmHandleState
    }

    def 'Transition to next state from DELETING State'() {
        given: 'a cm handle with a DELETING state'
            def cmHandleState = CmHandleState.DELETING
        when: 'the state transitions to next state'
            cmHandleState = cmHandleState.nextState()
        then: 'the cm handle state remains as DELETING'
            assert CmHandleState.DELETING == cmHandleState
    }

    def 'Transition to lock from DELETING State'() {
        given: 'a cm handle with a DELETING state'
            def cmHandleState = CmHandleState.DELETING
        when: 'the state transitions to the lock'
            cmHandleState = cmHandleState.lock(CmHandleState.LockReasonEnum.LOCKED_MISBEHAVING, 'some error message')
        then: 'the cm handle state remains as DELETING'
            assert CmHandleState.DELETING == cmHandleState
    }

    def 'Transition to delete from DELETING State'() {
        given: 'a cm handle with a READY state'
            def cmHandleState = CmHandleState.DELETING
        when: 'the state transitions to delete'
            cmHandleState = cmHandleState.delete()
        then: 'the cm handle state remains as DELETING'
            assert CmHandleState.DELETING == cmHandleState
    }

}
