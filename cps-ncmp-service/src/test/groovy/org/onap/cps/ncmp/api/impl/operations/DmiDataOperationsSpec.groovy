/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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

import org.onap.cps.ncmp.api.impl.client.DmiRestClient
import org.onap.cps.ncmp.api.impl.config.NcmpConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ContextConfiguration

import static org.onap.cps.ncmp.api.impl.operations.DmiOperations.DataStoreEnum.PASSTHROUGH_OPERATIONAL
import static org.onap.cps.ncmp.api.impl.operations.DmiOperations.DataStoreEnum.PASSTHROUGH_RUNNING
import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum.CREATE
import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum.UPDATE
import org.springframework.http.HttpStatus

@SpringBootTest
@ContextConfiguration(classes = [NcmpConfiguration.DmiProperties, DmiDataOperations])
class DmiDataOperationsSpec extends DmiOperationsBaseSpec {

    @Autowired
    DmiDataOperations objectUnderTest

    def 'call get resource data for #expectedDatastoreInUrl from DMI #scenario.'() {
        given: 'a persistence cm handle for #cmHandleId'
            mockPersistenceCmHandleRetrieval(additionalProperties)
        and: 'a positive response from dmi service when it is called with the expected parameters'
            def responseFromDmi = new ResponseEntity<Object>(HttpStatus.OK)
            mockDmiRestClient.postOperationWithJsonData(
                "${dmiServiceName}/dmi/v1/ch/${cmHandleId}/data/ds/ncmp-datastore:${expectedDatastoreInUrl}?resourceIdentifier=${resourceIdentifier}${expectedOptionsInUrl}",
                expectedJson, [Accept:['sample accept header']]) >> responseFromDmi
        when: 'get resource data is invoked'
            def result = objectUnderTest.getResourceDataFromDmi(cmHandleId,resourceIdentifier, options,'sample accept header', dataStore)
        then: 'the result is the response from the dmi service'
            assert result == responseFromDmi
        where: 'the following parameters are used'
            scenario             | additionalProperties       | dataStore               | options     || expectedJson                                                 | expectedDatastoreInUrl    | expectedOptionsInUrl
            'without properties' | []                         | PASSTHROUGH_OPERATIONAL | '(a=1,b=2)' || '{"operation":"read","cmHandleProperties":{}}'               | 'passthrough-operational' | '&options=(a=1,b=2)'
            'null properties'    | null                       | PASSTHROUGH_OPERATIONAL | '(a=1,b=2)' || '{"operation":"read","cmHandleProperties":{}}'               | 'passthrough-operational' | '&options=(a=1,b=2)'
            'with properties'    | [sampleAdditionalProperty] | PASSTHROUGH_OPERATIONAL | '(a=1,b=2)' || '{"operation":"read","cmHandleProperties":{"prop1":"val1"}}' | 'passthrough-operational' | '&options=(a=1,b=2)'
            'null options'       | [sampleAdditionalProperty] | PASSTHROUGH_OPERATIONAL | null        || '{"operation":"read","cmHandleProperties":{"prop1":"val1"}}' | 'passthrough-operational' | ''
            'empty options'      | [sampleAdditionalProperty] | PASSTHROUGH_OPERATIONAL | ''          || '{"operation":"read","cmHandleProperties":{"prop1":"val1"}}' | 'passthrough-operational' | ''
            'datastore running'  | []                         | PASSTHROUGH_RUNNING     | '(a=1,b=2)' || '{"operation":"read","cmHandleProperties":{}}'               | 'passthrough-running'     | '&options=(a=1,b=2)'
    }

    def 'Write data for pass-through:running datastore in DMI.'() {
        given: 'a persistence cm handle for #cmHandleId'
            mockPersistenceCmHandleRetrieval([sampleAdditionalProperty])
        and: 'a positive response from dmi service when it is called with the expected parameters'
            def expectedUrl = "${dmiServiceName}/dmi/v1/ch/${cmHandleId}/data/ds" +
                "/ncmp-datastore:passthrough-running?resourceIdentifier=${resourceIdentifier}"
            def expectedJson = '{"operation":"' + expectedOperationInUrl + '","dataType":"some data type","data":"requestData","cmHandleProperties":{"prop1":"val1"}}'
            def responseFromDmi = new ResponseEntity<Object>(HttpStatus.OK)
            mockDmiRestClient.postOperationWithJsonData(expectedUrl, expectedJson, [:]) >> responseFromDmi
        when: 'write resource method is invoked'
            def result = objectUnderTest.writeResourceDataPassThroughRunningFromDmi(cmHandleId,'parent/child', operation, 'requestData', 'some data type')
        then: 'the result is the response from the dmi service'
            assert result == responseFromDmi
        where: 'the following operation is performed'
            operation || expectedOperationInUrl
            CREATE    || 'create'
            UPDATE    || 'update'
    }

}