package org.onap.cps.ncmp.rest.util

import org.onap.cps.ncmp.api.inventory.DataStoreSyncState
import org.onap.cps.ncmp.api.inventory.models.CmHandleState
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.api.inventory.models.CompositeStateBuilder
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.api.inventory.models.TrustLevel
import org.onap.cps.ncmp.rest.model.CmHandleCompositeState
import spock.lang.Specification

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import static org.onap.cps.ncmp.api.inventory.models.LockReasonCategory.MODULE_SYNC_FAILED

class RestOutputCmHandleMapperSpec extends Specification {

    CmHandleStateMapper mockCmHandleStateMapper = Mock()
    RestOutputCmHandleMapper objectUnderTest = new RestOutputCmHandleMapper(mockCmHandleStateMapper)

    def 'Map cm handles to rest output #scenario.'() {
        given: 'cm handle'
            def ncmpServiceCmHandle = getNcmpServiceCmHandle(trustLevel)
        and: 'state'
            mockCmHandleStateMapper.toCmHandleCompositeStateExternalLockReason(ncmpServiceCmHandle.getCompositeState()) >> getState()
        when: 'called'
            def response = objectUnderTest.toRestOutputCmHandle(ncmpServiceCmHandle, includePrivateProperties)
        then: 'result'
            response
        where:
        scenario                           | includePrivateProperties | trustLevel          || result
        'not including private properties' | false                    | null                || false
        'including private properties'     | true                     | TrustLevel.NONE     || false
        'with trust level'                 | false                    | TrustLevel.COMPLETE || true
    }

    def getNcmpServiceCmHandle(trustLevel) {
        def compositeState = new CompositeStateBuilder()
                .withCmHandleState(CmHandleState.ADVISED).build()
        return new NcmpServiceCmHandle(cmHandleId: 'ch-1', dmiProperties: ['some key': 'some value'],
                                       dmiServiceName: 'some dmi', currentTrustLevel: trustLevel,
                                       publicProperties: ['public property key': 'public property value'],
                                       alternateId: 'alt-1', compositeState: compositeState)
    }

    def getState() {
        def state = new CmHandleCompositeState()
        state.setCmHandleState(CmHandleState.ADVISED.name())
        return state
    }
}
