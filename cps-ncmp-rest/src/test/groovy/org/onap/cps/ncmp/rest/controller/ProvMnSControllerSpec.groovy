/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 OpenInfra Foundation Europe. All rights reserved.
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

import io.netty.handler.timeout.TimeoutException
import org.onap.cps.api.exceptions.DataValidationException
import org.onap.cps.ncmp.api.data.models.OperationType
import org.onap.cps.ncmp.api.exceptions.PolicyExecutorException
import org.onap.cps.ncmp.exceptions.NoAlternateIdMatchFoundException
import org.onap.cps.ncmp.impl.provmns.ParameterHelper
import org.onap.cps.ncmp.impl.provmns.model.PatchItem
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.TestPropertySource

import static org.springframework.http.HttpStatus.BAD_REQUEST
import static org.springframework.http.HttpStatus.CONFLICT
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import static org.springframework.http.HttpStatus.I_AM_A_TEAPOT
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE
import static org.springframework.http.HttpStatus.NOT_FOUND
import static org.springframework.http.HttpStatus.OK
import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put

@TestPropertySource(properties = ["ncmp.policy-executor.enabled=true"])
class ProvMnSControllerSpec extends ProvMnSControllerBaseSpec {

    def 'Get data #scenario.'() {
        given: 'ProvMnS url'
            def provMnSUrl = "$provMnSBasePath/v1/myClass=id1"
        and: 'cm handle can be found'
            setupMocksForCmHandle('/myClass=id1')
        and: 'dmi provides a response'
            mockDmiRestClient.synchronousGetOperation(*_) >> new ResponseEntity<>(responseContentFromDmi, responseStatusFromDmi)
        when: 'get request is performed'
            def response = mvc.perform(get(provMnSUrl)).andReturn().response
        then: 'response status is the same as what DMI gave'
            assert response.status == responseStatusFromDmi.value()
        and: 'the content is whatever the DMI returned'
            assert response.contentAsString == responseContentFromDmi
        where: 'following responses returned by DMI'
            scenario         | responseStatusFromDmi | responseContentFromDmi
            'happy flow'     | OK                    | 'content from DMI'
            'error from DMI' | I_AM_A_TEAPOT         | 'error details from DMI'
    }

    def 'Get data with #scenario.'() {
        given: 'ProvMnS url'
            def provMnSUrl = "$provMnSBasePath/v1/myClass=id1"
        and: 'alternate Id can be matched'
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('/myClass=id1', "/") >> 'ch-1'
        and: 'persistence service returns yangModelCmHandle'
            mockInventoryPersistence.getYangModelCmHandle('ch-1') >> yangModelCmHandle
        and: 'dmi provides a response'
            mockDmiRestClient.synchronousGetOperation(*_) >> new ResponseEntity<>('Response from DMI service', OK)
        when: 'get request is performed'
            def response = mvc.perform(get(provMnSUrl)).andReturn().response
        then: 'response status as expected'
            assert response.status == expectedHttpStatus.value()
        and: 'the body contains the expected type'
            assert response.contentAsString.contains('"type":"' + expectedType)
        and: 'the body contains the expected title'
            assert response.contentAsString.contains('"title":"' + expectedTitle)
        where: 'following scenario'
            scenario              | yangModelCmHandle           || expectedHttpStatus   | expectedType              | expectedTitle
            'no data producer id' | cmHandleWithoutDataProducer || UNPROCESSABLE_ENTITY | 'SERVER_LIMITATION'       | 'Registered DMI does not support the ProvMnS interface.'
            'cm Handle NOT READY' | cmHandleNotReady            || NOT_ACCEPTABLE       | 'APPLICATION_LAYER_ERROR' | 'ch-1 is not in READY state. Current state: ADVISED'
    }

