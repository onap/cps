package org.onap.cps.utils

import com.hazelcast.map.IMap
import org.onap.cps.TestUtils
import org.onap.cps.api.CpsAdminService
import org.onap.cps.api.impl.YangTextSchemaSourceSetCache
import org.onap.cps.cache.AnchorDataCacheEntry
import org.onap.cps.spi.model.Anchor
import org.onap.cps.spi.model.DataNode
import org.onap.cps.yang.YangTextSchemaSourceSet
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import spock.lang.Specification

class PrefixResolverSpec extends Specification {

    def mockCpsAdminService = Mock(CpsAdminService)

    def mockYangTextSchemaSourceSetCache = Mock(YangTextSchemaSourceSetCache)

    def mockAnchorDataCache = Mock(IMap<String, AnchorDataCacheEntry>)

    def objectUnderTest = new PrefixResolver(mockCpsAdminService, mockYangTextSchemaSourceSetCache, mockAnchorDataCache)

    def 'get prefix for a data node using its schema context'() {
        given: 'a data node form the test-tree model'
            def dataNode = new DataNode(dataspace: 'some dataspace', anchorName: 'someAnchor' , xpath: xpath)
        and: 'the system can resolve any anchor'
            mockCpsAdminService.getAnchor(*_) >> new Anchor()
        and: 'the system cache contains the schema context for the tre-tree module'
            def mockYangTextSchemaSourceSet = Mock(YangTextSchemaSourceSet)
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('test-tree.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
            mockYangTextSchemaSourceSetCache.get(*_) >> mockYangTextSchemaSourceSet
            mockYangTextSchemaSourceSet.getSchemaContext() >> schemaContext
        expect: ' the prefix of the yang module'
            assert objectUnderTest.getPrefix(dataNode) == expectedPrefix
        where:
            xpath                         || expectedPrefix
            '/test-tree'                  || 'tree'
            '/test-tree/with/descendants' || 'tree'
            '/test-tree[@id=1]'           || 'tree'
            '/not-defined'                || ''
            'invalid'                     || ''
    }

}
