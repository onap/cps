/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2021 Pantheon.tech
 *  Modifications Copyright (C) 2020-2021 Bell Canada.
 *  Modifications Copyright (C) 2021-2025 Nordix Foundation
 *  Modifications Copyright (C) 2022-2025 TechMahindra Ltd.
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

package org.onap.cps.rest.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.CpsAnchorService
import org.onap.cps.api.CpsNotificationService
import org.onap.cps.impl.DataNodeBuilder
import org.onap.cps.utils.JsonObjectMapper
import org.onap.cps.utils.PrefixResolver

import static org.onap.cps.api.parameters.CascadeDeleteAllowed.CASCADE_DELETE_PROHIBITED
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put

import org.mapstruct.factory.Mappers
import org.onap.cps.api.CpsAnchorService
import org.onap.cps.api.CpsDataspaceService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.api.exceptions.AlreadyDefinedException
import org.onap.cps.api.exceptions.SchemaSetInUseException
import org.onap.cps.api.model.Anchor
import org.onap.cps.api.model.Dataspace
import org.onap.cps.api.model.SchemaSet
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import spock.lang.Specification

import static org.onap.cps.api.parameters.CascadeDeleteAllowed.CASCADE_DELETE_PROHIBITED
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@WebMvcTest(AdminRestController)
class AdminRestControllerSpec extends Specification {

    @SpringBean
    CpsModuleService mockCpsModuleService = Mock()

    @SpringBean
    CpsDataspaceService mockCpsDataspaceService = Mock()

    @SpringBean
    CpsAnchorService mockCpsAnchorService = Mock()

    @SpringBean
    CpsNotificationService mockCpsNotificationService = Mock()

    @SpringBean
    CpsRestInputMapper cpsRestInputMapper = Mappers.getMapper(CpsRestInputMapper)

    @SpringBean
    JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

    @SpringBean
    PrefixResolver prefixResolver = Mock()

    @Autowired
    MockMvc mvc

    @Value('${rest.api.cps-base-path}')
    def basePath

    def dataspaceName = 'my_dataspace'
    def anchorName = 'my_anchor'
    def schemaSetName = 'my_schema_set'
    def anchor = new Anchor(name: anchorName, dataspaceName: dataspaceName, schemaSetName: schemaSetName)
    def dataspace = new Dataspace(name: dataspaceName)

    def 'Create new dataspace with #scenario.'() {
        when: 'post is invoked on endpoint for creating a dataspace'
            def response =
                mvc.perform(
                    post("/cps/api/${apiVersion}/dataspaces")
                        .param('dataspace-name', dataspaceName))
                    .andReturn().response
        then: 'service method is invoked with expected parameters'
            1 * mockCpsDataspaceService.createDataspace(dataspaceName)
        and: 'dataspace is create successfully'
            response.status == HttpStatus.CREATED.value()
            assert response.getContentAsString() == expectedResponseBody
        where: 'following cases are tested'
            scenario | apiVersion || expectedResponseBody
            'V1 API' | 'v1'       || 'my_dataspace'
            'V2 API' | 'v2'       || ''
    }

    def 'Create dataspace over existing with same name.'() {
        given: 'the endpoint to create a dataspace'
            def createDataspaceEndpoint = "$basePath/v1/dataspaces"
        and: 'the service method throws an exception indicating the dataspace is already defined'
            def thrownException = new AlreadyDefinedException(dataspaceName, new RuntimeException())
            mockCpsDataspaceService.createDataspace(dataspaceName) >> { throw thrownException }
        when: 'post is invoked'
            def response =
                    mvc.perform(
                            post(createDataspaceEndpoint)
                                    .param('dataspace-name', dataspaceName))
                            .andReturn().response
        then: 'dataspace creation fails'
            response.status == HttpStatus.CONFLICT.value()
    }

