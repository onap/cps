/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Pantheon.tech
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.rest.controller

import org.modelmapper.ModelMapper
import org.onap.cps.api.CpsAdminService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.spi.exceptions.DataspaceAlreadyDefinedException
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import spock.lang.Specification

@WebMvcTest
class AdminRestControllerSpec extends Specification {

    @SpringBean
    CpsModuleService mockCpsModuleService = Mock()

    @SpringBean
    CpsAdminService mockCpsAdminService = Mock()

    @SpringBean
    ModelMapper modelMapper = Mock();

    @Autowired
    MockMvc mvc

    def 'Create new dataspace'() {
        when:
            def response = performCreateDataspaceRequest("new-dataspace")
        then: 'Service method is invoked with expected parameters'
            1 * mockCpsAdminService.createDataspace("new-dataspace")
        and:
            response.status == HttpStatus.CREATED.value()
    }

    def 'Create dataspace over existing with same name'() {
        given:
            def thrownException = new DataspaceAlreadyDefinedException("", new RuntimeException())
            mockCpsAdminService.createDataspace("existing-dataspace") >> { throw thrownException }
        when:
            def response = performCreateDataspaceRequest("existing-dataspace")
        then:
            response.status == HttpStatus.BAD_REQUEST.value()
    }

    def 'Create schema set from yang file'() {
        def yangResourceMapCapture
        given:
            def multipartFile = createMultipartFile("filename.yang", "content")
        when:
            def response = performCreateSchemaSetRequest(multipartFile)
        then: 'Service method is invoked with expected parameters'
            1 * mockCpsModuleService.createSchemaSet('test-dataspace', 'test-schema-set', _) >>
                    { args -> yangResourceMapCapture = args[2] }
            yangResourceMapCapture['filename.yang'] == 'content'
        and: 'Response code indicates success'
            response.status == HttpStatus.CREATED.value()
    }

    def 'Create schema set from file with invalid filename extension'() {
        given:
            def multipartFile = createMultipartFile("filename.doc", "content")
        when:
            def response = performCreateSchemaSetRequest(multipartFile)
        then:
            response.status == HttpStatus.BAD_REQUEST.value()
    }

    def performCreateDataspaceRequest(String dataspaceName) {
        return mvc.perform(
                MockMvcRequestBuilders
                        .post('/v1/dataspaces')
                        .param('dataspace-name', dataspaceName)
        ).andReturn().response
    }

    def createMultipartFile(filename, content) {
        return new MockMultipartFile("file", filename, "text/plain", content.getBytes())
    }

    def performCreateSchemaSetRequest(multipartFile) {
        return mvc.perform(
                MockMvcRequestBuilders
                        .multipart('/v1/dataspaces/test-dataspace/schema-sets')
                        .file(multipartFile)
                        .param('schemaSetName', 'test-schema-set')
        ).andReturn().response
    }

}
