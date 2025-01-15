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

import org.onap.cps.api.model.DataNode
import org.onap.cps.impl.CpsDeltaServiceImpl
import spock.lang.Specification

class CpsDeltaServiceImplSpec extends Specification{

    def objectUnderTest = new CpsDeltaServiceImpl()


    static def sourceDataNodeWithLeafData = [new DataNode(xpath: '/parent', leaves: ['parent-leaf': 'parent-payload-in-source'])]
    static def sourceDataNodeWithoutLeafData = [new DataNode(xpath: '/parent')]
    static def targetDataNodeWithLeafData = [new DataNode(xpath: '/parent', leaves: ['parent-leaf': 'parent-payload-in-target'])]
    static def targetDataNodeWithoutLeafData = [new DataNode(xpath: '/parent')]
    static def sourceDataNodeWithMultipleLeaves = [new DataNode(xpath: '/parent', leaves: ['leaf-1': 'leaf-1-in-source', 'leaf-2': 'leaf-2-in-source'])]
    static def targetDataNodeWithMultipleLeaves = [new DataNode(xpath: '/parent', leaves: ['leaf-1': 'leaf-1-in-target', 'leaf-2': 'leaf-2-in-target'])]

    def 'Get delta between data nodes for REMOVED data'() {
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

    def 'Get delta between data nodes for ADDED data'() {
        when: 'attempt to get delta between 2 data nodes'
            def result = objectUnderTest.getDeltaReports([], targetDataNodeWithLeafData)
        then: 'the delta report contains expected "create" action'
            assert result[0].action.equals('create')
        and: 'the delta report contains expected xpath'
            assert result[0].xpath == '/parent'
        and: 'the delta report contains no source data'
            assert result[0].sourceData == null
        and: 'the delta report contains expected target data'
            assert result[0].targetData == ['parent-leaf': 'parent-payload-in-target']
    }

    def 'Delta Report between leaves for parent and child nodes'() {
        given: 'Two data nodes'
            def sourceDataNode  = [new DataNode(xpath: '/parent', leaves: ['parent-leaf': 'parent-payload'], childDataNodes: [new DataNode(xpath: '/parent/child', leaves: ['child-leaf': 'child-payload'])])]
            def targetDataNode  = [new DataNode(xpath: '/parent', leaves: ['parent-leaf': 'parent-payload-updated'], childDataNodes: [new DataNode(xpath: '/parent/child', leaves: ['child-leaf': 'child-payload-updated'])])]
        when: 'attempt to get delta between 2 data nodes'
            def result = objectUnderTest.getDeltaReports(sourceDataNode, targetDataNode)
        then: 'the delta report contains expected details for parent node'
            assert result[0].action.equals('replace')
            assert result[0].xpath == '/parent'
            assert result[0].sourceData == ['parent-leaf': 'parent-payload']
            assert result[0].targetData == ['parent-leaf': 'parent-payload-updated']
        and: 'the delta report contains expected details for child node'
            assert result[1].action.equals('replace')
            assert result[1].xpath == '/parent/child'
            assert result[1].sourceData == ['child-leaf': 'child-payload']
            assert result[1].targetData == ['child-leaf': 'child-payload-updated']
    }

    def 'Delta report between leaves, #scenario'() {
        when: 'attempt to get delta between 2 data nodes'
            def result = objectUnderTest.getDeltaReports(sourceDataNode, targetDataNode)
        then: 'the delta report contains expected "replace" action'
            assert result[0].action.equals('replace')
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
        then: 'the delta report is empty'
            assert result.isEmpty()
    }
}
