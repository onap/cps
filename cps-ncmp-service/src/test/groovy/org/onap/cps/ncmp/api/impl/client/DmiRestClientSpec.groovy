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

package org.onap.cps.ncmp.api.impl.client

import org.onap.cps.ncmp.api.impl.config.NcmpConfiguration
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.client.RestTemplate
import spock.lang.Specification

@SpringBootTest
@ContextConfiguration(classes = [NcmpConfiguration.DmiProperties, DmiRestClient])
class DmiRestClientSpec extends Specification {

    @SpringBean
    RestTemplate mockRestTemplate = Mock(RestTemplate)

    @Autowired
    DmiRestClient objectUnderTest

    def 'DMI PUT operation.'() {
        given: 'a PUT url'
            def getResourceDataUrl = 'http://some-uri/getResourceDataUrl'
        and: 'the rest template returns a valid response entity'
            def mockResponseEntity = Mock(ResponseEntity)
            mockRestTemplate.exchange(getResourceDataUrl, HttpMethod.PUT, _ as HttpEntity, Object.class) >> mockResponseEntity
        when: 'PUT operation is invoked'
            def result = objectUnderTest.putOperationWithJsonData(getResourceDataUrl, 'json-data', new HttpHeaders())
        then: 'the output of the method is equal to the output from the test template'
            result == mockResponseEntity
    }

    def 'DMI POST operation.'() {
        given: 'a POST url'
            def getResourceDataUrl = 'http://some-uri/createResourceDataUrl'
        and: 'the rest template returns a valid response entity'
            def mockResponseEntity = Mock(ResponseEntity)
            mockRestTemplate.postForEntity(getResourceDataUrl, _ as HttpEntity, String.class) >> mockResponseEntity
        when: 'POST operation is invoked'
            def result = objectUnderTest.postOperationWithJsonData(getResourceDataUrl, 'json-data', new HttpHeaders())
        then: 'the output of the method is equal to the output from the test template'
            result == mockResponseEntity
    }

}
