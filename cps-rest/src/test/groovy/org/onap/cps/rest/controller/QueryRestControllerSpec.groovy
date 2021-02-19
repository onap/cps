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

import com.google.common.collect.ImmutableMap
import org.modelmapper.ModelMapper
import org.onap.cps.api.CpsAdminService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.api.CpsQueryService
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@WebMvcTest
class QueryRestControllerSpec extends Specification {

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

    def dataspaceName = 'my_dataspace'
    def anchorName = 'my_anchor'

    @Shared
    static DataNode dataNodeNoChildren = new DataNodeBuilder().withXpath("/xpath")
            .withLeaves(ImmutableMap.of("leaf", "value")).build()

    def 'Query data node by cps path for the given dataspace and anchor.'() {
        given: 'service method returns a list of data nodes'
            ArrayList <DataNode> dataNodeList = new ArrayList();
            dataNodeList.add(dataNodeNoChildren)
            def cpsPath ='/xpath/leaves[@leaf=\'value\']'
            mockCpsQueryService.queryDataNodes(dataspaceName, anchorName, cpsPath) >> dataNodeList
        and: 'an endpoint'
            def dataNodeEndpoint = "$basePath/v1/dataspaces/$dataspaceName/anchors/$anchorName/nodes/query"
        when: 'query data nodes API is invoked'
            def response = mvc.perform(
                    get(dataNodeEndpoint)
                            .param('cps-path', cpsPath)
            ).andReturn().response
        then: 'the response contains the correct data'
            response.status == HttpStatus.OK.value()
            response.getContentAsString().contains('leaf')
    }
}