/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
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

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.CpsAdminService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.ncmp.api.impl.exception.DmiRequestException
import org.onap.cps.ncmp.api.impl.operations.DmiDataOperations
import org.onap.cps.ncmp.api.impl.operations.DmiModelOperations
import org.onap.cps.ncmp.api.impl.operations.YangModelCmHandleRetriever
import org.onap.cps.ncmp.api.models.CmHandleRegistrationResponse
import org.onap.cps.ncmp.api.models.DmiPluginRegistration
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import org.onap.cps.spi.exceptions.DataNodeNotFoundException
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.spi.exceptions.SchemaSetNotFoundException
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Shared
import spock.lang.Specification

import static org.onap.cps.ncmp.api.models.CmHandleRegistrationResponse.RegistrationError.CM_HANDLE_ALREADY_EXIST
import static org.onap.cps.ncmp.api.models.CmHandleRegistrationResponse.RegistrationError.CM_HANDLE_DOES_NOT_EXIST
import static org.onap.cps.ncmp.api.models.CmHandleRegistrationResponse.RegistrationError.UNKNOWN_ERROR
import static org.onap.cps.spi.CascadeDeleteAllowed.CASCADE_DELETE_ALLOWED

/*
    TODO
    1. Way to validate the order ? -> Dont know how
    2. Failure of one type of operations should not impact rest
    3. Validate the response from the update is being passed to main response
    4. Validate it for update

 */

class NetworkCmProxyDataServiceImplRegistrationSpec extends Specification {

    @Shared
    def ncmpServiceCmHandle = new NcmpServiceCmHandle()

    @Shared
    def cmHandlesArray = ['cmHandle001']

    def mockCpsDataService = Mock(CpsDataService)
    def mockCpsModuleService = Mock(CpsModuleService)
    def spiedJsonObjectMapper = Spy(new JsonObjectMapper(new ObjectMapper()))
    def mockCpsAdminService = Mock(CpsAdminService)
    def mockDmiModelOperations = Mock(DmiModelOperations)
    def mockDmiDataOperations = Mock(DmiDataOperations)
    def mockNetworkCmProxyDataServicePropertyHandler = Mock(NetworkCmProxyDataServicePropertyHandler)
    def mockYangModelCmHandleRetriever = Mock(YangModelCmHandleRetriever)

    def noTimestamp = null
    def objectUnderTest = getObjectUnderTestWithModelSyncDisabled();

    def 'Register or re-register a DMI Plugin for the given cm-handle(s) with #scenario process.'() {
        given: 'a registration'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server')
            ncmpServiceCmHandle.cmHandleID = '123'
            ncmpServiceCmHandle.dmiProperties = [dmiProp1: 'dmiValue1', dmiProp2: 'dmiValue2']
            ncmpServiceCmHandle.publicProperties = [publicProp1: 'publicValue1', publicProp2: 'publicValue2']
            dmiPluginRegistration.createdCmHandles = createdCmHandles
            dmiPluginRegistration.updatedCmHandles = updatedCmHandles
            dmiPluginRegistration.removedCmHandles = removedCmHandles
            def expectedJsonData = '{"cm-handles":[{"id":"123","dmi-service-name":"my-server","dmi-data-service-name":null,"dmi-model-service-name":null,' +
                '"additional-properties":[{"name":"dmiProp1","value":"dmiValue1"},{"name":"dmiProp2","value":"dmiValue2"}],' +
                '"public-properties":[{"name":"publicProp1","value":"publicValue1"},{"name":"publicProp2","value":"publicValue2"}]' +
                '}]}'
        when: 'registration is updated and modules are synced'
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'save list elements is invoked with the expected parameters'
            expectedCallsToSaveNode * mockCpsDataService.saveListElements('NCMP-Admin', 'ncmp-dmi-registry',
                '/dmi-registry', expectedJsonData, noTimestamp)
        and: 'update data node leaves is called with correct parameters'
            expectedCallsToUpdateCmHandleProperty * mockNetworkCmProxyDataServicePropertyHandler.updateCmHandleProperties(updatedCmHandles)
        and: 'delete schema set is invoked with the correct parameters'
            expectedCallsToDeleteSchemaSetAndListElement * mockCpsModuleService.deleteSchemaSet('NFP-Operational', 'cmHandle001', CASCADE_DELETE_ALLOWED)
        and: 'delete list or list element is invoked with the correct parameters'
            expectedCallsToDeleteSchemaSetAndListElement * mockCpsDataService.deleteListOrListElement('NCMP-Admin',
                'ncmp-dmi-registry', "/dmi-registry/cm-handles[@id='cmHandle001']", noTimestamp)
        where:
            scenario                    | createdCmHandles      | updatedCmHandles      | removedCmHandles || expectedCallsToSaveNode | expectedCallsToDeleteSchemaSetAndListElement | expectedCallsToUpdateCmHandleProperty
            'create'                    | [ncmpServiceCmHandle] | []                    | []               || 1                       | 0                                            | 0
            'update'                    | []                    | [ncmpServiceCmHandle] | []               || 0                       | 0                                            | 1
            'delete'                    | []                    | []                    | cmHandlesArray   || 0                       | 1                                            | 0
            'create, update and delete' | [ncmpServiceCmHandle] | [ncmpServiceCmHandle] | cmHandlesArray   || 1                       | 1                                            | 1
            'no valid data'             | []                    | []                    | []               || 0                       | 0                                            | 0
    }

