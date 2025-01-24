/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Bell Canada
 *  Modifications Copyright (C) 2021-2025 Nordix Foundation
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

package org.onap.cps.ncmp.rest.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.TestUtils
import org.onap.cps.ncmp.impl.NetworkCmProxyInventoryFacadeImpl
import org.onap.cps.ncmp.api.inventory.models.CmHandleQueryServiceParameters
import org.onap.cps.ncmp.api.inventory.models.CmHandleRegistrationResponse
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistration
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistrationResponse
import org.onap.cps.ncmp.rest.model.CmHandleQueryParameters
import org.onap.cps.ncmp.rest.model.CmHandlerRegistrationErrorResponse
import org.onap.cps.ncmp.rest.model.DmiPluginRegistrationErrorResponse
import org.onap.cps.ncmp.rest.model.RestDmiPluginRegistration
import org.onap.cps.ncmp.rest.util.NcmpRestInputMapper
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@WebMvcTest(NetworkCmProxyInventoryController)
@Import(ObjectMapper)
class NetworkCmProxyInventoryControllerSpec extends Specification {

    @Autowired
    MockMvc mvc

    @SpringBean
    NetworkCmProxyInventoryFacadeImpl mockNetworkCmProxyInventoryFacade = Mock()

    @SpringBean
    NcmpRestInputMapper ncmpRestInputMapper = Mock()

    DmiPluginRegistration mockDmiPluginRegistration = Mock()

    CmHandleQueryServiceParameters cmHandleQueryServiceParameters = Mock()

    @SpringBean
    JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

    @Value('${rest.api.ncmp-inventory-base-path}/v1')
    def ncmpBasePathV1

