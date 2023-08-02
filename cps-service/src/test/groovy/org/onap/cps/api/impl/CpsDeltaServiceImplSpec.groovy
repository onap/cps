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

    def 'Get delta between data nodes for remove operation'() {
        when: 'attempt to get delta between 2 data nodes'
            def deltaReport = objectUnderTest.getDeltaReport(sourceDataNode as Collection<DataNode>, emptyDataNode)
        then: 'the expected data is present in the delta report'
            assert deltaReport.first().action.equals("remove")
            assert deltaReport.first().xpath == "/parent/child"
            assert deltaReport.first().sourceData == expectedPayload
        where: 'following data was used'
            scenario                      | sourceDataNode                   || expectedPayload
            'data node with leaf data'    | dataNodeWithLeafAndChildDataNode || ['child-leaf': 'child-payload']
            'data node without leaf data' | dataNodeWithChildDataNode        || null
    }

    def 'Get delta between data nodes for add operation'() {
        given: 'attempt to get delta between 2 data nodes'
            def deltaReport = objectUnderTest.getDeltaReport(emptyDataNode, targetDataNode)
        and: 'the expected data is present in the delta report'
            assert deltaReport.first().action.equals("add")
            assert deltaReport.first().xpath == "/parent/child"
            assert deltaReport.first().targetData == expectedPayload
        where: 'following data was used'
            scenario                      | targetDataNode                   || expectedPayload
            'data node with leaf data'    | dataNodeWithLeafAndChildDataNode || ['child-leaf': 'child-payload']
            'data node without leaf data' | dataNodeWithChildDataNode        || null
    }
}
