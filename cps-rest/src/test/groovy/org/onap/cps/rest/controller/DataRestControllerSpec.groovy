/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021-2022 Bell Canada.
 *  Modifications Copyright (C) 2022 Deutsche Telekom AG
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
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsFacade
import org.onap.cps.utils.ContentType
import org.onap.cps.utils.DateTimeUtility
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Shared
import spock.lang.Specification

import static org.onap.cps.api.parameters.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.api.parameters.FetchDescendantsOption.OMIT_DESCENDANTS
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put

@WebMvcTest(DataRestController)
class DataRestControllerSpec extends Specification {

    @SpringBean
    CpsFacade mockCpsFacade = Mock()

    @SpringBean
    CpsDataService mockCpsDataService = Mock()

    @SpringBean
    JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

    @Autowired
    MockMvc mvc

    @Value('${rest.api.cps-base-path}')
    def basePath

    def dataNodeBaseEndpointV1
    def dataNodeBaseEndpointV2
    def dataNodeBaseEndpointV3
    def dataspaceName = 'my_dataspace'
    def anchorName = 'my_anchor'
    def noTimestamp = null

    @Shared
    def requestBodyJson = '{"some-key":"some-value","categories":[{"books":[{"authors":["Iain M. Banks"]}]}]}'

    @Shared
    def expectedJsonData = '{"some-key":"some-value","categories":[{"books":[{"authors":["Iain M. Banks"]}]}]}'

    @Shared
    def requestBodyXml = '<?xml version=\'1.0\' encoding=\'UTF-8\'?>\n<bookstore xmlns="org:onap:ccsdk:sample">\n</bookstore>'

    @Shared
    def expectedXmlData = '<?xml version=\'1.0\' encoding=\'UTF-8\'?>\n<bookstore xmlns="org:onap:ccsdk:sample">\n</bookstore>'

    def setup() {
        dataNodeBaseEndpointV1 = "$basePath/v1/dataspaces/$dataspaceName"
        dataNodeBaseEndpointV2 = "$basePath/v2/dataspaces/$dataspaceName"
        dataNodeBaseEndpointV3 = "$basePath/v3/dataspaces/$dataspaceName"
    }

    def 'Create a node: #scenario.'() {
        given: 'endpoint to create a node'
            def endpoint = "$dataNodeBaseEndpointV1/anchors/$anchorName/nodes"
        when: 'post is invoked with datanode endpoint and json'
            def response =
                mvc.perform(
                    post(endpoint)
                        .contentType(contentType)
                        .param('xpath', parentNodeXpath)
                        .content(requestBody)
                ).andReturn().response
        then: 'a created response is returned'
            response.status == HttpStatus.CREATED.value()
        then: 'the cps data service was called with the correct parameters'
            1 * mockCpsDataService.saveData(dataspaceName, anchorName, expectedData, noTimestamp, expectedContentType)
        where: 'following xpath parameters are are used'
            scenario                                   | parentNodeXpath | contentType                | expectedContentType | requestBody     | expectedData
            'JSON content: no xpath parameter'         | ''              | MediaType.APPLICATION_JSON | ContentType.JSON    | requestBodyJson | expectedJsonData
            'JSON content: xpath parameter point root' | '/'             | MediaType.APPLICATION_JSON | ContentType.JSON    | requestBodyJson | expectedJsonData
            'XML content: no xpath parameter'          | ''              | MediaType.APPLICATION_XML  | ContentType.XML     | requestBodyXml  | expectedXmlData
            'XML content: xpath parameter point root'  | '/'             | MediaType.APPLICATION_XML  | ContentType.XML     | requestBodyXml  | expectedXmlData
    }

