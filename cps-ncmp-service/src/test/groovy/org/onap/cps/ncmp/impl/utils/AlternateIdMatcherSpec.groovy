/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the 'License');
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.impl.utils

import org.onap.cps.ncmp.api.exceptions.CmHandleNotFoundException
import org.onap.cps.ncmp.exceptions.NoAlternateIdMatchFoundException
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.onap.cps.spi.model.DataNode
import spock.lang.Specification

class AlternateIdMatcherSpec extends Specification {

    def mockInventoryPersistence = Mock(InventoryPersistence)
    def objectUnderTest = new AlternateIdMatcher(mockInventoryPersistence)

    def setup() {
        given: 'cm handle in the registry with alternate id /a/b'
            mockInventoryPersistence.getCmHandleDataNodeByAlternateId('/a/b') >> new DataNode()
        and: 'no other cm handle'
            mockInventoryPersistence.getCmHandleDataNodeByAlternateId(_) >> { throw new CmHandleNotFoundException('') }
    }

    def 'Finding longest alternate id matches.'() {
        expect: 'querying for alternate id a matching result found'
            assert objectUnderTest.getCmHandleDataNodeByLongestMatchingAlternateId(targetAlternateId, '/') != null
        where: 'the following parameters are used'
            scenario                                | targetAlternateId
            'exact match'                           | '/a/b'
            'parent match'                          | '/a/b/c'
            'grand parent match'                    | '/a/b/c/d'
            'trailing separator match'              | '/a/b/'
            'trailing hash'                         | '/a/b#q'
            'trailing hash parent match'            | '/a/b/c#q'
            'trailing hash grand parent match'      | '/a/b/c/d#q'
            'trailing separator then hash match'    | '/a/b/#q'
    }

    def 'Attempt to find longest alternate id match without any matches.'() {
        when: 'attempt to find alternateId'
            objectUnderTest.getCmHandleDataNodeByLongestMatchingAlternateId(targetAlternateId, '/')
        then: 'no alternate id match found exception thrown'
            def thrown = thrown(NoAlternateIdMatchFoundException)
        and: 'the exception has the relevant details from the error response'
            assert thrown.message == 'No matching cm handle found using alternate ids'
            assert thrown.details == 'cannot find a datanode with alternate id ' + targetAlternateId
        where: 'the following parameters are used'
            scenario                   | targetAlternateId
            'no match for parent only' | '/a'
            'no match for other child' | '/a/c'
            'no match at all'          | '/x/y'
    }

    def 'Get cmHandle id from passed cmHandleReference (cmHandleId scenario)' () {
        when: 'a cmHandleCmReference is passed in'
            def result = objectUnderTest.getCmHandleId(cmHandleReference)
        then: 'the inventory persistence service returns a cm handle (or not)'
            mockInventoryPersistence.isExistingCmHandleId(cmHandleReference) >> existingCmHandleIdResponse
            mockInventoryPersistence.getCmHandleDataNodeByAlternateId(cmHandleReference) >> alternateIdGetResponse
        and: 'correct result is returned'
            assert result == cmHandleReference
        where:
            cmHandleReference | existingCmHandleIdResponse | alternateIdGetResponse
            'ch-1'            |  true                      |  ''
            'alt-1'           |  false                     |  new DataNode(leaves: [id:'alt-1'])
    }
}
