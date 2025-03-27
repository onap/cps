/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.datajobs.DataJobService
import org.onap.cps.ncmp.api.datajobs.models.DataJobMetadata
import org.onap.cps.ncmp.api.datajobs.models.DataJobRequest
import org.onap.cps.ncmp.api.datajobs.models.DataJobWriteRequest
import org.onap.cps.ncmp.api.datajobs.models.WriteOperation
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import spock.lang.Specification
import org.springframework.http.MediaType
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@SpringBootTest(classes = [DataJobController])
@EnableAutoConfiguration
@AutoConfigureMockMvc
class DataJobControllerSpec extends Specification {

    @Autowired
    MockMvc mvc

    @SpringBean
    DataJobService mockDataJobService = Mock()

    @SpringBean
    ObjectMapper objectMapper = new ObjectMapper()

    @SpringBean
    JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(objectMapper)

    def 'successful writeDataJob call responds with OK'() {
        given: 'a valid DataJobWriteRequest having authorization, dataJobId, and request payload'
            def authorization = ''
            def dataJobId = 'job123'
            def dataJobMetadata = new DataJobMetadata('any-destination', 'application/vnd.3gpp.object-tree-hierarchical+json', 'application/3gpp-json-patch+json')
            def writeOperations = [
                        new WriteOperation('/path/to/node', 'create', 'op123', 'value1'),
                        new WriteOperation('/path/to/anotherNode', 'update', 'op124', 42)
                ]
            def dataJobWriteRequest = new DataJobWriteRequest(writeOperations)
            def requestPayloadAsString = jsonObjectMapper.asJsonString(new DataJobRequest(dataJobMetadata, dataJobWriteRequest))
        when: 'write data job API is called'
            def result = mvc.perform(
                    post("/donotuse/${dataJobId}/write")
                            .header('Authorization', authorization)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestPayloadAsString)
            )
        then: 'Response is 200 OK'
            result.andExpect(MockMvcResultMatchers.status().isOk())
        and: 'Ensure the service method is called once with expected parameters'
            1 * mockDataJobService.writeDataJob(authorization, dataJobId, dataJobMetadata, dataJobWriteRequest)
    }
}

