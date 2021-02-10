/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
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

package org.onap.cps.model

import org.onap.cps.TestUtils
import org.onap.cps.spi.model.DataNodeBuilder
import org.onap.cps.utils.YangUtils
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import spock.lang.Specification

class DataNodeBuilderSpec extends Specification {

    Map<String, Map<String, Object>> expectedLeavesByXpathMap = [
            '/test-tree'                             : [],
            '/test-tree/branch[@name=\'Left\']'      : [name: 'Left'],
            '/test-tree/branch[@name=\'Left\']/nest' : [name: 'Small', birds: ['Sparrow', 'Robin', 'Finch']],
            '/test-tree/branch[@name=\'Right\']'     : [name: 'Right'],
            '/test-tree/branch[@name=\'Right\']/nest': [name: 'Big', birds: ['Owl', 'Raven', 'Crow']]
    ]

    def 'Converting NormalizedNode (tree) to a DataNode (tree).'() {
        given: 'the schema context for expected model'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('test-tree.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent) getSchemaContext()
        and: 'the json data parsed into normalized node object'
            def jsonData = TestUtils.getResourceFileContent('test-tree.json')
            def normalizedNode = YangUtils.parseJsonData(jsonData, schemaContext)
        when: 'the normalized node is converted to a data node'
            def result = new DataNodeBuilder().withNormalizedNodeTree(normalizedNode).build()
            def mappedResult = TestUtils.getFlattenMapByXpath(result)
        then: '5 DataNode objects with unique xpath were created in total'
            mappedResult.size() == 5
        and: 'all expected xpaths were built'
            mappedResult.keySet().containsAll(expectedLeavesByXpathMap.keySet())
        and: 'each data node contains the expected attributes'
            mappedResult.each {
                xpath, dataNode -> assertLeavesMaps(dataNode.getLeaves(), expectedLeavesByXpathMap[xpath])
            }
    }

    def 'Converting NormalizedNode (tree) to a DataNode (tree) for known parent node.'() {
        given: 'a schema context for expected model'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('test-tree.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent) getSchemaContext()
        and: 'the json data parsed into normalized node object'
            def jsonData = '{ "branch": [{ "name": "Branch", "nest": { "name": "Nest", "birds": ["bird"] } }] }'
            def normalizedNode = YangUtils.parseJsonData(jsonData, schemaContext, "/test-tree")
        when: 'the normalized node is converted to a data node with parent node xpath defined'
            def result = new DataNodeBuilder()
                    .withNormalizedNodeTree(normalizedNode)
                    .withParentNodeXpath("/test-tree")
                    .build()
            def mappedResult = TestUtils.getFlattenMapByXpath(result)
        then: '2 DataNode objects with unique xpath were created in total'
            mappedResult.size() == 2
        and: 'all expected xpaths were built'
            mappedResult.keySet()
                    .containsAll(['/test-tree/branch[@name=\'Branch\']', '/test-tree/branch[@name=\'Branch\']/nest'])
    }

    def static assertLeavesMaps(actualLeavesMap, expectedLeavesMap) {
        expectedLeavesMap.each { key, value ->
            {
                def actualValue = actualLeavesMap[key]
                if (value instanceof Collection<?> && actualValue instanceof Collection<?>) {
                    assert value.size() == actualValue.size()
                    assert value.containsAll(actualValue)
                } else {
                    assert value == actualValue
                }
            }
        }
    }

}
