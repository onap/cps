/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
 *  Modifications Copyright (C) 2022 Bell Canada
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

package org.onap.cps.ncmp.api.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.hazelcast.map.IMap
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.ncmp.api.NetworkCmProxyCmHandlerQueryService
import org.onap.cps.ncmp.api.impl.event.lcm.LcmEventsCmHandleStateHandler
import org.onap.cps.ncmp.api.impl.exception.DmiRequestException
import org.onap.cps.ncmp.api.impl.operations.DmiDataOperations
import org.onap.cps.ncmp.api.inventory.CmHandleQueries
import org.onap.cps.ncmp.api.inventory.CmHandleState
import org.onap.cps.ncmp.api.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.models.CmHandleRegistrationResponse
import org.onap.cps.ncmp.api.models.DmiPluginRegistration
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import org.onap.cps.spi.exceptions.AlreadyDefinedExceptionBatch
import org.onap.cps.spi.exceptions.DataNodeNotFoundException
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.spi.exceptions.SchemaSetNotFoundException
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Shared
import spock.lang.Specification

import static org.onap.cps.ncmp.api.models.CmHandleRegistrationResponse.RegistrationError.CM_HANDLE_DOES_NOT_EXIST
import static org.onap.cps.ncmp.api.models.CmHandleRegistrationResponse.RegistrationError.CM_HANDLE_ALREADY_EXIST
import static org.onap.cps.ncmp.api.models.CmHandleRegistrationResponse.RegistrationError.CM_HANDLE_INVALID_ID
import static org.onap.cps.ncmp.api.models.CmHandleRegistrationResponse.RegistrationError.UNKNOWN_ERROR
import static org.onap.cps.ncmp.api.models.CmHandleRegistrationResponse.Status
import static org.onap.cps.spi.CascadeDeleteAllowed.CASCADE_DELETE_ALLOWED

class NetworkCmProxyDataServiceImplRegistrationSpec extends Specification {

