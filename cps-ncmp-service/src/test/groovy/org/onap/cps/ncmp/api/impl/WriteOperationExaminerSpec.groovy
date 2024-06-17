
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
        mockAlternateIdMatcher.getCmHandleDataNodeByLongestMatchingAlternateId('fdn2', '/') >> ch2
        mockAlternateIdMatcher.getCmHandleDataNodeByLongestMatchingAlternateId('fdn3', '/') >> ch3
        mockAlternateIdMatcher.getCmHandleDataNodeByLongestMatchingAlternateId('fdn4', '/') >> ch4
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
            def dmiWriteOperationsPerProducerKey = objectUnderTest.splitDmiWriteOperationsFromRequest('some id', dataJobWriteRequest)
        then: 'we get the expected number of keys and values.'
            def producerKeysAsStrings = []
            dmiWriteOperationsPerProducerKey.keySet().each {
                producerKeysAsStrings.add(it.toString())
            }
            assert producerKeysAsStrings.size() == expectedKeys.size()
            assert expectedKeys.containsAll(producerKeysAsStrings)
        where:
            writeOperationFdns               || expectedKeys
            ['fdn1']                         || ['dmiA#p1']
            ['fdn1','fdn1']                  || ['dmiA#p1']
            ['fdn1','fdn2']                  || ['dmiA#p1']
            ['fdn1','fdn3']                  || ['dmiA#p1', 'dmiA#p2']
            ['fdn1','fdn4']                  || ['dmiA#p1', 'dmiB#p1']
            ['fdn1', 'fdn2', 'fdn3', 'fdn4'] || ['dmiA#p1', 'dmiA#p2', 'dmiB#p1']
            ['fdn2', 'fdn3', 'fdn4', 'fdn1'] || ['dmiA#p1', 'dmiA#p2', 'dmiB#p1']
    }

    def 'Validate the ordering of the created sub jobs.'() {
        given: 'a few write operations for the same producer'
            def writeOperations = []
            (1..3).each {
                writeOperations.add(new WriteOperation('fdn1', '', it as String, null))
            }
        and: 'operation is wrapped in a write request'
            def dataJobWriteRequest = new DataJobWriteRequest(writeOperations)
        when: 'the DMI write operations are split from the request'
            def dmiWriteOperations = objectUnderTest.splitDmiWriteOperationsFromRequest('some id', dataJobWriteRequest).iterator().next().value
        then: 'we get the operation ids in the expected order.'
            assert dmiWriteOperations.operationId == ['1', '2', '3']
    }
}