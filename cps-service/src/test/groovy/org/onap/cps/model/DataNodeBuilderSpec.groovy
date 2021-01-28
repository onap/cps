package org.onap.cps.model

import org.onap.cps.TestUtils
import org.onap.cps.spi.model.DataNode
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

    def 'Converting Normalized Node (tree) to a DataNode (tree).'() {
        given: 'a Yang module'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('test-tree.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent) getSchemaContext()
        and: 'a normalized node for that model'
            def jsonData = TestUtils.getResourceFileContent('test-tree.json')
            def normalizedNode = YangUtils.parseJsonData(jsonData, schemaContext)
        when: 'the normalized node is converted to a DataNode (tree)'
            def result = new DataNodeBuilder().withNormalizedNodeTree(normalizedNode).build()
            def mappedResult = treeToFlatMapByXpath(new HashMap<>(), result)
        then: '5 DataNode objects with unique xpath were created in total'
            mappedResult.size() == 5
        and: 'all expected xpaths were built'
            mappedResult.keySet().containsAll(expectedLeavesByXpathMap.keySet())
        and: 'each data node contains the expected attributes'
            mappedResult.each {
                xpath, dataNode -> assertLeavesMaps(dataNode.getLeaves(), expectedLeavesByXpathMap[xpath])
            }
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

    def treeToFlatMapByXpath(Map<String, DataNode> flatMap, DataNode dataNodeTree) {
        flatMap.put(dataNodeTree.getXpath(), dataNodeTree)
        dataNodeTree.getChildDataNodes()
                .forEach(childDataNode -> treeToFlatMapByXpath(flatMap, childDataNode))
        return flatMap
    }
}
