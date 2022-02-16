/*
 *  ============LICENSE_START=======================================================
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

package org.onap.cps.ncmp.api.impl.operations

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.impl.config.NcmpConfiguration
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared

import static org.onap.cps.ncmp.api.impl.operations.DmiOperations.DataStoreEnum.PASSTHROUGH_OPERATIONAL
import static org.onap.cps.ncmp.api.impl.operations.DmiOperations.DataStoreEnum.PASSTHROUGH_RUNNING
import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum.CREATE
import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum.UPDATE
import org.springframework.http.HttpStatus

@SpringBootTest
@ContextConfiguration(classes = [NcmpConfiguration.DmiProperties, DmiDataOperations])
class DmiDataOperationsSpec extends DmiOperationsBaseSpec {

    @Shared
    def expectedJsonWithoutDmiProp = '{"operation":"read","cmHandleProperties":{}}'
    @Shared
    def expectedJsonWithDmiProp = '{"operation":"read","cmHandleProperties":{"prop1":"val1"}}'
    @Shared
    def passThroughOperational = 'passthrough-operational'
    @Shared
    def passThroughRunning = 'passthrough-running'
    @Shared
    def expectedOption = '&options=(a=1,b=2)'
    @Shared
    def option = '(a=1,b=2)'
    def dmiServiceBaseUrl = "${dmiServiceName}/dmi/v1/ch/${cmHandleId}/data/ds" +
            "/ncmp-datastore:"
    def NO_TOPIC = null
    def NO_REQUEST_ID = null

    @SpringBean
    JsonObjectMapper spiedJsonObjectMapper = Spy(new JsonObjectMapper(new ObjectMapper()))

    @Autowired
    DmiDataOperations objectUnderTest

    def 'call get resource data for #expectedDatastoreInUrl from DMI without topic #scenario.'() {
        given: 'a persistence cm handle for #cmHandleId'
            mockPersistenceCmHandleRetrieval(dmiProperties)
        and: 'a positive response from DMI service when it is called with the expected parameters'
            def responseFromDmi = new ResponseEntity<Object>(HttpStatus.OK)
            mockDmiRestClient.postOperationWithJsonData(
                    dmiServiceBaseUrl + "${expectedDatastoreInUrl}?resourceIdentifier=${resourceIdentifier}${expectedOptionsInUrl}",
                expectedJson, [Accept:['sample accept header']]) >> responseFromDmi
        when: 'get resource data is invoked'
            def result = objectUnderTest.getResourceDataFromDmi(cmHandleId,resourceIdentifier, options,'sample accept header', dataStore, NO_REQUEST_ID, NO_TOPIC)
        then: 'the result is the response from the DMI service'
            assert result == responseFromDmi
        where: 'the following parameters are used'
            scenario             | dmiProperties       | dataStore               | options || expectedJson               | expectedDatastoreInUrl | expectedOptionsInUrl
            'without properties' | []                  | PASSTHROUGH_OPERATIONAL | option  || expectedJsonWithoutDmiProp | passThroughOperational | expectedOption
            'with properties'    | [dmiSampleProperty] | PASSTHROUGH_OPERATIONAL | option  || expectedJsonWithDmiProp    | passThroughOperational | expectedOption
            'null options'       | [dmiSampleProperty] | PASSTHROUGH_OPERATIONAL | null    || expectedJsonWithDmiProp    | passThroughOperational | ''
            'datastore running'  | []                  | PASSTHROUGH_RUNNING     | option  || expectedJsonWithoutDmiProp | passThroughRunning     | expectedOption
    }

    def 'Write data for pass-through:running datastore in DMI.'() {
        given: 'a persistence cm handle for #cmHandleId'
            mockPersistenceCmHandleRetrieval([dmiSampleProperty])
        and: 'a positive response from DMI service when it is called with the expected parameters'
            def expectedUrl = dmiServiceBaseUrl+"passthrough-running?resourceIdentifier=${resourceIdentifier}"
            def expectedJson = '{"operation":"' + expectedOperationInUrl + '","dataType":"some data type","data":"requestData","cmHandleProperties":{"prop1":"val1"}}'
            def responseFromDmi = new ResponseEntity<Object>(HttpStatus.OK)
            mockDmiRestClient.postOperationWithJsonData(expectedUrl, expectedJson, [:]) >> responseFromDmi
        when: 'write resource method is invoked'
            def result = objectUnderTest.writeResourceDataPassThroughRunningFromDmi(cmHandleId,'parent/child', operation, 'requestData', 'some data type')
        then: 'the result is the response from the DMI service'
            assert result == responseFromDmi
        where: 'the following operation is performed'
            operation || expectedOperationInUrl
            CREATE    || 'create'
            UPDATE    || 'update'
    }
}