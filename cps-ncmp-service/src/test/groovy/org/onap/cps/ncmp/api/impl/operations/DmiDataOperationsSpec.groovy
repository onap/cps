/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation
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

import org.onap.cps.events.EventsPublisher

import static org.onap.cps.events.mapper.CloudEventMapper.toTargetEvent
import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.PASSTHROUGH_OPERATIONAL
import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.PASSTHROUGH_RUNNING
import static org.onap.cps.ncmp.api.impl.operations.OperationType.CREATE
import static org.onap.cps.ncmp.api.impl.operations.OperationType.READ
import static org.onap.cps.ncmp.api.impl.operations.OperationType.UPDATE
import static org.onap.cps.ncmp.api.NcmpResponseStatus.UNABLE_TO_READ_RESOURCE_DATA
import static org.onap.cps.ncmp.api.NcmpResponseStatus.DMI_SERVICE_NOT_RESPONDING

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.impl.config.NcmpConfiguration

import org.onap.cps.ncmp.api.impl.exception.HttpClientRequestException
import org.onap.cps.ncmp.api.impl.utils.DmiServiceUrlBuilder
import org.onap.cps.ncmp.api.impl.utils.context.CpsApplicationContext
import org.onap.cps.ncmp.api.models.DataOperationRequest
import org.onap.cps.ncmp.events.async1_0_0.DataOperationEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ContextConfiguration
import org.springframework.http.HttpStatus
import spock.lang.Shared
import java.util.concurrent.TimeoutException

@SpringBootTest
@ContextConfiguration(classes = [EventsPublisher, CpsApplicationContext, NcmpConfiguration.DmiProperties, DmiDataOperations])
class DmiDataOperationsSpec extends DmiOperationsBaseSpec {

    @SpringBean
    DmiServiceUrlBuilder dmiServiceUrlBuilder = Mock()
    def dmiServiceBaseUrl = "${dmiServiceName}/dmi/v1/ch/${cmHandleId}/data/ds/ncmp-datastore:"
    def NO_TOPIC = null
    def NO_REQUEST_ID = null
    @Shared
    def OPTIONS_PARAM = '(a=1,b=2)'

    @SpringBean
    JsonObjectMapper spiedJsonObjectMapper = Spy(new JsonObjectMapper(new ObjectMapper()))

    @Autowired
    DmiDataOperations objectUnderTest

    @SpringBean
    EventsPublisher eventsPublisher = Stub()

    def 'call get resource data for #expectedDatastoreInUrl from DMI without topic #scenario.'() {
        given: 'a cm handle for #cmHandleId'
            mockYangModelCmHandleRetrieval(dmiProperties)
        and: 'a positive response from DMI service when it is called with the expected parameters'
            def responseFromDmi = new ResponseEntity<Object>(HttpStatus.OK)
            def expectedUrl = dmiServiceBaseUrl + "${expectedDatastoreInUrl}?resourceIdentifier=${resourceIdentifier}${expectedOptionsInUrl}"
            mockDmiRestClient.postOperationWithJsonData(expectedUrl, expectedJson, READ) >> responseFromDmi
            dmiServiceUrlBuilder.getDmiDatastoreUrl(_, _) >> expectedUrl
        when: 'get resource data is invoked'
            def result = objectUnderTest.getResourceDataFromDmi(dataStore.datastoreName, cmHandleId, resourceIdentifier,
                    options, NO_TOPIC, NO_REQUEST_ID)
        then: 'the result is the response from the DMI service'
            assert result == responseFromDmi
        where: 'the following parameters are used'
            scenario                               | dmiProperties               | dataStore               | options       || expectedJson                                                 | expectedDatastoreInUrl    | expectedOptionsInUrl
            'without properties'                   | []                          | PASSTHROUGH_OPERATIONAL | OPTIONS_PARAM || '{"operation":"read","cmHandleProperties":{}}'               | 'passthrough-operational' | '&options=(a=1,b=2)'
            'with properties'                      | [yangModelCmHandleProperty] | PASSTHROUGH_OPERATIONAL | OPTIONS_PARAM || '{"operation":"read","cmHandleProperties":{"prop1":"val1"}}' | 'passthrough-operational' | '&options=(a=1,b=2)'
            'null options'                         | [yangModelCmHandleProperty] | PASSTHROUGH_OPERATIONAL | null          || '{"operation":"read","cmHandleProperties":{"prop1":"val1"}}' | 'passthrough-operational' | ''
            'empty options'                        | [yangModelCmHandleProperty] | PASSTHROUGH_OPERATIONAL | ''            || '{"operation":"read","cmHandleProperties":{"prop1":"val1"}}' | 'passthrough-operational' | ''
            'datastore running without properties' | []                          | PASSTHROUGH_RUNNING     | OPTIONS_PARAM || '{"operation":"read","cmHandleProperties":{}}'               | 'passthrough-running'     | '&options=(a=1,b=2)'
            'datastore running with properties'    | [yangModelCmHandleProperty] | PASSTHROUGH_RUNNING     | OPTIONS_PARAM || '{"operation":"read","cmHandleProperties":{"prop1":"val1"}}' | 'passthrough-running'     | '&options=(a=1,b=2)'
    }

