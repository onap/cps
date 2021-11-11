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

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.impl.client.DmiRestClient
import org.onap.cps.ncmp.api.models.DmiRequestBody
import org.onap.cps.ncmp.api.models.PersistenceCmHandle
import org.springframework.http.HttpHeaders
import spock.lang.Shared
import spock.lang.Specification

class DmiDataOperationsSpec extends Specification {

    @Shared
    def sampleAdditionalProperty = new PersistenceCmHandle.AdditionalProperty('prop1', 'val1')
    def mockDmiRestClient = Mock(DmiRestClient)
    def mockCmHandlePropertiesRetriever = Mock(PersistenceCmHandleRetriever)
    def objectMapper = new ObjectMapper()

    DmiDataOperations objectUnderTest =
        new DmiDataOperations(mockCmHandlePropertiesRetriever, objectMapper, mockDmiRestClient)

    def 'call get resource data for pass-through:operational datastore from DMI.'() {
        given: 'persistenceCmHandle is setup and returned'
            def persistenceCmHandle = new PersistenceCmHandle()
            persistenceCmHandle.dmiDataServiceName = 'some service name'
            persistenceCmHandle.additionalProperties = additionalProperties
            mockCmHandlePropertiesRetriever.retrieveCmHandleDmiServiceNameAndProperties('some CmHandle') >> persistenceCmHandle
        when: 'get resource data is called to DMI'
            objectUnderTest.getResourceDataFromDmi('some CmHandle',
                    'parent/child',
                    '(a=1,b=2)',
                    'accept something',
                    DmiOperations.DataStoreEnum.PASSTHROUGH_OPERATIONAL)
        then: 'the put operation is executed with the correct URL and a json body with read operation and cm handle properties'
            1 * mockDmiRestClient.putOperationWithJsonData(expectedUrl, expectedJson, expectedHttpHeaders)
        where:
            scenario             | additionalProperties       || expectedJson                                                 | expectedHttpHeaders           | expectedUrl
            'with properties'    | [sampleAdditionalProperty] || '{"operation":"read","cmHandleProperties":{"prop1":"val1"}}' | [Accept:['accept something']] | 'some service name/dmi/v1/ch/some CmHandle/data/ds/ncmp-datastore:passthrough-operational?resourceIdentifier=parent/child&options=(a=1,b=2)'
            'without properties' | []                         || '{"operation":"read","cmHandleProperties":{}}'               | [Accept:['accept something']] | 'some service name/dmi/v1/ch/some CmHandle/data/ds/ncmp-datastore:passthrough-operational?resourceIdentifier=parent/child&options=(a=1,b=2)'
    }

}
