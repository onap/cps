package org.onap.cps.ncmp.utils

import org.onap.cps.ncmp.api.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.exceptions.NoAlternateIdParentFoundException
import org.onap.cps.spi.exceptions.DataNodeNotFoundException
import org.onap.cps.spi.model.DataNode
import spock.lang.Specification

class AlternateIdMatcherSpec extends Specification {

    def mockInventoryPersistence = Mock(InventoryPersistence)
    def dataNodeFoundException = new DataNodeNotFoundException('', '')
    def objectUnderTest = new AlternateIdMatcher(mockInventoryPersistence)

    def 'Find cm handle parent data node using alternate ids'() {
        given: 'cm handle in the registry with alternateId /a/b'
            mockInventoryPersistence.getCmHandleDataNodeByAlternateId(alternateId) >> new DataNode()
        expect: 'querying for alternate id a matching result found'
            assert objectUnderTest.getCmHandleDataNodeByLongestMatchAlternateId(alternateId, '/') != null
        where: 'the following parameters are used'
            scenario                              | alternateId
            'exact match'                         | '/a/b'
            'exact match with trailing separator' | '/a/b/'
            'child match'                         | '/a/b/c'
    }

    def 'Find cm handle parent data node using alternate id mismatch'() {
        given: 'cm handle not found in the registry with a given alternate id'
            mockInventoryPersistence.getCmHandleDataNodeByAlternateId(_) >> {throw dataNodeFoundException}
        when: 'attempt to find alternateId'
            objectUnderTest.getCmHandleDataNodeByLongestMatchAlternateId('/a', '/')
        then: 'no alternate id found exception thrown'
            def thrown = thrown(NoAlternateIdParentFoundException)
        and: 'the exception has the relevant details from the error response'
            assert thrown.message == 'No matching (parent) cm handle found using alternate ids'
            assert thrown.details == 'cannot find a datanode with alternate id /a'
    }
}
