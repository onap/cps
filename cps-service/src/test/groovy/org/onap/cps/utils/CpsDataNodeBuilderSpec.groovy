package org.onap.cps.utils

import org.onap.cps.TestUtils
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import spock.lang.Specification

class CpsDataNodeBuilderSpec extends Specification {

    def 'Converting Normalized Node (tree) to a DataNode (tree).'() {
        given: 'a Yang module'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('bookstore.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent)getSchemaContext()
        and: 'a normalized node for that model'
            def jsonData = TestUtils.getResourceFileContent('bookstore.json')
            def normalizedNode = YangUtils.parseJsonData(jsonData, schemaContext)
        when: 'the normalized node is converted to a DataNode (tree)'
            def result = CpsDataNodeBuilder.createDataNodeTreeFromNormalizedNode(normalizedNode)
        then: 'the system creates a (root) fragment without a parent and 2 children (categories)'
            result.childDataNodes.size() == 2
        and: 'each child (category) has the root fragment (result) as parent and in turn as 1 child (a list of books)'
            result.childDataNodes.each { it.childDataNodes.size() == 1 }
        and: 'the fragments have the correct xpaths'
            assert result.xpath == '/bookstore'
            assert result.childDataNodes.collect { it.xpath }
                    .containsAll(["/bookstore/categories[@code='01']", "/bookstore/categories[@code='02']"])
    }
}
