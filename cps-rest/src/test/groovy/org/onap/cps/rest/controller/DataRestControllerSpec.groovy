/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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

import static org.onap.cps.spi.FetchChildrenOption.INCLUDE_ALL_CHILDREN
import static org.onap.cps.spi.FetchChildrenOption.OMIT_CHILDREN
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

import com.google.common.collect.ImmutableMap
import org.modelmapper.ModelMapper
import org.onap.cps.api.CpsAdminService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsModuleService
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
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import javax.annotation.PostConstruct

@WebMvcTest
class DataRestControllerSpec extends Specification {
    @SpringBean
    CpsDataService mockCpsDataService = Mock()

    @SpringBean
    CpsModuleService mockCpsModuleService = Mock()

    @SpringBean
    CpsAdminService mockCpsAdminService = Mock()

    @SpringBean
    ModelMapper modelMapper = Mock()

    @Autowired
    MockMvc mvc

    @Value('${rest.api.base-path}')
    def basePath

    String dataNodeEndpoint

    def dataspaceName = 'my_dataspace'
    def anchorName = 'my_anchor'

    @Shared
    static DataNode dataNodeNoChildren = new DataNodeBuilder().withXpath("/no-child")
            .withLeaves(ImmutableMap.of("leaf", "value")).build()
    @Shared
    static DataNode dataNodeWithChild = new DataNodeBuilder().withXpath("/parent")
            .withChildDataNodes(Arrays.asList(
                    new DataNodeBuilder().withXpath("/parent/child").build()
            )).build()

    @PostConstruct
    def initEndpoints() {
        dataNodeEndpoint = "$basePath/v1/dataspaces/$dataspaceName/anchors/$anchorName/nodes"
    }


    @Unroll
    def 'Get data node with #scenario.'() {
        given: 'the service returns data node #scenario'
            mockCpsDataService.getDataNode(dataspaceName, anchorName, xpath, fetchChildrenOption) >> dataNode
        when: 'get request is performed through REST API'
            def response = mvc.perform(
                    get(dataNodeEndpoint)
                            .param("cps-path", xpath)
                            .param("include-children", includeChildren)
            ).andReturn().response
        then: 'assert the success response returned'
            response.status == HttpStatus.OK.value()
        and: 'response contains expected value'
            response.contentAsString.contains(checkString)
        where:
            scenario      | dataNode           | xpath       | includeChildren | fetchChildrenOption  || checkString
            'no children' | dataNodeNoChildren | '/no-child' | ''              | OMIT_CHILDREN        || 'leaf'
            'one child'   | dataNodeWithChild  | '/parent'   | 'true'          | INCLUDE_ALL_CHILDREN || 'child'
    }

    @Unroll
    def 'Get data node error scenario: #scenario.'() {
        given: 'the service returns throws an exception'
            mockCpsDataService.getDataNode(dataspaceName, anchorName, xpath, _) >> { throw exception }
        when: 'get request is performed through REST API'
            def response = mvc.perform(
                    get(dataNodeEndpoint).param("cps-path", xpath)
            ).andReturn().response
        then: 'assert the success response returned'
            response.status == httpStatus.value()
        where:
            scenario       | xpath     | exception                                 || httpStatus
            'no dataspace' | '/x-path' | new DataspaceNotFoundException('')        || HttpStatus.BAD_REQUEST
            'no anchor'    | '/x-path' | new AnchorNotFoundException('', '')       || HttpStatus.BAD_REQUEST
            'no data'      | '/x-path' | new DataNodeNotFoundException('', '', '') || HttpStatus.NOT_FOUND
            'empty path'   | ''        | new IllegalStateException()               || HttpStatus.NOT_IMPLEMENTED
    }
}
