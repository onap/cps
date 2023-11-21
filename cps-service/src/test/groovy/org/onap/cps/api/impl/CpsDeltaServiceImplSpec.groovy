/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 TechMahindra Ltd.
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

package org.onap.cps.api.impl

import org.onap.cps.spi.model.DataNode
import org.onap.cps.spi.model.DataNodeBuilder
import spock.lang.Shared
import spock.lang.Specification

class CpsDeltaServiceImplSpec extends Specification{

    def objectUnderTest = new CpsDeltaServiceImpl()

    @Shared
    def dataNodeWithLeafAndChildDataNode = [new DataNodeBuilder().withXpath('/parent').withLeaves(['parent-leaf': 'parent-payload'])
                            .withChildDataNodes([new DataNodeBuilder().withXpath("/parent/child").withLeaves('child-leaf': 'child-payload').build()]).build()]
    @Shared
    def dataNodeWithChildDataNode = [new DataNodeBuilder().withXpath('/parent').withLeaves(['parent-leaf': 'parent-payload'])
                                             .withChildDataNodes([new DataNodeBuilder().withXpath("/parent/child").build()]).build()]

    def 'Get delta between data nodes for removed data where source data node has #scenario'() {
        when: 'attempt to get delta between 2 data nodes'
            def result = objectUnderTest.getDeltaReports(sourceDataNode as Collection<DataNode>, [])
        then: 'the delta report contains "remove" action with right data'
            assert result.get(1).action.equals("remove")
            assert result.get(1).xpath == "/parent/child"
            assert result.get(1).sourceData == expectedSourceData
        where: 'following data was used'
            scenario       | sourceDataNode                   || expectedSourceData
            'leaf data'    | dataNodeWithLeafAndChildDataNode || ['child-leaf': 'child-payload']
            'no leaf data' | dataNodeWithChildDataNode        || null
    }

    def 'Get delta between data nodes with new data where target data node has #scenario'() {
        when: 'attempt to get delta between 2 data nodes'
            def result = objectUnderTest.getDeltaReports([], targetDataNode)
        then: 'the delta report contains "add" action with right data'
            assert result.get(1).action.equals("add")
            assert result.get(1).xpath == "/parent/child"
            assert result.get(1).targetData == expectedTargetData
        where: 'following data was used'
            scenario       | targetDataNode                   || expectedTargetData
            'leaf data'    | dataNodeWithLeafAndChildDataNode || ['child-leaf': 'child-payload']
            'no leaf data' | dataNodeWithChildDataNode        || null
    }

    def 'Get delta between data nodes for updated data, #scenario'() {
        given: 'Two data nodes'
        def dataNode1 = [new DataNodeBuilder().withXpath('/parent').withLeaves(['parent-leaf': 'parent-payload'])
                                                        .withChildDataNodes([new DataNodeBuilder().withXpath("/parent/child").withLeaves('child-leaf': 'child-payload').build()]).build()]
        def dataNode2 = [new DataNodeBuilder().withXpath('/parent').withLeaves(['parent-leaf': 'parent-payload-updated'])
                                                        .withChildDataNodes([new DataNodeBuilder().withXpath("/parent/child").withLeaves('child-leaf': 'child-payload-updated').build()]).build()]
        when: 'attempt to get delta between 2 data nodes'
            def result = objectUnderTest.getDeltaReports(dataNode1, dataNode2)
        then: 'the delta report contains "update" action with right data, and the same is true for child data nodes'
            assert result.get(index).action.equals("update")
            assert result.get(index).xpath == expectedXpath
            assert result.get(index).sourceData == expectedSourceData
            assert result.get(index).targetData == expectedTargetData
        where: 'the following data was used'
            scenario           | index || expectedXpath   | expectedSourceData                | expectedTargetData
            'parent data node' | 0     || "/parent"       | ['parent-leaf': 'parent-payload'] | ['parent-leaf': 'parent-payload-updated']
            'child data node'  | 1     || "/parent/child" | ['child-leaf': 'child-payload']   | ['child-leaf': 'child-payload-updated']
    }
}
