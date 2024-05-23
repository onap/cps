package org.onap.cps.ncmp.utils

import org.onap.cps.ncmp.api.impl.inventory.CmHandleQueries
import org.onap.cps.ncmp.api.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.exceptions.NoAlternateIdParentFoundException
import org.onap.cps.spi.exceptions.DataNodeNotFoundException
import org.onap.cps.spi.model.DataNode
import spock.lang.Specification

import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS

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

    def 'Find cm handle parent data node using alternate ids mismatches'() {
        given: 'cm handle in the registry with alternateId'
            mockInventoryPersistence.getCmHandleDataNodeByAlternateId(alternateId) >> {throw dataNodeFoundException}
        when: 'attempt to find alternateId'
            objectUnderTest.getCmHandleDataNodeByLongestMatchAlternateId(alternateId, '/')
        then: 'no alternate id found exception thrown'
            def thrown = thrown(NoAlternateIdParentFoundException)
        and: 'the exception has the relevant details from the error response'
            assert thrown.message == 'No matching (parent) cm handle found using alternate ids'
            assert thrown.details == 'cannot find a datanode with alternate id ' + alternateId
        where: 'the following parameters are used'
            scenario                              | alternateId | cpsPath
            'no match for parent only'            | '/a'        | '/a/b'
            'no match at all'                     | '/x/y/z'    | '/a/b'
            'no match with trailing separator'    | '/c/d/'     | '/c/d'
    }
}