    def 'Get data attempt with exception: #exceptionDuringProcessing.'() {
        given: 'ProvMnS url'
            def provMnSUrl = "$provMnSBasePath/v1/myClass=id1"
        and: 'an exception happens during the process (method not relevant)'
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(*_) >> {throw exceptionDuringProcessing}
        when: 'get request is performed'
            def response = mvc.perform(get(provMnSUrl)).andReturn().response
        then: 'response status is #expectedHttpStatus'
            assert response.status == expectedHttpStatus.value()
        and: 'the body contains the expected type'
            assert response.contentAsString.contains('"type":"' + expectedType)
        and: 'the body contains the expected title'
            assert response.contentAsString.contains('"title":"' + expectedTitle)
        where: 'following exceptions occur'
            exceptionDuringProcessing                           || expectedHttpStatus    | expectedType              | expectedTitle
            new NoAlternateIdMatchFoundException('myTarget')    || NOT_FOUND             | 'IE_NOT_FOUND'            | '/myClass=id1 not found'
            new Exception("my message", new TimeoutException()) || GATEWAY_TIMEOUT       | 'APPLICATION_LAYER_ERROR' | 'my message'
            new Exception("my message")                         || INTERNAL_SERVER_ERROR | 'APPLICATION_LAYER_ERROR' | 'my message'
    }

    def 'Get data attempt with invalid URL: #scenario.'() {
        given: 'ProvMnS url'
            def provMnSUrl = "$provMnSBasePath/$version/$fdn$queryParameter"
        when: 'get request is performed'
            def response = mvc.perform(get(provMnSUrl)).andReturn().response
        then: 'response status as expected'
            assert response.status == expectedHttpStatus
        and: 'the body contains the expected 3GPP error data (when applicable)'
            assert response.contentAsString.contains(expectedType)
        where: 'following scenario'
            scenario                 | version | fdn            | queryParameter     || expectedHttpStatus || expectedType                 | expectedTitle
            'invalid version'        | 'v0'    | 'not relevant' | 'not relevant'     || 404                || NO_CONTENT                   | NO_CONTENT
            'no fdn'                 | 'v1'    | ''             | ''                 || 422                || '"type":"SERVER_LIMITATION"' | '"title":"/ProvMnS/v1/ not a valid path"'
            'fdn without class name' | 'v1'    | 'someClass'    | ''                 || 422                || '"type":"SERVER_LIMITATION"' | '"title":"/ProvMnS/v1/segment not a valid path"'
            'incorrect scope(type)'  | 'v1'    | 'someClass=1'  | '?scopeType=WRONG' || 400                || NO_CONTENT                   | NO_CONTENT
    }

    def 'Get data with list parameter: #scenario.'() {
        given: 'ProvMnS url'
            def provMnSUrl = "$provMnSBasePath/v1/myClass=id1$parameterInProvMnsRequest"
        and: 'cm handle can be found'
            setupMocksForCmHandle('/myClass=id1')
        when: 'get request is performed'
             mvc.perform(get(provMnSUrl))
        then: 'the request to dmi contains the expected url parameters and values and then returns OK'
            1 * mockDmiRestClient.synchronousGetOperation(*_) >> {arguments -> def urlTemplateParameters = arguments[1]
                assert urlTemplateParameters.urlTemplate.contains(expectedParameterInUri)
                assert urlTemplateParameters.urlVariables().get(parameterName) == expectedParameterValuesInDmiRequest
                return new ResponseEntity<>('Some response from DMI service', OK)
            }
        where: 'attributes is populated with the following '
            scenario               | parameterName | parameterInProvMnsRequest   || expectedParameterInUri     || expectedParameterValuesInDmiRequest
            'some attributes'      | 'attributes'  | '?attributes=value1,value2' || '?attributes={attributes}' || 'value1,value2'
            'empty attributes'     | 'attributes'  | '?attributes='              || '?attributes={attributes}' || ''
            'no attributes (null)' | 'attributes'  | ''                          || ''                         || null
            'some fields'          | 'fields'      | '?fields=value3,value4'     || '?fields={fields}'         || 'value3,value4'
            'empty fields'         | 'fields'      | '?fields='                  || '?fields={fields}'         || ''
            'no fields (null)'     | 'fields'      | ''                          || ''                         || null
    }

