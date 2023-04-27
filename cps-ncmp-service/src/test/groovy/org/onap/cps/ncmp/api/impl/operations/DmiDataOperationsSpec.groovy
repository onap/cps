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

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.impl.config.NcmpConfiguration
import org.onap.cps.ncmp.api.impl.utils.DmiServiceUrlBuilder
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ContextConfiguration
import org.springframework.http.HttpStatus
import spock.lang.Shared

import static org.onap.cps.ncmp.api.impl.operations.DataStoreEnum.PASSTHROUGH_OPERATIONAL
import static org.onap.cps.ncmp.api.impl.operations.DataStoreEnum.PASSTHROUGH_RUNNING
import static org.onap.cps.ncmp.api.impl.operations.OperationEnum.CREATE
import static org.onap.cps.ncmp.api.impl.operations.OperationEnum.READ
import static org.onap.cps.ncmp.api.impl.operations.OperationEnum.UPDATE

@SpringBootTest
@ContextConfiguration(classes = [NcmpConfiguration.DmiProperties, DmiDataOperations])
class DmiDataOperationsSpec extends DmiOperationsBaseSpec {

    @SpringBean
    DmiServiceUrlBuilder dmiServiceUrlBuilder = Mock()
    def dmiServiceBaseUrl = "${dmiServiceName}/dmi/v1/ch/${cmHandleId}/data/ds/ncmp-datastore:"
    def NO_TOPIC = null
    def NO_REQUEST_ID = null
    @Shared
    def OPTIONS_PARAM = '(a=1,b=2)'
    @Shared
    def expectedBulkRequestAsJson = '{"operation": "read","data": {"fe1c1f1a070c4ce5bbfda7198592a0d3": {"neType": "RadioNode"},"b8e42eed0d9541ed8d8839e8eb86b3e0": {"neType": "RadioNode"}},"requestId": "bbb67474-f705-410a-93d1-b2844e7f45fd"}'

    @SpringBean
    JsonObjectMapper spiedJsonObjectMapper = Spy(new JsonObjectMapper(new ObjectMapper()))

    @Autowired
    DmiDataOperations objectUnderTest

    def 'call get resource data for #expectedDatastoreInUrl from DMI without topic #scenario.'() {
        given: 'a cm handle for #cmHandleId'
            mockYangModelCmHandleRetrieval(dmiProperties)
        and: 'a positive response from DMI service when it is called with the expected parameters'
            def responseFromDmi = new ResponseEntity<Object>(HttpStatus.OK)
            def expectedUrl = dmiServiceBaseUrl + "${expectedDatastoreInUrl}?resourceIdentifier=${resourceIdentifier}${expectedOptionsInUrl}"
            mockDmiRestClient.postOperationWithJsonData(expectedUrl, expectedJson, READ) >> responseFromDmi
            dmiServiceUrlBuilder.getDmiDatastoreUrl(_, _) >> expectedUrl
        when: 'get resource data is invoked'
            def result = objectUnderTest.getResourceDataFromDmi(dataStore.value, cmHandleId, resourceIdentifier,
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

    def 'call get bulk resource data for #dataStore from DMI service with topic #scenario.'() {
        given: 'collection of yang model cm Handles'
            mockYangModelCmHandleCollectionRetrieval([yangModelCmHandleProperty])
        and: 'a positive response from DMI service when it is called with the expected parameters'
            def responseFromDmi = new ResponseEntity<Object>(HttpStatus.ACCEPTED)
            def expectedDmiBulkResourceDataUrl = "ncmp/v1/batch/data/ds/${dataStore}?resourceIdentifier=parent/child%26options=(a=1,b=2)&topic=my-topic-name&options=(fields=schemas/schema)"
            mockDmiRestClient.postOperationWithJsonData(expectedDmiBulkResourceDataUrl, expectedBulkRequestAsJson, READ) >> responseFromDmi
            dmiServiceUrlBuilder.getBulkRequestUrl(_, _) >> expectedDmiBulkResourceDataUrl
        when: 'get resource data for bulk cm handle is invoked'
            def result = objectUnderTest.getResourceDataFromDmi( dataStore.value, [cmHandleId], resourceIdentifier,
                    OPTIONS_PARAM,  'some-topic','requestId')
        then: 'the result is the response from the DMI service'
            assert result == responseFromDmi
        where: 'the following parameters are used'
            scenario                | dataStore
            'datastore operational' | PASSTHROUGH_OPERATIONAL
            'datastore running'     | PASSTHROUGH_RUNNING
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
            def result = objectUnderTest.getResourceDataFromDmi( PASSTHROUGH_OPERATIONAL.value, cmHandleId, NO_REQUEST_ID)
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
}
