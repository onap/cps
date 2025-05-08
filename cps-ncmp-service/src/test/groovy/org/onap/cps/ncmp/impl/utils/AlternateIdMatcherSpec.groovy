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
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import spock.lang.Specification

class AlternateIdMatcherSpec extends Specification {

    def mockCmHandleIdPerAlternateId = Mock(IMap)

    def objectUnderTest = new AlternateIdMatcher(mockCmHandleIdPerAlternateId)

    def testYangModelCmHandle = new YangModelCmHandle(id:1)

    def 'Finding longest alternate id matches, scenario: #scenario.'() {
        given: ' a match for alternate id "/a/b"'
            mockCmHandleIdPerAlternateId.get('/a/b') >> 'ch1'
        expect: 'a match has been found'
            assert objectUnderTest.getCmHandleIdByLongestMatchingAlternateId(targetAlternateId, '/') != null
        where: 'the following alternate ids are used'
            scenario                                                   | targetAlternateId
            'exact match'                                              | '/a/b'
            'parent match'                                             | '/a/b/c'
            'grand parent match'                                       | '/a/b/c/d'
            'trailing separator match'                                 | '/a/b/'
            'with attribute path component and exact match'            | '/a/b#q'
            'with attribute path component and parent match'           | '/a/b/c#q'
            'with attribute path component and grand parent match'     | '/a/b/c/d#q'
            'with attribute path component and additional slash match' | '/a/b/#q'
    }

    def 'Finding longest alternate id matches for a batch.'() {
        given: 'a batch of alternate ids'
            def aBatchOfAlternateIds = ['content does','not matter']
        and: 'the cached map returns a map of some matches'
            mockCmHandleIdPerAlternateId.getAll(_) >> [fdn1:'ch1', fdn2:'ch2']
        when: 'getting the matches alternate ids for the batch'
            def result = objectUnderTest.getCmHandleIdsByLongestMatchingAlternateIds(aBatchOfAlternateIds, '/')
        then: 'the result are the ids (values) from the cached map'
            assert result == ['ch1', 'ch2']
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
            'no root'                  | 'c'
    }

    def 'Find cm handle with longest match using pre-loaded map, scenario: #scenario.'() {
        given: 'preloaded map with one yang model cm handle and its alternate id'
            def cmHandlePerAlternateId = ['/a/b': testYangModelCmHandle]
        when: 'getting the best matching yang model cm handle'
            def result = objectUnderTest.getCmHandleByLongestMatchingAlternateId(targetAlternateId, '/', cmHandlePerAlternateId)
        then: 'the correct yang model cm handle is found'
            assert result == testYangModelCmHandle
        where: 'the following alternate ids are used'
            scenario                                                   | targetAlternateId
            'exact match'                                              | '/a/b'
            'parent match'                                             | '/a/b/c'
            'grand parent match'                                       | '/a/b/c/d'
            'trailing separator match'                                 | '/a/b/'
            'with attribute path component and exact match'            | '/a/b#q'
            'with attribute path component and parent match'           | '/a/b/c#q'
            'with attribute path component and grand parent match'     | '/a/b/c/d#q'
            'with attribute path component and additional slash match' | '/a/b/#q'
    }

    def 'Attempt to find cm handle with longest match using pre-loaded map without any matches.'() {
        given: 'preloaded map with one yang model cm handle and its alternate id'
            def cmHandlePerAlternateId = ['/a/b': testYangModelCmHandle]
        when: 'attempt to find yang model cm handle'
            objectUnderTest.getCmHandleByLongestMatchingAlternateId(targetAlternateId, '/', cmHandlePerAlternateId)
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
        when: 'getting a cm handle id from the reference'
            objectUnderTest.getCmHandleId('nonExistingId')
        then: 'an exception is thrown'
            def thrownException = thrown(CmHandleNotFoundException)
            assert thrownException.getMessage().contains('Cm handle not found')
    }
}