    def 'Patch request with #scenario.'() {
        given: 'ProvMnS url'
            mockedCmHandle.getAlternateId() >> alternateId
            def provmnsUrl = "$provMnSBasePath/v1$fdn"
        and: 'alternate Id can be matched'
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(fdn, "/") >> 'mock'
        and: 'persistence service returns yangModelCmHandle'
            mockInventoryPersistence.getYangModelCmHandle('mock') >> mockedCmHandle
        and: 'dmi provides a response'
            mockDmiRestClient.synchronousPatchOperation(*_) >> new ResponseEntity<>('content from DMI', OK)
        when: 'patch request is performed'
            def response = mvc.perform(patch(provmnsUrl)
                    .header('Authorization', 'my authorization')
                    .contentType(contentMediaType)
                    .content(jsonBody))
                    .andReturn().response
        then: 'the response contains the expected content'
            assert response.contentAsString.contains('content from DMI')
        and: 'policy executor was invoked with the expected parameters'
            1 * mockPolicyExecutor.checkPermission(mockedCmHandle, OperationType.UPDATE, 'my authorization', expectedResourceIdForPolicyExecutor, expectedChangeRequest)
        where: 'following scenarios are applied'
            scenario                 | contentMediaType    | alternateId                      | fdn                                        | jsonBody           || expectedResourceIdForPolicyExecutor | expectedChangeRequest
            'modify grandchild'      | patchMediaType      | '/subnetwork=1/managedElement=2' | '/subnetwork=1/managedElement=2/child=id1' | patchJsonBody      || '/child=id1'                        | '{"otherChild":[{"id":"id2","attributes":{"attr1":"test"}}]}'
            '3gpp modify grandchild' | patchMediaType3gpp  | '/subnetwork=1/managedElement=2' | '/subnetwork=1/managedElement=2/child=id1' | patchJsonBody3gpp  || '/child=id1'                        | '{"otherChild":[{"id":"id2","attributes":{"attr1":"test"}}]}'
            'no subnetwork'          | patchMediaType      | '/managedElement=2'              | '/managedElement=2/child=id1'              | patchJsonBody      || '/child=id1'                        | '{"otherChild":[{"id":"id2","attributes":{"attr1":"test"}}]}'
            'modify first child'     | patchMediaType      | '/subnetwork=1/managedElement=2' | '/subnetwork=1/managedElement=2'           | patchJsonBody      || ''                                  | '{"otherChild":[{"id":"id2","attributes":{"attr1":"test"}}]}'
    }

    def 'Attempt patch request with root MO, #scenario.'() {
        given: 'ProvMnS url'
            def provmnsUrl = "$provMnSBasePath/v1$fdn"
        and: 'alternate Id can be matched'
            mockedCmHandle.getAlternateId() >> alternateId
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(fdn, "/") >> 'mock'
        and: 'persistence service returns yangModelCmHandle'
            mockInventoryPersistence.getYangModelCmHandle('mock') >> mockedCmHandle
        when: 'patch request is performed'
            def response = mvc.perform(patch(provmnsUrl)
                    .header('Authorization', 'my authorization')
                    .contentType(patchMediaType)
                    .content(jsonBody))
                    .andReturn().response
        then: 'policy executor is never invoked'
            0 * mockPolicyExecutor._
        and: 'the response status is BAD_REQUEST'
            assert response.status == BAD_REQUEST.value()
        and: 'the response content contains the required error details'
            assert response.contentAsString.contains('VALIDATION_ERROR')
            assert response.contentAsString.contains('not supported')
            assert response.contentAsString.contains(fdn)
        where: 'following scenarios are applied'
            scenario              | alternateId                      | fdn                              | jsonBody
            'modify alternate id' | '/subnetwork=1/managedElement=2' | '/subnetwork=1/managedElement=2' | patchWithoutChild
            'modify root MO'      | '/managedElement=2'              | '/managedElement=2'              | patchWithoutChild
    }

    def 'Patch request with error from DMI.'() {
        given: 'ProvMnS url'
            def provmnsUrl = "$provMnSBasePath/v1/managedElement=1/myClass=id1"
        and: 'cm handle can be found'
            setupMocksForCmHandle('/managedElement=1/myClass=id1')
        and: 'dmi provides an error response'
            mockDmiRestClient.synchronousPatchOperation(*_) >> new ResponseEntity<>('content from DMI', I_AM_A_TEAPOT)
        when: 'patch request is performed'
            def response = mvc.perform(patch(provmnsUrl)
                    .header('Authorization', 'my authorization')
                    .contentType(patchMediaType)
                    .content(patchJsonBody))
                    .andReturn().response
        then: 'response status is the same as what DMI gave'
            assert response.status == I_AM_A_TEAPOT.value()
        and: 'the response contains the expected content'
            assert response.contentAsString.contains('content from DMI')
    }

