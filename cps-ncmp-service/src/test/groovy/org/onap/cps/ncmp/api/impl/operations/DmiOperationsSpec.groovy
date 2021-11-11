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
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@SpringBootTest
@ContextConfiguration(classes = [NcmpConfiguration.DmiProperties, DmiOperations])
class DmiOperationsSpec extends Specification {

    @SpringBean
    DmiRestClient mockDmiRestClient = Mock()

    @Autowired
    DmiOperations objectUnderTest = new DmiOperations(mockDmiRestClient)

    def 'call get resource data for pass-through:operational datastore from DMI.'() {
        given: 'expected url'
            def expectedUrl = 'testDmiBasePath/dmi/v1/ch/testCmhandle/data/ds' +
                    '/ncmp-datastore:passthrough-operational?resourceIdentifier=parent/child&options=(a=1,b=2)'
        when: 'get resource data is called to DMI'
            objectUnderTest.getResourceDataOperationalFromDmi('testDmiBasePath',
                    'testCmhandle',
                    'parent/child',
                    '(a=1,b=2)',
                    'testAcceptJson',
                    'testJsonbody')
        then: 'the put operation is executed with the correct URL'
            1 * mockDmiRestClient.putOperationWithJsonData(expectedUrl, 'testJsonbody', _ as HttpHeaders)
    }
    def 'call get resource data for pass-through:running datastore from DMI.'() {
        given: 'expected url'
            def expectedUrl = 'testDmiBasePath/dmi/v1/ch/testCmhandle/data/ds' +
                    '/ncmp-datastore:passthrough-running?resourceIdentifier=parent/child&options=(a=1,b=2)'
        when: 'get resource data is called to DMI'
            objectUnderTest.getResourceDataPassThroughRunningFromDmi('testDmiBasePath',
                    'testCmhandle',
                    'parent/child',
                    '(a=1,b=2)',
                    'testAcceptJson',
                    'testJsonbody')
        then: 'the put operation is executed with the correct URL'
            1 * mockDmiRestClient.putOperationWithJsonData(expectedUrl, 'testJsonbody', _ as HttpHeaders)
    }
    def 'call get resource data for pass-through:operational datastore from DMI when options is null.'() {
        given: 'expected url'
        def expectedUrl = 'testDmiBasePath/dmi/v1/ch/testCmhandle/data/ds' +
                '/ncmp-datastore:passthrough-operational?resourceIdentifier=parent/child'
        when: 'get resource data is called to DMI'
        objectUnderTest.getResourceDataOperationalFromDmi('testDmiBasePath',
                'testCmhandle',
                'parent/child',
                null,
                'testAcceptJson',
                'testJsonbody')
        then: 'the put operation is executed with the correct URL'
        1 * mockDmiRestClient.putOperationWithJsonData(expectedUrl, 'testJsonbody', _ as HttpHeaders)
    }
    def 'call create resource data for pass-through:running datastore from DMI.'() {
        given: 'expected url'
            def expectedUrl = 'testDmiBasePath/dmi/v1/ch/testCmhandle/data/ds' +
                    '/ncmp-datastore:passthrough-running?resourceIdentifier=parent/child'
        when: 'get resource data is called to DMI'
            objectUnderTest.createResourceDataPassThroughRunningFromDmi('testDmiBasePath',
                    'testCmhandle',
                    'parent/child',
                    'testJsonbody')
        then: 'the put operation is executed with the correct URL'
            1 * mockDmiRestClient.postOperationWithJsonData(expectedUrl, 'testJsonbody', _ as HttpHeaders)
    }

    def 'Call get resource from dmi with json data.'() {
        given: 'expected url & json data'
            def requestBody = 'some json'
            def expectedUrl = 'testDmiBasePath/dmi/v1/ch/testCmHandle/modules'
            def expectedHttpHeaders = new HttpHeaders()
        when: 'get resource data is called to dmi'
            objectUnderTest.getResourceFromDmiWithJsonData('testDmiBasePath',
                    requestBody,
                    'testCmHandle',
                    'modules')
        then: 'the post operation is executed with the correct URL and json data'
            1 * mockDmiRestClient.postOperationWithJsonData(expectedUrl, requestBody, expectedHttpHeaders)
    }
}
