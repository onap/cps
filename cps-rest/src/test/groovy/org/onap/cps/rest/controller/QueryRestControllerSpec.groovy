/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation
 *  Modifications Copyright (C) 2021-2022 Bell Canada.
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2022-2024 TechMahindra Ltd.
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
import org.onap.cps.api.CpsQueryService
import org.onap.cps.spi.api.PaginationOption
import org.onap.cps.spi.api.model.DataNodeBuilder
import org.onap.cps.utils.JsonObjectMapper
import org.onap.cps.utils.PrefixResolver
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.onap.cps.spi.api.FetchDescendantsOption.DIRECT_CHILDREN_ONLY
import static org.onap.cps.spi.api.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.api.FetchDescendantsOption.OMIT_DESCENDANTS
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@WebMvcTest(QueryRestController)
class QueryRestControllerSpec extends Specification {

    @SpringBean
    CpsQueryService mockCpsQueryService = Mock()

    @SpringBean
    CpsAnchorService mockCpsAnchorService = Mock()

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
    def cpsPath = 'some cps-path'
    def dataNodeEndpointV2

    def setup() {
         dataNodeEndpointV2 = "$basePath/v2/dataspaces/$dataspaceName/anchors/$anchorName/nodes/query"
    }

    def 'Query data node by cps path for the given dataspace and anchor with #scenario.'() {
        given: 'service method returns a list containing a data node'
            def dataNode1 = new DataNodeBuilder().withXpath('/xpath')
                    .withLeaves([leaf: 'value', leafList: ['leaveListElement1', 'leaveListElement2']]).build()
            mockCpsQueryService.queryDataNodes(dataspaceName, anchorName, cpsPath, expectedCpsDataServiceOption) >> [dataNode1, dataNode1]
        and: 'the query endpoint'
            def dataNodeEndpoint = "$basePath/v1/dataspaces/$dataspaceName/anchors/$anchorName/nodes/query"
        when: 'query data nodes API is invoked'
            def response =
                    mvc.perform(
                            get(dataNodeEndpoint)
                                    .param('cps-path', cpsPath)
                                    .param('include-descendants', includeDescendantsOption))
                            .andReturn().response
        then: 'the response contains the the datanode in json format'
            response.status == HttpStatus.OK.value()
            response.getContentAsString().contains('{"xpath":{"leaf":"value","leafList":["leaveListElement1","leaveListElement2"]}}')
        where: 'the following options for include descendants are provided in the request'
            scenario                    | includeDescendantsOption || expectedCpsDataServiceOption
            'no descendants by default' | ''                       || OMIT_DESCENDANTS
            'no descendant explicitly'  | 'false'                  || OMIT_DESCENDANTS
            'descendants'               | 'true'                   || INCLUDE_ALL_DESCENDANTS
    }