    def 'Attempt Patch request with malformed json.'() {
        given: 'ProvMnS url'
            def provmnsUrl = "$provMnSBasePath/v1/myClass=id1"
        and: 'cm handle can be found'
            setupMocksForCmHandle('/myClass=id1')
        when: 'patch request is performed'
            def response = mvc.perform(patch(provmnsUrl)
                .contentType(patchMediaType)
                .content('{malformed}'))
                .andReturn().response
        then: 'response status is Bad Request'
            assert response.status == BAD_REQUEST.value()
        and: 'the response content is empty'
            assert response.contentAsString.isEmpty()
    }

    def 'Attempt Patch request with json exception during processing.'() {
        given: 'ProvMnS url'
            def provmnsUrl = "$provMnSBasePath/v1/myClass=id1"
        and: 'cm handle can be found'
            setupMocksForCmHandle('/myClass=id1')
        and: 'validation exception occurs'
            spiedJsonObjectMapper.asJsonString(_) >> { throw new DataValidationException('my message','some details') }
        when: 'patch request is performed'
            def response = mvc.perform(patch(provmnsUrl)
                .contentType(patchMediaType)
                .content(patchJsonBody))
                .andReturn().response
        then: 'response status is Bad Request'
            assert response.status == BAD_REQUEST.value()
        and: 'the response contains the correct type and original exception message'
            assert response.contentAsString.contains('"type":"VALIDATION_ERROR"')
            assert response.contentAsString.contains('my message')
    }

    def 'Patch remove request.'() {
        given: 'ProvMnS url'
            def url = "$provMnSBasePath/v1/managedElement=1/myClass=id1"
        and: 'cm handle can be found'
            setupMocksForCmHandle('/managedElement=1/myClass=id1')
            def expectedResourceIdentifier = '/myClass=id1/childClass=1/grandchildClass=1'
        when: 'patch request is performed'
            def response = mvc.perform(patch(url)
                .header('Authorization', 'my authorization')
                .contentType(patchMediaType)
                .content('[{"op":"remove","path":"/childClass=1/grandchildClass=1"}]'))
                .andReturn().response
        then: 'response status is OK'
            assert response.status == OK.value()
        and: 'Policy Executor was invoked with correct details'
            1 * mockPolicyExecutor.checkPermission(validCmHandle, OperationType.DELETE, 'my authorization', expectedResourceIdentifier, expectedDeleteChangeRequest)
    }

    def 'Patch request with no permission from Coordination Management (aka Policy Executor).'() {
        given: 'ProvMnS url'
            def url = "$provMnSBasePath/v1/ManageElement=1/myClass=id1"
        and: 'cm handle can be found'
            setupMocksForCmHandle('/ManageElement=1/myClass=id1')
        and: 'the permission is denied (Policy Executor throws an exception)'
            mockPolicyExecutor.checkPermission(*_) >> {throw new PolicyExecutorException('denied for test','details',null)}
        when: 'patch request is performed'
            def response = mvc.perform(patch(url)
                    .contentType(patchMediaType)
                    .content(patchJsonBody))
                    .andReturn().response
        then: 'response status is CONFLICT (409)'
            assert response.status == CONFLICT.value()
        and: 'response contains the correct type'
            assert response.contentAsString.contains('"type":"APPLICATION_LAYER_ERROR"')
        and: 'response contains the bad operation index'
            assert response.contentAsString.contains('"badOp":"/0"')
        and: 'response contains the message from Policy Executor (as title)'
            assert response.contentAsString.contains('"title":"denied for test"')
    }

