/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Pantheon.tech
 *  Modifications Copyright (C) 2020 Bell Canada. All rights reserved.
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
import org.onap.cps.spi.exceptions.SchemaSetInUseException
import org.onap.cps.spi.model.Anchor
import org.onap.cps.spi.model.SchemaSet
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification
import spock.lang.Unroll

import static org.onap.cps.spi.CascadeDeleteAllowed.CASCADE_DELETE_PROHIBITED
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@WebMvcTest
class AdminRestControllerSpec extends Specification {

    @SpringBean
    CpsModuleService mockCpsModuleService = Mock()

    @SpringBean
    CpsAdminService mockCpsAdminService = Mock()

    @SpringBean
    ModelMapper modelMapper = Mock()

    @Autowired
    MockMvc mvc

    def anchorsEndpoint = '/v1/dataspaces/my_dataspace/anchors'
    def schemaSetsEndpoint = '/v1/dataspaces/test-dataspace/schema-sets'
    def schemaSetEndpoint = schemaSetsEndpoint + '/my_schema_set'

    def anchor = new Anchor(name: 'my_anchor')
    def anchorList = [anchor]

    def 'Create new dataspace'() {
        when:
            def response = performCreateDataspaceRequest("new-dataspace")
        then: 'Service method is invoked with expected parameters'
            1 * mockCpsAdminService.createDataspace("new-dataspace")
        and: 'Dataspace is create successfully'
            response.status == HttpStatus.CREATED.value()
    }

    def 'Create dataspace over existing with same name'() {
        given:
            def thrownException = new DataspaceAlreadyDefinedException("", new RuntimeException())
            mockCpsAdminService.createDataspace("existing-dataspace") >> { throw thrownException }
        when:
            def response = performCreateDataspaceRequest("existing-dataspace")
        then: 'Dataspace creation fails'
            response.status == HttpStatus.BAD_REQUEST.value()
    }

    def 'Create schema set from yang file.'() {
        def yangResourceMapCapture
        given: 'single yang file'
            def multipartFile = createMultipartFile("filename.yang", "content")
        when: 'file uploaded with schema set create request'
            def response = performCreateSchemaSetRequest(multipartFile)
        then: 'associated service method is invoked with expected parameters'
            1 * mockCpsModuleService.createSchemaSet('test-dataspace', 'test-schema-set', _) >>
                    { args -> yangResourceMapCapture = args[2] }
            yangResourceMapCapture['filename.yang'] == 'content'
        and: 'response code indicates success'
            response.status == HttpStatus.CREATED.value()
    }

    def 'Create schema set from zip archive.'() {
        def yangResourceMapCapture
        given: 'zip archive with multiple .yang files inside'
            def multipartFile = createZipMultipartFileFromResource("/yang-files-set.zip")
        when: 'file uploaded with schema set create request'
            def response = performCreateSchemaSetRequest(multipartFile)
        then: 'associated service method is invoked with expected parameters'
            1 * mockCpsModuleService.createSchemaSet('test-dataspace', 'test-schema-set', _) >>
                    { args -> yangResourceMapCapture = args[2] }
            yangResourceMapCapture['assembly.yang'] == "fake assembly content 1\n"
            yangResourceMapCapture['component.yang'] == "fake component content 1\n"
        and: 'response code indicates success'
            response.status == HttpStatus.CREATED.value()
    }

    @Unroll
    def 'Create schema set from zip archive having #caseDescriptor.'() {
        when: 'zip archive having #caseDescriptor is uploaded with create schema set request'
            def response = performCreateSchemaSetRequest(multipartFile)
        then: 'create schema set rejected'
            response.status == HttpStatus.BAD_REQUEST.value()
        where: 'following cases are tested'
            caseDescriptor                        | multipartFile
            'no .yang files inside'               | createZipMultipartFileFromResource("/no-yang-files.zip")
            'multiple .yang files with same name' | createZipMultipartFileFromResource("/yang-files-multiple-sets.zip")
    }

    def 'Create schema set from file with unsupported filename extension.'() {
        given: 'file with unsupported filename extension (.doc)'
            def multipartFile = createMultipartFile("filename.doc", "content")
        when: 'file uploaded with schema set create request'
            def response = performCreateSchemaSetRequest(multipartFile)
        then: 'create schema set rejected'
            response.status == HttpStatus.BAD_REQUEST.value()
    }