    @Shared
    def ncmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: 'some-cm-handle-id')

    def mockCpsModuleService = Mock(CpsModuleService)
    def spiedJsonObjectMapper = Spy(new JsonObjectMapper(new ObjectMapper()))
    def mockDmiDataOperations = Mock(DmiDataOperations)
    def mockNetworkCmProxyDataServicePropertyHandler = Mock(NetworkCmProxyDataServicePropertyHandler)
    def mockInventoryPersistence = Mock(InventoryPersistence)
    def mockCmhandleQueries = Mock(CmHandleQueries)
    def stubbedNetworkCmProxyCmHandlerQueryService = Stub(NetworkCmProxyCmHandlerQueryService)
    def mockLcmEventsCmHandleStateHandler = Mock(LcmEventsCmHandleStateHandler)
    def mockCpsDataService = Mock(CpsDataService)
    def mockModuleSyncStartedOnCmHandles = Mock(IMap<String, Object>)
    def objectUnderTest = getObjectUnderTest()

    def 'DMI Registration: Create, Update & Delete operations are processed in the right order'() {
        given: 'a registration with operations of all three types'
            def dmiRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server')
            dmiRegistration.setCreatedCmHandles([new NcmpServiceCmHandle(cmHandleId: 'cmhandle-1', publicProperties: ['publicProp1': 'value'], dmiProperties: [:])])
            dmiRegistration.setUpdatedCmHandles([new NcmpServiceCmHandle(cmHandleId: 'cmhandle-2', publicProperties: ['publicProp1': 'value'], dmiProperties: [:])])
            dmiRegistration.setRemovedCmHandles(['cmhandle-2'])
        when: 'registration is processed'
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiRegistration)
            // Spock validated invocation order between multiple then blocks
        then: 'cm-handles are removed first'
            1 * objectUnderTest.parseAndRemoveCmHandlesInDmiRegistration(*_)
        and: 'de-registered cm handle entry is removed from in progress map'
            1 * mockModuleSyncStartedOnCmHandles.remove('cmhandle-2')
        then: 'cm-handles are created'
            1 * objectUnderTest.parseAndCreateCmHandlesInDmiRegistrationAndSyncModules(*_)
        then: 'cm-handles are updated'
            1 * mockNetworkCmProxyDataServicePropertyHandler.updateCmHandleProperties(*_)
    }

    def 'DMI Registration: Response from all operations types are in response'() {
        given: 'a registration with operations of all three types'
            def dmiRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server')
            dmiRegistration.setCreatedCmHandles([new NcmpServiceCmHandle(cmHandleId: 'cmhandle-1', publicProperties: ['publicProp1': 'value'], dmiProperties: [:])])
            dmiRegistration.setUpdatedCmHandles([new NcmpServiceCmHandle(cmHandleId: 'cmhandle-2', publicProperties: ['publicProp1': 'value'], dmiProperties: [:])])
            dmiRegistration.setRemovedCmHandles(['cmhandle-2'])
        and: 'update cm-handles can be processed successfully'
            def updateResponses = [CmHandleRegistrationResponse.createSuccessResponse('cmhandle-2')]
            mockNetworkCmProxyDataServicePropertyHandler.updateCmHandleProperties(*_) >> updateResponses
        and: 'create cm-handles can be processed successfully'
            def createdResponses = [CmHandleRegistrationResponse.createSuccessResponse('cmhandle-1')]
            objectUnderTest.parseAndCreateCmHandlesInDmiRegistrationAndSyncModules(*_) >> createdResponses
        and: 'delete cm-handles can be processed successfully'
            def removeResponses = [CmHandleRegistrationResponse.createSuccessResponse('cmhandle-3')]
            objectUnderTest.parseAndRemoveCmHandlesInDmiRegistration(*_) >> removeResponses
        when: 'registration is processed'
            def response = objectUnderTest.updateDmiRegistrationAndSyncModule(dmiRegistration)
        then: 'response has values from all operations'
            response.getRemovedCmHandles() == removeResponses
            response.getCreatedCmHandles() == createdResponses
            response.getUpdatedCmHandles() == updateResponses
    }

    def 'Create CM-handle Validation: Registration with valid Service names: #scenario'() {
        given: 'a registration '
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: dmiPlugin, dmiModelPlugin: dmiModelPlugin,
                dmiDataPlugin: dmiDataPlugin)
            dmiPluginRegistration.createdCmHandles = [ncmpServiceCmHandle]
        when: 'update registration and sync module is called with correct DMI plugin information'
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'create cm handles registration and sync modules is called with the correct plugin information'
            1 * objectUnderTest.parseAndCreateCmHandlesInDmiRegistrationAndSyncModules(dmiPluginRegistration)
        where:
            scenario                          | dmiPlugin  | dmiModelPlugin | dmiDataPlugin
            'combined DMI plugin'             | 'service1' | ''             | ''
            'data & model DMI plugins'        | ''         | 'service1'     | 'service2'
            'data & model using same service' | ''         | 'service1'     | 'service1'
    }

    def 'Create CM-handle Validation: Invalid DMI plugin service name with #scenario'() {
        given: 'a registration '
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: dmiPlugin, dmiModelPlugin: dmiModelPlugin,
                dmiDataPlugin: dmiDataPlugin)
            dmiPluginRegistration.createdCmHandles = [ncmpServiceCmHandle]
        when: 'registration is called with incorrect DMI plugin information'
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'a DMI Request Exception is thrown with correct message details'
            def exceptionThrown = thrown(DmiRequestException.class)
            assert exceptionThrown.getMessage().contains(expectedMessageDetails)
        and: 'registration is not called'
            0 * objectUnderTest.parseAndCreateCmHandlesInDmiRegistrationAndSyncModules(dmiPluginRegistration)
        where:
            scenario                         | dmiPlugin  | dmiModelPlugin | dmiDataPlugin || expectedMessageDetails
            'empty DMI plugins'              | ''         | ''             | ''            || 'No DMI plugin service names'
            'blank DMI plugins'              | ' '        | ' '            | ' '           || 'No DMI plugin service names'
            'null DMI plugins'               | null       | null           | null          || 'No DMI plugin service names'
            'all DMI plugins'                | 'service1' | 'service2'     | 'service3'    || 'Cannot register combined plugin service name and other service names'
            '(combined)DMI and Data Plugin'  | 'service1' | ''             | 'service2'    || 'Cannot register combined plugin service name and other service names'
            '(combined)DMI and model Plugin' | 'service1' | 'service2'     | ''            || 'Cannot register combined plugin service name and other service names'
            'only model DMI plugin'          | ''         | 'service1'     | ''            || 'Cannot register just a Data or Model plugin service name'
            'only data DMI plugin'           | ''         | ''             | 'service1'    || 'Cannot register just a Data or Model plugin service name'
    }

    def 'Create CM-Handle Successfully: #scenario.'() {
        given: 'a registration without cm-handle properties'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server')
            dmiPluginRegistration.createdCmHandles = [new NcmpServiceCmHandle(cmHandleId: 'cmhandle', dmiProperties: dmiProperties, publicProperties: publicProperties)]
        when: 'registration is updated'
            def response = objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'a successful response is received'
            response.getCreatedCmHandles().size() == 1
            with(response.getCreatedCmHandles().get(0)) {
                assert it.status == Status.SUCCESS
                assert it.cmHandle == 'cmhandle'
            }
        and: 'state handler is invoked with the expected parameters'
            1 * mockLcmEventsCmHandleStateHandler.updateCmHandleStateBatch(_) >> {
                args ->
                    {
                        def cmHandleStatePerCmHandle = (args[0] as Map)
                        cmHandleStatePerCmHandle.each {
                            assert (it.key.id == 'cmhandle'
                                    && it.key.dmiServiceName == 'my-server'
                                    && it.value == CmHandleState.ADVISED)
                        }
                    }
            }
        where:
            scenario                          | dmiProperties            | publicProperties               || expectedDmiProperties                      | expectedPublicProperties
            'with dmi & public properties'    | ['dmi-key': 'dmi-value'] | ['public-key': 'public-value'] || '[{"name":"dmi-key","value":"dmi-value"}]' | '[{"name":"public-key","value":"public-value"}]'
            'with only public properties'     | [:]                      | ['public-key': 'public-value'] || '[]'                                       | '[{"name":"public-key","value":"public-value"}]'
            'with only dmi properties'        | ['dmi-key': 'dmi-value'] | [:]                            || '[{"name":"dmi-key","value":"dmi-value"}]' | '[]'
            'without dmi & public properties' | [:]                      | [:]                            || '[]'                                       | '[]'
    }

    def 'Create CM-Handle Multiple Requests: All cm-handles creation requests are processed with some failures'() {
        given: 'a registration with three cm-handles to be created'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server',
                    createdCmHandles: [new NcmpServiceCmHandle(cmHandleId: 'cmhandle1'),
                                       new NcmpServiceCmHandle(cmHandleId: 'cmhandle2'),
                                       new NcmpServiceCmHandle(cmHandleId: 'cmhandle3')])
        and: 'cm-handle creation is successful for 1st and 3rd; failed for 2nd'
            def xpath = "somePathWithId[@id='cmhandle2']"
            mockLcmEventsCmHandleStateHandler.updateCmHandleStateBatch(*_) >> { throw new AlreadyDefinedExceptionBatch([xpath]) }
        when: 'registration is updated to create cm-handles'
            def response = objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'a response is received for all cm-handles'
            response.getCreatedCmHandles().size() == 1
        and: 'all cm-handles creation fails'
            response.getCreatedCmHandles().each {
                assert it.cmHandle == 'cmhandle2'
                assert it.status == Status.FAILURE
                assert it.registrationError == CM_HANDLE_ALREADY_EXIST
                assert it.errorText == 'cm-handle already exists'
            }
    }

    def 'Create CM-Handle Error Handling: Registration fails: #scenario'() {
        given: 'a registration without cm-handle properties'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server')
            dmiPluginRegistration.createdCmHandles = [new NcmpServiceCmHandle(cmHandleId: cmHandleId)]
        and: 'cm-handler registration fails: #scenario'
            mockLcmEventsCmHandleStateHandler.updateCmHandleStateBatch(*_) >> { throw exception }
        when: 'registration is updated'
            def response = objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'a failure response is received'
            response.getCreatedCmHandles().size() == 1
            with(response.getCreatedCmHandles().get(0)) {
                assert it.status == Status.FAILURE
                assert it.cmHandle ==  cmHandleId
                assert it.registrationError == expectedError
                assert it.errorText == expectedErrorText
            }
        where:
            scenario                                        | cmHandleId             | exception                                                                  || expectedError           | expectedErrorText
            'cm-handle already exist'                       | 'cmhandle'             | new AlreadyDefinedExceptionBatch(["path[@id='${cmHandleId}']".toString()]) || CM_HANDLE_ALREADY_EXIST | 'cm-handle already exists'
            'unknown exception while registering cm-handle' | 'cmhandle'             | new RuntimeException('Failed')                                             || UNKNOWN_ERROR           | 'Failed'
    }

    def 'Update CM-Handle: Update Operation Response is added to the response'() {
        given: 'a registration to update CmHandles'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server',
                updatedCmHandles: [{}])
        and: 'cm-handle updates can be processed successfully'
            def updateOperationResponse = [CmHandleRegistrationResponse.createSuccessResponse('cm-handle-1'),
                                           CmHandleRegistrationResponse.createFailureResponse('cm-handle-2', new Exception("Failed")),
                                           CmHandleRegistrationResponse.createFailureResponse('cm-handle-3', CM_HANDLE_DOES_NOT_EXIST),
                                           CmHandleRegistrationResponse.createFailureResponse('cm handle 4', CM_HANDLE_INVALID_ID)]
            mockNetworkCmProxyDataServicePropertyHandler.updateCmHandleProperties(_) >> updateOperationResponse
        when: 'registration is updated'
            def response = objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'the response contains updateOperationResponse'
            assert response.getUpdatedCmHandles().size() == 4
            assert response.getUpdatedCmHandles().containsAll(updateOperationResponse)
    }

    def 'Remove CmHandle Successfully: #scenario'() {
        given: 'a registration'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server',
                removedCmHandles: ['cmhandle'])
        and: '#scenario'
            mockCpsModuleService.deleteSchemaSet(_, 'cmhandle', CASCADE_DELETE_ALLOWED) >>
                { if (!schemaSetExist) { throw new SchemaSetNotFoundException("", "") } }
        when: 'registration is updated to delete cmhandle'
            def response = objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'the cmHandle state is updated to "DELETING"'
            1 * mockLcmEventsCmHandleStateHandler.updateCmHandleState(_, CmHandleState.DELETING)
        and: 'method to delete relevant schema set is called once'
            1 * mockInventoryPersistence.deleteSchemaSetWithCascade(_)
        and: 'method to delete relevant list/list element is called once'
            1 * mockInventoryPersistence.deleteListOrListElement(_)
        and: 'successful response is received'
            assert response.getRemovedCmHandles().size() == 1
            with(response.getRemovedCmHandles().get(0)) {
                assert it.status == Status.SUCCESS
                assert it.cmHandle == 'cmhandle'
            }
        and: 'the cmHandle state is updated to "DELETED"'
            1 * mockLcmEventsCmHandleStateHandler.updateCmHandleState(_, CmHandleState.DELETED)
        where:
            scenario                                            | schemaSetExist
            'schema-set exists and can be deleted successfully' | true
            'schema-set does not exist'                         | false
    }

    def 'Remove CmHandle: Partial Success'() {
        given: 'a registration with three cm-handles to be deleted'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server',
                removedCmHandles: ['cmhandle1', 'cmhandle2', 'cmhandle3'])
        and: 'cm-handle deletion is successful for 1st and 3rd; failed for 2nd'
            mockInventoryPersistence.deleteListOrListElement(_) >> {} >> { throw new RuntimeException("Failed") } >> {}
        when: 'registration is updated to delete cmhandles'
            def response = objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'a response is received for all cm-handles'
            response.getRemovedCmHandles().size() == 3
        and: 'successfully de-registered cm handle entries are removed from in progress map'
            1 * mockModuleSyncStartedOnCmHandles.remove('cmhandle1')
            1 * mockModuleSyncStartedOnCmHandles.remove('cmhandle3')
        and: 'failed de-registered cm handle entries should not be removed from in progress map'
            0 * mockModuleSyncStartedOnCmHandles.remove('cmhandle2')
        and: '1st and 3rd cm-handle deletes successfully'
            with(response.getRemovedCmHandles().get(0)) {
                assert it.status == Status.SUCCESS
                assert it.cmHandle == 'cmhandle1'
            }
            with(response.getRemovedCmHandles().get(2)) {
                assert it.status == Status.SUCCESS
                assert it.cmHandle == 'cmhandle3'
            }
        and: '2nd cm-handle deletion fails'
            with(response.getRemovedCmHandles().get(1)) {
                assert it.status == Status.FAILURE
                assert it.registrationError == UNKNOWN_ERROR
                assert it.errorText == 'Failed'
                assert it.cmHandle == 'cmhandle2'
            }
    }

    def 'Remove CmHandle Error Handling: Schema Set Deletion failed'() {
        given: 'a registration'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server',
                removedCmHandles: ['cmhandle'])
        and: 'schema set deletion failed with unknown error'
            mockInventoryPersistence.deleteSchemaSetWithCascade(_) >> { throw new RuntimeException('Failed') }
        when: 'registration is updated to delete cmhandle'
            def response = objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'no exception is thrown'
            noExceptionThrown()
        and: 'cm-handle is not deleted'
            0 * mockInventoryPersistence.deleteListOrListElement(_)
        and: 'the cmHandle state is not updated to "DELETED"'
            0 * mockLcmEventsCmHandleStateHandler.updateCmHandleState(_, CmHandleState.DELETED)
        and: 'a failure response is received'
            assert response.getRemovedCmHandles().size() == 1
            with(response.getRemovedCmHandles().get(0)) {
                assert it.status == Status.FAILURE
                assert it.cmHandle == 'cmhandle'
                assert it.errorText == 'Failed'
                assert it.registrationError == UNKNOWN_ERROR
            }
    }

    def 'Remove CmHandle Error Handling: #scenario'() {
        given: 'a registration'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server',
                removedCmHandles: ['cmhandle'])
        and: 'cm-handle deletion throws exception'
            mockInventoryPersistence.deleteListOrListElement(_) >> { throw deleteListElementException }
        when: 'registration is updated to delete cmhandle'
            def response = objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'a failure response is received'
            assert response.getRemovedCmHandles().size() == 1
            with(response.getRemovedCmHandles().get(0)) {
                assert it.status == Status.FAILURE
                assert it.cmHandle == 'cmhandle'
                assert it.registrationError == expectedError
                assert it.errorText == expectedErrorText
            }
        and: 'the cm handle state is not updated to "DELETED"'
            0 * mockLcmEventsCmHandleStateHandler.updateCmHandleState(_, CmHandleState.DELETED)
        where:
            scenario                     | cmHandleId             | deleteListElementException                ||  expectedError           | expectedErrorText
            'cm-handle does not exist'   | 'cmhandle'             | new DataNodeNotFoundException("", "", "") || CM_HANDLE_DOES_NOT_EXIST | 'cm-handle does not exist'
            'cm-handle has invalid name' | 'cm handle with space' | new DataValidationException("", "")       || CM_HANDLE_INVALID_ID     | 'cm-handle has an invalid character(s) in id'
            'an unexpected exception'    | 'cmhandle'             | new RuntimeException("Failed")            || UNKNOWN_ERROR            | 'Failed'
    }

    def getObjectUnderTest() {
        return Spy(new NetworkCmProxyDataServiceImpl(spiedJsonObjectMapper, mockDmiDataOperations,
                mockNetworkCmProxyDataServicePropertyHandler, mockInventoryPersistence, mockCmhandleQueries,
                stubbedNetworkCmProxyCmHandlerQueryService, mockLcmEventsCmHandleStateHandler, mockCpsDataService,
                mockModuleSyncStartedOnCmHandles))
    }
}
