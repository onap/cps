/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 Nordix Foundation
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
import org.onap.cps.api.CpsFacade
import org.onap.cps.api.model.QueryRequest
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

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

    def 'Execute query with valid query request inputs for #scenario.'() {
        given: 'The query endpoint'
        def mockJsonObjectMapper = Mock(JsonObjectMapper)
        def dataNodeEndpoint = "/cps/api/query/my-dataspace/my-anchor"
        and: 'A valid query request and mock response'
        def queryRequest = [
                xpath: xpath,
                select: selectFields,
                condition: whereCondition
        ]
        def queryResult = expectedResult
        mockCpsFacade.executeCustomQuery('my-dataspace', 'my-anchor', xpath, selectFields, whereCondition) >> queryResult
        mockJsonObjectMapper.asJsonString(queryResult) >> expectedJsonOutput
        mockJsonObjectMapper.asJsonString(queryRequest) >> queryRequestJson

        when: 'Query data nodes API is invoked with POST request'
        def response = mvc.perform(post(dataNodeEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(queryRequestJson))
                .andReturn().response
        println "Response Status: ${response.status}, Content: ${response.getContentAsString()}, Content-Type: ${response.getHeader('Content-Type')}"

        then: 'The call is delegated to the CpsFacade with correct parameters'
        1 * mockCpsFacade.executeCustomQuery('my-dataspace', 'my-anchor', xpath, selectFields, whereCondition) >> expectedResult

        and: 'The response is OK, has correct Content-Type, and contains the query result in JSON format'
        assert response.status == HttpStatus.OK.value()
        assert response.getHeader('Content-Type') == MediaType.APPLICATION_JSON_VALUE
        assert response.getContentAsString() == expectedJsonOutput

        where: 'The following valid query parameters are used'
        scenario                            | xpath         | selectFields         | whereCondition         | expectedResult                                    | expectedJsonOutput                                | queryRequestJson
        'all fields provided'              | '/my/path'    | ['field1', 'field2'] | 'field1 = "value1"'    | [[field1: 'value1', field2: 'value2']]           | '[{"field1":"value1","field2":"value2"}]'         | '{"xpath":"/my/path","select":["field1","field2"],"condition":"field1 = \\"value1\\""}'
        'single select field'              | '/my/path'    | ['field1']           | 'field1 = "value1"'    | [[field1: 'value1']]                             | '[{"field1":"value1"}]'                           | '{"xpath":"/my/path","select":["field1"],"condition":"field1 = \\"value1\\""}'
        'no where condition'               | '/my/path'    | ['field1', 'field2'] | null                   | [[field1: 'value1', field2: 'value2']]           | '[{"field1":"value1","field2":"value2"}]'         | '{"xpath":"/my/path","select":["field1","field2"],"condition":null}'
        'empty select fields'              | '/my/path'    | []                   | 'field1 = "value1"'    | [[:]]                                            | '[{}]'                                            | '{"xpath":"/my/path","select":[],"condition":"field1 = \\"value1\\""}'
    }

    def 'Set where condition in QueryRequest for #scenario.'() {
        given: 'A new QueryRequest instance'
        def queryRequest = new QueryRequest()

        when: 'setWhere is called with a condition'
        queryRequest.setWhere(whereCondition)

        then: 'The condition is correctly set and retrievable via getCondition'
        queryRequest.getCondition() == expectedCondition

        where: 'The following conditions are tested'
        scenario                   | whereCondition         | expectedCondition
        'valid condition'         | 'field1 = "value1"'    | 'field1 = "value1"'
        'null condition'          | null                   | null
        'empty condition'         | ''                     | ''
    }
}
