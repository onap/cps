/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.operations

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.events.EventsPublisher
import org.onap.cps.ncmp.api.impl.config.DmiProperties
import org.onap.cps.ncmp.api.impl.exception.DmiClientRequestException
import org.onap.cps.ncmp.api.impl.utils.context.CpsApplicationContext
import org.onap.cps.ncmp.api.impl.utils.url.builder.UriTemplateParameters
import org.onap.cps.ncmp.api.models.CmResourceAddress
import org.onap.cps.ncmp.api.models.DataOperationRequest
import org.onap.cps.ncmp.events.async1_0_0.DataOperationEvent
import org.onap.cps.ncmp.impl.inventory.models.CmHandleState
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ContextConfiguration
import reactor.core.publisher.Mono
import spock.lang.Shared


import static org.onap.cps.ncmp.api.NcmpResponseStatus.UNKNOWN_ERROR
import static org.onap.cps.ncmp.api.impl.events.mapper.CloudEventMapper.toTargetEvent
import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.PASSTHROUGH_OPERATIONAL
import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.PASSTHROUGH_RUNNING
import static org.onap.cps.ncmp.api.impl.operations.OperationType.CREATE
import static org.onap.cps.ncmp.api.impl.operations.OperationType.READ
import static org.onap.cps.ncmp.api.impl.operations.OperationType.UPDATE
import static org.onap.cps.ncmp.api.impl.operations.RequiredDmiService.DATA

@SpringBootTest
@ContextConfiguration(classes = [EventsPublisher, CpsApplicationContext, DmiProperties, DmiDataOperations])
class DmiDataOperationsSpec extends DmiOperationsBaseSpec {

    def dmiServiceBaseUrl = "${dmiServiceName}/dmi/v1/ch/${cmHandleId}/data/ds/ncmp-datastore:"
    def NO_TOPIC = null
    def NO_REQUEST_ID = null
    def NO_AUTH_HEADER = null

    @Shared
    def OPTIONS_PARAM = '(a=1,b=2)'

    @SpringBean
    JsonObjectMapper spiedJsonObjectMapper = Spy(new JsonObjectMapper(new ObjectMapper()))

    @Autowired
    DmiDataOperations objectUnderTest

    @SpringBean
    EventsPublisher eventsPublisher = Stub()

    def 'call get resource data for #expectedDataStore from DMI without topic #scenario.'() {
        given: 'a cm handle for #cmHandleId'
            mockYangModelCmHandleRetrieval(dmiProperties)
        and: 'a positive response from DMI service when it is called with the expected parameters'
            def responseFromDmi = Mono.just(new ResponseEntity<Object>('{some-key:some-value}', HttpStatus.OK))
            def expectedUriTemplateWithVariables = getExpectedUriTemplateWithVariables(expectedOptions, expectedDataStore)
            def expectedJson = '{"operation":"read","cmHandleProperties":' + expectedProperties + ',"moduleSetTag":""}'
            mockDmiRestClient.asynchronousPostOperationWithJsonData(DATA, expectedUriTemplateWithVariables, expectedJson, READ, NO_AUTH_HEADER) >> responseFromDmi
        when: 'get resource data is invoked'
            def cmResourceAddress = new CmResourceAddress(expectedDataStore.datastoreName, cmHandleId, resourceIdentifier)
            def result = objectUnderTest.getResourceDataFromDmi(cmResourceAddress, expectedOptions, NO_TOPIC, NO_REQUEST_ID, NO_AUTH_HEADER).block()
        then: 'the result is the response from the DMI service'
            assert result.body == '{some-key:some-value}'
            assert result.statusCode.'2xxSuccessful'
        where: 'the following parameters are used'
        scenario                               | dmiProperties               | expectedDataStore       | expectedOptions || expectedProperties
        'without properties'                   | []                          | PASSTHROUGH_OPERATIONAL | OPTIONS_PARAM   || '{}'
        'with properties'                      | [yangModelCmHandleProperty] | PASSTHROUGH_OPERATIONAL | OPTIONS_PARAM   || '{"prop1":"val1"}'
        'null options'                         | [yangModelCmHandleProperty] | PASSTHROUGH_OPERATIONAL | null            || '{"prop1":"val1"}'
        'empty options'                        | [yangModelCmHandleProperty] | PASSTHROUGH_OPERATIONAL | ''              || '{"prop1":"val1"}'
        'datastore running without properties' | []                          | PASSTHROUGH_RUNNING     | OPTIONS_PARAM   || '{}'
        'datastore running with properties'    | [yangModelCmHandleProperty] | PASSTHROUGH_RUNNING     | OPTIONS_PARAM   || '{"prop1":"val1"}'
    }