    def 'Execute (async) data operation from DMI service.'() {
        given: 'collection of yang model cm Handles and data operation request'
            mockYangModelCmHandleCollectionRetrieval([yangModelCmHandleProperty])
            def dataOperationBatchRequestJsonData = TestUtils.getResourceFileContent('dataOperationRequest.json')
            def dataOperationRequest = spiedJsonObjectMapper.convertJsonString(dataOperationBatchRequestJsonData, DataOperationRequest.class)
            dataOperationRequest.dataOperationDefinitions[0].cmHandleIds = [cmHandleId]
            def requestBodyAsJsonStringArg = null
        and: 'a positive response from DMI service when it is called with valid request parameters'
            def responseFromDmi = new ResponseEntity<Object>(HttpStatus.ACCEPTED)
            def expectedDmiBatchResourceDataUrl = "ncmp/v1/data/topic=my-topic-name"
            def expectedBatchRequestAsJson = '{"operations":[{"operation":"read","operationId":"operational-14","datastore":"ncmp-datastore:passthrough-operational","options":"some option","resourceIdentifier":"some resource identifier","cmHandles":[{"id":"some-cm-handle","cmHandleProperties":{"prop1":"val1"}}]}]}'
            mockDmiRestClient.postOperationWithJsonData(expectedDmiBatchResourceDataUrl, _, READ.operationName) >> responseFromDmi
            dmiServiceUrlBuilder.getDataOperationRequestUrl(_, _) >> expectedDmiBatchResourceDataUrl
        when: 'get resource data for group of cm handles are invoked'
            objectUnderTest.requestResourceDataFromDmi('my-topic-name', dataOperationRequest, 'requestId')
        then: 'wait a little to allow execution of service method by task executor (on separate thread)'
            Thread.sleep(100)
        then: 'validate ncmp generated dmi request body json args'
            1 * mockDmiRestClient.postOperationWithJsonData(expectedDmiBatchResourceDataUrl, _, READ) >> { args -> requestBodyAsJsonStringArg = args[1] }
            assert requestBodyAsJsonStringArg == expectedBatchRequestAsJson
    }

    def 'Execute (async) data operation from DMI service for #scenario.'() {
        given: 'data operation request body and dmi resource url'
            def dmiDataOperation = DmiDataOperation.builder().operationId('some-operation-id').build()
            dmiDataOperation.getCmHandles().add(CmHandle.builder().id('some-cm-handle-id').build())
            def dmiDataOperationResourceDataUrl = "http://dmi-service-name:dmi-port/dmi/v1/data?topic=my-topic-name&requestId=some-request-id"
            def actualDataOperationCloudEvent = null
        when: 'exception occurs after sending request to dmi service'
            objectUnderTest.handleTaskCompletionException(new Throwable(exception), dmiDataOperationResourceDataUrl, List.of(dmiDataOperation))
        then: 'a cloud event is published'
            eventsPublisher.publishCloudEvent('my-topic-name', 'some-request-id', _) >> { args -> actualDataOperationCloudEvent = args[2] }
        and: 'the event contains the expected error details'
            def eventDataValue = extractDataValue(actualDataOperationCloudEvent)
            assert eventDataValue.operationId == dmiDataOperation.operationId
            assert eventDataValue.ids == dmiDataOperation.cmHandles.id
            assert eventDataValue.statusCode == responseCode.code
            assert eventDataValue.statusMessage == responseCode.message
        where: 'the following exceptions are occurred'
            scenario                        | exception                                                                                                || responseCode
            'http client request exception' | new HttpClientRequestException('error-message', 'error-details', HttpStatus.SERVICE_UNAVAILABLE.value()) || UNABLE_TO_READ_RESOURCE_DATA
            'timeout exception'             | new TimeoutException()                                                                                   || DMI_SERVICE_NOT_RESPONDING
    }

    def 'call get all resource data.'() {
        given: 'the system returns a cm handle with a sample property'
            mockYangModelCmHandleRetrieval([yangModelCmHandleProperty])
        and: 'a positive response from DMI service when it is called with the expected parameters'
            def responseFromDmi = new ResponseEntity<Object>(HttpStatus.OK)
            def expectedUrl = dmiServiceBaseUrl + "passthrough-operational?resourceIdentifier=/"
            mockDmiRestClient.postOperationWithJsonData(expectedUrl, '{"operation":"read","cmHandleProperties":{"prop1":"val1"}}', READ) >> responseFromDmi
            dmiServiceUrlBuilder.getDmiDatastoreUrl(_, _) >> expectedUrl
        when: 'get resource data is invoked'
            def result = objectUnderTest.getResourceDataFromDmi( PASSTHROUGH_OPERATIONAL.datastoreName, cmHandleId, NO_REQUEST_ID)
        then: 'the result is the response from the DMI service'
            assert result == responseFromDmi
    }

    def 'Write data for pass-through:running datastore in DMI.'() {
        given: 'a cm handle for #cmHandleId'
            mockYangModelCmHandleRetrieval([yangModelCmHandleProperty])
        and: 'a positive response from DMI service when it is called with the expected parameters'
            def expectedUrl = dmiServiceBaseUrl + "passthrough-running?resourceIdentifier=${resourceIdentifier}"
            def expectedJson = '{"operation":"' + expectedOperationInUrl + '","dataType":"some data type","data":"requestData","cmHandleProperties":{"prop1":"val1"}}'
            def responseFromDmi = new ResponseEntity<Object>(HttpStatus.OK)
            dmiServiceUrlBuilder.getDmiDatastoreUrl(_, _) >> expectedUrl
            mockDmiRestClient.postOperationWithJsonData(expectedUrl, expectedJson, operation) >> responseFromDmi
        when: 'write resource method is invoked'
            def result = objectUnderTest.writeResourceDataPassThroughRunningFromDmi(cmHandleId, 'parent/child', operation, 'requestData', 'some data type')
        then: 'the result is the response from the DMI service'
            assert result == responseFromDmi
        where: 'the following operation is performed'
            operation || expectedOperationInUrl
            CREATE    || 'create'
            UPDATE    || 'update'
    }

    def extractDataValue(actualDataOperationCloudEvent) {
        return toTargetEvent(actualDataOperationCloudEvent, DataOperationEvent.class).data.responses[0]
    }
}
