package org.onap.cps.ncmp.init

import org.onap.cps.ncmp.api.inventory.NetworkCmProxyInventoryFacade
import org.onap.cps.ncmp.api.inventory.models.CmHandleRegistrationResponse
import org.onap.cps.ncmp.api.inventory.models.CmHandleState
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistrationResponse
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.impl.inventory.CmHandleQueryService
import spock.lang.Specification

class InventoryDataMigrationSpec extends Specification {

    def cmHandleQueryService = Mock(CmHandleQueryService)
    def networkCmProxyInventoryFacade = Mock(NetworkCmProxyInventoryFacade)
    def inventoryDataMigration = new InventoryDataMigration(cmHandleQueryService, networkCmProxyInventoryFacade)

    def "Should successfully migrate CM handles grouped by DMI service"() {
        given: 'a list of CM handle IDs'
        def cmHandleIds = ['handle1', 'handle2', 'handle3']
        cmHandleQueryService.getAllCmHandleReferences(false) >> cmHandleIds

        and: 'CM handles with different DMI services'
        def handle1 = createCmHandle('handle1', 'dmi1')
        def handle2 = createCmHandle('handle2', 'dmi1')
        def handle3 = createCmHandle('handle3', 'dmi2')

        and: 'their composite states'
        def state1 = new CompositeState(cmHandleState: CmHandleState.READY)
        def state2 = new CompositeState(cmHandleState: CmHandleState.ADVISED)
        def state3 = new CompositeState(cmHandleState: CmHandleState.READY)

        and: 'successful response for updates'
        def successResponse = new DmiPluginRegistrationResponse(
                updatedCmHandles: [
                        CmHandleRegistrationResponse.createSuccessResponse('handle1'),
                        CmHandleRegistrationResponse.createSuccessResponse('handle2')
                ]
        )

        when: 'migration is performed'
        inventoryDataMigration.migrate()

        then: 'handles are retrieved and grouped correctly'
        1 * networkCmProxyInventoryFacade.getNcmpServiceCmHandle('handle1') >> handle1
        1 * networkCmProxyInventoryFacade.getNcmpServiceCmHandle('handle2') >> handle2
        1 * networkCmProxyInventoryFacade.getNcmpServiceCmHandle('handle3') >> handle3

        and: 'states are retrieved'
        1 * networkCmProxyInventoryFacade.getCmHandleCompositeState('handle1') >> state1
        1 * networkCmProxyInventoryFacade.getCmHandleCompositeState('handle2') >> state2
        1 * networkCmProxyInventoryFacade.getCmHandleCompositeState('handle3') >> state3

        and: 'updates are performed per DMI service'
        1 * networkCmProxyInventoryFacade.updateDmiRegistration({ registration ->
            registration.dmiPlugin == 'dmi1' &&
                    registration.updatedCmHandles*.cmHandleId.sort() == ['handle1', 'handle2']
        }) >> successResponse

        1 * networkCmProxyInventoryFacade.updateDmiRegistration({ registration ->
            registration.dmiPlugin == 'dmi2' &&
                    registration.updatedCmHandles*.cmHandleId == ['handle3']
        }) >> new DmiPluginRegistrationResponse(
                updatedCmHandles: [CmHandleRegistrationResponse.createSuccessResponse('handle3')]
        )
    }

    def "Should handle invalid CM handles gracefully"() {
        given: 'a list with some invalid CM handle IDs'
        def cmHandleIds = ['valid1', 'invalid1', 'valid2']
        cmHandleQueryService.getAllCmHandleReferences(false) >> cmHandleIds

        and: 'some valid and invalid handles'
        def validHandle1 = createCmHandle('valid1', 'dmi1')
        def validHandle2 = createCmHandle('valid2', 'dmi1')

        and: 'their states'
        def state1 = new CompositeState(cmHandleState: CmHandleState.READY)
        def state2 = new CompositeState(cmHandleState: CmHandleState.READY)

        when: 'migration is performed'
        inventoryDataMigration.migrate()

        then: 'handles are attempted to be retrieved'
        1 * networkCmProxyInventoryFacade.getNcmpServiceCmHandle('valid1') >> validHandle1
        1 * networkCmProxyInventoryFacade.getNcmpServiceCmHandle('invalid1') >> null
        1 * networkCmProxyInventoryFacade.getNcmpServiceCmHandle('valid2') >> validHandle2

        and: 'states are retrieved for valid handles'
        1 * networkCmProxyInventoryFacade.getCmHandleCompositeState('valid1') >> state1
        1 * networkCmProxyInventoryFacade.getCmHandleCompositeState('valid2') >> state2

        and: 'update is performed only for valid handles'
        1 * networkCmProxyInventoryFacade.updateDmiRegistration({ registration ->
            registration.dmiPlugin == 'dmi1' &&
                    registration.updatedCmHandles*.cmHandleId.sort() == ['valid1', 'valid2']
        })
    }

    private static NcmpServiceCmHandle createCmHandle(String id, String dmiService) {
        def handle = new NcmpServiceCmHandle()
        handle.setCmHandleId(id)
        handle.setDmiServiceName(dmiService)
        return handle
    }
}