    @Unroll
    def 'Create schema set from #fileType file with IOException occurrence on processing.'() {
        when: 'file uploaded with schema set create request'
            def response = performCreateSchemaSetRequest(createMultipartFileForIOException(fileType))
        then: 'the error response returned indicating internal server error occurrence'
            response.status == HttpStatus.INTERNAL_SERVER_ERROR.value()
        where: 'following file types are used'
            fileType << ['YANG', 'ZIP']
    }


    def 'Delete schema set.'() {
        when: 'delete schema set endpoint is invoked'
            def response = performDeleteRequest(schemaSetEndpoint)
        then: 'associated service method is invoked with expected parameters'
            1 * mockCpsModuleService.deleteSchemaSet('test-dataspace', 'my_schema_set', CASCADE_DELETE_PROHIBITED)
        and: 'response code indicates success'
            response.status == HttpStatus.NO_CONTENT.value()
    }

    def 'Delete schema set which is in use.'() {
        given: 'the service method throws an exception indicating the schema set is in use'
            def thrownException = new SchemaSetInUseException('test-dataspace', 'my_schema_set')
            mockCpsModuleService.deleteSchemaSet('test-dataspace', 'my_schema_set', CASCADE_DELETE_PROHIBITED) >>
                    { throw thrownException }
        when: 'delete schema set endpoint is invoked'
            def response = performDeleteRequest(schemaSetEndpoint)
        then: 'schema set deletion fails with conflict response code'
            response.status == HttpStatus.CONFLICT.value()
    }

    def performCreateDataspaceRequest(String dataspaceName) {
        return mvc.perform(
                post('/v1/dataspaces').param('dataspace-name', dataspaceName)
        ).andReturn().response
    }

    def createMultipartFile(filename, content) {
        return new MockMultipartFile("file", filename, "text/plain", content.getBytes())
    }

    def createZipMultipartFileFromResource(resourcePath) {
        return new MockMultipartFile("file", "test.zip", "application/zip",
                getClass().getResource(resourcePath).getBytes())
    }

    def createMultipartFileForIOException(extension) {
        def multipartFile = Mock(MultipartFile)
        multipartFile.getOriginalFilename() >> "TEST." + extension
        multipartFile.getBytes() >> { throw new IOException() }
        multipartFile.getInputStream() >> { throw new IOException() }
        return multipartFile;
    }

    def performCreateSchemaSetRequest(multipartFile) {
        return mvc.perform(
                multipart(schemaSetsEndpoint)
                        .file(multipartFile)
                        .param('schema-set-name', 'test-schema-set')
        ).andReturn().response
    }

    def performDeleteRequest(String uri) {
        return mvc.perform(delete(uri)).andReturn().response
    }

    def 'Get existing schema set'() {
        given:
            mockCpsModuleService.getSchemaSet('test-dataspace', 'my_schema_set') >>
                    new SchemaSet(name: 'my_schema_set', dataspaceName: 'test-dataspace')
        when: 'get schema set API is invoked'
            def response = mvc.perform(get(schemaSetEndpoint)).andReturn().response
        then: 'the correct schema set is returned'
            response.status == HttpStatus.OK.value()
            response.getContentAsString().contains('my_schema_set')
    }

    def 'Create Anchor'() {
        given:
            def requestParams = new LinkedMultiValueMap<>()
            requestParams.add('schema-set-name', 'my_schema-set')
            requestParams.add('anchor-name', 'my_anchor')
        when: 'post is invoked'
            def response = mvc.perform(post(anchorsEndpoint).contentType(MediaType.APPLICATION_JSON)
                    .params(requestParams as MultiValueMap)).andReturn().response
        then: 'Anchor is created successfully'
            1 * mockCpsAdminService.createAnchor('my_dataspace', 'my_schema-set', 'my_anchor')
            response.status == HttpStatus.CREATED.value()
            response.getContentAsString().contains('my_anchor')
    }

    def 'Get existing anchor'() {
        given:
            mockCpsAdminService.getAnchors('my_dataspace') >> anchorList
        when: 'get all anchors API is invoked'
            def response = mvc.perform(get(anchorsEndpoint)).andReturn().response
        then: 'the correct anchor is returned'
            response.status == HttpStatus.OK.value()
            response.getContentAsString().contains('my_anchor')
    }
}
