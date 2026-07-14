/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.impl.cache

import com.hazelcast.map.IMap
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import spock.lang.Specification

class CmHandleIdPerReferenceMapSpec extends Specification {

    def mockIMap = Mock(IMap)

    def objectUnderTest = new CmHandleIdPerReferenceMap(mockIMap)

    def 'Adding cm handles with alternate id.'() {
        given: 'a cm handle with an alternate id'
            def yangModelCmHandle = new YangModelCmHandle(id: 'ch-1', alternateId: 'alt-1')
        when: 'the cm handle is added to the map'
            objectUnderTest.putAll([yangModelCmHandle])
        then: 'both cmHandleId and alternateId entries are stored'
            1 * mockIMap.putAll(['ch-1': 'ch-1', 'alt-1': 'ch-1'])
    }

    def 'Adding cm handles without alternate id.'() {
        given: 'a cm handle without an alternate id'
            def yangModelCmHandle = new YangModelCmHandle(id: 'ch-1', alternateId: alternateId)
        when: 'the cm handle is added to the map'
            objectUnderTest.putAll([yangModelCmHandle])
        then: 'only the cmHandleId entry is stored'
            1 * mockIMap.putAll(['ch-1': 'ch-1'])
        where: 'the following alternate ids are used'
            scenario | alternateId
            'null'   | null
            'empty'  | ''
            'blank'  | '   '
    }

    def 'Adding multiple cm handles in a batch.'() {
        given: 'multiple cm handles with and without alternate ids'
            def cmHandle1 = new YangModelCmHandle(id: 'ch-1', alternateId: 'alt-1')
            def cmHandle2 = new YangModelCmHandle(id: 'ch-2', alternateId: '')
            def cmHandle3 = new YangModelCmHandle(id: 'ch-3', alternateId: 'alt-3')
        when: 'all cm handles are added to the map'
            objectUnderTest.putAll([cmHandle1, cmHandle2, cmHandle3])
        then: 'all expected entries are stored in a single putAll call'
            1 * mockIMap.putAll(['ch-1': 'ch-1', 'alt-1': 'ch-1', 'ch-2': 'ch-2', 'ch-3': 'ch-3', 'alt-3': 'ch-3'])
    }

    def 'Removing cm handles with alternate id.'() {
        given: 'a cm handle with an alternate id'
            def yangModelCmHandle = new YangModelCmHandle(id: 'ch-1', alternateId: 'alt-1')
        when: 'the cm handle is removed from the map'
            objectUnderTest.removeAll([yangModelCmHandle])
        then: 'both the cmHandleId and alternateId entries are deleted'
            1 * mockIMap.delete('ch-1')
            1 * mockIMap.delete('alt-1')
    }

    def 'Removing cm handles without alternate id.'() {
        given: 'a cm handle without an alternate id'
            def yangModelCmHandle = new YangModelCmHandle(id: 'ch-1', alternateId: alternateId)
        when: 'the cm handle is removed from the map'
            objectUnderTest.removeAll([yangModelCmHandle])
        then: 'only the cmHandleId entry is deleted'
            1 * mockIMap.delete('ch-1')
        and: 'no other delete calls are made'
            0 * mockIMap.delete(_)
        where: 'the following alternate ids are used'
            scenario | alternateId
            'null'   | null
            'empty'  | ''
            'blank'  | '   '
    }

    def 'Updating alternate id for a cm handle.'() {
        when: 'the alternate id is updated'
            objectUnderTest.updateAlternateId('ch-1', 'new-alt-1')
        then: 'the cmHandleId self-reference is set'
            1 * mockIMap.set('ch-1', 'ch-1')
        and: 'the new alternate id mapping is set'
            1 * mockIMap.set('new-alt-1', 'ch-1')
    }

    def 'Getting a cm handle id by reference.'() {
        given: 'the underlying map returns a value for a reference'
            mockIMap.get('some-reference') >> 'ch-1'
        when: 'getting by reference'
            def result = objectUnderTest.get('some-reference')
        then: 'the expected cm handle id is returned'
            assert result == 'ch-1'
    }

    def 'Getting a cm handle id for non-existing reference.'() {
        given: 'the underlying map returns null'
            mockIMap.get('unknown') >> null
        when: 'getting by unknown reference'
            def result = objectUnderTest.get('unknown')
        then: 'null is returned'
            assert result == null
    }

    def 'Getting all cm handle ids for a set of references.'() {
        given: 'the underlying map returns results for a set of references'
            def references = ['alt-1', 'ch-2'] as Set
            mockIMap.getAll(references) >> ['alt-1': 'ch-1', 'ch-2': 'ch-2']
        when: 'getting all by references'
            def result = objectUnderTest.getAll(references)
        then: 'the expected map is returned'
            assert result == ['alt-1': 'ch-1', 'ch-2': 'ch-2']
    }

    def 'Checking if a reference exists in the map.'() {
        given: 'the underlying map contains a key'
            mockIMap.containsKey('ch-1') >> true
            mockIMap.containsKey('unknown') >> false
        expect: 'containsKey returns correct results'
            assert objectUnderTest.containsKey('ch-1') == true
            assert objectUnderTest.containsKey('unknown') == false
    }

    def 'Searching by key pattern.'() {
        given: 'the underlying map returns values matching a predicate'
            mockIMap.values(_) >> ['ch-1', 'ch-2']
        when: 'searching by key like pattern'
            def result = objectUnderTest.getByKeyLike('Ireland')
        then: 'the matching values are returned'
            assert result.containsAll(['ch-1', 'ch-2'])
    }

    def 'Checking if the map is empty.'() {
        given: 'the underlying map reports its empty state'
            mockIMap.isEmpty() >> expectedResult
        expect: 'isEmpty returns the correct value'
            assert objectUnderTest.isEmpty() == expectedResult
        where: 'the following states are used'
            scenario    | expectedResult
            'empty'     | true
            'not empty' | false
    }

    def 'Getting the size of the map.'() {
        given: 'the underlying map has a specific size'
            mockIMap.size() >> 42
        when: 'getting the size'
            def result = objectUnderTest.size()
        then: 'the correct size is returned'
            assert result == 42
    }
}
