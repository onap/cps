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
    @Shared
    def emptyDataNode = [new DataNodeBuilder().withXpath('/parent').build()]

    def 'Get delta between data nodes for removed data where source data node has #scenario'() {
        when: 'attempt to get delta between 2 data nodes'
            def result = objectUnderTest.getDeltaReports(sourceDataNode as Collection<DataNode>, emptyDataNode)
        then: 'the delta report contains "remove" action with right data'
            assert result.first().action.equals("remove")
            assert result.first().xpath == "/parent/child"
            assert result.first().sourceData == expectedSourceData
        where: 'following data was used'
            scenario       | sourceDataNode                   || expectedSourceData
            'leaf data'    | dataNodeWithLeafAndChildDataNode || ['child-leaf': 'child-payload']
            'no leaf data' | dataNodeWithChildDataNode        || null
    }

    def 'Get delta between data nodes with new data where target data node has #scenario'() {
        when: 'attempt to get delta between 2 data nodes'
            def result = objectUnderTest.getDeltaReports(emptyDataNode, targetDataNode)
        then: 'the delta report contains "add" action with right data'
            assert result.first().action.equals("add")
            assert result.first().xpath == "/parent/child"
            assert result.first().targetData == expectedTargetData
        where: 'following data was used'
            scenario       | targetDataNode                   || expectedTargetData
            'leaf data'    | dataNodeWithLeafAndChildDataNode || ['child-leaf': 'child-payload']
            'no leaf data' | dataNodeWithChildDataNode        || null
    }
}
