/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021 Bell Canada.
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

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put

import org.onap.cps.api.CpsDataService
import org.onap.cps.spi.model.DataNode
import org.onap.cps.spi.model.DataNodeBuilder
import org.onap.cps.utils.DateTimeUtility
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Shared
import spock.lang.Specification

@WebMvcTest(DataRestController)
class DataRestControllerSpec extends Specification {

    @SpringBean
    CpsDataService mockCpsDataService = Mock()

    @Autowired
    MockMvc mvc

    @Value('${rest.api.cps-base-path}')
    def basePath

    def dataNodeBaseEndpoint
    def dataspaceName = 'my_dataspace'
    def anchorName = 'my_anchor'
    def noTimestamp = null

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
        given: 'some json to create a data node'
            def endpoint = "$dataNodeBaseEndpoint/anchors/$anchorName/nodes"
            def json = 'some json (this is not validated)'
        when: 'post is invoked with datanode endpoint and json'
            def response =
                mvc.perform(
                    post(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param('xpath', parentNodeXpath)
                        .content(json)
                ).andReturn().response
        then: 'a created response is returned'
            response.status == HttpStatus.CREATED.value()
        then: 'the java API was called with the correct parameters'
            1 * mockCpsDataService.saveData(dataspaceName, anchorName, json, noTimestamp)
        where: 'following xpath parameters are are used'
            scenario                     | parentNodeXpath
            'no xpath parameter'         | ''
            'xpath parameter point root' | '/'
    }

