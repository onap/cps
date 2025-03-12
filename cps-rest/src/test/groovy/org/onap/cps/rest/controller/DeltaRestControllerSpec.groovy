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

import static org.onap.cps.api.parameters.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.api.parameters.FetchDescendantsOption.OMIT_DESCENDANTS
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart

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

    @Shared
    def requestBodyJson = '{"some-key":"some-value","categories":[{"books":[{"authors":["Iain M. Banks"]}]}]}'
    @Shared
    def expectedJsonData = '{"some-key":"some-value","categories":[{"books":[{"authors":["Iain M. Banks"]}]}]}'
    @Shared
    static MultipartFile multipartYangFile = new MockMultipartFile('file', 'filename.yang', 'text/plain', 'content'.getBytes())

    def setup() {
        dataNodeBaseEndpointV2 = "$basePath/v2/dataspaces/$dataspaceName/anchors/$anchorName/delta"
    }

    def 'Get delta between two anchors'() {
        given: 'the service returns a list containing delta reports'
            def deltaReports = new DeltaReportBuilder().actionReplace().withXpath('some xpath').withSourceData('some key': 'some value').withTargetData('some key': 'some value').build()
            def xpath = 'some xpath'
            mockCpsDeltaService.getDeltaByDataspaceAndAnchors(dataspaceName, anchorName, 'targetAnchor', xpath, OMIT_DESCENDANTS) >> [deltaReports]
        when: 'get delta request is performed using REST API'
            def response =
                mvc.perform(get(dataNodeBaseEndpointV2)
                    .param('target-anchor-name', 'targetAnchor')
                    .param('xpath', xpath))
                    .andReturn().response
        then: 'expected response code is returned'
            assert response.status == HttpStatus.OK.value()
        and: 'the response contains expected value'
            assert response.contentAsString.contains('[{\"action\":\"replace\",\"xpath\":\"some xpath\",\"sourceData\":{\"some key\":\"some value\"},\"targetData\":{\"some key\":\"some value\"}}]')
    }

    def 'Get delta between anchor and JSON payload with multipart file'() {
        given: 'sample delta report, xpath, yang model file and json payload'
            def deltaReports = new DeltaReportBuilder().actionCreate().withXpath('some xpath').build()
            def xpath = 'some xpath'
        and: 'the service layer returns a list containing delta reports'
            mockCpsDeltaService.getDeltaByDataspaceAnchorAndPayload(dataspaceName, anchorName, xpath, ['filename.yang':'content'], expectedJsonData, INCLUDE_ALL_DESCENDANTS) >> [deltaReports]
        when: 'get delta request is performed using REST API'
            def response =
                mvc.perform(multipart(dataNodeBaseEndpointV2)
                    .file(multipartYangFile)
                    .param('json', requestBodyJson)
                    .param('xpath', xpath)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andReturn().response
        then: 'expected response code is returned'
            assert response.status == HttpStatus.OK.value()
        and: 'the response contains expected value'
            assert response.contentAsString.contains('[{\"action\":\"create\",\"xpath\":\"some xpath\"}]')
    }

    def 'Get delta between anchor and JSON payload without multipart file'() {
        given: 'sample delta report, xpath, and json payload'
            def deltaReports = new DeltaReportBuilder().actionRemove().withXpath('some xpath').build()
            def xpath = 'some xpath'
        and: 'the service layer returns a list containing delta reports'
            mockCpsDeltaService.getDeltaByDataspaceAnchorAndPayload(dataspaceName, anchorName, xpath, [:], expectedJsonData, INCLUDE_ALL_DESCENDANTS) >> [deltaReports]
        when: 'get delta request is performed using REST API'
            def response =
                mvc.perform(multipart(dataNodeBaseEndpointV2)
                    .param('json', requestBodyJson)
                    .param('xpath', xpath)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andReturn().response
        then: 'expected response code is returned'
            assert response.status == HttpStatus.OK.value()
        and: 'the response contains expected value'
            assert response.contentAsString.contains('[{\"action\":\"remove\",\"xpath\":\"some xpath\"}]')
    }
}
