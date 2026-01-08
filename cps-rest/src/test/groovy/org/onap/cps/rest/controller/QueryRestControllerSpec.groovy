/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 Nordix Foundation
 *  Modifications Copyright (C) 2021-2022 Bell Canada.
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2022-2024 Deutsche Telekom AG
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
import org.onap.cps.api.CpsFacade
import org.onap.cps.api.parameters.PaginationOption
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.onap.cps.api.parameters.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.api.parameters.FetchDescendantsOption.OMIT_DESCENDANTS
import static org.onap.cps.api.parameters.PaginationOption.NO_PAGINATION
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@WebMvcTest(QueryRestController)
class QueryRestControllerSpec extends Specification {

    @SpringBean
    CpsFacade mockCpsFacade = Mock()

    @SpringBean
    JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

    @Autowired
    MockMvc mvc

    @Value('${rest.api.cps-base-path}')
    def basePath

    def dataNodeAsMap = ['prefixedPath':[path:[leaf:'value']]]

    def 'Query data node (v1) by cps path for the given dataspace and anchor with #scenario.'() {
        given: 'the query endpoint'
            def dataNodeEndpoint = "$basePath/v1/dataspaces/my_dataspace/anchors/my_anchor/nodes/query"
        when: 'query data nodes API is invoked'
            def response = mvc.perform(get(dataNodeEndpoint).param('cps-path', 'my/path').param('include-descendants', includeDescendantsOption))
                .andReturn().response
        then: 'the call is delegated to the cps service facade which returns a list containing one data node as a map'
            1 * mockCpsFacade.executeAnchorQuery('my_dataspace', 'my_anchor', 'my/path', expectedCpsDataServiceOption) >> [dataNodeAsMap]
        then: 'the response contains the the datanode in json format'
            assert response.status == HttpStatus.OK.value()
            assert response.getContentAsString() == '[{"prefixedPath":{"path":{"leaf":"value"}}}]'
        where: 'the following options for include descendants are provided in the request'
            scenario                    | includeDescendantsOption || expectedCpsDataServiceOption
            'no descendants by default' | ''                       || OMIT_DESCENDANTS
            'descendants'               | 'true'                   || INCLUDE_ALL_DESCENDANTS
    }

    def 'Query data node (v2) by cps path for given dataspace and anchor with #scenario'() {
        given: 'the query endpoint'
            def dataNodeEndpointV2  = "$basePath/v2/dataspaces/my_dataspace/anchors/my_anchor/nodes/query"
        when: 'query data nodes API is invoked'
            def response = mvc.perform(get(dataNodeEndpointV2).contentType(contentType).param('cps-path', 'my/path') .param('descendants', includeDescendantsOptionString))
                    .andReturn().response
        then: 'the call is delegated to the cps service facade which returns a list containing one data node as a map'
            1 * mockCpsFacade.executeAnchorQuery('my_dataspace', 'my_anchor', 'my/path',
                { descendantsOption -> assert descendantsOption.depth == expectedDepth }) >> [dataNodeAsMap]
        and: 'the response contains the datanode in the expected format'
            assert response.status == HttpStatus.OK.value()
            assert response.getContentAsString() == expectedOutput
        where: 'the following options for include descendants are provided in the request'
            scenario               | includeDescendantsOptionString | contentType                || expectedDepth || expectedOutput
            'direct children JSON' | 'direct'                       | MediaType.APPLICATION_JSON || 1             || '[{"prefixedPath":{"path":{"leaf":"value"}}}]'
            'descendants JSON'     | '2'                            | MediaType.APPLICATION_JSON || 2             || '[{"prefixedPath":{"path":{"leaf":"value"}}}]'
            'descendants XML'      | '2'                            | MediaType.APPLICATION_XML  || 2             || '<prefixedPath><path><leaf>value</leaf></path></prefixedPath>'
    }