    def 'Get a dataspace.'() {
        given: 'service method returns a dataspace'
            mockCpsDataspaceService.getDataspace(dataspaceName) >> dataspace
        and: 'the endpoint for getting a dataspace by name'
            def getDataspaceEndpoint = "$basePath/v1/admin/dataspaces/$dataspaceName"
        when: 'get dataspace API is invoked'
            def response = mvc.perform(get(getDataspaceEndpoint)).andReturn().response
        then: 'the correct dataspace is returned'
            response.status == HttpStatus.OK.value()
            response.getContentAsString().contains(dataspaceName)
    }

    def 'Clean a dataspace.'() {
        given: 'service method returns a dataspace'
            mockCpsDataspaceService.getDataspace(dataspaceName) >> dataspace
        and: 'the endpoint for cleaning a dataspace'
            def postCleanDataspaceEndpoint = "$basePath/v1/admin/dataspaces/$dataspaceName/actions/clean"
        when: 'post is invoked on the clean dataspace endpoint'
            def response = mvc.perform(post(postCleanDataspaceEndpoint)).andReturn().response
        then: 'no content is returned'
            response.status == HttpStatus.NO_CONTENT.value()
    }

    def 'Get all dataspaces.'() {
        given: 'service method returns all dataspace'
            mockCpsDataspaceService.getAllDataspaces() >> [dataspace, new Dataspace(name: "dataspace-test2")]
        and: 'an endpoint'
            def getAllDataspaceEndpoint = "$basePath/v1/admin/dataspaces"
        when: 'get all dataspace API is invoked'
            def response = mvc.perform(get(getAllDataspaceEndpoint)).andReturn().response
        then: 'the correct dataspace is returned'
            response.status == HttpStatus.OK.value()
            response.getContentAsString().contains(dataspaceName)
            response.getContentAsString().contains("dataspace-test2")
    }

    def 'Create schema set from yang file with #scenario.'() {
        def yangResourceMapCapture
        given: 'single yang file'
            def multipartFile = createMultipartFile("filename.yang", "content")
        when: 'file uploaded with schema set create request'
            def response =
                    mvc.perform(
                            multipart("/cps/api/${apiVersion}/dataspaces/my_dataspace/schema-sets")
                                    .file(multipartFile)
                                    .param('schema-set-name', schemaSetName))
                            .andReturn().response
        then: 'associated service method is invoked with expected parameters'
            1 * mockCpsModuleService.createSchemaSet(dataspaceName, schemaSetName, _) >>
                    { args -> yangResourceMapCapture = args[2] }
            yangResourceMapCapture['filename.yang'] == 'content'
        and: 'response code indicates success'
            assert response.status == HttpStatus.CREATED.value()
            assert response.getContentAsString() == expectedResponseBody
        where: 'following cases are tested'
            scenario | apiVersion || expectedResponseBody
            'V1 API' | 'v1'       || 'my_schema_set'
            'V2 API' | 'v2'       || ''
    }

    def 'Create schema set from zip archive with #scenario.'() {
        def yangResourceMapCapture
        given: 'zip archive with multiple .yang files inside'
            def multipartFile = createZipMultipartFileFromResource("/yang-files-set.zip")
        when: 'file uploaded with schema set create request'
            def response =
                    mvc.perform(
                            multipart("/cps/api/${apiVersion}/dataspaces/my_dataspace/schema-sets")
                                    .file(multipartFile)
                                    .param('schema-set-name', schemaSetName))
                            .andReturn().response
        then: 'associated service method is invoked with expected parameters'
            1 * mockCpsModuleService.createSchemaSet(dataspaceName, schemaSetName, _) >> { args -> yangResourceMapCapture = args[2] }
            yangResourceMapCapture['assembly.yang'] == "fake assembly content 1\n"
            yangResourceMapCapture['component.yang'] == "fake component content 1\n"
        and: 'response code indicates success'
            assert response.status == HttpStatus.CREATED.value()
            assert response.getContentAsString() == expectedResponseBody
        where: 'following cases are tested'
            scenario | apiVersion || expectedResponseBody
            'V1 API' | 'v1'       || 'my_schema_set'
            'V2 API' | 'v2'       || ''
    }