    def 'Register a DMI Plugin for the given cm-handle(s) without DMI properties.'() {
        given: 'a registration without cm-handle properties'
            NetworkCmProxyDataServiceImpl objectUnderTest = getObjectUnderTestWithModelSyncDisabled()
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server')
            ncmpServiceCmHandle.cmHandleID = '123'
            ncmpServiceCmHandle.dmiProperties = Collections.emptyMap()
            ncmpServiceCmHandle.publicProperties = Collections.emptyMap()
            dmiPluginRegistration.createdCmHandles = [ncmpServiceCmHandle]
            def expectedJsonData = '{"cm-handles":[{"id":"123","dmi-service-name":"my-server","dmi-data-service-name":null,"dmi-model-service-name":null,"additional-properties":[],"public-properties":[]}]}'
        when: 'registration is updated'
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'save list elements is invoked with the expected parameters'
            1 * mockCpsDataService.saveListElements('NCMP-Admin', 'ncmp-dmi-registry',
                '/dmi-registry', expectedJsonData, noTimestamp)
    }

    def 'Register a DMI Plugin for a given cm-handle(s) with JSON processing errors during process.'() {
        given: 'a registration without cm-handle properties '
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'some-plugin')
            dmiPluginRegistration.createdCmHandles = [ncmpServiceCmHandle]
        and: 'an json processing exception occurs'
            spiedJsonObjectMapper.asJsonString(_) >> { throw (new JsonProcessingException('')) }
        when: 'registration is updated and modules are synced'
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
    }

    def 'Update CMHandle: Update Operation Response is added to the response'() {
        given: 'a registration to update CmHandles'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server',
                updatedCmHandles: [{}])
        and: 'cm-handle updates can be processed successfully'
            def updateOperationResponse = [CmHandleRegistrationResponse.createSuccessResponse('cm-handle-1'),
                CmHandleRegistrationResponse.createFailureResponse('cm-handle-2', new Exception("Failed")),
                CmHandleRegistrationResponse.createFailureResponse('cm-handle-3', CM_HANDLE_DOES_NOT_EXIST)]
            mockNetworkCmProxyDataServicePropertyHandler.updateCmHandleProperties(_) >> updateOperationResponse
        when: 'registration is updated'
            def response = objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'the response contains updateOperationResponse'
            assert response.getUpdatedCmHandles().size() == 3
            assert response.getUpdatedCmHandles().containsAll(updateOperationResponse)
    }

    def 'Remove CmHandle: #scenario'() {
        given: 'a registration'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server',
                removedCmHandles: ['cmhandle'])
        and: '#scenario'
            mockCpsModuleService.deleteSchemaSet(_, 'cmhandle',) >> schemaSetResponse
        when: 'registration is updated to delete cmhandle'
            def response = objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'delete list or list element is called'
            1 * mockCpsDataService.deleteListOrListElement(_, _, _, _)
        and: 'successful response is received'
            assert response.getRemovedCmHandles().size() == 1
            with(response.getRemovedCmHandles().get(0)) {
                assert it.status == CmHandleRegistrationResponse.Status.SUCCESS
                assert it.cmHandle == 'cmhandle'
            }
        where:
            scenario                                            | schemaSetResponse
            'schema-set exists and can be deleted successfully' | {}
            'schema-set does not exist'                         | { throw (new SchemaSetNotFoundException("", "")) }
    }

    def 'Remove CmHandle: All cm-handles delete requests are processed'() {
        given: 'a registration with three cm-handles to be deleted'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server',
                removedCmHandles: ['cmhandle1', 'cmhandle2', 'cmhandle3'])
        and: 'cm-handle deletion is successful for 1st and 3rd; failed for 2nd'
            mockCpsDataService.deleteListOrListElement(_, _, _, _) >> {} >> { throw new RuntimeException("Failed") } >> {}
        when: 'registration is updated to delete cmhandles'
            def response = objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'a response is received for all cm-handles'
            response.getRemovedCmHandles().size() == 3
        and: '1st and 3rd cm-handle deletes successfully'
            with(response.getRemovedCmHandles().get(0)) {
                assert it.status == CmHandleRegistrationResponse.Status.SUCCESS
            }
            with(response.getRemovedCmHandles().get(2)) {
                assert it.status == CmHandleRegistrationResponse.Status.SUCCESS
            }
        and: '2nd cmhandle deletion fails'
            with(response.getRemovedCmHandles().get(1)) {
                assert it.status == CmHandleRegistrationResponse.Status.FAILURE
                assert it.registrationError == UNKNOWN_ERROR
                assert it.errorText == 'Failed'
            }
    }

    def 'Remove CmHandle Error Handling: Schema Set Deletion failed'() {
        given: 'a registration'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: 'my-server',
                removedCmHandles: ['cmhandle'])
        and: 'schema set deletion failed with unknown error'
            mockCpsModuleService.deleteSchemaSet(_, _, _) >> { throw new RuntimeException('Failed') }
        when: 'registration is updated to delete cmhandle'
            def response = objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        and: 'cm-handle is not deleted'
            0 * mockCpsDataService.deleteListOrListElement(_, _, _, _)
        then: 'a failure response is received'
            assert response.getRemovedCmHandles().size() == 1
            with(response.getRemovedCmHandles().get(0)) {
                assert it.status == CmHandleRegistrationResponse.Status.FAILURE
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
            mockCpsDataService.deleteListOrListElement(_, _, _, _) >> { throw deleteListElementException }
        when: 'registration is updated to delete cmhandle'
            def response = objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'a failure response is received'
            assert response.getRemovedCmHandles().size() == 1
            with(response.getRemovedCmHandles().get(0)) {
                assert it.status == CmHandleRegistrationResponse.Status.FAILURE
                assert it.cmHandle == 'cmhandle'
                assert it.registrationError == expectedError
                assert it.errorText == expectedErrorText
            }
        where:
            scenario                   | deleteListElementException                | expectedError            | expectedErrorText
            'cm-handle does not exist' | new DataNodeNotFoundException("", "", "") | CM_HANDLE_DOES_NOT_EXIST | 'cm-handle does not exist'
            'an unexpected exception'  | new RuntimeException("Failed")            | UNKNOWN_ERROR            | 'Failed'
    }

    def 'Dmi plugin registration with #scenario'() {
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

    def 'Invalid DMI plugin registration with #scenario'() {
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

    def 'Exception thrown on CM-Handle registration update request'() {
        given: 'dmi plugin registration input update request'
            def dmiPluginReg = new DmiPluginRegistration();
            dmiPluginReg.dmiPlugin = 'onap.dmap.plugin';
            dmiPluginReg.updatedCmHandles = [new NcmpServiceCmHandle(cmHandleID: 'unknownHandle')]
        and: 'update data node leaves is unable to find data node'
            mockNetworkCmProxyDataServicePropertyHandler.updateCmHandleProperties(*_) >> { throw new DataNodeNotFoundException('NCMP-Admin', 'ncmp-dmi-registry') }
        when: 'update dmi registration is called'
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginReg)
        then: 'data validation exception is thrown'
            def exceptionThrown = thrown(DataValidationException.class)
            assert exceptionThrown.getDetails().contains('DataNode not found')
    }

    def getObjectUnderTestWithModelSyncDisabled() {
        def objectUnderTest = Spy(new NetworkCmProxyDataServiceImpl(mockCpsDataService, spiedJsonObjectMapper, mockDmiDataOperations, mockDmiModelOperations,
            mockCpsModuleService, mockCpsAdminService, mockNetworkCmProxyDataServicePropertyHandler, mockYangModelCmHandleRetriever))
        objectUnderTest.syncModulesAndCreateAnchor(*_) >> null
        return objectUnderTest
    }
}