    def 'Query data node v2 API by cps path for the given dataspace and anchor with #scenario and media type JSON'() {
        given: 'service method returns a list containing a data node'
            def dataNode = new DataNodeBuilder().withXpath('/xpath')
                .withLeaves([leaf: 'value', leafList: ['leaveListElement1', 'leaveListElement2']]).build()
            mockCpsQueryService.queryDataNodes(dataspaceName, anchorName, cpsPath, { descendantsOption ->
                assert descendantsOption.depth == expectedDepth
            }) >> [dataNode, dataNode]
        when: 'query data nodes API is invoked'
            def response =
                mvc.perform(
                    get(dataNodeEndpointV2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param('cps-path', cpsPath)
                        .param('descendants', includeDescendantsOptionString))
                    .andReturn().response
        then: 'the response contains the datanode in the expected JSON format'
            assert response.status == HttpStatus.OK.value()
            assert response.getContentAsString().contains('{"xpath":{"leaf":"value","leafList":["leaveListElement1","leaveListElement2"]}}')
        where: 'the following options for include descendants are provided in the request'
            scenario          | includeDescendantsOptionString || expectedDepth
            'direct children' | 'direct'                       || 1
            'descendants'     | '2'                            || 2
    }

    def 'Query data node v2 API by cps path for the given dataspace and anchor with #scenario and media type XML'() {
        given: 'service method returns a list containing a data node'
            def dataNode = new DataNodeBuilder().withXpath('/xpath')
                .withLeaves([leaf: 'value', leafList: ['leaveListElement1', 'leaveListElement2']]).build()
            mockCpsQueryService.queryDataNodes(dataspaceName, anchorName, cpsPath, { descendantsOption ->
                assert descendantsOption.depth == expectedDepth
            }) >> [dataNode, dataNode]
        when: 'query data nodes API is invoked'
            def response =
                mvc.perform(
                    get(dataNodeEndpointV2)
                        .contentType(MediaType.APPLICATION_XML)
                        .param('cps-path', cpsPath)
                        .param('descendants', includeDescendantsOptionString))
                    .andReturn().response
        then: 'the response contains the datanode in the expected XML format'
            assert response.status == HttpStatus.OK.value()
            assert response.getContentAsString().contains('<xpath><leaf>value</leaf><leafList>leaveListElement1</leafList><leafList>leaveListElement2</leafList></xpath>')
        where: 'the following options for include descendants are provided in the request'
            scenario          | includeDescendantsOptionString || expectedDepth
            'direct children' | 'direct'                       || 1
            'descendants'     | '2'                            || 2
    }

    def 'Query data node by cps path for the given dataspace across all anchors with #scenario.'() {
        given: 'service method returns a list containing a data node from different anchors'
            def dataNode1 = new DataNodeBuilder().withXpath('/xpath')
                .withAnchor('my_anchor')
                .withLeaves([leaf: 'value', leafList: ['leaveListElement1', 'leaveListElement2']]).build()
            def dataNode2 = new DataNodeBuilder().withXpath('/xpath')
                .withAnchor('my_anchor_2')
                .withLeaves([leaf: 'value', leafList: ['leaveListElement3', 'leaveListElement4']]).build()
        and: 'second data node for the same anchor'
            def dataNode3 = new DataNodeBuilder().withXpath('/xpath')
                .withAnchor('my_anchor_2')
                .withLeaves([leaf: 'value', leafList: ['leaveListElement5', 'leaveListElement6']]).build()
        and: 'the query endpoint'
            def dataspaceName = 'my_dataspace'
            def cpsPath = 'some/cps/path'
            def dataNodeEndpoint = "$basePath/v2/dataspaces/$dataspaceName/nodes/query"
            mockCpsQueryService.queryDataNodesAcrossAnchors(dataspaceName, cpsPath,
                expectedCpsDataServiceOption, PaginationOption.NO_PAGINATION) >> [dataNode1, dataNode2, dataNode3]
            mockCpsQueryService.countAnchorsForDataspaceAndCpsPath(dataspaceName, cpsPath) >> 2
        when: 'query data nodes API is invoked'
            def response =
                mvc.perform(
                        get(dataNodeEndpoint)
                                .param('cps-path', cpsPath)
                                .param('descendants', includeDescendantsOptionString))
                        .andReturn().response
        then: 'the response contains the the datanode in json format'
            response.status == HttpStatus.OK.value()
            response.getContentAsString().contains('{"xpath":{"leaf":"value","leafList":["leaveListElement1","leaveListElement2"]}}')
            response.getContentAsString().contains('{"xpath":{"leaf":"value","leafList":["leaveListElement3","leaveListElement4"]}}')
            response.getContentAsString().contains('{"xpath":{"leaf":"value","leafList":["leaveListElement5","leaveListElement6"]}}')
        where: 'the following options for include descendants are provided in the request'
            scenario                    | includeDescendantsOptionString || expectedCpsDataServiceOption
            'no descendants by default' | ''                             || OMIT_DESCENDANTS
            'no descendant explicitly'  | 'none'                         || OMIT_DESCENDANTS
            'descendants'               | 'all'                          || INCLUDE_ALL_DESCENDANTS
            'direct children'           | 'direct'                       || DIRECT_CHILDREN_ONLY
    }

    def 'Query data node by cps path for the given dataspace across all anchors with pagination #scenario.'() {
        given: 'service method returns a list containing a data node from different anchors'
        def dataNode1 = new DataNodeBuilder().withXpath('/xpath')
                .withAnchor('my_anchor')
                .withLeaves([leaf: 'value', leafList: ['leaveListElement1', 'leaveListElement2']]).build()
        def dataNode2 = new DataNodeBuilder().withXpath('/xpath')
                .withAnchor('my_anchor_2')
                .withLeaves([leaf: 'value', leafList: ['leaveListElement3', 'leaveListElement4']]).build()
        and: 'the query endpoint'
            def dataspaceName = 'my_dataspace'
            def cpsPath = 'some/cps/path'
            def dataNodeEndpoint = "$basePath/v2/dataspaces/$dataspaceName/nodes/query"
            mockCpsQueryService.queryDataNodesAcrossAnchors(dataspaceName, cpsPath,
                INCLUDE_ALL_DESCENDANTS, new PaginationOption(pageIndex,pageSize)) >> [dataNode1, dataNode2]
            mockCpsQueryService.countAnchorsForDataspaceAndCpsPath(dataspaceName, cpsPath) >> totalAnchors
        when: 'query data nodes API is invoked'
            def response =
                mvc.perform(
                        get(dataNodeEndpoint)
                                .param('cps-path', cpsPath)
                                .param('descendants', "all")
                                .param('pageIndex', String.valueOf(pageIndex))
                                .param('pageSize', String.valueOf(pageSize)))
                        .andReturn().response
        then: 'the response contains the the datanode in json format'
            assert response.status == HttpStatus.OK.value()
            assert Integer.valueOf(response.getHeaderValue("total-pages")) == expectedTotalPageSize
            assert response.getContentAsString().contains('{"xpath":{"leaf":"value","leafList":["leaveListElement1","leaveListElement2"]}}')
            assert response.getContentAsString().contains('{"xpath":{"leaf":"value","leafList":["leaveListElement3","leaveListElement4"]}}')
        where: 'the following options for include descendants are provided in the request'
            scenario                     | pageIndex | pageSize | totalAnchors || expectedTotalPageSize
            '1st page with all anchors'  | 1         | 3        | 3            || 1
            '1st page with less anchors' | 1         | 2        | 3            || 2
    }

    def 'Query data node across all anchors with pagination option with #scenario.'() {
        given: 'service method returns a list containing a data node from different anchors'
        def dataNode1 = new DataNodeBuilder().withXpath('/xpath')
                .withAnchor('my_anchor')
                .withLeaves([leaf: 'value', leafList: ['leaveListElement1', 'leaveListElement2']]).build()
        def dataNode2 = new DataNodeBuilder().withXpath('/xpath')
                .withAnchor('my_anchor_2')
                .withLeaves([leaf: 'value', leafList: ['leaveListElement3', 'leaveListElement4']]).build()
        and: 'the query endpoint'
            def dataspaceName = 'my_dataspace'
            def cpsPath = 'some/cps/path'
            def dataNodeEndpoint = "$basePath/v2/dataspaces/$dataspaceName/nodes/query"
            mockCpsQueryService.queryDataNodesAcrossAnchors(dataspaceName, cpsPath,
                INCLUDE_ALL_DESCENDANTS, PaginationOption.NO_PAGINATION) >> [dataNode1, dataNode2]
            mockCpsQueryService.countAnchorsForDataspaceAndCpsPath(dataspaceName, cpsPath) >> 2
        when: 'query data nodes API is invoked'
            def response =
                mvc.perform(
                        get(dataNodeEndpoint)
                                .param('cps-path', cpsPath)
                                .param('descendants', "all")
                                .param(parameterName, "1"))
                        .andReturn().response
        then: 'the response contains the the datanode in json format'
            assert response.status == HttpStatus.OK.value()
            assert Integer.valueOf(response.getHeaderValue("total-pages")) == 1
            assert response.getContentAsString().contains('{"xpath":{"leaf":"value","leafList":["leaveListElement1","leaveListElement2"]}}')
            assert response.getContentAsString().contains('{"xpath":{"leaf":"value","leafList":["leaveListElement3","leaveListElement4"]}}')
        where:
            scenario           | parameterName
            'only page size'   | 'pageSize'
            'only page index'  | 'pageIndex'
    }
}