    def 'Create a schema set from a yang file that is greater than 1MB #scenario.'() {
        given: 'a yang file greater than 1MB'
            def multipartFile = createMultipartFileFromResource("/model-over-1mb.yang")
        when: 'a file is uploaded to the create schema set endpoint'
            def response =
                    mvc.perform(
                            multipart("/cps/api/${apiVersion}/dataspaces/my_dataspace/schema-sets")
                                    .file(multipartFile)
                                    .param('schema-set-name', schemaSetName))
                            .andReturn().response
        then: 'the associated service method is invoked'
            1 * mockCpsModuleService.createSchemaSet(dataspaceName, schemaSetName, _)
        and: 'the response code indicates success'
            assert response.status == HttpStatus.CREATED.value()
            assert response.getContentAsString() == expectedResponseBody
        where: 'following cases are tested'
            scenario | apiVersion || expectedResponseBody
            'V1 API' | 'v1'       || 'my_schema_set'
            'V2 API' | 'v2'       || ''
    }

    def 'Create schema set from zip archive having #caseDescriptor.'() {
        given: 'the endpoint to create a schema set'
            def schemaSetEndpoint = "$basePath/v1/dataspaces/$dataspaceName/schema-sets"
        when: 'zip archive having #caseDescriptor is uploaded with create schema set request'
            def response =
                    mvc.perform(
                            multipart(schemaSetEndpoint)
                                    .file(multipartFile)
                                    .param('schema-set-name', schemaSetName))
                            .andReturn().response
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
        and: 'the endpoint to create a schema set'
            def schemaSetEndpoint = "$basePath/v1/dataspaces/$dataspaceName/schema-sets"
        when: 'file uploaded with schema set create request'
            def response =
                    mvc.perform(
                            multipart(schemaSetEndpoint)
                                    .file(multipartFile)
                                    .param('schema-set-name', schemaSetName))
                            .andReturn().response
        then: 'create schema set rejected'
            response.status == HttpStatus.BAD_REQUEST.value()
    }

    def 'Create schema set from #fileType file with IOException occurrence on processing.'() {
        given: 'the endpoint to create a schema set'
            def schemaSetEndpoint = "$basePath/v1/dataspaces/$dataspaceName/schema-sets"
        when: 'file uploaded with schema set create request'
            def multipartFile = createMultipartFileForIOException(fileType)
            def response =
                    mvc.perform(
                            multipart(schemaSetEndpoint)
                                    .file(multipartFile)
                                    .param('schema-set-name', schemaSetName))
                            .andReturn().response
        then: 'the error response returned indicating internal server error occurrence'
            response.status == HttpStatus.INTERNAL_SERVER_ERROR.value()
        where: 'following file types are used'
            fileType << ['YANG', 'ZIP']
    }

    def 'Delete schema set.'() {
        given: 'the endpoint for deleting a schema set'
            def schemaSetEndpoint = "$basePath/v1/dataspaces/$dataspaceName/schema-sets/$schemaSetName"
        when: 'delete schema set endpoint is invoked'
            def response = mvc.perform(delete(schemaSetEndpoint)).andReturn().response
        then: 'associated service method is invoked with expected parameters'
            1 * mockCpsModuleService.deleteSchemaSet(dataspaceName, schemaSetName, CASCADE_DELETE_PROHIBITED)
        and: 'response code indicates success'
            response.status == HttpStatus.NO_CONTENT.value()
    }

    def 'Delete schema set which is in use.'() {
        given: 'service method throws an exception indicating the schema set is in use'
            def thrownException = new SchemaSetInUseException(dataspaceName, schemaSetName)
            mockCpsModuleService.deleteSchemaSet(dataspaceName, schemaSetName, CASCADE_DELETE_PROHIBITED) >>
                    { throw thrownException }
        and: 'the endpoint for deleting a schema set'
            def schemaSetEndpoint = "$basePath/v1/dataspaces/$dataspaceName/schema-sets/$schemaSetName"
        when: 'delete schema set endpoint is invoked'
            def response = mvc.perform(delete(schemaSetEndpoint)).andReturn().response
        then: 'schema set deletion fails with conflict response code'
            response.status == HttpStatus.CONFLICT.value()
    }

