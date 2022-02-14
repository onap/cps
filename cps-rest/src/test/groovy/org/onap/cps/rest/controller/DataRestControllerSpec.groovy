/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021-2022 Bell Canada.
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
import org.onap.cps.spi.model.DataNode
import org.onap.cps.spi.model.DataNodeBuilder
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

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put

@WebMvcTest(DataRestController)
class DataRestControllerSpec extends Specification {

    @SpringBean
    CpsDataService mockCpsDataService = Mock()

    @SpringBean
    JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

    @Autowired
    MockMvc mvc

    @Value('${rest.api.cps-base-path}')
    def basePath

    def dataNodeBaseEndpoint
    def dataspaceName = 'my_dataspace'
    def anchorName = 'my_anchor'
    def noTimestamp = null
    def requestBody = '{"some-key" : "some-value","categories":[{"books":[{"authors":["Iain M. Banks"]}]}]}'
    def expectedJsonData = '{"some-key":"some-value","categories":[{"books":[{"authors":["Iain M. Banks"]}]}]}'

    @Shared
    static DataNode dataNodeWithLeavesNoChildren = new DataNodeBuilder().withXpath('/xpath')
        .withLeaves([leaf: 'value', leafList: ['leaveListElement1', 'leaveListElement2']]).build()

    @Shared
    static DataNode dataNodeWithChild = new DataNodeBuilder().withXpath('/parent')
        .withChildDataNodes([new DataNodeBuilder().withXpath("/parent/child").build()]).build()

    def setup() {
        dataNodeBaseEndpoint = "$basePath/v1/dataspaces/$dataspaceName"
    }

