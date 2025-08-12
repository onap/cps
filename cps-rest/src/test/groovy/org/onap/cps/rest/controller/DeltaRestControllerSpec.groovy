/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 TechMahindra Ltd.
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
import org.onap.cps.api.CpsDeltaService
import org.onap.cps.impl.DeltaReportBuilder
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.web.multipart.MultipartFile
import spock.lang.Shared
import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

import static org.onap.cps.api.parameters.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.api.parameters.FetchDescendantsOption.OMIT_DESCENDANTS
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@WebMvcTest(DeltaRestController)
class DeltaRestControllerSpec extends Specification {

    @SpringBean
    CpsDeltaService mockCpsDeltaService = Mock()

    @SpringBean
    JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

    @Autowired
    MockMvc mvc

    @Value('${rest.api.cps-base-path}')
    def basePath

    def dataNodeBaseEndpointV2
    def dataspaceName = 'my_dataspace'
    def anchorName = 'my_anchor'
    def NO_GROUPING = false

    @Shared
    def requestBodyJson = '{"some-key":"some-value","categories":[{"books":[{"authors":["Iain M. Banks"]}]}]}'
    @Shared
    def expectedJsonData = '{"some-key":"some-value","categories":[{"books":[{"authors":["Iain M. Banks"]}]}]}'
    @Shared
    static MultipartFile multipartYangFile = new MockMultipartFile('yangResourceFile', 'filename.yang', 'text/plain', 'content'.getBytes())
    @Shared
    Path targetDataAsJsonFile
    @Shared
    MockMultipartFile multipartTargetDataAsJsonFile

    def setup() {
        dataNodeBaseEndpointV2 = "$basePath/v2/dataspaces/$dataspaceName/anchors/$anchorName/delta"
        targetDataAsJsonFile = Files.createTempFile('requestBody', '.json')
        Files.write(targetDataAsJsonFile, requestBodyJson.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE)
        multipartTargetDataAsJsonFile = new MockMultipartFile('targetDataAsJsonFile', targetDataAsJsonFile.fileName.toString(), 'application/json', Files.readAllBytes(targetDataAsJsonFile))
    }

    def cleanup() {
        Files.deleteIfExists(targetDataAsJsonFile)
    }

    def 'Get delta between two anchors with content type #scenario'() {
        given: 'the service returns a list containing delta reports'
            def xpath = 'some xpath'
            def deltaReports = new DeltaReportBuilder().actionReplace().withXpath(xpath).withSourceData('some_key': 'some value').withTargetData('some_key': 'some value').build()
                mockCpsDeltaService.getDeltaByDataspaceAndAnchors(dataspaceName, anchorName, 'targetAnchor', xpath, OMIT_DESCENDANTS, NO_GROUPING) >> [deltaReports]
        when: 'get delta request is performed using REST API'
            def response = mvc.perform(get(dataNodeBaseEndpointV2)
                .contentType(contentType)
                .accept(contentType)
                .param('target-anchor-name', 'targetAnchor')
                .param('xpath', xpath))
                .andReturn().response
        then: 'expected response code is returned'
            assert response.status == HttpStatus.OK.value()
        and: 'the response contains expected value'
            assert response.contentAsString.contains(expectedResponse)
        where:
            scenario  | contentType                   || expectedResponse
            'JSON'    | MediaType.APPLICATION_JSON    || '[{"action":"replace","xpath":"some xpath","sourceData":{"some_key":"some value"},"targetData":{"some_key":"some value"}}]'
            'XML'     | MediaType.APPLICATION_XML     || '<deltaReports><deltaReport id="1"><action>replace</action><xpath>some xpath</xpath><source-data><some_key>some value</some_key></source-data><target-data><some_key>some value</some_key></target-data></deltaReport></deltaReports>'
    }

