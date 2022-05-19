package org.onap.cps.ncmp.api.inventory.sync.helpers

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.CpsDataService
import org.onap.cps.ncmp.api.inventory.CmHandleState
import org.onap.cps.ncmp.api.inventory.CompositeState
import org.onap.cps.ncmp.api.inventory.CompositeStateBuilder
import org.onap.cps.ncmp.api.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.inventory.LockReasonCategory
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.model.DataNode
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Shared
import spock.lang.Specification

import java.time.OffsetDateTime

class InventoryPersistenceSpec extends Specification {

    def spiedJsonObjectMapper = Spy(new JsonObjectMapper(new ObjectMapper()))

    def mockCpsDataService = Mock(CpsDataService)

    def mockCompositeStateBuilder = Mock(CompositeStateBuilder)

    def objectUnderTest = new InventoryPersistence(spiedJsonObjectMapper, mockCpsDataService)

    def 'Get a Cm Handle Composite State'() {
        given: 'a valid cm handle id'
            def cmHandleId = 'Some-Cm-Handle'
            def dataNode = new DataNode(leaves: ['cm-handle-state': 'ADVISED'])
            def compositeState = new CompositeState(cmHandleState: 'ADVISED')
        when: 'get cm handle state is invoked'
            objectUnderTest.getCmHandleState(cmHandleId)
        then: 'cps data service get data node is invoked and returns a valid data node'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                '/dmi-registry/cm-handles[@id=\'Some-Cm-Handle\']/state', FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        and: 'a composite state is returned from the data node'
            mockCompositeStateBuilder.fromDataNode(dataNode) >> compositeState
        and: 'the composite state contains the correct values'
            compositeState.cmHandleState == CmHandleState.ADVISED
    }

    def 'Update Cm Handle with #scenario State'() {
        given: 'a cm handle and a composite state'
            def cmHandleId = 'Some-Cm-Handle'
            def compositeState = new CompositeState(cmHandleState: cmHandleState)
        when: 'update cm handle state is invoked with the #scenario state'
            objectUnderTest.updateCmHandleState(cmHandleId, compositeState)
        then: 'update node leaves is invoked with the correct params'
            1 * mockCpsDataService.replaceNodeTree('NCMP-Admin', 'ncmp-dmi-registry', '/dmi-registry/cm-handles[@id=\'Some-Cm-Handle\']', expectedJsonData, _ as OffsetDateTime)
        where: 'the following states are used'
             scenario | cmHandleState        || expectedJsonData
            'READY'   | CmHandleState.READY  || '{"state":{"cm-handle-state":"READY"}}'
            'LOCKED'  | CmHandleState.LOCKED || '{"state":{"cm-handle-state":"LOCKED"}}'
    }

}
