/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

import com.hazelcast.map.IMap
import org.onap.cps.ncmp.api.exceptions.CmHandleNotFoundException
import org.onap.cps.ncmp.exceptions.NoAlternateIdMatchFoundException
import spock.lang.Specification

class AlternateIdMatcherSpec extends Specification {

    def mockCmHandleIdPerAlternateId = Mock(IMap)

    def objectUnderTest = new AlternateIdMatcher(mockCmHandleIdPerAlternateId)

    def 'Finding longest alternate id matches.'() {
        given:
            mockCmHandleIdPerAlternateId.get('/a/b') >> 'ch1'
        expect: 'querying for alternate id a matching result found'
            assert objectUnderTest.getCmHandleIdByLongestMatchingAlternateId(targetAlternateId, '/') != null
        where: 'the following parameters are used'
            scenario                             | targetAlternateId
            'exact match'                        | '/a/b'
            'parent match'                       | '/a/b/c'
            'grand parent match'                 | '/a/b/c/d'
            'trailing separator match'           | '/a/b/'
            'trailing hash'                      | '/a/b#q'
            'trailing hash parent match'         | '/a/b/c#q'
            'trailing hash grand parent match'   | '/a/b/c/d#q'
            'trailing separator then hash match' | '/a/b/#q'
    }

    def 'Attempt to find longest alternate id match without any matches.'() {
        when: 'attempt to find alternateId'
            objectUnderTest.getCmHandleIdByLongestMatchingAlternateId(targetAlternateId, '/')
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
        given: 'cmHandleIdPerAlternateId cache contains the given reference'
            mockCmHandleIdPerAlternateId.get(cmHandleReference) >> returnedCacheValue
            mockCmHandleIdPerAlternateId.containsValue(cmHandleReference) >> true
        when: 'getting a cm handle id from the reference'
            def result = objectUnderTest.getCmHandleId(cmHandleReference)
        then: 'the expected cm handle id is returned'
            assert result == expectedResult
        where: 'the following parameters are used'
            scenario    | cmHandleReference| returnedCacheValue|| expectedResult
            'standard'  | 'ch-id-1'        | null              || 'ch-id-1'
            'alternate' | 'alt-id=1'       | 'ch-id-2'         || 'ch-id-2'
    }

    def 'Get cm handle id when given reference DOES NOT exist in cache.'() {
        given: 'cmHandleIdPerAlternateId cache returns null'
            mockCmHandleIdPerAlternateId.get('nonExistingId') >> null
        when: 'getting a cm handle id from the reference'
            objectUnderTest.getCmHandleId('nonExistingId')
        then: 'an exception is thrown'
            def thrownException = thrown(CmHandleNotFoundException)
            assert thrownException.getMessage().contains('Cm handle not found')
    }
}