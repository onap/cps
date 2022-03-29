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
    def resourceUrl = 'some url'

    def 'DMI POST operation with JSON.'() {
        given: 'the rest template returns a valid response entity'
            def mockResponseEntity = Mock(ResponseEntity)
            mockRestTemplate.postForEntity(resourceUrl, _ as HttpEntity, Object.class) >> mockResponseEntity
        when: 'POST operation is invoked'
            def result = objectUnderTest.postOperationWithJsonData(resourceUrl, 'json-data')
        then: 'the output of the method is equal to the output from the test template'
            result == mockResponseEntity
    }

}