    def 'Get existing schema set.'() {
        given: 'service method returns a new schema set'
            mockCpsModuleService.getSchemaSet(dataspaceName, schemaSetName) >>
                    new SchemaSet(name: schemaSetName, dataspaceName: dataspaceName)
        and: 'the endpoint for getting a schema set'
            def schemaSetEndpoint = "$basePath/v1/dataspaces/$dataspaceName/schema-sets/$schemaSetName"
        when: 'get schema set API is invoked'
            def response = mvc.perform(get(schemaSetEndpoint)).andReturn().response
        then: 'the correct schema set is returned'
            response.status == HttpStatus.OK.value()
            response.getContentAsString().contains(schemaSetName)
    }

    def 'Get all schema sets for a given dataspace name.'() {
        given: 'service method returns all schema sets for a dataspace'
            mockCpsModuleService.getSchemaSets(dataspaceName) >>
                [new SchemaSet(name: schemaSetName, dataspaceName: dataspaceName),
                new SchemaSet(name: "test-schemaset", dataspaceName: dataspaceName)]
        and: 'the  endpoint for getting all schema sets'
            def schemaSetEndpoint = "$basePath/v1/dataspaces/$dataspaceName/schema-sets"
        when: 'get schema sets API is invoked'
            def response = mvc.perform(get(schemaSetEndpoint)).andReturn().response
        then: 'the correct schema sets is returned'
            assert response.status == HttpStatus.OK.value()
            assert response.getContentAsString() == '[{"dataspaceName":"my_dataspace","moduleReferences":[],"name":' +
                   '"my_schema_set"},{"dataspaceName":"my_dataspace","moduleReferences":[],"name":"test-schemaset"}]'
    }

    def 'Create Anchor with #scenario.'() {
        given: 'request parameters'
            def requestParams = new LinkedMultiValueMap<>()
            requestParams.add('schema-set-name', schemaSetName)
            requestParams.add('anchor-name', anchorName)
        when: 'post is invoked on the create anchors endpoint'
            def response =
                    mvc.perform(
                            post("/cps/api/${apiVersion}/dataspaces/my_dataspace/anchors")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .params(requestParams as MultiValueMap))
                                    .andReturn().response
        then: 'anchor is created successfully'
            1 * mockCpsAnchorService.createAnchor(dataspaceName, schemaSetName, anchorName)
            assert response.status == HttpStatus.CREATED.value()
            assert response.getContentAsString() == expectedResponseBody
        where: 'following cases are tested'
            scenario | apiVersion || expectedResponseBody
            'V1 API' | 'v1'       || 'my_anchor'
            'V2 API' | 'v2'       || ''
    }

    def 'Get existing anchors.'() {
        given: 'service method returns a list of (one) anchors'
            mockCpsAnchorService.getAnchors(dataspaceName) >> [anchor]
        and: 'the endpoint for getting all anchors'
            def anchorEndpoint = "$basePath/v1/dataspaces/$dataspaceName/anchors"
        when: 'get all anchors API is invoked'
            def response = mvc.perform(get(anchorEndpoint)).andReturn().response
        then: 'the correct anchor is returned'
            response.status == HttpStatus.OK.value()
            response.getContentAsString().contains(anchorName)
    }

    def 'Get existing anchor by dataspace and anchor name.'() {
        given: 'service method returns an anchor'
            mockCpsAnchorService.getAnchor(dataspaceName, anchorName) >>
                    new Anchor(name: anchorName, dataspaceName: dataspaceName, schemaSetName: schemaSetName)
        and: 'the endpoint for getting an anchor'
            def anchorEndpoint = "$basePath/v1/dataspaces/$dataspaceName/anchors/$anchorName"
        when: 'get anchor API is invoked'
            def response = mvc.perform(get(anchorEndpoint)).andReturn().response
            def responseContent = response.getContentAsString()
        then: 'the correct anchor is returned'
            response.status == HttpStatus.OK.value()
            responseContent.contains(anchorName)
            responseContent.contains(dataspaceName)
            responseContent.contains(schemaSetName)
    }