    def 'Execute (async) data operation from DMI service.'() {
        given: 'collection of yang model cm Handles and data operation request'
            mockYangModelCmHandleCollectionRetrieval([yangModelCmHandleProperty])
            def dataOperationBatchRequestJsonData = TestUtils.getResourceFileContent('dataOperationRequest.json')
            def dataOperationRequest = spiedJsonObjectMapper.convertJsonString(dataOperationBatchRequestJsonData, DataOperationRequest.class)
            dataOperationRequest.dataOperationDefinitions[0].cmHandleIds = [cmHandleId]
        and: 'a positive response from DMI service when it is called with valid request parameters'
            def responseFromDmi = Mono.just(new ResponseEntity<Object>(HttpStatus.ACCEPTED))
            def expectedTemplateWithVariables = new UriTemplateParameters('someServiceName/{dmiBasePath}/v1/data?requestId={requestId}&topic={topic}', ['requestId': 'requestId', 'topic': 'my-topic-name', 'dmiBasePath': 'dmi'])
            def expectedBatchRequestAsJson = '{"operations":[{"operation":"read","operationId":"operational-14","datastore":"ncmp-datastore:passthrough-operational","options":"some option","resourceIdentifier":"some resource identifier","cmHandles":[{"id":"some-cm-handle","moduleSetTag":"","cmHandleProperties":{"prop1":"val1"}}]}]}'
            mockDmiRestClient.asynchronousPostOperationWithJsonData(DATA, expectedTemplateWithVariables, _, READ, NO_AUTH_HEADER) >> responseFromDmi
        when: 'get resource data for group of cm handles is invoked'
            objectUnderTest.requestResourceDataFromDmi('my-topic-name', dataOperationRequest, 'requestId', NO_AUTH_HEADER)
        then: 'the post operation was called with the expected URL and JSON request body'
            1 * mockDmiRestClient.asynchronousPostOperationWithJsonData(DATA, expectedTemplateWithVariables, expectedBatchRequestAsJson, READ, NO_AUTH_HEADER)
    }

    def 'Execute (async) data operation from DMI service with Exception.'() {
        given: 'collection of yang model cm Handles and data operation request'
            mockYangModelCmHandleCollectionRetrieval([yangModelCmHandleProperty])
            def dataOperationBatchRequestJsonData = TestUtils.getResourceFileContent('dataOperationRequest.json')
            def dataOperationRequest = spiedJsonObjectMapper.convertJsonString(dataOperationBatchRequestJsonData, DataOperationRequest.class)
            dataOperationRequest.dataOperationDefinitions[0].cmHandleIds = [cmHandleId]
        and: 'the published cloud event will be captured'
            def actualDataOperationCloudEvent = null
            eventsPublisher.publishCloudEvent('my-topic-name', 'my-request-id', _) >> { args -> actualDataOperationCloudEvent = args[2] }
        and: 'a DMI client request exception is thrown when DMI service is called'
            mockDmiRestClient.asynchronousPostOperationWithJsonData(*_) >> { Mono.error(new DmiClientRequestException(123, '', '', UNKNOWN_ERROR)) }
        when: 'attempt to get resource data for group of cm handles is invoked'
            objectUnderTest.requestResourceDataFromDmi('my-topic-name', dataOperationRequest, 'my-request-id', NO_AUTH_HEADER)
        then: 'the event contains the expected error details'
            def eventDataValue = extractDataValue(actualDataOperationCloudEvent)
            assert eventDataValue.statusCode == '108'
            assert eventDataValue.statusMessage == UNKNOWN_ERROR.message
        and: 'the event contains the correct operation details'
            assert eventDataValue.operationId == dataOperationRequest.dataOperationDefinitions[0].operationId
            assert eventDataValue.ids == dataOperationRequest.dataOperationDefinitions[0].cmHandleIds
    }