    def 'Create a node with observed-timestamp'() {
        given: 'some json to create a data node'
            def endpoint = "$dataNodeBaseEndpoint/anchors/$anchorName/nodes"
            def json = 'some json (this is not validated)'
        when: 'post is invoked with datanode endpoint and json'
            def response =
                mvc.perform(
                    post(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param('xpath', '')
                        .param('observed-timestamp', observedTimestamp)
                        .content(json)
                ).andReturn().response
        then: 'a created response is returned'
            response.status == expectedHttpStatus.value()
        then: 'the java API was called with the correct parameters'
            expectedApiCount * mockCpsDataService.saveData(dataspaceName, anchorName, json,
                { it == DateTimeUtility.toOffsetDateTime(observedTimestamp) })
        where:
            scenario                          | observedTimestamp              || expectedApiCount | expectedHttpStatus
            'with observed-timestamp'         | '2021-03-03T23:59:59.999-0400' || 1                | HttpStatus.CREATED
            'with invalid observed-timestamp' | 'invalid'                      || 0                | HttpStatus.BAD_REQUEST
    }

    def 'Create a child node'() {
        given: 'some json to create a data node'
            def endpoint = "$dataNodeBaseEndpoint/anchors/$anchorName/nodes"
            def json = 'some json (this is not validated)'
        and: 'parent node xpath'
            def parentNodeXpath = 'some xpath'
        when: 'post is invoked with datanode endpoint and json'
            def postRequestBuilder = post(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .param('xpath', parentNodeXpath)
                .content(json)
            if (observedTimestamp != null)
                postRequestBuilder.param('observed-timestamp', observedTimestamp)
            def response =
                mvc.perform(postRequestBuilder).andReturn().response
        then: 'a created response is returned'
            response.status == HttpStatus.CREATED.value()
        then: 'the java API was called with the correct parameters'
            1 * mockCpsDataService.saveData(dataspaceName, anchorName, parentNodeXpath, json,
                DateTimeUtility.toOffsetDateTime(observedTimestamp))
        where:
            scenario                     | observedTimestamp
            'with observed-timestamp'    | '2021-03-03T23:59:59.999-0400'
            'without observed-timestamp' | null
    }

    def 'Create list node child elements #scenario.'() {
        given: 'parent node xpath and json data inputs'
            def parentNodeXpath = 'parent node xpath'
            def jsonData = 'json data'
        when: 'post is invoked list-node endpoint'
            def postRequestBuilder = post("$dataNodeBaseEndpoint/anchors/$anchorName/list-nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .param('xpath', parentNodeXpath)
                .content(jsonData)
            if (observedTimestamp != null)
                postRequestBuilder.param('observed-timestamp', observedTimestamp)
            def response = mvc.perform(postRequestBuilder).andReturn().response
        then: 'a created response is returned'
            response.status == expectedHttpStatus.value()
        then: 'the java API was called with the correct parameters'
            expectedApiCount * mockCpsDataService.saveListNodeData(dataspaceName, anchorName, parentNodeXpath, jsonData,
                { it == DateTimeUtility.toOffsetDateTime(observedTimestamp) })
        where:
            scenario                          | observedTimestamp              || expectedApiCount | expectedHttpStatus
            'with observed-timestamp'         | '2021-03-03T23:59:59.999-0400' || 1                | HttpStatus.CREATED
            'without observed-timestamp'      | null                           || 1                | HttpStatus.CREATED
            'with invalid observed-timestamp' | 'invalid'                      || 0                | HttpStatus.BAD_REQUEST
    }

    def 'Get data node with leaves'() {
        given: 'the service returns data node leaves'
            def xpath = 'some xPath'
            def endpoint = "$dataNodeBaseEndpoint/anchors/$anchorName/node"
            mockCpsDataService.getDataNode(dataspaceName, anchorName, xpath, OMIT_DESCENDANTS) >> dataNodeWithLeavesNoChildren
        when: 'get request is performed through REST API'
            def response =
                mvc.perform(get(endpoint).param('xpath', xpath))
                    .andReturn().response
        then: 'a success response is returned'
            response.status == HttpStatus.OK.value()
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
        and: 'the response contains child is #expectChildInResponse'
            response.contentAsString.contains('"child"') == expectChildInResponse
        where:
            scenario                    | dataNode                     | includeDescendantsOption || expectedCpsDataServiceOption | expectChildInResponse
            'no descendants by default' | dataNodeWithLeavesNoChildren | ''                       || OMIT_DESCENDANTS             | false
            'no descendant explicitly'  | dataNodeWithLeavesNoChildren | 'false'                  || OMIT_DESCENDANTS             | false
            'with descendants'          | dataNodeWithChild            | 'true'                   || INCLUDE_ALL_DESCENDANTS      | true
    }

    def 'Update data node leaves: #scenario.'() {
        given: 'json data'
            def jsonData = 'json data'
            def endpoint = "$dataNodeBaseEndpoint/anchors/$anchorName/nodes"
        when: 'patch request is performed'
            def response =
                mvc.perform(
                    patch(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonData)
                        .param('xpath', inputXpath)
                ).andReturn().response
        then: 'the service method is invoked with expected parameters'
            1 * mockCpsDataService.updateNodeLeaves(dataspaceName, anchorName, xpathServiceParameter, jsonData, null)
        and: 'response status indicates success'
            response.status == HttpStatus.OK.value()
        where:
            scenario               | inputXpath    || xpathServiceParameter
            'root node by default' | ''            || '/'
            'root node by choice'  | '/'           || '/'
            'some xpath by parent' | '/some/xpath' || '/some/xpath'
    }

    def 'Update data node leaves with observedTimestamp'() {
        given: 'json data'
            def jsonData = 'json data'
            def endpoint = "$dataNodeBaseEndpoint/anchors/$anchorName/nodes"
        when: 'patch request is performed'
            def response =
                mvc.perform(
                    patch(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonData)
                        .param('xpath', '/')
                        .param('observed-timestamp', observedTimestamp)
                ).andReturn().response
        then: 'the service method is invoked with expected parameters'
            expectedApiCount * mockCpsDataService.updateNodeLeaves(dataspaceName, anchorName, '/', jsonData,
                { it == DateTimeUtility.toOffsetDateTime(observedTimestamp) })
        and: 'response status indicates success'
            response.status == expectedHttpStatus.value()
        where:
            scenario                          | observedTimestamp              || expectedApiCount | expectedHttpStatus
            'with observed-timestamp'         | '2021-03-03T23:59:59.999-0400' || 1                | HttpStatus.OK
            'with invalid observed-timestamp' | 'invalid'                      || 0                | HttpStatus.BAD_REQUEST
    }

    def 'Replace data node tree: #scenario.'() {
        given: 'json data'
            def jsonData = 'json data'
            def endpoint = "$dataNodeBaseEndpoint/anchors/$anchorName/nodes"
        when: 'put request is performed'
            def response =
                mvc.perform(
                    put(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonData)
                        .param('xpath', inputXpath))
                    .andReturn().response
        then: 'the service method is invoked with expected parameters'
            1 * mockCpsDataService.replaceNodeTree(dataspaceName, anchorName, xpathServiceParameter, jsonData, noTimestamp)
        and: 'response status indicates success'
            response.status == HttpStatus.OK.value()
        where:
            scenario               | inputXpath    || xpathServiceParameter
            'root node by default' | ''            || '/'
            'root node by choice'  | '/'           || '/'
            'some xpath by parent' | '/some/xpath' || '/some/xpath'
    }

    def 'Replace data node tree with observedTimestamp.'() {
        given: 'json data'
            def jsonData = 'json data'
            def endpoint = "$dataNodeBaseEndpoint/anchors/$anchorName/nodes"
        when: 'put request is performed'
            def response =
                mvc.perform(
                    put(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonData)
                        .param('xpath', '')
                        .param('observed-timestamp', observedTimestamp))
                    .andReturn().response
        then: 'the service method is invoked with expected parameters'
            expectedApiCount * mockCpsDataService.replaceNodeTree(dataspaceName, anchorName, '/', jsonData,
                { it == DateTimeUtility.toOffsetDateTime(observedTimestamp) })
        and: 'response status indicates success'
            response.status == expectedHttpStatus.value()
        where:
            scenario                          | observedTimestamp              || expectedApiCount | expectedHttpStatus
            'with observed-timestamp'         | '2021-03-03T23:59:59.999-0400' || 1                | HttpStatus.OK
            'with invalid observed-timestamp' | 'invalid'                      || 0                | HttpStatus.BAD_REQUEST
    }

    def 'Replace list node child elements.'() {
        given: 'parent node xpath and json data inputs'
            def parentNodeXpath = 'parent node xpath'
            def jsonData = 'json data'
        when: 'put is invoked list-node endpoint'
            def putRequestBuilder = put("$dataNodeBaseEndpoint/anchors/$anchorName/list-nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .param('xpath', parentNodeXpath)
                .content(jsonData)
            if (observedTimestamp != null)
                putRequestBuilder.param('observed-timestamp', observedTimestamp)
            def response = mvc.perform(putRequestBuilder).andReturn().response
        then: 'a success response is returned'
            response.status == expectedHttpStatus.value()
        and: 'the java API was called with the correct parameters'
            expectedApiCount * mockCpsDataService.replaceListNodeData(dataspaceName, anchorName, parentNodeXpath, jsonData,
                { it == DateTimeUtility.toOffsetDateTime(observedTimestamp) })
        where:
            scenario                          | observedTimestamp              || expectedApiCount | expectedHttpStatus
            'with observed-timestamp'         | '2021-03-03T23:59:59.999-0400' || 1                | HttpStatus.OK
            'without observed-timestamp'      | null                           || 1                | HttpStatus.OK
            'with invalid observed-timestamp' | 'invalid'                      || 0                | HttpStatus.BAD_REQUEST
    }

    def 'Delete list node child elements. #scenario'() {
        given: 'list node xpath'
            def listNodeXpath = 'list node xpath'
        when: 'delete is invoked list-node endpoint'
            def deleteRequestBuilder = delete("$dataNodeBaseEndpoint/anchors/$anchorName/list-nodes")
                .param('xpath', listNodeXpath)
            if (observedTimestamp != null)
                deleteRequestBuilder.param('observed-timestamp', observedTimestamp)
            def response = mvc.perform(deleteRequestBuilder).andReturn().response
        then: 'a success response is returned'
            response.status == expectedHttpStatus.value()
        and: 'the java API was called with the correct parameters'
            expectedApiCount * mockCpsDataService.deleteListNodeData(dataspaceName, anchorName, listNodeXpath,
                { it == DateTimeUtility.toOffsetDateTime(observedTimestamp) })
        where:
            scenario                          | observedTimestamp              || expectedApiCount | expectedHttpStatus
            'with observed-timestamp'         | '2021-03-03T23:59:59.999-0400' || 1                | HttpStatus.NO_CONTENT
            'without observed-timestamp'      | null                           || 1                | HttpStatus.NO_CONTENT
            'with invalid observed-timestamp' | 'invalid'                      || 0                | HttpStatus.BAD_REQUEST
    }
}
