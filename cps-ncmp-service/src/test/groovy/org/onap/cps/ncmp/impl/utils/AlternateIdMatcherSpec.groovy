/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 Nordix Foundation
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
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.utils.CpsValidatorImpl
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@SpringBootTest
@ContextConfiguration(classes = [InventoryPersistence])
class AlternateIdMatcherSpec extends Specification {

    def mockInventoryPersistence = Mock(InventoryPersistence)

    def objectUnderTest = new AlternateIdMatcher(mockInventoryPersistence, new CpsValidatorImpl())

    def setup() {
        given: 'cm handle in the registry with alternate id /a/b'
            mockInventoryPersistence.getYangModelCmHandleByAlternateId('/a/b') >> new YangModelCmHandle()
        and: 'no other cm handle'
            mockInventoryPersistence.getYangModelCmHandleByAlternateId(_) >> { throw new CmHandleNotFoundException('') }
    }

    def 'Finding longest alternate id matches.'() {
        expect: 'querying for alternate id a matching result found'
            assert objectUnderTest.getYangModelCmHandleByLongestMatchingAlternateId(targetAlternateId, '/') != null
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
            objectUnderTest.getYangModelCmHandleByLongestMatchingAlternateId(targetAlternateId, '/')
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

    def 'Get cm handle id from a cm handle reference that is a #scenario id.' () {
        given: 'inventory persistence service confirms the reference exists as an id or not (#isExistingCmHandleId)'
            mockInventoryPersistence.isExistingCmHandleId(cmHandleReference) >> isExistingCmHandleId
        when: 'getting a cm handle id from the reference'
            def result = objectUnderTest.getCmHandleId(cmHandleReference)
        then: 'a call to find the cm handle by alternate id is only made when needed'
            if (isExistingCmHandleId) {
                0 * mockInventoryPersistence.getYangModelCmHandleByAlternateId(*_)
            } else {
                1 * mockInventoryPersistence.getYangModelCmHandleByAlternateId(cmHandleReference) >> new YangModelCmHandle(id: 'ch-id-2')
            }
        and: 'the expected cm handle id is returned'
            assert result == expectedCmHandleId
        where: 'the following parameters are used'
            scenario    | cmHandleReference | isExistingCmHandleId || expectedCmHandleId
            'standard'  | 'ch-id-1'         | true                 || 'ch-id-1'
            'alternate' | 'alt-id=1'        | false                || 'ch-id-2'
    }
}
