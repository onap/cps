package org.onap.cps.ncmp.impl.datajobs

import org.onap.cps.ncmp.api.datajobs.models.DataJobWriteRequest
import org.onap.cps.ncmp.api.datajobs.models.WriteOperation
import org.onap.cps.ncmp.utils.AlternateIdMatcher
import org.onap.cps.spi.model.DataNode
import spock.lang.Specification

class WriteRequestExaminerSpec extends Specification {

    def mockAlternateIdMatcher = Mock(AlternateIdMatcher)
    def objectUnderTest = new WriteRequestExaminer(mockAlternateIdMatcher)

    def setup() {
        def ch1 = new DataNode(leaves: [id: 'ch1', 'dmi-service-name': 'dmiA', 'data-producer-identifier': 'p1'])
        def ch2 = new DataNode(leaves: [id: 'ch2', 'dmi-service-name': 'dmiA', 'data-producer-identifier': 'p1'])
        def ch3 = new DataNode(leaves: [id: 'ch3', 'dmi-service-name': 'dmiA', 'data-producer-identifier': 'p2'])
        def ch4 = new DataNode(leaves: [id: 'ch4', 'dmi-service-name': 'dmiB', 'data-producer-identifier': 'p1'])
        mockAlternateIdMatcher.getCmHandleDataNodeByLongestMatchingAlternateId('fdn1', '/') >> ch1
    }

    def 'Create a map of dmi write requests per producer key.'() {
        given: 'write request'
            def writeOperations = []
            writeOperationFdns.each {
                writeOperations.add(new WriteOperation(it, '', '', null))
            }
        and: 'operation is wrapped in a write request'
            def dataJobWriteRequest = new DataJobWriteRequest(writeOperations)
        when: 'the DMI write operations are split from the request'
            def dmi3ggpWriteOperationsPerProducerKey = objectUnderTest.splitDmiWriteOperationsFromRequest('some id', dataJobWriteRequest)
        then: 'we get a map of 3gpp write operations per producer key'
            def producerKeys = dmi3ggpWriteOperationsPerProducerKey.keySet()
            assert producerKeys == expectedKeys as Set
        where:
            writeOperationFdns || expectedNumberOfProducerKeys || expectedKeys
            ['fdn1']           || 1                            || ["dmiA#p1"]
            ['fdn1','fdn1']    || 1                            || ["dmiA#p1"]
    }
}