    def 'call get all resource data.'() {
        given: 'the system returns a cm handle with a sample property and sample module set tag'
            mockYangModelCmHandleRetrieval([yangModelCmHandleProperty], 'my-module-set-tag')
        and: 'a positive response from DMI service when it is called with the expected parameters'
            def responseFromDmi = new ResponseEntity<Object>(HttpStatus.OK)
            def expectedTemplateWithVariables = new UriTemplateParameters('someServiceName/{dmiBasePath}/v1/ch/{cmHandleId}/data/ds/{datastore}?resourceIdentifier={resourceIdentifier}', ['resourceIdentifier': '/', 'datastore': 'ncmp-datastore:passthrough-operational', 'dmiBasePath': 'dmi', 'cmHandleId': cmHandleId])
            def expectedJson = '{"operation":"read","cmHandleProperties":{"prop1":"val1"},"moduleSetTag":"my-module-set-tag"}'
            mockDmiRestClient.synchronousPostOperationWithJsonData(DATA, expectedTemplateWithVariables, expectedJson, READ, null) >> responseFromDmi
        when: 'get resource data is invoked'
            def result = objectUnderTest.getAllResourceDataFromDmi(cmHandleId, NO_REQUEST_ID)
        then: 'the result is the response from the DMI service'
            assert result == responseFromDmi
    }

    def 'Write data for pass-through:running datastore in DMI.'() {
        given: 'a cm handle for #cmHandleId'
            mockYangModelCmHandleRetrieval([yangModelCmHandleProperty])
        and: 'a positive response from DMI service when it is called with the expected parameters'
            def expectedTemplateWithVariables = new UriTemplateParameters('someServiceName/{dmiBasePath}/v1/ch/{cmHandleId}/data/ds/{datastore}?resourceIdentifier={resourceIdentifier}', ['resourceIdentifier': resourceIdentifier, 'datastore': 'ncmp-datastore:passthrough-running', 'dmiBasePath': 'dmi', 'cmHandleId': cmHandleId])
            def expectedJson = '{"operation":"' + expectedOperationInUrl + '","dataType":"some data type","data":"requestData","cmHandleProperties":{"prop1":"val1"},"moduleSetTag":""}'
            def responseFromDmi = new ResponseEntity<Object>(HttpStatus.OK)
            mockDmiRestClient.synchronousPostOperationWithJsonData(DATA, expectedTemplateWithVariables, expectedJson, operation, NO_AUTH_HEADER) >> responseFromDmi
        when: 'write resource method is invoked'
            def result = objectUnderTest.writeResourceDataPassThroughRunningFromDmi(cmHandleId, 'parent/child', operation, 'requestData', 'some data type', NO_AUTH_HEADER)
        then: 'the result is the response from the DMI service'
            assert result == responseFromDmi
        where: 'the following operation is performed'
            operation || expectedOperationInUrl
            CREATE    || 'create'
            UPDATE    || 'update'
    }

    def 'State Ready validation'() {
        given: ' a yang model cm handle'
            populateYangModelCmHandle([] ,'')
        when: 'Validating State of #cmHandleState'
            def caughtException = null
            try {
                objectUnderTest.validateIfCmHandleStateReady(yangModelCmHandle, cmHandleState)
            } catch (Exception e) {
                caughtException = e
            }
        then: 'only when not ready a  exception is thrown'
            if (expecteException) {
                assert caughtException.details.contains('not in READY state')
            } else {
                assert caughtException == null
            }
        where: ' the following states are used'
            cmHandleState         || expecteException
            CmHandleState.READY   || false
            CmHandleState.ADVISED || true
    }

    def extractDataValue(actualDataOperationCloudEvent) {
        return toTargetEvent(actualDataOperationCloudEvent, DataOperationEvent).data.responses[0]
    }

    def getExpectedUriTemplateWithVariables(expectedOptions, expectedDataStore) {
        def expectedUriTemplate = (expectedOptions == null || expectedOptions == '')
                ? 'someServiceName/{dmiBasePath}/v1/ch/{cmHandleId}/data/ds/{datastore}?resourceIdentifier={resourceIdentifier}'
                : 'someServiceName/{dmiBasePath}/v1/ch/{cmHandleId}/data/ds/{datastore}?resourceIdentifier={resourceIdentifier}&options={options}'
        def uriVariables = (expectedOptions == null || expectedOptions == '')
                ? ['resourceIdentifier': 'parent/child', 'datastore': expectedDataStore.datastoreName, 'dmiBasePath': 'dmi', 'cmHandleId': cmHandleId]
                : ['resourceIdentifier': 'parent/child', 'datastore': expectedDataStore.datastoreName, 'dmiBasePath': 'dmi', 'options': expectedOptions, 'cmHandleId': cmHandleId]
        return new UriTemplateParameters(expectedUriTemplate, uriVariables)
    }
}
