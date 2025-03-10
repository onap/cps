/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Nordix Foundation
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

import org.onap.cps.api.CpsAnchorService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.model.DataNode
import org.onap.cps.api.parameters.FetchDescendantsOption
import org.onap.cps.utils.PrefixResolver
import spock.lang.Specification

class CpsFacadeImplSpec extends Specification {

    def mockCpsDataService = Mock(CpsDataService)
    def mockCpsAnchorService = Mock(CpsAnchorService)
    def mockPrefixResolver = Mock(PrefixResolver)

    def myFetchDescendantsOption = FetchDescendantsOption.OMIT_DESCENDANTS

    def objectUnderTest = new CpsFacadeImpl(mockCpsDataService, mockCpsAnchorService, mockPrefixResolver)

    def setup() {
        def dataNode1 = new DataNode(xpath:'/path1')
        def dataNode2 = new DataNode(xpath:'/path2')
        mockCpsDataService.getDataNodes('my dataspace', 'my anchor', 'my path', myFetchDescendantsOption) >> [ dataNode1, dataNode2]
        mockPrefixResolver.getPrefix(_, '/path1') >> 'prefix1'
        mockPrefixResolver.getPrefix(_, '/path2') >> 'prefix2'
    }

    def 'Get data node (singular).'() {
        when: 'get data node by dataspace and anchor'
            def result = objectUnderTest.getNodeByDataspaceAndAnchor('my dataspace', 'my anchor', 'my path', myFetchDescendantsOption)
        then: 'only the first node (from the data service result) is returned'
            assert result.size() == 1
            assert result.keySet()[0] == 'prefix1:path1'
    }

    def 'Get data nodes (plural).'() {
        when: 'get data node by dataspace and anchor'
            def result = objectUnderTest.getNodesByDataspaceAndAnchor('my dataspace', 'my anchor', 'my path', myFetchDescendantsOption)
        then: 'all nodes (from the data service result) are returned'
            assert result.size() == 2
            assert result[0].keySet()[0] == 'prefix1:path1'
            assert result[1].keySet()[0] == 'prefix2:path2'
    }

}