    def 'Patch request with incorrect Media Types #scenario.'() {
        given: 'ProvMnS url'
            def url = "$provMnSBasePath/v1/someClass=someId"
        when: 'patch request is performed'
            def response = mvc.perform(patch(url)
                .contentType(contentType)
                .accept(acceptType)
                .content(patchJsonBody))
                .andReturn().response
        then: 'response status is #expectedHttpStatus'
            assert response.status == expectedHttpStatus.value()
        where: 'following media types are used'
            scenario             | contentType        | acceptType                 || expectedHttpStatus
            'Content Type Wrong' | MediaType.TEXT_XML | MediaType.APPLICATION_JSON || UNSUPPORTED_MEDIA_TYPE
            'Accept Type Wrong'  | patchMediaType     | MediaType.TEXT_XML         || NOT_ACCEPTABLE
    }

    def 'Patch request with too many operations.'() {
        given: 'ProvMnS url'
            def url = "$provMnSBasePath/v1/someClass=someId"
        and: 'a patch request with more operations than the max allowed'
            def patchItems = []
            for (def i = 0; i <= maxNumberOfPatchOperations; i++) {
                patchItems.add(new PatchItem(op: 'REMOVE', path: 'somePath'))
            }
           def patchItemsJsonRequestBody = spiedJsonObjectMapper.asJsonString(patchItems)
        when: 'patch request is performed'
            def response = mvc.perform(patch(url)
                    .contentType(patchMediaType)
                    .content(patchItemsJsonRequestBody))
                    .andReturn().response
        then: 'response status is PAYLOAD_TOO_LARGE (413)'
            assert response.status == PAYLOAD_TOO_LARGE.value()
        and: 'response contains the correct type'
            assert response.contentAsString.contains('"type":"SERVER_LIMITATION"')
        and: 'response contains a title detail the limitations with the number of operations'
            assert response.contentAsString.contains('"title":"11 operations in request, this exceeds the maximum of 10"')
    }

    def 'Put request with #scenario.'() {
        given: 'ProvMnS url'
            def putUrl = "$provMnSBasePath/v1/ManagedElement=1/myClass=id1/childClass=1/grandChildClass=2"
        and: 'cm handle can be found'
            setupMocksForCmHandle('/ManagedElement=1/myClass=id1/childClass=1/grandChildClass=2')
        and: 'dmi provides a response'
            mockDmiRestClient.synchronousPutOperation(*_) >> new ResponseEntity<>(responseContentFromDmi, responseStatusFromDmi)
        and: 'The expected resource identifier for policy executor is the FDN to grandchild'
            def expectedResourceIdentifier = '/myClass=id1/childClass=1'
        and: 'The change request target identifier is the FDN to parent and last class as object name in change request'
            def expectedChangeRequest = '{"grandChildClass":[{"id":"2","attributes":{"attr1":"value1"}}]}'
        when: 'put request is performed'
            def response = mvc.perform(put(putUrl)
                    .header('Authorization', 'my authorization')
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(resourceAsJson))
                    .andReturn().response
        then: 'response status is the same as what DMI gave'
            assert response.status == responseStatusFromDmi.value()
        and: 'the content is whatever the DMI returned'
            assert response.contentAsString == responseContentFromDmi
        and: 'The policy executor was invoked with the expected parameters'
            1 * mockPolicyExecutor.checkPermission(validCmHandle, OperationType.CREATE, 'my authorization', expectedResourceIdentifier, expectedChangeRequest)
        where: 'following responses returned by DMI'
            scenario         | responseStatusFromDmi | responseContentFromDmi
            'happy flow'     | OK                    | 'content from DMI'
            'error from DMI' | I_AM_A_TEAPOT         | 'error details from DMI'
    }