    def 'Create a node: #scenario.'() {
        given: 'endpoint to create a node'
            def endpoint = "$dataNodeBaseEndpoint/anchors/$anchorName/nodes"
        when: 'post is invoked with datanode endpoint and json'
            def response =
                mvc.perform(
                    post(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param('xpath', parentNodeXpath)
                        .content(requestBody)
                ).andReturn().response
        then: 'a created response is returned'
            response.status == HttpStatus.CREATED.value()
        then: 'the java API was called with the correct parameters'
            1 * mockCpsDataService.saveData(dataspaceName, anchorName, expectedJsonData, noTimestamp)
        where: 'following xpath parameters are are used'
            scenario                     | parentNodeXpath
            'no xpath parameter'         | ''
            'xpath parameter point root' | '/'
    }

    def 'Create a node with observed-timestamp'() {
        given: 'endpoint to create a node'
            def endpoint = "$dataNodeBaseEndpoint/anchors/$anchorName/nodes"
        when: 'post is invoked with datanode endpoint and json'
            def response =
                mvc.perform(
                    post(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param('xpath', '')
                        .param('observed-timestamp', observedTimestamp)
                        .content(requestBody)
                ).andReturn().response
        then: 'a created response is returned'
            response.status == expectedHttpStatus.value()
        then: 'the java API was called with the correct parameters'
            expectedApiCount * mockCpsDataService.saveData(dataspaceName, anchorName, expectedJsonData,
                { it == DateTimeUtility.toOffsetDateTime(observedTimestamp) })
        where:
            scenario                          | observedTimestamp              || expectedApiCount | expectedHttpStatus
            'with observed-timestamp'         | '2021-03-03T23:59:59.999-0400' || 1                | HttpStatus.CREATED
            'with invalid observed-timestamp' | 'invalid'                      || 0                | HttpStatus.BAD_REQUEST
    }

    def 'Create a child node'() {
        given: 'endpoint to create a node'
            def endpoint = "$dataNodeBaseEndpoint/anchors/$anchorName/nodes"
        and: 'parent node xpath'
            def parentNodeXpath = 'some xpath'
        when: 'post is invoked with datanode endpoint and json'
            def postRequestBuilder = post(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .param('xpath', parentNodeXpath)
                .content(requestBody)
            if (observedTimestamp != null)
                postRequestBuilder.param('observed-timestamp', observedTimestamp)
            def response =
                mvc.perform(postRequestBuilder).andReturn().response
        then: 'a created response is returned'
            response.status == HttpStatus.CREATED.value()
        then: 'the java API was called with the correct parameters'
            1 * mockCpsDataService.saveData(dataspaceName, anchorName, parentNodeXpath, expectedJsonData,
                DateTimeUtility.toOffsetDateTime(observedTimestamp))
        where:
            scenario                     | observedTimestamp
            'with observed-timestamp'    | '2021-03-03T23:59:59.999-0400'
            'without observed-timestamp' | null
    }

    def 'Save list elements #scenario.'() {
        given: 'parent node xpath '
            def parentNodeXpath = 'parent node xpath'
        when: 'list-node endpoint is invoked with post (create) operation'
            def postRequestBuilder = post("$dataNodeBaseEndpoint/anchors/$anchorName/list-nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .param('xpath', parentNodeXpath)
                .content(requestBody)
            if (observedTimestamp != null)
                postRequestBuilder.param('observed-timestamp', observedTimestamp)
            def response = mvc.perform(postRequestBuilder).andReturn().response
        then: 'a created response is returned'
            response.status == expectedHttpStatus.value()
        then: 'the java API was called with the correct parameters'
            expectedApiCount * mockCpsDataService.saveListElements(dataspaceName, anchorName, parentNodeXpath, expectedJsonData,
                { it == DateTimeUtility.toOffsetDateTime(observedTimestamp) })
        where:
            scenario                          | observedTimestamp              || expectedApiCount | expectedHttpStatus
            'with observed-timestamp'         | '2021-03-03T23:59:59.999-0400' || 1                | HttpStatus.CREATED
            'without observed-timestamp'      | null                           || 1                | HttpStatus.CREATED
            'with invalid observed-timestamp' | 'invalid'                      || 0                | HttpStatus.BAD_REQUEST
    }

    def 'Get data node with leaves'() {
        given: 'the service returns data node leaves'
            def xpath = 'xpath'
            def endpoint = "$dataNodeBaseEndpoint/anchors/$anchorName/node"
            mockCpsDataService.getDataNode(dataspaceName, anchorName, xpath, OMIT_DESCENDANTS) >> dataNodeWithLeavesNoChildren
        when: 'get request is performed through REST API'
            def response =
                mvc.perform(get(endpoint).param('xpath', xpath))
                    .andReturn().response
        then: 'a success response is returned'
            response.status == HttpStatus.OK.value()
        then: 'the response contains the the datanode in json format'
            response.getContentAsString() == '{"xpath":{"leaf":"value","leafList":["leaveListElement1","leaveListElement2"]}}'
        and: 'response contains expected leaf and value'
            response.contentAsString.contains('"leaf":"value"')
        and: 'response contains expected leaf-list and values'
            response.contentAsString.contains('"leafList":["leaveListElement1","leaveListElement2"]')
    }

    def 'Get data node with #scenario.'() {
        given: 'the service returns data node with #scenario'
            def xpath = 'some xPath'
            def endpoint = "$dataNodeBaseEndpoint/anchors/$anchorName/node"
            mockCpsDataService.getDataNode(dataspaceName, anchorName, xpath, expectedCpsDataServiceOption) >> dataNode
        when: 'get request is performed through REST API'
            def response =
                mvc.perform(
                    get(endpoint)
                        .param('xpath', xpath)
                        .param('include-descendants', includeDescendantsOption))
                    .andReturn().response
        then: 'a success response is returned'
            response.status == HttpStatus.OK.value()
        and: 'the response contains the root node identifier: #expectedRootidentifier'
            response.contentAsString.contains(expectedRootidentifier)
        and: 'the response contains child is #expectChildInResponse'
            response.contentAsString.contains('"child"') == expectChildInResponse
        where:
            scenario                    | dataNode                     | includeDescendantsOption || expectedCpsDataServiceOption | expectChildInResponse | expectedRootidentifier
            'no descendants by default' | dataNodeWithLeavesNoChildren | ''                       || OMIT_DESCENDANTS             | false                 | 'xpath'
            'no descendant explicitly'  | dataNodeWithLeavesNoChildren | 'false'                  || OMIT_DESCENDANTS             | false                 | 'xpath'
            'with descendants'          | dataNodeWithChild            | 'true'                   || INCLUDE_ALL_DESCENDANTS      | true                  | 'parent'
    }

    def 'Update data node leaves: #scenario.'() {
        given: 'endpoint to update a node '
            def endpoint = "$dataNodeBaseEndpoint/anchors/$anchorName/nodes"
        when: 'patch request is performed'
            def response =
                mvc.perform(
                    patch(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .param('xpath', inputXpath)
                ).andReturn().response
        then: 'the service method is invoked with expected parameters'
            1 * mockCpsDataService.updateNodeLeaves(dataspaceName, anchorName, xpathServiceParameter, expectedJsonData, null)
        and: 'response status indicates success'
            response.status == HttpStatus.OK.value()
        where:
            scenario               | inputXpath    || xpathServiceParameter
            'root node by default' | ''            || '/'
            'root node by choice'  | '/'           || '/'
            'some xpath by parent' | '/some/xpath' || '/some/xpath'
    }

    def 'Update data node leaves with observedTimestamp'() {
        given: 'endpoint to update a node leaves '
            def endpoint = "$dataNodeBaseEndpoint/anchors/$anchorName/nodes"
        when: 'patch request is performed'
            def response =
                mvc.perform(
                    patch(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .param('xpath', '/')
                        .param('observed-timestamp', observedTimestamp)
                ).andReturn().response
        then: 'the service method is invoked with expected parameters'
            expectedApiCount * mockCpsDataService.updateNodeLeaves(dataspaceName, anchorName, '/', expectedJsonData,
                { it == DateTimeUtility.toOffsetDateTime(observedTimestamp) })
        and: 'response status indicates success'
            response.status == expectedHttpStatus.value()
        where:
            scenario                          | observedTimestamp              || expectedApiCount | expectedHttpStatus
            'with observed-timestamp'         | '2021-03-03T23:59:59.999-0400' || 1                | HttpStatus.OK
            'with invalid observed-timestamp' | 'invalid'                      || 0                | HttpStatus.BAD_REQUEST
    }

    def 'Replace data node tree: #scenario.'() {
        given: 'endpoint to replace node'
            def endpoint = "$dataNodeBaseEndpoint/anchors/$anchorName/nodes"
        when: 'put request is performed'
            def response =
                mvc.perform(
                    put(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .param('xpath', inputXpath))
                    .andReturn().response
        then: 'the service method is invoked with expected parameters'
            1 * mockCpsDataService.replaceNodeTree(dataspaceName, anchorName, xpathServiceParameter, expectedJsonData, noTimestamp)
        and: 'response status indicates success'
            response.status == HttpStatus.OK.value()
        where:
            scenario               | inputXpath    || xpathServiceParameter
            'root node by default' | ''            || '/'
            'root node by choice'  | '/'           || '/'
            'some xpath by parent' | '/some/xpath' || '/some/xpath'
    }

    def 'Replace data node tree with observedTimestamp.'() {
        given: 'endpoint to replace node'
            def endpoint = "$dataNodeBaseEndpoint/anchors/$anchorName/nodes"
        when: 'put request is performed'
            def response =
                mvc.perform(
                    put(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .param('xpath', '')
                        .param('observed-timestamp', observedTimestamp))
                    .andReturn().response
        then: 'the service method is invoked with expected parameters'
            expectedApiCount * mockCpsDataService.replaceNodeTree(dataspaceName, anchorName, '/', expectedJsonData,
                { it == DateTimeUtility.toOffsetDateTime(observedTimestamp) })
        and: 'response status indicates success'
            response.status == expectedHttpStatus.value()
        where:
            scenario                          | observedTimestamp              || expectedApiCount | expectedHttpStatus
            'with observed-timestamp'         | '2021-03-03T23:59:59.999-0400' || 1                | HttpStatus.OK
            'with invalid observed-timestamp' | 'invalid'                      || 0                | HttpStatus.BAD_REQUEST
    }

    def 'Replace list content #scenario.'() {
        when: 'list-nodes endpoint is invoked with put (update) operation'
            def putRequestBuilder = put("$dataNodeBaseEndpoint/anchors/$anchorName/list-nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .param('xpath', 'parent xpath')
                .content(requestBody)
            if (observedTimestamp != null)
                putRequestBuilder.param('observed-timestamp', observedTimestamp)
            def response = mvc.perform(putRequestBuilder).andReturn().response
        then: 'a success response is returned'
            response.status == expectedHttpStatus.value()
        and: 'the java API was called with the correct parameters'
            expectedApiCount * mockCpsDataService.replaceListContent(dataspaceName, anchorName, 'parent xpath', expectedJsonData,
                { it == DateTimeUtility.toOffsetDateTime(observedTimestamp) })
        where:
            scenario                          | observedTimestamp              || expectedApiCount | expectedHttpStatus
            'with observed-timestamp'         | '2021-03-03T23:59:59.999-0400' || 1                | HttpStatus.OK
            'without observed-timestamp'      | null                           || 1                | HttpStatus.OK
            'with invalid observed-timestamp' | 'invalid'                      || 0                | HttpStatus.BAD_REQUEST
    }

    def 'Delete list element #scenario.'() {
        when: 'list-nodes endpoint is invoked with delete operation'
            def deleteRequestBuilder = delete("$dataNodeBaseEndpoint/anchors/$anchorName/list-nodes")
                .param('xpath', 'list element xpath')
            if (observedTimestamp != null)
                deleteRequestBuilder.param('observed-timestamp', observedTimestamp)
            def response = mvc.perform(deleteRequestBuilder).andReturn().response
        then: 'a success response is returned'
            response.status == expectedHttpStatus.value()
        and: 'the java API was called with the correct parameters'
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
            def deleteDataNodeRequest = delete( "$dataNodeBaseEndpoint/anchors/$anchorName/nodes")
                .param('xpath', dataNodeXpath)
        and: 'observed timestamp is added to the parameters'
            if (observedTimestamp != null)
                deleteDataNodeRequest.param('observed-timestamp', observedTimestamp)
            def response = mvc.perform(deleteDataNodeRequest).andReturn().response
        then: 'a successful response is returned'
            response.status == expectedHttpStatus.value()
        and: 'the api is called with the correct parameters'
            expectedApiCount * mockCpsDataService.deleteDataNode(dataspaceName, anchorName, dataNodeXpath,
                { it == DateTimeUtility.toOffsetDateTime(observedTimestamp) })
        where:
            scenario                            | observedTimestamp                 || expectedApiCount | expectedHttpStatus
            'with observed timestamp'           | '2021-03-03T23:59:59.999-0400'    || 1                | HttpStatus.NO_CONTENT
            'without observed timestamp'        | null                              || 1                | HttpStatus.NO_CONTENT
            'with invalid observed timestamp'   | 'invalid'                         || 0                | HttpStatus.BAD_REQUEST
    }
}