    def 'Create a node with observed-timestamp.'() {
        given: 'endpoint to create a node'
            def endpoint = "$dataNodeBaseEndpointV1/anchors/$anchorName/nodes"
        when: 'post is invoked with datanode endpoint and json'
            def response =
                mvc.perform(
                    post(endpoint)
                        .contentType(contentType)
                        .param('xpath', '')
                        .param('observed-timestamp', observedTimestamp)
                        .content(content)
                ).andReturn().response
        then: 'a created response is returned'
            response.status == expectedHttpStatus.value()
        then: 'the cps data service was called with the correct parameters'
            expectedApiCount * mockCpsDataService.saveData(dataspaceName, anchorName, expectedData,
                { it == DateTimeUtility.toOffsetDateTime(observedTimestamp) }, expectedContentType)
        where:
            scenario                          | observedTimestamp              | contentType                | content         || expectedApiCount | expectedHttpStatus     | expectedData     | expectedContentType
            'with observed-timestamp JSON'    | '2021-03-03T23:59:59.999-0400' | MediaType.APPLICATION_JSON | requestBodyJson || 1                | HttpStatus.CREATED     | expectedJsonData | ContentType.JSON
            'with observed-timestamp XML'     | '2021-03-03T23:59:59.999-0400' | MediaType.APPLICATION_XML  | requestBodyXml  || 1                | HttpStatus.CREATED     | expectedXmlData  | ContentType.XML
            'with invalid observed-timestamp' | 'invalid'                      | MediaType.APPLICATION_JSON | requestBodyJson || 0                | HttpStatus.BAD_REQUEST | expectedJsonData | ContentType.JSON
    }

