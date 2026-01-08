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

import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.handler.timeout.TimeoutException
import org.onap.cps.api.exceptions.DataValidationException
import org.onap.cps.ncmp.api.data.models.OperationType
import org.onap.cps.ncmp.api.exceptions.PolicyExecutorException
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.exceptions.NoAlternateIdMatchFoundException
import org.onap.cps.ncmp.impl.data.policyexecutor.OperationDetailsFactory
import org.onap.cps.ncmp.impl.data.policyexecutor.PolicyExecutor
import org.onap.cps.ncmp.impl.dmi.DmiRestClient
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.impl.provmns.ParametersBuilder
import org.onap.cps.ncmp.impl.provmns.model.PatchItem
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.ADVISED
import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.READY
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

@WebMvcTest([ProvMnSController, OperationDetailsFactory])
class ProvMnSControllerSpec extends Specification {

    @SpringBean
    ParametersBuilder parametersBuilder = new ParametersBuilder()

    @SpringBean
    AlternateIdMatcher mockAlternateIdMatcher = Mock()

    @SpringBean
    InventoryPersistence mockInventoryPersistence = Mock()

    @SpringBean
    DmiRestClient mockDmiRestClient = Mock()

    @Autowired
    OperationDetailsFactory operationDetailsFactory

    @SpringBean
    PolicyExecutor mockPolicyExecutor = Mock()

    @Autowired
    MockMvc mvc

    @SpringBean
    ObjectMapper objectMapper = new ObjectMapper()

    @SpringBean
    JsonObjectMapper spiedJsonObjectMapper = Spy(new JsonObjectMapper(objectMapper))

    static def resourceAsJson = '{"id":"test", "objectClass": "Test", "attributes": { "attr1": "value1"} }'
    static def validCmHandle = new YangModelCmHandle(id:'ch-1', dmiServiceName: 'someDmiService', dataProducerIdentifier: 'someDataProducerId', compositeState: new CompositeState(cmHandleState: READY))
    static def cmHandleWithoutDataProducer = new YangModelCmHandle(id:'ch-1', dmiServiceName: 'someDmiService', compositeState: new CompositeState(cmHandleState: READY))
    static def cmHandleNotReady            = new YangModelCmHandle(id:'ch-1', dmiServiceName: 'someDmiService', dataProducerIdentifier: 'someDataProducerId', compositeState: new CompositeState(cmHandleState: ADVISED))

    static def patchMediaType       = new MediaType('application', 'json-patch+json')
    static def patchMediaType3gpp   = new MediaType('application', '3gpp-json-patch+json')
    static def patchJsonBody        = '[{"op":"replace","path":"/child=id2/attributes","value":{"attr1":"test"}}]'
    static def patchJsonBody3gpp    = '[{"op":"replace","path":"/child=id2#/attributes/attr1","value":"test"}]'
    static def patchJsonBodyInvalid = '[{"op":"replace","path":"/test","value":{}}]'

    static def expectedDeleteOperationDetails = '{"operation":"delete","targetIdentifier":"","changeRequest":{}}'

    @Value('${rest.api.provmns-base-path}')
    def provMnSBasePath

    @Value('${app.ncmp.provmns.max-patch-operations:10}')
    int maxNumberOfPatchOperations

    static def NO_CONTENT = ''

    def 'Get resource data #scenario.'() {
        given: 'resource data url'
            def getUrl = "$provMnSBasePath/v1/myClass=id1?otherQueryParameter=ignored"
        and: 'alternate Id can be matched'
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('/myClass=id1', "/") >> 'ch-1'
        and: 'persistence service returns yangModelCmHandle'
            mockInventoryPersistence.getYangModelCmHandle('ch-1') >> validCmHandle
        and: 'dmi provides a response'
            mockDmiRestClient.synchronousGetOperation(*_) >> new ResponseEntity<>(responseContentFromDmi, responseStatusFromDmi)
        when: 'get data resource request is performed'
            def response = mvc.perform(get(getUrl)).andReturn().response
        then: 'response status is the same as what DMI gave'
            assert response.status == responseStatusFromDmi.value()
        and: 'the content is whatever the DMI returned'
            assert response.contentAsString == responseContentFromDmi
        where: 'following responses returned by DMI'
            scenario         | responseStatusFromDmi | responseContentFromDmi
            'happy flow'     | OK                    | 'content from DMI'
            'error from DMI' | I_AM_A_TEAPOT         | 'error details from DMI'
    }

