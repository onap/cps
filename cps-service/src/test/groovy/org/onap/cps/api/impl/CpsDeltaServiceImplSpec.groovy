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
import spock.lang.Shared
import spock.lang.Specification

class CpsDeltaServiceImplSpec extends Specification{

    def objectUnderTest = new CpsDeltaServiceImpl()

    @Shared
    static Collection<DataNode> sourceDataNodeWithLeafData = [new DataNode(xpath: '/parent', leaves: ['parent-leaf': 'parent-payload-in-source'])]
    @Shared
    static Collection<DataNode> sourceDataNodeWithoutLeafData = [new DataNode(xpath: '/parent')]
    @Shared
    static Collection<DataNode> targetDataNodeWithLeafData = [new DataNode(xpath: '/parent', leaves: ['parent-leaf': 'parent-payload-in-target'])]
    @Shared
    static Collection<DataNode> targetDataNodeWithoutLeafData = [new DataNode(xpath: '/parent')]
    @Shared
    static Collection<DataNode> sourceDataNodeWithMultipleLeaves = [new DataNode(xpath: '/parent', leaves: ['leaf-1': 'leaf-1-in-source', 'leaf-2': 'leaf-2-in-source'])]
    @Shared
    static Collection<DataNode> targetDataNodeWithMultipleLeaves = [new DataNode(xpath: '/parent', leaves: ['leaf-1': 'leaf-1-in-target', 'leaf-2': 'leaf-2-in-target'])]

    def 'Get delta between data nodes for REMOVED data where source data node has #scenario'() {
        when: 'attempt to get delta between 2 data nodes'
            def result = objectUnderTest.getDeltaReports(sourceDataNodeWithLeafData, [])
        then: 'the delta report contains expected "remove" action'
            assert result[0].action.equals('remove')
        and : 'the delta report contains the expected xpath'
            assert result[0].xpath == '/parent'
        and: 'the delta report contains expected source data'
            assert result[0].sourceData == ['parent-leaf': 'parent-payload-in-source']
        and: 'the delta report contains no target data'
            assert  result[0].targetData == null
    }

    def 'Get delta between data nodes with ADDED data where target data node has #scenario'() {
        when: 'attempt to get delta between 2 data nodes'
            def result = objectUnderTest.getDeltaReports([], targetDataNodeWithLeafData)
        then: 'the delta report contains expected "add" action'
            assert result[0].action.equals('add')
        and: 'the delta report contains expected xpath'
            assert result[0].xpath == '/parent'
        and: 'the delta report contains no source data'
            assert result[0].sourceData == null
        and: 'the delta report contains expected target data'
            assert result[0].targetData == ['parent-leaf': 'parent-payload-in-target']
    }

    def 'Delta Report between leaves for parent and child nodes, #scenario'() {
        given: 'Two data nodes'
            def sourceDataNode  = [new DataNode(xpath: '/parent', leaves: ['parent-leaf': 'parent-payload'], childDataNodes: [new DataNode(xpath: '/parent/child', leaves: ['child-leaf': 'child-payload'])])]
            def targetDataNode  = [new DataNode(xpath: '/parent', leaves: ['parent-leaf': 'parent-payload-updated'], childDataNodes: [new DataNode(xpath: '/parent/child', leaves: ['child-leaf': 'child-payload-updated'])])]
        when: 'attempt to get delta between 2 data nodes'
            def result = objectUnderTest.getDeltaReports(sourceDataNode, targetDataNode)
        then: 'the delta report contains expected "update" action'
            assert result[index].action.equals('update')
        and: 'the delta report contains expected xpath'
            assert result[index].xpath == expectedXpath
        and: 'the delta report contains expected source and target data'
            assert result[index].sourceData == expectedSourceData
            assert result[index].targetData == expectedTargetData
        where: 'the following data was used'
            scenario           | index || expectedXpath   | expectedSourceData                | expectedTargetData
            'parent data node' | 0     || '/parent'       | ['parent-leaf': 'parent-payload'] | ['parent-leaf': 'parent-payload-updated']
            'child data node'  | 1     || '/parent/child' | ['child-leaf': 'child-payload']   | ['child-leaf': 'child-payload-updated']
    }

    def 'Delta report between leaves, #scenario'() {
        when: 'attempt to get delta between 2 data nodes'
            def result = objectUnderTest.getDeltaReports(sourceDataNode, targetDataNode)
        then: 'the delta report contains expected "update" action'
            assert result[0].action.equals('update')
        and: 'the delta report contains expected xpath'
            assert result[0].xpath == '/parent'
        and: 'the delta report contains expected source and target data'
            assert result[0].sourceData == expectedSourceData
            assert result[0].targetData == expectedTargetData
        where: 'the following data was used'
            scenario                                           | sourceDataNode                   | targetDataNode                   || expectedSourceData                                           | expectedTargetData
            'source and target data nodes have leaves'         | sourceDataNodeWithLeafData       | targetDataNodeWithLeafData       || ['parent-leaf': 'parent-payload-in-source']                  | ['parent-leaf': 'parent-payload-in-target']
            'only source data node has leaves'                 | sourceDataNodeWithLeafData       | targetDataNodeWithoutLeafData    || ['parent-leaf': 'parent-payload-in-source']                  | null
            'only target data node has leaves'                 | sourceDataNodeWithoutLeafData    | targetDataNodeWithLeafData       || null                                                         | ['parent-leaf': 'parent-payload-in-target']
            'source and target dsta node with multiple leaves' | sourceDataNodeWithMultipleLeaves | targetDataNodeWithMultipleLeaves || ['leaf-1': 'leaf-1-in-source', 'leaf-2': 'leaf-2-in-source'] | ['leaf-1': 'leaf-1-in-target', 'leaf-2': 'leaf-2-in-target']
    }

    def 'Get delta between data nodes for updated data, where source and target data nodes have no leaves '() {
        when: 'attempt to get delta between 2 data nodes'
            def result = objectUnderTest.getDeltaReports(sourceDataNodeWithoutLeafData, targetDataNodeWithoutLeafData)
        then: 'the delta report contains "update" action with right data'
            assert result.isEmpty()
    }
}
