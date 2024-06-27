/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */
package org.onap.cps.ncmp.rest.stub

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.data.models.DatastoreType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SampleCpsNcmpClientSpec extends Specification {

    static final String CM_HANDLE = "anything"

    static final String DATA_STORE_NAME = DatastoreType.PASSTHROUGH_OPERATIONAL.getDatastoreName()

    @LocalServerPort
    def port

    final def testRestTemplate = new TestRestTemplate()

    @Value('${rest.api.ncmp-stub-base-path}')
    def stubBasePath

    @Autowired
    ObjectMapper objectMapper

    def 'Test the invocation of the stub API' () throws JsonMappingException, JsonProcessingException {

        when: 'Get resource data for cm handle URL is invoked'
            def url = "${getBaseUrl()}/v1/ch/${CM_HANDLE}/data/ds/${DATA_STORE_NAME}?resourceIdentifier=parent&options=(a=1,b=2)"
            def response = testRestTemplate.getForEntity(url, String.class)
        then: 'Response is OK'
            response.getStatusCode() == HttpStatus.OK

        and: 'Response body contains customized stub response that contains correct bookstore category code'
            def typeRef = new TypeReference<HashMap<String, Object>>() {}
            def map = objectMapper.readValue(response.getBody(), typeRef)
            def obj1 = (Map<String, Object>) map.get("stores:bookstore")
            def obj2 = (List<Object>) obj1.get("categories")
            def obj3 = (Map<String, Object>) obj2.iterator().next()

            assert obj3.get("code") == "02"
    }

    def String getBaseUrl() {
        return "http://localhost:" + port + stubBasePath
    }
}
