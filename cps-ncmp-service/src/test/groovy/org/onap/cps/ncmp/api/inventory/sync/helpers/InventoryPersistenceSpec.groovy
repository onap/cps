package org.onap.cps.ncmp.api.inventory.sync.helpers

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.CpsDataService
import org.onap.cps.ncmp.api.inventory.CmHandleState
import org.onap.cps.ncmp.api.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.inventory.LockReasonCategory
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

import java.time.OffsetDateTime

class InventoryPersistenceSpec extends Specification {

    def spiedJsonObjectMapper = Spy(new JsonObjectMapper(new ObjectMapper()))

    def mockCpsDataService = Mock(CpsDataService)

    def objectUnderTest = new InventoryPersistence(spiedJsonObjectMapper, mockCpsDataService)

    def 'Update Cm Handle #scenario State'() {
        given: 'a cm handle'
            def cmHandleId = 'Some-Cm-Handle'
        when: 'update cm handle state is invoked with the #scenario state'
            objectUnderTest.updateCmHandleState(cmHandleState, cmHandleId)
        then: 'update node leaves is invoked with the correct params'
            1 * mockCpsDataService.updateNodeLeaves('NCMP-Admin', 'ncmp-dmi-registry', '/dmi-registry/cm-handles[@id=\'Some-Cm-Handle\']', expectedJsonData, _ as OffsetDateTime)
        where: 'the following states are used'
             scenario | cmHandleState        || expectedJsonData
            'READY'   | CmHandleState.READY  || '{"state":{"cm-handle-state":"READY"}}'
            'LOCKED'  | CmHandleState.LOCKED || '{"state":{"cm-handle-state":"LOCKED"}}'
    }

    def 'Save Lock Reason and Details'() {
        given: 'a cm handle'
            def cmHandleId = 'Some-Cm-Handle'
        and: 'the expected Json data'
            def expectedJsonData = '{"lock-reason":{"reason":"LOCKED_MISBEHAVING","details":"some lock reason details"}}'
        when: 'save lock reason and details is invoked'
            objectUnderTest.saveLockReasonAndDetails(cmHandleId, LockReasonCategory.LOCKED_MISBEHAVING, 'some lock reason details')
        then: 'save data is invoked with the correct params'
            1 * mockCpsDataService.saveData('NCMP-Admin', 'ncmp-dmi-registry', '/dmi-registry/cm-handles[@id=\'Some-Cm-Handle\']/state', expectedJsonData, _ as OffsetDateTime)
    }

}