    def 'Dmi plugin registration #scenario'() {
        given: 'a dmi plugin registration with #scenario'
            def jsonData = TestUtils.getResourceFileContent(dmiRegistrationJson)
        and: 'the expected rest input as an object'
            def expectedRestDmiPluginRegistration = jsonObjectMapper.convertJsonString(jsonData, RestDmiPluginRegistration)
        and: 'the converter returns a dmi registration (only for the expected input object)'
            ncmpRestInputMapper.toDmiPluginRegistration(expectedRestDmiPluginRegistration) >> mockDmiPluginRegistration
        when: 'post request is performed & registration is called with correct DMI plugin information'
            def response = mvc.perform(
                post("$ncmpBasePathV1/ch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonData)
            ).andReturn().response
        then: 'the converted object is forwarded to the registration service'
            1 * mockNetworkCmProxyInventoryFacade.updateDmiRegistration(mockDmiPluginRegistration) >> new DmiPluginRegistrationResponse()
        and: 'response status is no content'
            response.status == HttpStatus.OK.value()
        where: 'the following registration json is used'
            scenario                                                                       | dmiRegistrationJson
            'multiple services, added, updated and removed cm handles and many properties' | 'dmi_registration_all_singing_and_dancing.json'
            'updated cm handle with updated/new and removed properties'                    | 'dmi_registration_updates_only.json'
            'without any properties'                                                       | 'dmi_registration_without_properties.json'
    }

    def 'Dmi plugin registration with invalid json'() {
        given: 'a dmi plugin registration with #scenario'
            def jsonDataWithUndefinedDataLabel = '{"notAdmiPlugin":""}'
        when: 'post request is performed & registration is called with correct DMI plugin information'
            def response = mvc.perform(
                post("$ncmpBasePathV1/ch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonDataWithUndefinedDataLabel)
            ).andReturn().response
        then: 'response status is bad request'
            response.status == HttpStatus.BAD_REQUEST.value()
    }

    def 'CmHandle search endpoint test #scenario.'() {
        given: 'a query object'
            def cmHandleQueryParameters = jsonObjectMapper.asJsonString(new CmHandleQueryParameters())
        and: 'the mapper service returns a converted object'
            ncmpRestInputMapper.toCmHandleQueryServiceParameters(_) >> cmHandleQueryServiceParameters
        and: 'the service returns the desired results'
            mockNetworkCmProxyInventoryFacade.executeParameterizedCmHandleIdSearch(cmHandleQueryServiceParameters, _) >> serviceMockResponse
        when: 'post request is performed & search is called with the given request parameters'
            def response = mvc.perform(
                    post("$ncmpBasePathV1/ch/searches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cmHandleQueryParameters)
            ).andReturn().response
        then: 'response status is OK'
            assert response.status == HttpStatus.OK.value()
        and: 'the response data matches the service response.'
            jsonObjectMapper.convertJsonString(response.getContentAsString(), List) == serviceMockResponse
        where: 'the service respond with'
            scenario             | serviceMockResponse
            'empty response'     | []
            'populates response' | ['cmHandle1', 'cmHandle2']
    }

    def 'CmHandle search endpoint test #scenario with blank cmHandleQueryParameters.'() {
        given: 'a query object'
            def cmHandleQueryParameters = "{}"
        and: 'the mapper service returns a converted object'
            ncmpRestInputMapper.toCmHandleQueryServiceParameters(_) >> cmHandleQueryServiceParameters
        and: 'the service returns the desired results'
            mockNetworkCmProxyInventoryFacade.executeParameterizedCmHandleIdSearch(cmHandleQueryServiceParameters, _) >> serviceMockResponse
        when: 'post request is performed & search is called with the given request parameters'
            def response = mvc.perform(
                    post("$ncmpBasePathV1/ch/searches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cmHandleQueryParameters)
            ).andReturn().response
        then: 'response status is OK'
            assert response.status == HttpStatus.OK.value()
        and: 'the response data matches the service response.'
            jsonObjectMapper.convertJsonString(response.getContentAsString(), List) == serviceMockResponse
        where: 'the service respond with'
            scenario             | serviceMockResponse
            'empty response'     | []
            'populates response' | ['cmHandle1', 'cmHandle2']
    }

    def 'CmHandle search endpoint Error Handling.'() {
        given: 'the mapper service returns a converted object'
            ncmpRestInputMapper.toCmHandleQueryServiceParameters(_) >> cmHandleQueryServiceParameters
        and: 'the service returns the desired results'
            mockNetworkCmProxyInventoryFacade.executeParameterizedCmHandleIdSearch(cmHandleQueryServiceParameters) >> []
        when: 'post request is performed & search is called with the given request parameters'
            def response = mvc.perform(
                    post("$ncmpBasePathV1/ch/searches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cmHandleQueryParameters)
            ).andReturn().response
        then: 'response status is BAD_REQUEST'
            assert response.status == HttpStatus.BAD_REQUEST.value()
        where: 'the cmHandleQueryParameters are'
            scenario          | cmHandleQueryParameters
            'empty string'    | ""
            'non-json string' | "this is a test"
    }

    def 'DMI Registration: All cm-handles operations processed successfully.'() {
        given: 'a dmi plugin registration'
            def dmiRegistrationRequest = '{}'
        and: 'service can register cm-handles successfully'
            def dmiRegistrationResponse = new DmiPluginRegistrationResponse(
                createdCmHandles: [CmHandleRegistrationResponse.createSuccessResponse('cm-handle-1')],
                updatedCmHandles: [CmHandleRegistrationResponse.createSuccessResponse('cm-handle-2')],
                removedCmHandles: [CmHandleRegistrationResponse.createSuccessResponse('cm-handle-3')]
            )
            mockNetworkCmProxyInventoryFacade.updateDmiRegistration(*_) >> dmiRegistrationResponse
        when: 'registration endpoint is invoked'
            def response = mvc.perform(
                post("$ncmpBasePathV1/ch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(dmiRegistrationRequest)
            ).andReturn().response
        then: 'response status is ok'
            response.status == HttpStatus.OK.value()
        and: 'the response body is empty'
            response.getContentAsString() == ''

    }

    def 'DMI Registration Error Handling: #scenario.'() {
        given: 'a dmi plugin registration'
            def dmiRegistrationRequest = '{}'
        and: '#scenario: service failed to register few cm-handle'
            def dmiRegistrationResponse = new DmiPluginRegistrationResponse(
                createdCmHandles: [createCmHandleResponse],
                updatedCmHandles: [updateCmHandleResponse],
                removedCmHandles: [removeCmHandleResponse],
                upgradedCmHandles: [upgradeCmHandleResponse]
            )
            mockNetworkCmProxyInventoryFacade.updateDmiRegistration(*_) >> dmiRegistrationResponse
        when: 'registration endpoint is invoked'
            def response = mvc.perform(
                post("$ncmpBasePathV1/ch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(dmiRegistrationRequest)
            ).andReturn().response
        then: 'response status is internal server error or conflict'
            response.status == httpStatus
        and: 'the response body is in the expected format'
            def responseBody = jsonObjectMapper.convertJsonString(response.getContentAsString(), DmiPluginRegistrationErrorResponse)
        and: 'contains only the failure or conflict cm handle ids'
            responseBody.getFailedCreatedCmHandles() == expectedFailedCreatedCmHandle
            responseBody.getFailedUpdatedCmHandles() == expectedFailedUpdateCmHandle
            responseBody.getFailedRemovedCmHandles() == expectedFailedRemovedCmHandle
            responseBody.getFailedUpgradeCmHandles() == expectedFailedUpgradedCmHandle
        where:
            scenario                | createCmHandleResponse                 | updateCmHandleResponse                 | removeCmHandleResponse                 | upgradeCmHandleResponse                | httpStatus                               || expectedFailedCreatedCmHandle                 | expectedFailedUpdateCmHandle                  | expectedFailedRemovedCmHandle                 | expectedFailedUpgradedCmHandle                 
            'only create failed'    | expectedFailedResponse('cm-handle-1')  | expectedSuccessResponse('cm-handle-2') | expectedSuccessResponse('cm-handle-3') | expectedSuccessResponse('cm-handle-4') | HttpStatus.CONFLICT.value()              || [expectedUnknownErrorResponse('cm-handle-1')] | []                                            | []                                            | []                                             
            'only update failed'    | expectedSuccessResponse('cm-handle-1') | expectedFailedResponse('cm-handle-2')  | expectedSuccessResponse('cm-handle-3') | expectedSuccessResponse('cm-handle-4') | HttpStatus.CONFLICT.value()              || []                                            | [expectedUnknownErrorResponse('cm-handle-2')] | []                                            | []                                             
            'only delete failed'    | expectedSuccessResponse('cm-handle-1') | expectedSuccessResponse('cm-handle-2') | expectedFailedResponse('cm-handle-3')  | expectedSuccessResponse('cm-handle-4') | HttpStatus.CONFLICT.value()              || []                                            | []                                            | [expectedUnknownErrorResponse('cm-handle-3')] | []                                             
            'only upgrade failed'   | expectedSuccessResponse('cm-handle-1') | expectedSuccessResponse('cm-handle-2') | expectedSuccessResponse('cm-handle-3') | expectedFailedResponse('cm-handle-4')  | HttpStatus.CONFLICT.value()              || []                                            | []                                            | []                                            | [expectedUnknownErrorResponse('cm-handle-4')]  
            'all four failed'       | expectedFailedResponse('cm-handle-1')  | expectedFailedResponse('cm-handle-2')  | expectedFailedResponse('cm-handle-3')  | expectedFailedResponse('cm-handle-4')  | HttpStatus.INTERNAL_SERVER_ERROR.value() || [expectedUnknownErrorResponse('cm-handle-1')] | [expectedUnknownErrorResponse('cm-handle-2')] | [expectedUnknownErrorResponse('cm-handle-3')] | [expectedUnknownErrorResponse('cm-handle-4')]  
            'create update failed'  | expectedFailedResponse('cm-handle-1')  | expectedFailedResponse('cm-handle-2')  | expectedSuccessResponse('cm-handle-3') | expectedSuccessResponse('cm-handle-4') | HttpStatus.CONFLICT.value()              || [expectedUnknownErrorResponse('cm-handle-1')] | [expectedUnknownErrorResponse('cm-handle-2')] | []                                            | []                                             
            'create delete failed'  | expectedFailedResponse('cm-handle-1')  | expectedSuccessResponse('cm-handle-2') | expectedFailedResponse('cm-handle-3')  | expectedSuccessResponse('cm-handle-4') | HttpStatus.CONFLICT.value()              || [expectedUnknownErrorResponse('cm-handle-1')] | []                                            | [expectedUnknownErrorResponse('cm-handle-3')] | []                                             
            'update delete failed'  | expectedSuccessResponse('cm-handle-1') | expectedFailedResponse('cm-handle-2')  | expectedFailedResponse('cm-handle-3')  | expectedSuccessResponse('cm-handle-4') | HttpStatus.CONFLICT.value()              || []                                            | [expectedUnknownErrorResponse('cm-handle-2')] | [expectedUnknownErrorResponse('cm-handle-3')] | []                                             
            'delete upgrade failed' | expectedSuccessResponse('cm-handle-1') | expectedSuccessResponse('cm-handle-2') | expectedFailedResponse('cm-handle-3')  | expectedFailedResponse('cm-handle-4')  | HttpStatus.CONFLICT.value()              || []                                            | []                                            | [expectedUnknownErrorResponse('cm-handle-3')] | [expectedUnknownErrorResponse('cm-handle-4')]  
    }

    def 'Get all cm handle references by DMI plugin identifier when #scenario.'() {
        given: 'an endpoint for returning cm handle references for a registered dmi plugin'
            def getUrl = "$ncmpBasePathV1/ch/cmHandles?dmi-plugin-identifier=some-dmi-plugin-identifier"+outputAlternateId
        and: 'a collection of cm handle references are returned'
            mockNetworkCmProxyInventoryFacade.getAllCmHandleReferencesByDmiPluginIdentifier('some-dmi-plugin-identifier', false)
                    >> ['cm-handle-id-1','cm-handle-id-2']
            mockNetworkCmProxyInventoryFacade.getAllCmHandleReferencesByDmiPluginIdentifier('some-dmi-plugin-identifier', true)
                    >> ['alternate-id-1','alternate-id-2']
        when: 'the endpoint is invoked'
            def response = mvc.perform(
                    get(getUrl)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON_VALUE)
            ).andReturn().response
        then: 'the response matches the result returned by the service layer'
            assert response.contentAsString.contains(firstReference)
            assert response.contentAsString.contains(secondReference)
        where:
            scenario                | outputAlternateId         || firstReference    | secondReference
            'output returns cm handle ids'  | ''                        ||  'cm-handle-id-1' | 'cm-handle-id-2'
            'output returns alternate ids'  | '&outputAlternateId=true' ||  'alternate-id-1' | 'alternate-id-2'
    }

    def expectedUnknownErrorResponse(cmHandle) {
        return new CmHandlerRegistrationErrorResponse('cmHandle': cmHandle, 'errorCode': '108', 'errorText': 'Failed')
    }

    def expectedFailedResponse(cmHandle) {
        return CmHandleRegistrationResponse.createFailureResponse(cmHandle, new RuntimeException('Failed'))
    }

    def expectedSuccessResponse(cmHandle) {
        return CmHandleRegistrationResponse.createSuccessResponse(cmHandle)
    }

}