    def 'Get delta between anchor and JSON payload with yangResourceFile'() {
        given: 'sample delta report, xpath, yang model file and json payload'
            def deltaReports = new DeltaReportBuilder().actionCreate().withXpath('some xpath').build()
            def xpath = 'some xpath'
        and: 'the service layer returns a list containing delta reports'
            mockCpsDeltaService.getDeltaByDataspaceAnchorAndPayload(dataspaceName, anchorName, xpath, ['filename.yang':'content'], expectedJsonData, INCLUDE_ALL_DESCENDANTS, NO_GROUPING) >> [deltaReports]
        when: 'get delta request is performed using REST API'
            def response =
                mvc.perform(multipart(dataNodeBaseEndpointV2)
                    .file(multipartYangFile)
                    .file(multipartTargetDataAsJsonFile)
                    .param('xpath', xpath)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andReturn().response
        then: 'expected response code is returned'
            assert response.status == HttpStatus.OK.value()
        and: 'the response contains expected value'
            assert response.contentAsString.contains('[{\"action\":\"create\",\"xpath\":\"some xpath\"}]')
    }

    def 'Get delta between anchor and JSON payload without yangResourceFile'() {
        given: 'sample delta report, xpath, and json payload'
            def deltaReports = new DeltaReportBuilder().actionRemove().withXpath('some xpath').build()
            def xpath = 'some xpath'
        and: 'the service layer returns a list containing delta reports'
            mockCpsDeltaService.getDeltaByDataspaceAnchorAndPayload(dataspaceName, anchorName, xpath, [:], expectedJsonData, INCLUDE_ALL_DESCENDANTS, NO_GROUPING) >> [deltaReports]
        when: 'get delta request is performed using REST API'
            def response =
                mvc.perform(multipart(dataNodeBaseEndpointV2)
                    .file(multipartTargetDataAsJsonFile)
                    .param('xpath', xpath)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andReturn().response
        then: 'expected response code is returned'
            assert response.status == HttpStatus.OK.value()
        and: 'the response contains expected value'
            assert response.contentAsString.contains('[{\"action\":\"remove\",\"xpath\":\"some xpath\"}]')
    }

    def 'Attempt to get delta between anchor and JSON payload with an Empty File'() {
        given: 'xpath, yang model file and empty json payload'
            def xpath = 'some xpath'
            def emptyTargetDataAsJsonFile = new MockMultipartFile('targetDataAsJsonFile', 'empty.json', 'application/json', new byte[0])
        when: 'get delta request is performed using REST API'
            def response = mvc.perform(multipart(dataNodeBaseEndpointV2)
                .file(emptyTargetDataAsJsonFile)
                .param('xpath', xpath)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn()
                .response
        then: 'expected response code is returned'
            assert response.status == HttpStatus.BAD_REQUEST.value()
        then: 'the response contains expected error message'
           assert response.contentAsString.contains("JSON file is required")
    }

    def 'Get delta between anchor and JSON payload with an invalid data'() {
        given: 'xpath, yang model file and empty json payload'
            def xpath = 'some xpath'
            def invalidJsonContent = '{'
            def invalidTargetDataAsJsonFile = new MockMultipartFile('targetDataAsJsonFile', 'invalid.json', 'application/json', invalidJsonContent.getBytes())
        when: 'get delta request is performed using REST API'
            def response = mvc.perform(multipart(dataNodeBaseEndpointV2)
                .file(invalidTargetDataAsJsonFile)
                .param('xpath', xpath)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andReturn()
                .response
        then: 'expected response code is returned'
            assert response.status == HttpStatus.BAD_REQUEST.value()
        then: 'the response contains expected error message'
            assert response.contentAsString.contains("Parsing error occurred while converting JSON content to Json Node")
    }

    def 'Apply changes from a delta report, in JSON format, on an anchor'() {
        given: 'sample delta report, xpath, and json payload'
            def deltaReports = 'some delta report'
            def applyDeltaEndpointV2 = "$basePath/v2/dataspaces/$dataspaceName/anchors/$anchorName/applyChangesInDeltaReport"
        when: 'request to apply delta report to an anchor is performed using REST API'
            def response =
                mvc.perform(post(applyDeltaEndpointV2)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(deltaReports)
                ).andReturn().response
        then: 'expected response code is returned'
            assert response.status == HttpStatus.CREATED.value()
    }
}