    def 'Validate data using create a node API.'() {
        given: 'an endpoint to create a node'
            def endpoint = "$dataNodeBaseEndpointV1/anchors/$anchorName/nodes"
            def parentNodeXpath = '/'
        and: 'dryRunEnabled flag is set to true'
            def dryRunEnabled = 'true'
        when: 'post is invoked with json data and dry-run flag enabled'
            def response =
                mvc.perform(
                    post(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param('xpath', parentNodeXpath)
                        .param('dry-run', dryRunEnabled)
                        .content(requestBodyJson)
                ).andReturn().response
        then: 'a 200 OK response is returned'
            response.status == HttpStatus.OK.value()
        then: 'the cps data service was called with correct parameters'
            1 * mockCpsDataService.validateData(dataspaceName, anchorName, parentNodeXpath, requestBodyJson, ContentType.JSON)
    }

    def 'Create a child node #scenario.'() {
        given: 'endpoint to create a node'
            def endpoint = "$dataNodeBaseEndpointV1/anchors/$anchorName/nodes"
        and: 'parent node xpath'
            def parentNodeXpath = 'some xpath'
        when: 'post is invoked with datanode endpoint and json'
            def postRequestBuilder = post(endpoint)
                .contentType(contentType)
                .param('xpath', parentNodeXpath)
                .content(requestBody)
            if (observedTimestamp != null)
                postRequestBuilder.param('observed-timestamp', observedTimestamp)
            def response =
                mvc.perform(postRequestBuilder).andReturn().response
        then: 'a created response is returned'
            response.status == HttpStatus.CREATED.value()
        then: 'the cps data service was called with the correct parameters'
            1 * mockCpsDataService.saveData(dataspaceName, anchorName, parentNodeXpath, expectedData,
                DateTimeUtility.toOffsetDateTime(observedTimestamp), expectedContentType)
        where:
            scenario                          | observedTimestamp              | contentType                | requestBody     | expectedData     | expectedContentType
            'with observed-timestamp JSON'    | '2021-03-03T23:59:59.999-0400' | MediaType.APPLICATION_JSON | requestBodyJson | expectedJsonData | ContentType.JSON
            'with observed-timestamp XML'     | '2021-03-03T23:59:59.999-0400' | MediaType.APPLICATION_XML  | requestBodyXml  | expectedXmlData  | ContentType.XML
            'without observed-timestamp JSON' | null                           | MediaType.APPLICATION_JSON | requestBodyJson | expectedJsonData | ContentType.JSON
            'without observed-timestamp XML'  | null                           | MediaType.APPLICATION_XML  | requestBodyXml  | expectedXmlData  | ContentType.XML
    }

    def 'save list elements under root node #scenario.'() {
        given: 'root node xpath '
            def rootNodeXpath = '/'
        when: 'list-node endpoint is invoked with post (create) operation'
            def postRequestBuilder = post("$dataNodeBaseEndpointV1/anchors/$anchorName/list-nodes")
                .contentType(contentType)
                .param('xpath', rootNodeXpath )
                .content(requestBody)
            if (observedTimestamp != null)
                postRequestBuilder.param('observed-timestamp', observedTimestamp)
            def response = mvc.perform(postRequestBuilder).andReturn().response
        then: 'a created response is returned'
            response.status == expectedHttpStatus.value()
        then: 'the java API was called with the correct parameters'
            expectedApiCount * mockCpsDataService.saveListElements(dataspaceName, anchorName, rootNodeXpath, expectedData,
                { it == DateTimeUtility.toOffsetDateTime(observedTimestamp) }, expectedContentType)
        where:
            scenario                                            | observedTimestamp              | contentType                | requestBody     || expectedApiCount | expectedHttpStatus     | expectedData     | expectedContentType
            'Content type JSON with observed-timestamp'         | '2021-03-03T23:59:59.999-0400' | MediaType.APPLICATION_JSON | requestBodyJson || 1                | HttpStatus.CREATED     | expectedJsonData | ContentType.JSON
            'Content type JSON without observed-timestamp'      | null                           | MediaType.APPLICATION_JSON | requestBodyJson || 1                | HttpStatus.CREATED     | expectedJsonData | ContentType.JSON
            'Content type JSON with invalid observed-timestamp' | 'invalid'                      | MediaType.APPLICATION_JSON | requestBodyJson || 0                | HttpStatus.BAD_REQUEST | expectedJsonData | ContentType.JSON
            'Content type XML with observed-timestamp'          | '2021-03-03T23:59:59.999-0400' | MediaType.APPLICATION_XML  | requestBodyXml  || 1                | HttpStatus.CREATED     | expectedXmlData  | ContentType.XML
            'Content type XML without observed-timestamp'       | null                           | MediaType.APPLICATION_XML  | requestBodyXml  || 1                | HttpStatus.CREATED     | expectedXmlData  | ContentType.XML
            'Content type XML with invalid observed-timestamp'  | 'invalid'                      | MediaType.APPLICATION_XML  | requestBodyXml  || 0                | HttpStatus.BAD_REQUEST | expectedXmlData  | ContentType.XML
    }

    def 'Save list elements #scenario.'() {
        given: 'parent node xpath '
            def parentNodeXpath = 'parent node xpath'
        when: 'list-node endpoint is invoked with post (create) operation'
            def postRequestBuilder = post("$dataNodeBaseEndpointV1/anchors/$anchorName/list-nodes")
                .contentType(contentType)
                .param('xpath', parentNodeXpath)
                .content(requestBody)
            if (observedTimestamp != null)
                postRequestBuilder.param('observed-timestamp', observedTimestamp)
            def response = mvc.perform(postRequestBuilder).andReturn().response
        then: 'a created response is returned'
            response.status == expectedHttpStatus.value()
        then: 'the cps data service was called with the correct parameters when needed'
            expectedApiCount * mockCpsDataService.saveListElements(dataspaceName, anchorName, parentNodeXpath, expectedData,
                { it == DateTimeUtility.toOffsetDateTime(observedTimestamp) }, expectedContentType)
        where: 'the following parameters are used'
            scenario                                            | observedTimestamp              | contentType                | requestBody     || expectedApiCount | expectedHttpStatus     | expectedData     | expectedContentType
            'Content type JSON with observed-timestamp'         | '2021-03-03T23:59:59.999-0400' | MediaType.APPLICATION_JSON | requestBodyJson || 1                | HttpStatus.CREATED     | expectedJsonData | ContentType.JSON
            'Content type JSON without observed-timestamp'      | null                           | MediaType.APPLICATION_JSON | requestBodyJson || 1                | HttpStatus.CREATED     | expectedJsonData | ContentType.JSON
            'Content type JSON with invalid observed-timestamp' | 'invalid'                      | MediaType.APPLICATION_JSON | requestBodyJson || 0                | HttpStatus.BAD_REQUEST | expectedJsonData | ContentType.JSON
            'Content type XML with observed-timestamp'          | '2021-03-03T23:59:59.999-0400' | MediaType.APPLICATION_XML  | requestBodyXml  || 1                | HttpStatus.CREATED     | expectedXmlData  | ContentType.XML
            'Content type XML without observed-timestamp'       | null                           | MediaType.APPLICATION_XML  | requestBodyXml  || 1                | HttpStatus.CREATED     | expectedXmlData  | ContentType.XML
            'Content type XML with invalid observed-timestamp'  | 'invalid'                      | MediaType.APPLICATION_XML  | requestBodyXml  || 0                | HttpStatus.BAD_REQUEST | expectedXmlData  | ContentType.XML
    }

    def 'Validate data using Save list elements API'() {
        given: 'endpoint to save list elements'
            def endpoint = "$dataNodeBaseEndpointV1/anchors/$anchorName/list-nodes"
        and: 'dryRunEnabled flag is set to true'
            def dryRunEnabled = 'true'
        when: 'post request is performed'
            def response =
                mvc.perform(
                    post(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param('xpath', '/')
                        .content(requestBodyJson)
                        .param('dry-run', dryRunEnabled)
                ).andReturn().response
        then: 'a 200 OK response is returned'
            response.status == HttpStatus.OK.value()
        then: 'the cps data service was called with correct parameters'
            1 * mockCpsDataService.validateData(dataspaceName, anchorName, '/', requestBodyJson, ContentType.JSON)
    }

    def 'Get data nodes [V1] with #scenario.'() {
        given: 'the service returns data node with #scenario'
            def xpath = 'my/path'
            def endpoint = "$dataNodeBaseEndpointV1/anchors/$anchorName/node"
        when: 'get request is performed through REST API'
            def response =
                mvc.perform(
                    get(endpoint)
                        .param('xpath', xpath)
                        .param('include-descendants', includeDescendantsOption))
                    .andReturn().response
        then: 'the cps facade is called with the correct parameters'
            1 * mockCpsFacade.getFirstDataNodeByAnchor(dataspaceName, anchorName, xpath, expectedCpsDataServiceOption) >> [mocked:'result']
        then: 'a success response is returned'
            response.status == HttpStatus.OK.value()
        and: 'the response contains the facade result in json format'
            response.getContentAsString() == '{"mocked":"result"}'
        where: 'the following parameters are used'
            scenario                    | includeDescendantsOption || expectedCpsDataServiceOption
            'no descendants (default) ' | ''                       || OMIT_DESCENDANTS
            'with descendants'          | 'true'                   || INCLUDE_ALL_DESCENDANTS
    }

    def 'Get data node with #scenario using V2. output type #scenario.'() {
        given: 'the service returns data nodes with #scenario'
            def xpath = 'some xPath'
            def endpoint = "$dataNodeBaseEndpointV2/anchors/$anchorName/node"
        when: 'V2 of get request is performed through REST API'
            def response =
                mvc.perform(get(endpoint)
                        .contentType(contentType)
                        .param('xpath', xpath)
                        .param('descendants', 'all'))
                    .andReturn().response
        then: 'the cps service facade is called with the correct parameters and returns some data'
            1 * mockCpsFacade.getDataNodesByAnchor(dataspaceName, anchorName, xpath, INCLUDE_ALL_DESCENDANTS) >> [[mocked:'result1'], [mocked:'result2']]
        and: 'a success response is returned'
            assert response.status == HttpStatus.OK.value()
        and: 'the response is in the expected format'
            assert response.contentAsString == expectedResult
        where: 'the following content types are used'
            scenario | contentType                || expectedResult
            'XML'    | MediaType.APPLICATION_XML  || '<mocked>result1</mocked><mocked>result2</mocked>'
            'JSON'   | MediaType.APPLICATION_JSON || '[{"mocked":"result1"},{"mocked":"result2"}]'
    }

    def 'Get data node with #scenario using V3. output type #scenario.'() {
        given: 'the service returns data nodes with #scenario'
            def xpath = 'some xPath'
            def endpoint = "$dataNodeBaseEndpointV3/anchors/$anchorName/node"
        when: 'V3 of get request is performed through REST API'
            def response =
                mvc.perform(get(endpoint)
                    .contentType(contentType)
                    .param('xpath', xpath)
                    .param('descendants', 'all'))
                    .andReturn().response
        then: 'the cps service facade is called with the correct parameters and returns some data'
            1 * mockCpsFacade.getDataNodesByAnchorV3(dataspaceName, anchorName, xpath, INCLUDE_ALL_DESCENDANTS) >> [books: [[title: 'Book 1'], [title: 'Book 2']]]
        and: 'a success response is returned'
            assert response.status == HttpStatus.OK.value()
        and: 'the response is in the expected format'
            assert response.contentAsString == expectedResult
        where: 'the following content types are used'
            scenario | contentType                || expectedResult
            'XML'    | MediaType.APPLICATION_XML  || '<books><title>Book 1</title></books><books><title>Book 2</title></books>'
            'JSON'   | MediaType.APPLICATION_JSON || '{"books":[{"title":"Book 1"},{"title":"Book 2"}]}'
    }

    def 'Update data node leaves: #scenario.'() {
        given: 'endpoint to update a node '
            def endpoint = "$dataNodeBaseEndpointV1/anchors/$anchorName/nodes"
        when: 'patch request is performed'
            def response =
                mvc.perform(
                    patch(endpoint)
                        .contentType(contentType)
                        .content(requestBody)
                        .param('xpath', inputXpath)
                ).andReturn().response
        then: 'the cps data service method is invoked with expected parameters'
            1 * mockCpsDataService.updateNodeLeaves(dataspaceName, anchorName, xpathServiceParameter, expectedData, null, expectedContentType)
        and: 'response status indicates success'
            response.status == HttpStatus.OK.value()
        where:
            scenario                             | inputXpath    | contentType                || xpathServiceParameter | requestBody     | expectedData        | expectedContentType
            'JSON content: root node by default' | ''            | MediaType.APPLICATION_JSON || '/'                   | requestBodyJson | expectedJsonData    | ContentType.JSON
            'JSON content: root node by choice'  | '/'           | MediaType.APPLICATION_JSON || '/'                   | requestBodyJson | expectedJsonData    | ContentType.JSON
            'JSON content: some xpath by parent' | '/some/xpath' | MediaType.APPLICATION_JSON || '/some/xpath'         | requestBodyJson | expectedJsonData    | ContentType.JSON
            'XML content: root node by default'  | ''            | MediaType.APPLICATION_XML  || '/'                   | requestBodyXml  | expectedXmlData     | ContentType.XML
            'XML content: root node by choice'   | '/'           | MediaType.APPLICATION_XML  || '/'                   | requestBodyXml  | expectedXmlData     | ContentType.XML
            'XML content: some xpath by parent'  | '/some/xpath' | MediaType.APPLICATION_XML  || '/some/xpath'         | requestBodyXml  | expectedXmlData     | ContentType.XML
    }

    def 'Update data node leaves with observedTimestamp.'() {
        given: 'endpoint to update a node leaves '
            def endpoint = "$dataNodeBaseEndpointV1/anchors/$anchorName/nodes"
        when: 'patch request is performed'
            def response =
                mvc.perform(
                    patch(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyJson)
                        .param('xpath', '/')
                        .param('observed-timestamp', observedTimestamp)
                ).andReturn().response
        then: 'the cps data service method is invoked with expected parameters'
            expectedApiCount * mockCpsDataService.updateNodeLeaves(dataspaceName, anchorName, '/', expectedJsonData,
                { it == DateTimeUtility.toOffsetDateTime(observedTimestamp) }, ContentType.JSON)
        and: 'response status indicates success'
            response.status == expectedHttpStatus.value()
        where:
            scenario                          | observedTimestamp              || expectedApiCount | expectedHttpStatus
            'with observed-timestamp'         | '2021-03-03T23:59:59.999-0400' || 1                | HttpStatus.OK
            'with invalid observed-timestamp' | 'invalid'                      || 0                | HttpStatus.BAD_REQUEST
    }

    def 'Validate data using Update a node API.'() {
        given: 'endpoint to update a node leaves'
            def endpoint = "$dataNodeBaseEndpointV1/anchors/$anchorName/nodes"
        and: 'dryRunEnabled flag is set to true'
            def dryRunEnabled = 'true'
        when: 'patch request is performed'
            def response =
                mvc.perform(
                    patch(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyJson)
                        .param('xpath', '/')
                        .param('dry-run', dryRunEnabled)
                ).andReturn().response
        then: 'a 200 OK response is returned'
            response.status == HttpStatus.OK.value()
        then: 'the cps data service was called with correct parameters'
            1 * mockCpsDataService.validateData(dataspaceName, anchorName, '/', requestBodyJson, ContentType.JSON)
    }

    def 'Replace data node tree: #scenario.'() {
        given: 'endpoint to replace node'
            def endpoint = "$dataNodeBaseEndpointV1/anchors/$anchorName/nodes"
        when: 'put request is performed'
            def response =
                mvc.perform(
                    put(endpoint)
                        .contentType(contentType)
                        .content(requestBody)
                        .param('xpath', inputXpath))
                    .andReturn().response
        then: 'the cps data service method is invoked with expected parameters'
            1 * mockCpsDataService.updateDataNodeAndDescendants(dataspaceName, anchorName, xpathServiceParameter, expectedData, noTimestamp, expectedContentType)
        and: 'response status indicates success'
            response.status == HttpStatus.OK.value()
        where:
            scenario                             | inputXpath    | contentType                || xpathServiceParameter | requestBody     | expectedData     | expectedContentType
            'JSON content: root node by default' | ''            | MediaType.APPLICATION_JSON || '/'                   | requestBodyJson | expectedJsonData | ContentType.JSON
            'JSON content: root node by choice'  | '/'           | MediaType.APPLICATION_JSON || '/'                   | requestBodyJson | expectedJsonData | ContentType.JSON
            'JSON content: some xpath by parent' | '/some/xpath' | MediaType.APPLICATION_JSON || '/some/xpath'         | requestBodyJson | expectedJsonData | ContentType.JSON
            'XML content: root node by default'  | ''            | MediaType.APPLICATION_XML  || '/'                   | requestBodyXml  | expectedXmlData  | ContentType.XML
            'XML content: root node by choice'   | '/'           | MediaType.APPLICATION_XML  || '/'                   | requestBodyXml  | expectedXmlData  | ContentType.XML
            'XML content: some xpath by parent'  | '/some/xpath' | MediaType.APPLICATION_XML  || '/some/xpath'         | requestBodyXml  | expectedXmlData  | ContentType.XML
    }

    def 'Validate data using Replace data node API.'() {
        given: 'endpoint to replace node'
            def endpoint = "$dataNodeBaseEndpointV1/anchors/$anchorName/nodes"
        and: 'dryRunEnabled flag is set to true'
            def dryRunEnabled = 'true'
        when: 'put request is performed'
            def response =
                mvc.perform(
                    put(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyJson)
                        .param('xpath', '/')
                        .param('dry-run', dryRunEnabled)
                ).andReturn().response
        then: 'a 200 OK response is returned'
            response.status == HttpStatus.OK.value()
        then: 'the cps data service was called with correct parameters'
            1 * mockCpsDataService.validateData(dataspaceName, anchorName, '/', requestBodyJson, ContentType.JSON)
    }

    def 'Replace data node tree returns #hasNewNodes for #scenario.'() {
        given: 'endpoint to replace node'
        def endpoint = "$dataNodeBaseEndpointV2/anchors/$anchorName/nodes"
        when: 'put request is performed'
        def response =
                mvc.perform(
                        put(endpoint)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBodyJson)
                                .param('xpath', ''))
                        .andReturn().response
        then: 'the cps data service method is invoked with expected parameters'
        1 * mockCpsDataService.updateDataNodeAndDescendants(dataspaceName, anchorName, '/', expectedJsonData, noTimestamp, ContentType.JSON) >> hasNewNodes
        and: 'response status indicates success or creation'
        assert response.status == expectedStatus
        where:
        scenario                                      | hasNewNodes || expectedStatus
        'JSON content: root node updated only'        | false       || HttpStatus.OK.value()
        'JSON content: root node with new list items' | true        || HttpStatus.CREATED.value()
    }

    def 'Update data node and descendants with observedTimestamp.'() {
        given: 'endpoint to replace node'
            def endpoint = "$dataNodeBaseEndpointV1/anchors/$anchorName/nodes"
        when: 'put request is performed'
            def response =
                mvc.perform(
                    put(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyJson)
                        .param('xpath', '')
                        .param('observed-timestamp', observedTimestamp))
                    .andReturn().response
        then: 'the cps data service method is invoked with expected parameters'
            expectedApiCount * mockCpsDataService.updateDataNodeAndDescendants(dataspaceName, anchorName, '/', expectedJsonData,
                { it == DateTimeUtility.toOffsetDateTime(observedTimestamp) }, ContentType.JSON)
        and: 'response status indicates success'
            response.status == expectedHttpStatus.value()
        where:
            scenario                          | observedTimestamp              || expectedApiCount | expectedHttpStatus
            'with observed-timestamp'         | '2021-03-03T23:59:59.999-0400' || 1                | HttpStatus.OK
            'with invalid observed-timestamp' | 'invalid'                      || 0                | HttpStatus.BAD_REQUEST
    }

    def 'Replace list content #scenario.'() {
        when: 'list-nodes endpoint is invoked with put (update) operation'
            def putRequestBuilder = put("$dataNodeBaseEndpointV1/anchors/$anchorName/list-nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .param('xpath', 'parent xpath')
                .content(requestBodyJson)
            if (observedTimestamp != null)
                putRequestBuilder.param('observed-timestamp', observedTimestamp)
            def response = mvc.perform(putRequestBuilder).andReturn().response
        then: 'a success response is returned'
            response.status == expectedHttpStatus.value()
        and: 'the cps data service was called with the correct parameters'
            expectedApiCount * mockCpsDataService.replaceListContent(dataspaceName, anchorName, 'parent xpath', expectedJsonData,
                { it == DateTimeUtility.toOffsetDateTime(observedTimestamp) }, ContentType.JSON)
        where:
            scenario                          | observedTimestamp              || expectedApiCount | expectedHttpStatus
            'with observed-timestamp'         | '2021-03-03T23:59:59.999-0400' || 1                | HttpStatus.OK
            'without observed-timestamp'      | null                           || 1                | HttpStatus.OK
            'with invalid observed-timestamp' | 'invalid'                      || 0                | HttpStatus.BAD_REQUEST
    }

    def 'Replace list XML content #scenario.'() {
        when: 'list-nodes endpoint is invoked with put (update) operation'
            def putRequestBuilder = put("$dataNodeBaseEndpointV1/anchors/$anchorName/list-nodes")
                .contentType(MediaType.APPLICATION_XML)
                .param('xpath', 'parent xpath')
                .content(requestBodyXml)
            if (observedTimestamp != null)
                putRequestBuilder.param('observed-timestamp', observedTimestamp)
            def response = mvc.perform(putRequestBuilder).andReturn().response
        then: 'a success response is returned'
            response.status == expectedHttpStatus.value()
        and: 'the cps data service was called with the correct parameters'
            expectedApiCount * mockCpsDataService.replaceListContent(dataspaceName, anchorName, 'parent xpath', expectedXmlData,
                { it == DateTimeUtility.toOffsetDateTime(observedTimestamp) }, ContentType.XML)
        where:
            scenario                          | observedTimestamp              || expectedApiCount | expectedHttpStatus
            'with observed-timestamp'         | '2021-03-03T23:59:59.999-0400' || 1                | HttpStatus.OK
            'without observed-timestamp'      | null                           || 1                | HttpStatus.OK
            'with invalid observed-timestamp' | 'invalid'                      || 0                | HttpStatus.BAD_REQUEST
    }

    def 'Validate data using Replace list content API.'() {
        given: 'endpoint to replace list-nodes'
            def endpoint = "$dataNodeBaseEndpointV1/anchors/$anchorName/list-nodes"
        and: 'dryRunEnabled flag is set to true'
            def dryRunEnabled = 'true'
        when: 'put request is performed'
            def response =
                mvc.perform(
                    put(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param('xpath', '/')
                        .content(requestBodyJson)
                        .param('dry-run', dryRunEnabled)
                ).andReturn().response
        then: 'a 200 OK response is returned'
            response.status == HttpStatus.OK.value()
        then: 'the cps data service was called with correct parameters'
            1 * mockCpsDataService.validateData(dataspaceName, anchorName, '/', requestBodyJson, ContentType.JSON)
    }

    def 'Delete list element #scenario.'() {
        when: 'list-nodes endpoint is invoked with delete operation'
            def deleteRequestBuilder = delete("$dataNodeBaseEndpointV1/anchors/$anchorName/list-nodes")
                .param('xpath', 'list element xpath')
            if (observedTimestamp != null)
                deleteRequestBuilder.param('observed-timestamp', observedTimestamp)
            def response = mvc.perform(deleteRequestBuilder).andReturn().response
        then: 'a success response is returned'
            response.status == expectedHttpStatus.value()
        and: 'the cps data service was called with the correct parameters'
            expectedApiCount * mockCpsDataService.deleteListOrListElement(dataspaceName, anchorName, 'list element xpath',
                { it == DateTimeUtility.toOffsetDateTime(observedTimestamp) })
        where:
            scenario                          | observedTimestamp              || expectedApiCount | expectedHttpStatus
            'with observed-timestamp'         | '2021-03-03T23:59:59.999-0400' || 1                | HttpStatus.NO_CONTENT
            'without observed-timestamp'      | null                           || 1                | HttpStatus.NO_CONTENT
            'with invalid observed-timestamp' | 'invalid'                      || 0                | HttpStatus.BAD_REQUEST
    }

    def 'Delete data node #scenario.'() {
        given: 'data node xpath'
            def dataNodeXpath = '/dataNodeXpath'
        when: 'delete data node endpoint is invoked'
            def deleteDataNodeRequest = delete( "$dataNodeBaseEndpointV1/anchors/$anchorName/nodes")
                .param('xpath', dataNodeXpath)
        and: 'observed timestamp is added to the parameters'
            if (observedTimestamp != null)
                deleteDataNodeRequest.param('observed-timestamp', observedTimestamp)
            def response = mvc.perform(deleteDataNodeRequest).andReturn().response
        then: 'a successful response is returned'
            response.status == expectedHttpStatus.value()
        and: 'the cps data service is called with the correct parameters'
            expectedApiCount * mockCpsDataService.deleteDataNode(dataspaceName, anchorName, dataNodeXpath,
                { it == DateTimeUtility.toOffsetDateTime(observedTimestamp) })
        where:
            scenario                            | observedTimestamp                 || expectedApiCount | expectedHttpStatus
            'with observed timestamp'           | '2021-03-03T23:59:59.999-0400'    || 1                | HttpStatus.NO_CONTENT
            'without observed timestamp'        | null                              || 1                | HttpStatus.NO_CONTENT
            'with invalid observed timestamp'   | 'invalid'                         || 0                | HttpStatus.BAD_REQUEST
    }

}