    def 'Put request when cm handle not found.'() {
        given: 'ProvMnS url'
            def putUrl = "$provMnSBasePath/v1/myClass=id1"
        and: 'cannot match alternate ID'
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(*_) >> { throw new NoAlternateIdMatchFoundException('') }
        when: 'put request is performed'
            def response = mvc.perform(put(putUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(resourceAsJson))
                .andReturn().response
        then: 'response status NOT FOUND (404)'
            assert response.status == NOT_FOUND.value()
        and: 'the content indicates the FDN could not be found'
            assert response.contentAsString.contains('"title":"/myClass=id1 not found"')
    }

    def 'Delete request with #scenario.'() {
        given: 'ProvMnS url'
            def deleteUrl = "$provMnSBasePath/v1$fdn"
        and: 'alternate Id is mocked can be matched'
            mockedCmHandle.getAlternateId() >> ParameterHelper.extractParentFdn(fdn)
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(fdn, "/") >> 'mock'
        and: 'persistence service returns yangModelCmHandle'
            mockInventoryPersistence.getYangModelCmHandle('mock') >> mockedCmHandle
        and: 'dmi provides a response'
            mockDmiRestClient.synchronousDeleteOperation(*_) >> new ResponseEntity<>(responseContentFromDmi, responseStatusFromDmi)
        when: 'delete request is performed'
            def response = mvc.perform(delete(deleteUrl).header('Authorization', 'my authorization')).andReturn().response
        then: 'response status is the same as what DMI gave'
            assert response.status == responseStatusFromDmi.value()
        and: 'the content is whatever the DMI returned'
            assert response.contentAsString == responseContentFromDmi
        and: 'Policy Executor was invoked with correct resource identifier and almost empty operation details (not used for delete!)'
            1 * mockPolicyExecutor.checkPermission(_, OperationType.DELETE, 'my authorization', expectedResourceId, expectedDeleteChangeRequest)
        where: 'following responses returned by DMI'
            scenario                 | fdn                                                                         | responseStatusFromDmi | responseContentFromDmi     || expectedResourceId
            'happy flow'             | '/Subnetwork=1/ManagedElement=1/myClass=id1/childClass=1/grandChildClass=2' | OK                    | 'content from DMI'         || '/grandChildClass=2'
            'no child'               | '/Subnetwork=1/ManagedElement=1'                                            | OK                    | 'content from DMI'         || '/ManagedElement=1'
            'no subnetwork & child'  | '/ManagedElement=1'                                                         | OK                    | 'content from DMI'         || '/ManagedElement=1'
            'error from DMI'         | '/Subnetwork=1/ManagedElement=1/myClass=id1/childClass=1/grandChildClass=2' | I_AM_A_TEAPOT         | 'error details from DMI'   || '/grandChildClass=2'
    }

    def 'Delete request when cm handle not found.'() {
        given: 'ProvMnS url'
            def deleteUrl = "$provMnSBasePath/v1/myClass=id1"
        and: 'cannot match alternate ID'
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(*_) >> { throw new NoAlternateIdMatchFoundException('') }
        when: 'delete request is attempted'
            def response = mvc.perform(delete(deleteUrl)).andReturn().response
        then: 'response status is the same as what DMI gave'
            assert response.status == NOT_FOUND.value()
        and: 'the content indicates the FDN could not be found'
            assert response.contentAsString.contains('"title":"/myClass=id1 not found"')
    }

    def 'Attempt delete root MO, #scenario.'() {
        given: 'ProvMnS url'
            def deleteUrl = "$provMnSBasePath/v1$fdn"
        and: 'alternate Id is mocked can be matched'
            mockedCmHandle.getAlternateId() >> alternateId
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(fdn, "/") >> 'mock'
        and: 'persistence service returns yangModelCmHandle'
            mockInventoryPersistence.getYangModelCmHandle('mock') >> mockedCmHandle
        when: 'delete request is attempted'
            def response = mvc.perform(delete(deleteUrl).header('Authorization', 'my authorization')).andReturn().response
        then: 'policy executor is never invoked'
            0 * mockPolicyExecutor._
        and: 'the response status is BAD_REQUEST'
            assert response.status == BAD_REQUEST.value()
        and: 'the response content contains the required error details'
            assert response.contentAsString.contains('VALIDATION_ERROR')
            assert response.contentAsString.contains('not supported')
            assert response.contentAsString.contains(fdn)
        where: 'root MOs are targeted'
            scenario          | fdn                              | alternateId
            'with subnetwork' | '/Subnetwork=1/ManagedElement=1' | '/subnetwork=1/managedElement=1'
            'no subnetwork'   | '/ManagedElement=1'              | '/managedElement=1'
    }

}