    def 'Delete anchor.'() {
        given: 'the endpoint for deleting an anchor'
            def anchorEndpoint = "$basePath/v1/dataspaces/$dataspaceName/anchors/$anchorName"
        when: 'delete method is invoked on anchor endpoint'
            def response = mvc.perform(delete(anchorEndpoint)).andReturn().response
        then: 'associated service method is invoked with expected parameters'
            1 * mockCpsAnchorService.deleteAnchor(dataspaceName, anchorName)
        and: 'response code indicates success'
            response.status == HttpStatus.NO_CONTENT.value()
    }

    def 'Delete dataspace.'() {
        given: 'the endpoint for deleting a dataspace'
            def dataspaceEndpoint = "$basePath/v1/dataspaces"
        when: 'delete dataspace endpoint is invoked'
            def response = mvc.perform(delete(dataspaceEndpoint)
                .param('dataspace-name', dataspaceName))
                .andReturn().response
        then: 'associated service method is invoked with expected parameter'
            1 * mockCpsDataspaceService.deleteDataspace(dataspaceName)
        and: 'response code indicates success'
            response.status == HttpStatus.NO_CONTENT.value()
    }

    def 'Add notification subscription'() {
        given: 'an endpoint and its payload'
            def endpoint = "$basePath/v2/notification-subscription"
            def xpath = '/dataspaces'
            def jsonPayload = '{"dataspace":[{"name":"ds01"}]}'
        when: 'post request is performed'
            def response =
                mvc.perform(
                        post(endpoint)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonPayload))
                        .andReturn().response
        then: 'notification service method is invoked with expected parameter'
            1 * mockCpsNotificationService.createNotificationSubscription(jsonPayload, xpath)
        and: 'HTTP response code indicates success'
            response.status == HttpStatus.CREATED.value()
    }

    def 'delete notification subscription'() {
        given: 'an endpoint and xpath'
            def endpoint = "$basePath/v2/notification-subscription"
            def xpath = '/dataspaces'
        when: 'delete request is performed'
            def response = mvc.perform(delete(endpoint).param('xpath', xpath)).andReturn().response
        then: 'notification service method is invoked with expected parameter'
            1 * mockCpsNotificationService.deleteNotificationSubscription(xpath)
        and: 'HTTP response code indicates success'
            response.status == HttpStatus.NO_CONTENT.value()
    }

    def 'Get notification subscription.'() {
        given: 'and endpoint and xpath'
            def endPoint = "$basePath/v2/notification-subscription"
            def xpath = '/dataspaces'
        and: 'notification service method returns notification subscription data'
            mockCpsNotificationService.getNotificationSubscription(xpath) >> [new DataNodeBuilder().withXpath('/dataspaces')
                                                                                      .withLeaves([leaf: 'dataspace', leafList: ['ds01', 'ds02']]).build()]
        when: 'get notification subscription is invoked'
            def response = mvc.perform(get(endPoint).param('xpath', xpath)).andReturn().response
        then: 'HTTP response code indicates success'
            response.status == HttpStatus.OK.value()
        and: 'response message contains notification subscription data'
            response.getContentAsString() == '[{"dataspaces":{"leaf":"dataspace","leafList":["ds01","ds02"]}}]'
    }

    def createMultipartFile(filename, content) {
        return new MockMultipartFile("file", filename, "text/plain", content.getBytes())
    }

    def createZipMultipartFileFromResource(resourcePath) {
        return new MockMultipartFile("file", "test.zip", "application/zip",
                getClass().getResource(resourcePath).getBytes())
    }

    def createMultipartFileFromResource(resourcePath) {
        return new MockMultipartFile("file", "test.yang", "application/text",
                getClass().getResource(resourcePath).getBytes())
    }

    def createMultipartFileForIOException(extension) {
        def multipartFile = Mock(MockMultipartFile)
        multipartFile.getOriginalFilename() >> "TEST." + extension
        multipartFile.getBytes() >> { throw new IOException() }
        multipartFile.getInputStream() >> { throw new IOException() }
        return multipartFile
    }

}
