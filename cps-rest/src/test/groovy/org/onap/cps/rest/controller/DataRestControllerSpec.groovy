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
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.api.CpsQueryService
import org.onap.cps.spi.exceptions.AlreadyDefinedException
import org.onap.cps.spi.exceptions.AnchorNotFoundException
import org.onap.cps.spi.exceptions.DataNodeNotFoundException
import org.onap.cps.spi.exceptions.DataspaceNotFoundException
import org.onap.cps.spi.model.DataNode
import org.onap.cps.spi.model.DataNodeBuilder
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*

@WebMvcTest
class DataRestControllerSpec extends Specification {

    @SpringBean
    CpsDataService mockCpsDataService = Mock()

    @SpringBean
    CpsModuleService mockCpsModuleService = Mock()

    @SpringBean
    CpsAdminService mockCpsAdminService = Mock()

    @SpringBean
    CpsQueryService mockCpsQueryService = Mock()

    @SpringBean
    ModelMapper modelMapper = Mock()

    @Autowired
    MockMvc mvc

    @Value('${rest.api.cps-base-path}')
    def basePath

    def dataNodeBaseEndpoint
    def dataspaceName = 'my_dataspace'
    def anchorName = 'my_anchor'

    @Shared
    static DataNode dataNodeWithLeavesNoChildren = new DataNodeBuilder().withXpath('/xpath')
            .withLeaves([leaf: 'value', leafList: ['leaveListElement1', 'leaveListElement2']]).build()

    @Shared
    static DataNode dataNodeWithChild = new DataNodeBuilder().withXpath('/parent')
            .withChildDataNodes([new DataNodeBuilder().withXpath("/parent/child").build()]).build()

    def setup() {
        dataNodeBaseEndpoint = "$basePath/v1/dataspaces/$dataspaceName"
    }

    def 'Create a node.'() {
        given: 'some json to create a data node'
            def endpoint = "$dataNodeBaseEndpoint/anchors/$anchorName/nodes"
            def json = 'some json (this is not validated)'
        when: 'post is invoked with datanode endpoint and json'
            def response =
                    mvc.perform(
                            post(endpoint)
                                    .contentType(MediaType.APPLICATION_JSON).content(json))
                            .andReturn().response
        then: 'a created response is returned'
            response.status == HttpStatus.CREATED.value()
        then: 'the java API was called with the correct parameters'
            1 * mockCpsDataService.saveData(dataspaceName, anchorName, json)
    }

    @Unroll
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

    @Unroll
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

    @Unroll
    def 'Get data node error scenario: #scenario.'() {
        given: 'the service throws an exception'
            def endpoint = "$dataNodeBaseEndpoint/anchors/$anchorName/node"
            mockCpsDataService.getDataNode(dataspaceName, anchorName, xpath, _) >> { throw exception }
        when: 'get request is performed through REST API'
            def response =
                    mvc.perform(get(endpoint).param("xpath", xpath))
                            .andReturn().response
        then: 'a success response is returned'
            response.status == httpStatus.value()
        where:
            scenario                   | xpath     | exception                                 || httpStatus
            'no dataspace'             | '/x-path' | new DataspaceNotFoundException('')        || HttpStatus.BAD_REQUEST
            'no anchor'                | '/x-path' | new AnchorNotFoundException('', '')       || HttpStatus.BAD_REQUEST
            'no data'                  | '/x-path' | new DataNodeNotFoundException('', '', '') || HttpStatus.NOT_FOUND
            'empty path'               | ''        | new IllegalStateException()               || HttpStatus.NOT_IMPLEMENTED
            'data node already exists' | '/x-path' | new AlreadyDefinedException('', '')       || HttpStatus.CONFLICT
    }

    @Unroll
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
                                    .param('xpath', xpath)
                    ).andReturn().response
        then: 'the service method is invoked with expected parameters'
            1 * mockCpsDataService.updateNodeLeaves(dataspaceName, anchorName, xpathServiceParameter, jsonData)
        and: 'response status indicates success'
            response.status == HttpStatus.OK.value()
        where:
            scenario               | xpath    | xpathServiceParameter
            'root node by default' | ''       | '/'
            'node by parent xpath' | '/xpath' | '/xpath'
    }

    @Unroll
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
                                    .param('xpath', xpath))
                            .andReturn().response
        then: 'the service method is invoked with expected parameters'
            1 * mockCpsDataService.replaceNodeTree(dataspaceName, anchorName, xpathServiceParameter, jsonData)
        and: 'response status indicates success'
            response.status == HttpStatus.OK.value()
        where:
            scenario               | xpath    | xpathServiceParameter
            'root node by default' | ''       | '/'
            'node by parent xpath' | '/xpath' | '/xpath'
    }
}