    def 'Query data node (v2) by cps path for given dataspace and anchor with attribute-axis and #scenario'() {
        given: 'the query endpoint'
            def dataNodeEndpointV2  = "$basePath/v2/dataspaces/my_dataspace/anchors/my_anchor/nodes/query"
        when: 'query data nodes API is invoked'
            def response = mvc.perform(get(dataNodeEndpointV2).contentType(contentType).param('cps-path', '/my/path/@myAttribute').param('descendants', '0'))
                    .andReturn().response
        then: 'the call is delegated to the cps service facade which returns a list containing two attributes as maps'
            1 * mockCpsFacade.executeAnchorQuery('my_dataspace', 'my_anchor', '/my/path/@myAttribute', OMIT_DESCENDANTS) >> [['myAttribute':'value1'], ['myAttribute':'value2']]
        and: 'the response contains the datanode in the expected format'
            assert response.status == HttpStatus.OK.value()
            assert response.getContentAsString() == expectedOutput
        where: 'the following options for content type are provided in the request'
            scenario | contentType                || expectedOutput
            'JSON'   | MediaType.APPLICATION_JSON || '[{"myAttribute":"value1"},{"myAttribute":"value2"}]'
            'XML'    | MediaType.APPLICATION_XML  || '<myAttribute>value1</myAttribute><myAttribute>value2</myAttribute>'
    }

    def 'Query data node by cps path for given dataspace across all anchors'() {
        given: 'the query endpoint'
            def dataNodeEndpoint = "$basePath/v2/dataspaces/my_dataspace/nodes/query"
        and: 'the  cps service facade will say there are 123 pages '
            mockCpsFacade.countAnchorsInDataspaceQuery('my_dataspace', 'my/path', new PaginationOption(2,5) ) >> 123
        when: 'query data nodes API is invoked'
            def response = mvc.perform(
                        get(dataNodeEndpoint).param('cps-path', 'my/path').param('pageIndex', String.valueOf(2)).param('pageSize', String.valueOf(5)))
                        .andReturn().response
        then: 'the call is delegated to the cps service facade which returns a list containing one data node as a map'
            1 * mockCpsFacade.executeDataspaceQuery('my_dataspace', 'my/path', OMIT_DESCENDANTS, new PaginationOption(2,5)) >> [dataNodeAsMap]
        then: 'the response is OK and contains the the datanode in json format'
            assert response.status == HttpStatus.OK.value()
            assert response.getContentAsString() == '[{"prefixedPath":{"path":{"leaf":"value"}}}]'
        and: 'the header indicates the correct number of pages'
            assert response.getHeaderValue('total-pages') == '123'
    }

    def 'Query data node across all anchors with pagination option with #scenario i.e. no pagination.'() {
        given: 'the query endpoint'
            def dataNodeEndpoint = "$basePath/v2/dataspaces/my_dataspace/nodes/query"
        and: 'the  cps service facade will say there is 1 page '
            mockCpsFacade.countAnchorsInDataspaceQuery('my_dataspace', 'my/path', NO_PAGINATION ) >> 1
        when: 'query data nodes API is invoked'
            def response = mvc.perform(get(dataNodeEndpoint).param('cps-path', 'my/path').param(parameterName, '1'))
                .andReturn().response
        then: 'the call is delegated to the cps service facade which returns a list containing one data node as a map'
            1 * mockCpsFacade.executeDataspaceQuery('my_dataspace', 'my/path', OMIT_DESCENDANTS, PaginationOption.NO_PAGINATION) >> [dataNodeAsMap]
        then: 'the response is OK and contains the datanode in json format'
            assert response.status == HttpStatus.OK.value()
            assert response.getContentAsString() == '[{"prefixedPath":{"path":{"leaf":"value"}}}]'
        and: 'the header indicates the correct number of pages'
            assert response.getHeaderValue('total-pages') == '1'
        where: 'only the following rest parameter is used'
            scenario           | parameterName
            'only page size'   | 'pageSize'
            'only page index'  | 'pageIndex'
    }
}