    def 'Get resource data request with #scenario.'() {
        given: 'resource data url'
            def getUrl = "$provMnSBasePath/v1/myClass=id1?otherQueryParameter=ignored"
        and: 'alternate Id can be matched'
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('/myClass=id1', "/") >> 'ch-1'
        and: 'persistence service returns yangModelCmHandle'
            mockInventoryPersistence.getYangModelCmHandle('ch-1') >> yangModelCmHandle
        and: 'dmi provides a response'
            mockDmiRestClient.synchronousGetOperation(*_) >> new ResponseEntity<>('Response from DMI service', OK)
        when: 'get data resource request is performed'
            def response = mvc.perform(get(getUrl)).andReturn().response
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

    def 'Get resource data request with Exception: #exceptionDuringProcessing.'() {
        given: 'resource data url'
            def getUrl = "$provMnSBasePath/v1/myClass=id1"
        and: 'an exception happens during the process (method not relevant)'
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(*_) >> {throw exceptionDuringProcessing}
        when: 'get data resource request is performed'
            def response = mvc.perform(get(getUrl)).andReturn().response
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

    def 'Get resource data request with invalid URL: #scenario.'() {
        given: 'resource data url'
            def getUrl = "$provMnSBasePath/$version/$fdn$queryParameter"
        when: 'get data resource request is performed'
            def response = mvc.perform(get(getUrl)).andReturn().response
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

    def 'Get resource data request with list parameter: #scenario.'() {
        given: 'resource data url'
            def getUrl = "$provMnSBasePath/v1/myClass=id1$parameterInProvMnsRequest"
        and: 'alternate Id can be matched'
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('/myClass=id1', "/") >> 'ch-1'
        and: 'persistence service returns yangModelCmHandle'
            mockInventoryPersistence.getYangModelCmHandle('ch-1') >> validCmHandle
        when: 'get data resource request is performed'
             mvc.perform(get(getUrl))
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
        given: 'provmns url'
            def provmnsUrl = "$provMnSBasePath/v1/myClass=id1"
        and: 'alternate Id can be matched'
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('/myClass=id1', "/") >> 'ch-1'
        and: 'resource id for policy executor points to child node'
            def expectedResourceIdForPolicyExecutor = '/myClass=id1/child=id2'
        and: 'operation details has correct class and attributes, target identifier points to parent'
            def expectedOperationDetails = '{"operation":"update","targetIdentifier":"/myClass=id1","changeRequest":{"child":[{"id":"id2","attributes":{"attr1":"test"}}]}}'
        and: 'policy executor is invoked with those parameters'
            1 * mockPolicyExecutor.checkPermission(_, OperationType.UPDATE, _, expectedResourceIdForPolicyExecutor, expectedOperationDetails)
        and: 'persistence service returns yangModelCmHandle'
            mockInventoryPersistence.getYangModelCmHandle('ch-1') >> validCmHandle
        and: 'dmi provides a response'
            mockDmiRestClient.synchronousPatchOperation(*_) >> new ResponseEntity<>('content from DMI', responseStatusFromDmi)
        when: 'patch request is performed'
            def response = mvc.perform(patch(provmnsUrl)
                    .contentType(contentMediaType)
                    .content(jsonBody))
                    .andReturn().response
        then: 'response status is the same as what DMI gave'
            assert response.status == expectedResponseStatusFromProvMnS.value()
        and: 'the response contains the expected content'
            assert response.contentAsString.contains('content from DMI')
        where: 'following scenarios are applied'
            scenario          | contentMediaType   | jsonBody             | responseStatusFromDmi || expectedResponseStatusFromProvMnS
            'happy flow 3gpp' | patchMediaType3gpp | patchJsonBody3gpp    | OK                    || OK
            'happy flow'      | patchMediaType     | patchJsonBody        | OK                    || OK
            'error from DMI'  | patchMediaType     | patchJsonBody        | I_AM_A_TEAPOT         || I_AM_A_TEAPOT
    }

    def 'Attempt Patch request with malformed json.'() {
        given: 'provmns url'
            def provmnsUrl = "$provMnSBasePath/v1/myClass=id1"
        and: 'alternate Id can be matched'
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('/myClass=id1', "/") >> 'ch-1'
        and: 'persistence service returns yangModelCmHandle'
            mockInventoryPersistence.getYangModelCmHandle('ch-1') >> validCmHandle
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
        given: 'provmns url'
            def provmnsUrl = "$provMnSBasePath/v1/myClass=id1"
        and: 'alternate Id can be matched'
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('/myClass=id1', "/") >> 'ch-1'
        and: 'persistence service returns yangModelCmHandle'
            mockInventoryPersistence.getYangModelCmHandle('ch-1') >> validCmHandle
        and:
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
        given: 'resource data url'
            def url = "$provMnSBasePath/v1/myClass=id1"
        and: 'alternate Id can be matched'
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('/myClass=id1', "/") >> 'ch-1'
        and: 'persistence service returns valid yangModelCmHandle'
            mockInventoryPersistence.getYangModelCmHandle('ch-1') >> validCmHandle
            def expectedResourceIdentifier = '/myClass=id1/childClass=1/grandchildClass=1'
        when: 'patch data resource request is performed'
            def response = mvc.perform(patch(url)
                .contentType(patchMediaType)
                .content('[{"op":"remove","path":"/childClass=1/grandchildClass=1"}]'))
                .andReturn().response
        then: 'response status is OK'
            assert response.status == OK.value()
        and: 'Policy Executor invoked with correct details'
            1 * mockPolicyExecutor.checkPermission(_, OperationType.DELETE, _, expectedResourceIdentifier, expectedDeleteOperationDetails)
    }

    def 'Patch request with no permission from Coordination Management (aka Policy Executor).'() {
        given: 'resource data url'
            def url = "$provMnSBasePath/v1/myClass=id1"
        and: 'alternate Id can be matched'
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('/myClass=id1', "/") >> 'ch-1'
        and: 'persistence service returns valid yangModelCmHandle'
            mockInventoryPersistence.getYangModelCmHandle('ch-1') >> validCmHandle
        and: 'the permission is denied (Policy Executor throws an exception)'
            mockPolicyExecutor.checkPermission(*_) >> {throw new PolicyExecutorException('denied for test','details',null)}
        when: 'patch data resource request is performed'
            def response = mvc.perform(patch(url)
                    .contentType(patchMediaType)
                    .content(patchJsonBody))
                    .andReturn().response
        then: 'response status is CONFLICT (409)'
            assert response.status == CONFLICT.value()
        and: 'response contains the correct type'
            assert response.contentAsString.contains('"type":"APPLICATION_LAYER_ERROR"')
        and: 'response contains the bad operation'
            assert response.contentAsString.contains('"badOp":"/0"')
        and: 'response contains the message from Policy Executor (as title)'
            assert response.contentAsString.contains('"title":"denied for test"')
    }

    def 'Patch request with incorrect Media Types #scenario.'() {
        given: 'resource data url'
            def url = "$provMnSBasePath/v1/someClass=someId"
        when: 'patch data resource request is performed'
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
        given: 'resource data url'
            def url = "$provMnSBasePath/v1/someClass=someId"
        and: 'a patch request with more operations than the max allowed'
            def patchItems = []
            for (def i = 0; i <= maxNumberOfPatchOperations; i++) {
                patchItems.add(new PatchItem(op: 'REMOVE', path: 'somePath'))
            }
           def patchItemsJsonRequestBody = spiedJsonObjectMapper.asJsonString(patchItems)
        when: 'patch data resource request is performed'
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

    def 'Put resource data request with #scenario.'() {
        given: 'resource data url'
            def putUrl = "$provMnSBasePath/v1/myClass=id1/childClass=1/grandChildClass=2"
        and: 'alternate Id can be matched'
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('/myClass=id1/childClass=1/grandChildClass=2', "/") >> 'ch-1'
        and: 'persistence service returns yangModelCmHandle'
            mockInventoryPersistence.getYangModelCmHandle('ch-1') >> validCmHandle
        and: 'dmi provides a response'
            mockDmiRestClient.synchronousPutOperation(*_) >> new ResponseEntity<>(responseContentFromDmi, responseStatusFromDmi)
        and: 'The expected resource identifier for policy executor is the FDN to child'
            def expectedResourceIdentifier = '/myClass=id1/childClass=1/grandChildClass=2'
        and: 'The change request target identifier is the FDN to parent and last class as object name in change request'
            def expectedChangeRequest = '{"operation":"create","targetIdentifier":"/myClass=id1/childClass=1","changeRequest":{"grandChildClass":[{"id":"2","attributes":{"attr1":"value1"}}]}}'
        and: 'The policy executor is invoked with those expected parameters'
            1 * mockPolicyExecutor.checkPermission(_, OperationType.CREATE, _, expectedResourceIdentifier, expectedChangeRequest)
        when: 'put data resource request is performed'
            def response = mvc.perform(put(putUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(resourceAsJson))
                    .andReturn().response
        then: 'response status is the same as what DMI gave'
            assert response.status == responseStatusFromDmi.value()
        and: 'the content is whatever the DMI returned'
            assert response.contentAsString == responseContentFromDmi
        where: 'following responses returned by DMI'
            scenario         | responseStatusFromDmi | responseContentFromDmi
            'happy flow'     | OK                    | 'content from DMI'
            'error from DMI' | I_AM_A_TEAPOT         | 'error details from DMI'
    }

    def 'Put resource data request when cm handle not found.'() {
        given: 'resource data url'
            def putUrl = "$provMnSBasePath/v1/myClass=id1"
        and: 'cannot match alternate ID'
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(*_) >> { throw new NoAlternateIdMatchFoundException('') }
        when: 'put data resource request is performed'
            def response = mvc.perform(put(putUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(resourceAsJson))
                .andReturn().response
        then: 'response status NOT FOUND (404)'
            assert response.status == NOT_FOUND.value()
        and: 'the content indicates the FDN could not be found'
            assert response.contentAsString.contains('"title":"/myClass=id1 not found"')
    }

    def 'Delete resource data request with #scenario.'() {
        given: 'resource data url'
            def deleteUrl = "$provMnSBasePath/v1/myClass=id1/childClass=1/grandChildClass=2"
        and: 'alternate Id can be matched'
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('/myClass=id1/childClass=1/grandChildClass=2', "/") >> 'ch-1'
        and: 'persistence service returns yangModelCmHandle'
            mockInventoryPersistence.getYangModelCmHandle('ch-1') >> validCmHandle
        and: 'dmi provides a response'
            mockDmiRestClient.synchronousDeleteOperation(*_) >> new ResponseEntity<>(responseContentFromDmi, responseStatusFromDmi)
        when: 'Delete data resource request is performed'
            def response = mvc.perform(delete(deleteUrl)).andReturn().response
        then: 'response status is the same as what DMI gave'
            assert response.status == responseStatusFromDmi.value()
        and: 'Policy Executor was invoked with correct resource identifier and almost empty operation details (not used for delete!)'
            1 * mockPolicyExecutor.checkPermission(_, OperationType.DELETE, _, '/myClass=id1/childClass=1/grandChildClass=2', expectedDeleteOperationDetails)
        and: 'the content is whatever the DMI returned'
            assert response.contentAsString == responseContentFromDmi
        where: 'following responses returned by DMI'
            scenario         | responseStatusFromDmi | responseContentFromDmi
            'happy flow'     | OK                    | 'content from DMI'
            'error from DMI' | I_AM_A_TEAPOT         | 'error details from DMI'
    }

    def 'Delete resource data request when cm handle not found.'() {
        given: 'resource data url'
            def deleteUrl = "$provMnSBasePath/v1/myClass=id1"
        and: 'cannot match alternate ID'
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(*_) >> { throw new NoAlternateIdMatchFoundException('') }
        when: 'Delete data resource request is performed'
            def response = mvc.perform(delete(deleteUrl)).andReturn().response
        then: 'response status is the same as what DMI gave'
            assert response.status == NOT_FOUND.value()
        and: 'the content indicates the FDN could not be found'
            assert response.contentAsString.contains('"title":"/myClass=id1 not found"')
    }

}
