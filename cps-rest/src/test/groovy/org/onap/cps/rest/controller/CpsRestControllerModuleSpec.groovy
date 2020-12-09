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

import org.onap.cps.api.CpService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.rest.exceptions.CpsRestExceptionHandler
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import spock.lang.Specification

import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup

class CpsRestControllerModuleSpec extends Specification {

    def cpsRestController = new CpsRestController();
    def mockCpsModuleService = Mock(CpsModuleService.class)
    def mockMvc = standaloneSetup(cpsRestController).setControllerAdvice(new CpsRestExceptionHandler()).build()

    def setup() {
        cpsRestController.cpsModuleService = mockCpsModuleService
    }

    def 'Create schema set from file with bad name'() {
        given: 'File with unexpected extension in name'
            def multipartFile = createMultipartFile("filename.doc", "content")
        when:
            def response = performCreateSchemaSetRequest(multipartFile)
        then: 'Bad Request error response is returned'
            response.status == HttpStatus.BAD_REQUEST.value()
    }

    def 'Create schema set from empty file'() {
        given: 'Zero length file'
            def multipartFile = createMultipartFile("filename.yang", "")
        when:
            def response = performCreateSchemaSetRequest(multipartFile)
        then: 'Bad Request error response is returned'
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

    def createMultipartFile(filename, content) {
        return new MockMultipartFile("file", filename, "text/plain", content.getBytes())
    }

    def performCreateSchemaSetRequest(multipartFile) {
        return mockMvc.perform(
                MockMvcRequestBuilders
                        .multipart('/v1/dataspaces/test-dataspace/schema-sets')
                        .file(multipartFile)
                        .param('schemaSetName', 'test-schema-set')
        ).andReturn().response
    }

}
