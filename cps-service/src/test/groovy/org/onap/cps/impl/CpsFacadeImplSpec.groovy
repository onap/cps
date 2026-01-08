/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Nordix Foundation
 *  Modifications Copyright (C) 2025 Deutsche Telekom AG
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

package org.onap.cps.impl

import static org.onap.cps.api.parameters.FetchDescendantsOption.OMIT_DESCENDANTS
import static org.onap.cps.api.parameters.PaginationOption.NO_PAGINATION

import org.onap.cps.api.CpsAnchorService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsQueryService
import org.onap.cps.api.model.DataNode
import org.onap.cps.api.parameters.PaginationOption
import org.onap.cps.utils.DataMapper
import org.onap.cps.utils.PrefixResolver
import spock.lang.Specification

class CpsFacadeImplSpec extends Specification {

    def mockCpsDataService = Mock(CpsDataService)
    def mockCpsQueryService = Mock(CpsQueryService)
    def mockCpsAnchorService = Mock(CpsAnchorService)
    def mockPrefixResolver = Mock(PrefixResolver)
    def dataMapper = new DataMapper(mockCpsAnchorService, mockPrefixResolver)

    def myFetchDescendantsOption = OMIT_DESCENDANTS
    def myPaginationOption = NO_PAGINATION

    def objectUnderTest = new CpsFacadeImpl(mockCpsDataService, mockCpsQueryService , dataMapper)

    def dataNode1 = new DataNode(xpath:'/path1', anchorName: 'my anchor')
    def dataNode2 = new DataNode(xpath:'/path2', anchorName: 'my anchor')
    def dataNode3 = new DataNode(xpath:'/path3', anchorName: 'other anchor')

    def setup() {
        mockCpsDataService.getDataNodes('my dataspace', 'my anchor', 'my path', myFetchDescendantsOption) >> [ dataNode1, dataNode2]
        mockPrefixResolver.getPrefix(_, '/path1') >> 'prefix1'
        mockPrefixResolver.getPrefix(_, '/path2') >> 'prefix2'
        mockPrefixResolver.getPrefix(_, '/path3') >> 'prefix3'
    }

    def 'Get one data node.'() {
        when: 'get data node by dataspace and anchor'
            def result = objectUnderTest.getFirstDataNodeByAnchor('my dataspace', 'my anchor', 'my path', myFetchDescendantsOption)
        then: 'only the first node (from the data service result) is returned'
            assert result.size() == 1
            assert result.keySet()[0] == 'prefix1:path1'
    }

    def 'Get multiple data nodes.'() {
        when: 'get data node by dataspace and anchor'
            def result = objectUnderTest.getDataNodesByAnchor('my dataspace', 'my anchor', 'my path', myFetchDescendantsOption)
        then: 'all nodes (from the data service result) are returned'
            assert result.size() == 2
            assert result[0].keySet()[0] == 'prefix1:path1'
            assert result[1].keySet()[0] == 'prefix2:path2'
    }

    def 'Execute anchor query.'() {
        given: 'the cps query service returns two data nodes'
           mockCpsQueryService.queryDataNodes('my dataspace', 'my anchor', '/my/path', myFetchDescendantsOption) >> [ dataNode1, dataNode2]
        when: 'get data node by dataspace and anchor'
            def result = objectUnderTest.executeAnchorQuery('my dataspace', 'my anchor', '/my/path', myFetchDescendantsOption)
        then: 'all nodes (from the query service result) are returned'
            assert result.size() == 2
            assert result[0].keySet()[0] == 'prefix1:path1'
            assert result[1].keySet()[0] == 'prefix2:path2'
    }

    def 'Get multiple data nodes V3.'() {
        when: 'get data node by dataspace and anchor'
            def result = objectUnderTest.getDataNodesByAnchorV3('my dataspace', 'my anchor', 'my path', myFetchDescendantsOption)
        then: 'all nodes (from the data service result) are returned'
            assert result.size() == 2
    }

    def 'Execute anchor query with attribute-axis.'() {
        given: 'the cps query service returns two attribute values'
            mockCpsQueryService.queryDataLeaf('my dataspace', 'my anchor', '/my/path/@myAttribute', Object) >> ['value1', 'value2']
        when: 'get data using attribute axis'
            def result = objectUnderTest.executeAnchorQuery('my dataspace', 'my anchor', '/my/path/@myAttribute', myFetchDescendantsOption)
        then: 'attribute values (from the query service result) are returned'
            assert result.size() == 2
            assert result[0] == ['myAttribute': 'value1']
            assert result[1] == ['myAttribute': 'value2']
    }

    def 'Execute dataspace query.'() {
        given: 'the cps query service returns two data nodes (on two different anchors)'
            mockCpsQueryService.queryDataNodesAcrossAnchors('my dataspace', 'my cps path', myFetchDescendantsOption, myPaginationOption) >> [ dataNode1, dataNode2, dataNode3 ]
        when: 'get data node by dataspace and anchor'
            def result = objectUnderTest.executeDataspaceQuery('my dataspace', 'my cps path', myFetchDescendantsOption, myPaginationOption)
        then: 'all nodes (from the query service result) are returned, grouped by anchor'
            assert result.size() == 2
            assert result[0].toString() == '{anchorName=my anchor, dataNodes=[{prefix1:path1={}}, {prefix1:path2={}}]}'
            assert result[1].toString() == '{anchorName=other anchor, dataNodes=[{prefix3:path3={}}]}'
    }

    def 'How many pages (anchors) could be in the output with #scenario.'() {
        given: 'the query service says there are 10 anchors for the given query'
            mockCpsQueryService.countAnchorsForDataspaceAndCpsPath('my dataspace', 'my cps path') >> 10
        expect: 'the correct number of pages is returned'
            assert objectUnderTest.countAnchorsInDataspaceQuery('my dataspace', 'my cps path', paginationOption) == expectedNumberOfPages
        where: 'the following pagination options are used'
            scenario                        | paginationOption            || expectedNumberOfPages
            'no pagination'                 | NO_PAGINATION               || 1
            '1 anchor per page'             | new PaginationOption(1,1)   || 10
            '1 anchor per page, start at 2' | new PaginationOption(2,1)   || 10
            '2 anchors per page'            | new PaginationOption(1,2)   || 5
            '3 anchors per page'            | new PaginationOption(1,3)   || 4
            '10 anchors per page'           | new PaginationOption(1,10)  || 1
            '100 anchors per page'          | new PaginationOption(1,100) || 1
    }

}
