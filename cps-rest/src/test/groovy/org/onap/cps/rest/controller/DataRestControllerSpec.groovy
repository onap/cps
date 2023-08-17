/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021-2022 Bell Canada.
 *  Modifications Copyright (C) 2022 Deutsche Telekom AG
 *  Modifications Copyright (C) 2022-2023 TechMahindra Ltd.
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
import groovy.json.JsonSlurper
import org.onap.cps.api.CpsDataService
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.model.DataNode
import org.onap.cps.spi.model.DataNodeBuilder
import org.onap.cps.spi.model.DeltaReportBuilder
import org.onap.cps.utils.ContentType
import org.onap.cps.utils.DateTimeUtility
import org.onap.cps.utils.JsonObjectMapper
import org.onap.cps.utils.PrefixResolver
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

    @SpringBean
    PrefixResolver prefixResolver = Mock()

    @Autowired
    MockMvc mvc

    @Value('${rest.api.cps-base-path}')
    def basePath

    def dataNodeBaseEndpointV1
    def dataNodeBaseEndpointV2
    def dataspaceName = 'my_dataspace'
    def anchorName = 'my_anchor'
    def noTimestamp = null

    @Shared
    def requestBodyJson = '{"categories":[{"books":[{"authors":["Iain M. Banks"]}]}],"some-key":"some-value"}'

    @Shared
    def expectedJsonData = '{"categories":[{"books":[{"authors":["Iain M. Banks"]}]}],"some-key":"some-value"}'

    @Shared
    def requestBodyXml = '<?xml version=\'1.0\' encoding=\'UTF-8\'?>\n<bookstore xmlns="org:onap:ccsdk:sample">\n</bookstore>'

    @Shared
    def expectedXmlData = '<?xml version=\'1.0\' encoding=\'UTF-8\'?>\n<bookstore xmlns="org:onap:ccsdk:sample">\n</bookstore>'

    @Shared
    static DataNode dataNodeWithLeavesNoChildren = new DataNodeBuilder().withXpath('/parent-1')
        .withLeaves([leaf: 'value', leafList: ['leaveListElement1', 'leaveListElement2']]).build()

    @Shared
    static DataNode dataNodeWithLeavesNoChildren2 = new DataNodeBuilder().withXpath('/parent-2')
        .withLeaves([leaf: 'value']).build()

    @Shared
    static DataNode dataNodeWithChild = new DataNodeBuilder().withXpath('/parent')
        .withChildDataNodes([new DataNodeBuilder().withXpath("/parent/child").build()]).build()

    def setup() {
        dataNodeBaseEndpointV1 = "$basePath/v1/dataspaces/$dataspaceName"
        dataNodeBaseEndpointV2 = "$basePath/v2/dataspaces/$dataspaceName"
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
        then: 'the java API was called with the correct parameters'
            1 * mockCpsDataService.saveData(dataspaceName, anchorName, expectedData, noTimestamp, expectedContentType)
        where: 'following xpath parameters are are used'
            scenario                                   | parentNodeXpath | contentType                | expectedContentType | requestBody     | expectedData
            'JSON content: no xpath parameter'         | ''              | MediaType.APPLICATION_JSON | ContentType.JSON    | requestBodyJson | expectedJsonData
            'JSON content: xpath parameter point root' | '/'             | MediaType.APPLICATION_JSON | ContentType.JSON    | requestBodyJson | expectedJsonData
            'XML content: no xpath parameter'          | ''              | MediaType.APPLICATION_XML  | ContentType.XML     | requestBodyXml  | expectedXmlData
            'XML content: xpath parameter point root'  | '/'             | MediaType.APPLICATION_XML  | ContentType.XML     | requestBodyXml  | expectedXmlData
    }

    def 'Create a node with observed-timestamp'() {
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
        then: 'the java API was called with the correct parameters'
            expectedApiCount * mockCpsDataService.saveData(dataspaceName, anchorName, expectedData,
                { it == DateTimeUtility.toOffsetDateTime(observedTimestamp) }, expectedContentType)
        where:
            scenario                          | observedTimestamp              | contentType                | content         || expectedApiCount | expectedHttpStatus     | expectedData     | expectedContentType
            'with observed-timestamp JSON'    | '2021-03-03T23:59:59.999-0400' | MediaType.APPLICATION_JSON | requestBodyJson || 1                | HttpStatus.CREATED     | expectedJsonData | ContentType.JSON
            'with observed-timestamp XML'     | '2021-03-03T23:59:59.999-0400' | MediaType.APPLICATION_XML  | requestBodyXml  || 1                | HttpStatus.CREATED     | expectedXmlData  | ContentType.XML
            'with invalid observed-timestamp' | 'invalid'                      | MediaType.APPLICATION_JSON | requestBodyJson || 0                | HttpStatus.BAD_REQUEST | expectedJsonData | ContentType.JSON
    }

    def 'Create a child node #scenario'() {
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
        then: 'the java API was called with the correct parameters'
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
                .contentType(MediaType.APPLICATION_JSON)
                .param('xpath', rootNodeXpath )
                .content(requestBodyJson)
            if (observedTimestamp != null)
                postRequestBuilder.param('observed-timestamp', observedTimestamp)
            def response = mvc.perform(postRequestBuilder).andReturn().response
        then: 'a created response is returned'
            response.status == expectedHttpStatus.value()
        then: 'the java API was called with the correct parameters'
            expectedApiCount * mockCpsDataService.saveListElements(dataspaceName, anchorName, rootNodeXpath, expectedJsonData,
                { it == DateTimeUtility.toOffsetDateTime(observedTimestamp) })
        where:
            scenario                          | observedTimestamp              || expectedApiCount | expectedHttpStatus
            'with observed-timestamp'         | '2021-03-03T23:59:59.999-0400' || 1                | HttpStatus.CREATED
            'without observed-timestamp'      | null                           || 1                | HttpStatus.CREATED
            'with invalid observed-timestamp' | 'invalid'                      || 0                | HttpStatus.BAD_REQUEST
    }

    def 'Save list elements #scenario.'() {
        given: 'parent node xpath '
            def parentNodeXpath = 'parent node xpath'
        when: 'list-node endpoint is invoked with post (create) operation'
            def postRequestBuilder = post("$dataNodeBaseEndpointV1/anchors/$anchorName/list-nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .param('xpath', parentNodeXpath)
                .content(requestBodyJson)
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
            def xpath = 'parent-1'
            def endpoint = "$dataNodeBaseEndpointV1/anchors/$anchorName/node"
            mockCpsDataService.getDataNodes(dataspaceName, anchorName, xpath, OMIT_DESCENDANTS) >> [dataNodeWithLeavesNoChildren]
        when: 'get request is performed through REST API'
            def response =
                mvc.perform(get(endpoint).param('xpath', xpath))
                    .andReturn().response
        then: 'a success response is returned'
            response.status == HttpStatus.OK.value()
        then: 'the response contains the the datanode in json format'
            response.getContentAsString() == '{"parent-1":{"leaf":"value","leafList":["leaveListElement1","leaveListElement2"]}}'
        and: 'response contains expected leaf and value'
            response.contentAsString.contains('"leaf":"value"')
        and: 'response contains expected leaf-list and values'
            response.contentAsString.contains('"leafList":["leaveListElement1","leaveListElement2"]')
    }

    def 'Get data node with #scenario.'() {
        given: 'the service returns data node with #scenario'
            def xpath = 'some xPath'
            def endpoint = "$dataNodeBaseEndpointV1/anchors/$anchorName/node"
            mockCpsDataService.getDataNodes(dataspaceName, anchorName, xpath, expectedCpsDataServiceOption) >> [dataNode]
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
            'no descendants by default' | dataNodeWithLeavesNoChildren | ''                       || OMIT_DESCENDANTS             | false                 | 'parent-1'
            'no descendant explicitly'  | dataNodeWithLeavesNoChildren | 'false'                  || OMIT_DESCENDANTS             | false                 | 'parent-1'
            'with descendants'          | dataNodeWithChild            | 'true'                   || INCLUDE_ALL_DESCENDANTS      | true                  | 'parent'
    }

    def 'Get all the data trees as json array with root node xPath using V2'() {
        given: 'the service returns all data node leaves'
            def xpath = '/'
            def endpoint = "$dataNodeBaseEndpointV2/anchors/$anchorName/node"
            mockCpsDataService.getDataNodes(dataspaceName, anchorName, xpath, OMIT_DESCENDANTS) >> [dataNodeWithLeavesNoChildren, dataNodeWithLeavesNoChildren2]
        when: 'V2 of get request is performed through REST API'
            def response =
                mvc.perform(get(endpoint).param('xpath', xpath))
                    .andReturn().response
        then: 'a success response is returned'
            response.status == HttpStatus.OK.value()
        and: 'the response contains the datanode in json array format'
            response.getContentAsString() == '[{"parent-1":{"leaf":"value","leafList":["leaveListElement1","leaveListElement2"]}},' +
                '{"parent-2":{"leaf":"value"}}]'
        and: 'the json array contains expected number of data trees'
            def numberOfDataTrees = new JsonSlurper().parseText(response.getContentAsString()).iterator().size()
            assert numberOfDataTrees == 2
    }

    def 'Get data node with #scenario using V2.'() {
        given: 'the service returns data nodes with #scenario'
            def xpath = 'some xPath'
            def endpoint = "$dataNodeBaseEndpointV2/anchors/$anchorName/node"
            mockCpsDataService.getDataNodes(dataspaceName, anchorName, xpath, expectedCpsDataServiceOption) >> [dataNode]
        when: 'V2 of get request is performed through REST API'
            def response =
                mvc.perform(
                    get(endpoint)
                        .param('xpath', xpath)
                        .param('descendants', includeDescendantsOption))
                    .andReturn().response
        then: 'a success response is returned'
            response.status == HttpStatus.OK.value()
        and: 'the response contains the root node identifier: #expectedRootidentifier'
            response.contentAsString.contains(expectedRootidentifier)
        and: 'the response contains child is #expectChildInResponse'
            response.contentAsString.contains('"child"') == expectChildInResponse
        where:
            scenario                    | dataNode                     | includeDescendantsOption || expectedCpsDataServiceOption | expectChildInResponse | expectedRootidentifier
            'no descendants by default' | dataNodeWithLeavesNoChildren | ''                       || OMIT_DESCENDANTS             | false                 | 'parent-1'
            'no descendant explicitly'  | dataNodeWithLeavesNoChildren | '0'                      || OMIT_DESCENDANTS             | false                 | 'parent-1'
            'with descendants'          | dataNodeWithChild            | '-1'                     || INCLUDE_ALL_DESCENDANTS      | true                  | 'parent'
    }

    def 'Get data node using v2 api'() {
        given: 'the service returns data node'
            def xpath = 'some xPath'
            def endpoint = "$dataNodeBaseEndpointV2/anchors/$anchorName/node"
            mockCpsDataService.getDataNodes(dataspaceName, anchorName, xpath, { descendantsOption -> {
                assert descendantsOption.depth == 2}} as FetchDescendantsOption) >> [dataNodeWithChild]
        when: 'get request is performed through REST API'
            def response =
                mvc.perform(
                    get(endpoint)
                        .param('xpath', xpath)
                        .param('descendants', '2'))
                    .andReturn().response
        then: 'a success response is returned'
            assert response.status == HttpStatus.OK.value()
        and: 'the response contains the root node identifier'
            assert response.contentAsString.contains('parent')
        and: 'the response contains child is true'
            assert response.contentAsString.contains('"child"')
    }

    def 'Get delta between two anchors'() {
        given: 'the service returns a list containing delta reports'
            def deltaReports = new DeltaReportBuilder().actionAdd().withXpath('/bookstore').withSourceData('bookstore-name': 'Easons').withTargetData('bookstore-name': 'Easons').build()
            def xpath = 'some xpath'
            def endpoint = "$dataNodeBaseEndpointV2/anchors/sourceAnchor/delta"
            mockCpsDataService.getDeltaByDataspaceAndAnchors(dataspaceName, 'sourceAnchor', 'targetAnchor', xpath, OMIT_DESCENDANTS) >> [deltaReports]
        when: 'get delta request is performed using REST API'
            def response =
                mvc.perform(get(endpoint)
                    .param('target-anchor-name', 'targetAnchor')
                    .param('xpath', xpath))
                    .andReturn().response
        then: 'expected response code is returned'
            assert response.status == HttpStatus.OK.value()
        and: 'the response contains expected value'
            assert response.contentAsString.contains("[{\"action\":\"add\",\"xpath\":\"/bookstore\",\"sourceData\":{\"bookstore-name\":\"Easons\"},\"targetData\":{\"bookstore-name\":\"Easons\"}}]")
    }

    def 'Update data node leaves: #scenario.'() {
        given: 'endpoint to update a node '
            def endpoint = "$dataNodeBaseEndpointV1/anchors/$anchorName/nodes"
        when: 'patch request is performed'
            def response =
                mvc.perform(
                    patch(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyJson)
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
            def endpoint = "$dataNodeBaseEndpointV1/anchors/$anchorName/nodes"
        when: 'put request is performed'
            def response =
                mvc.perform(
                    put(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyJson)
                        .param('xpath', inputXpath))
                    .andReturn().response
        then: 'the service method is invoked with expected parameters'
            1 * mockCpsDataService.updateDataNodeAndDescendants(dataspaceName, anchorName, xpathServiceParameter, expectedJsonData, noTimestamp)
        and: 'response status indicates success'
            response.status == HttpStatus.OK.value()
        where:
            scenario               | inputXpath    || xpathServiceParameter
            'root node by default' | ''            || '/'
            'root node by choice'  | '/'           || '/'
            'some xpath by parent' | '/some/xpath' || '/some/xpath'
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
        then: 'the service method is invoked with expected parameters'
            expectedApiCount * mockCpsDataService.updateDataNodeAndDescendants(dataspaceName, anchorName, '/', expectedJsonData,
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
            def putRequestBuilder = put("$dataNodeBaseEndpointV1/anchors/$anchorName/list-nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .param('xpath', 'parent xpath')
                .content(requestBodyJson)
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
            def deleteRequestBuilder = delete("$dataNodeBaseEndpointV1/anchors/$anchorName/list-nodes")
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
            def deleteDataNodeRequest = delete( "$dataNodeBaseEndpointV1/anchors/$anchorName/nodes")
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
