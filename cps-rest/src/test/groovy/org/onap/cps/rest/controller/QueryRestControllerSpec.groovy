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
import com.google.gson.Gson
import org.modelmapper.ModelMapper
import org.onap.cps.api.CpsAdminService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.api.CpsQueryService
import org.onap.cps.spi.FetchDescendantsOption
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

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS
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

    @Shared
    static DataNode dataNodeWithLeavesNoChildren = new DataNodeBuilder().withXpath('/xpath')
            .withLeaves([leaf: 'value', leafList: ['leaveListElement1', 'leaveListElement2']]).build()

    @Shared
    static DataNode dataNodeWithChild = new DataNodeBuilder().withXpath('/parent')
            .withChildDataNodes([new DataNodeBuilder().withXpath("/parent/child").build()]).build()

    @Unroll
    def 'Query data node by cps path for the given dataspace and anchor with #scenario.'() {
        given: 'service method returns a list containing a data node'
            def dataspaceName = 'my_dataspace'
            def anchorName = 'my_anchor'
            def cpsPath = '/xpath/leaves[@leaf=\'value\']'
            def dataNodeList = new ArrayList();
            dataNodeList.add(dataNode)
            mockCpsQueryService.queryDataNodes(dataspaceName, anchorName, cpsPath, expectedCpsDataServiceOption) >> dataNodeList
        and: 'the query endpoint'
            def dataNodeEndpoint = "$basePath/v1/dataspaces/$dataspaceName/anchors/$anchorName/nodes/query"
        when: 'query data nodes API is invoked'
            def response = mvc.perform(get(dataNodeEndpoint)
                    .param('cps-path', cpsPath)
                    .param('include-descendants', includeDescendantsOption))
                    .andReturn().response
        then: 'the response contains the the datanode in json format'
            response.status == HttpStatus.OK.value()
            response.contentAsString.contains('"/parent/child"') == expectChildInResponse
            response.getContentAsString().contains(new Gson().toJson(dataNode))
        where:
            scenario                    | dataNode                     | includeDescendantsOption || expectedCpsDataServiceOption | expectChildInResponse
            'no descendants by default' | dataNodeWithLeavesNoChildren | ''                       || OMIT_DESCENDANTS             | false
            'no descendant explicitly'  | dataNodeWithLeavesNoChildren | 'false'                  || OMIT_DESCENDANTS             | false
            'descendants'               | dataNodeWithChild            | 'true'                   || INCLUDE_ALL_DESCENDANTS      | true
    }
}