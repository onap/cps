/*
 * ============LICENSE_START========================================================
 * Copyright (c) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
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

package org.onap.cps.ncmp.impl.inventory

import com.hazelcast.map.IMap
import java.util.stream.Collectors
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.api.exceptions.DataNodeNotFoundException
import spock.lang.Specification

class AlternateIdCheckerSpec extends Specification {

    def mockInventoryPersistenceService = Mock(InventoryPersistence)
    def mockCmHandleIdPerAlternateId = Mock(IMap)

    def objectUnderTest = new AlternateIdChecker(mockInventoryPersistenceService, mockCmHandleIdPerAlternateId)

    def 'Check a batch of created cm handles with #scenario.'() {
        given: 'a batch of 2 new cm handles with alternate ids #alt1 and #alt2'
            def batch = [new NcmpServiceCmHandle(cmHandleId: 'ch-1', alternateId: alt1),
                         new NcmpServiceCmHandle(cmHandleId: 'ch-2', alternateId: alt2)]
        and: 'the alternate ID map already contains cm handle(s) with these alternate ids: #altAlreadyInDb'
            mockCmHandleIdPerAlternateId.getAll(_) >>
                { args -> args[0].stream().filter(altId -> altAlreadyInDb.contains(altId)).collect(Collectors.toMap(altId->altId, altId->'dont matter')) }
        when: 'the batch of new cm handles is checked'
            def result = objectUnderTest.getIdsOfCmHandlesWithRejectedAlternateId(batch, AlternateIdChecker.Operation.CREATE)
        then: 'the result contains ids of the rejected cm handles'
            assert result == expectedRejectedCmHandleIds
        where: 'the following alternate ids are used'
            scenario                          | alt1   | alt2   | altAlreadyInDb   || expectedRejectedCmHandleIds
            'blank alternate ids'             | ''     | ''     | ['dont matter']  || []
            'null alternate ids'              | null   | null   | ['dont matter']  || []
            'new alternate ids'               | 'fdn1' | 'fdn2' | ['other fdn']    || []
            'one already used alternate id'   | 'fdn1' | 'fdn2' | ['fdn1']         || ['ch-1']
            'two already used alternate ids'  | 'fdn1' | 'fdn2' | ['fdn1', 'fdn2'] || ['ch-1', 'ch-2']
            'duplicate alternate id in batch' | 'fdn1' | 'fdn1' | ['dont matter']  || ['ch-2']
    }

    def 'Check a batch of updates to existing cm handles with #scenario.'() {
        given: 'a batch of 1 existing cm handle to update alternate id to #proposedAlt'
            def batch = [new NcmpServiceCmHandle(cmHandleId: 'ch-1', alternateId: proposedAlt)]
        and: 'the alternate ID map already contains cm handle(s) with these alternate ids: #altAlreadyInDb'
            mockCmHandleIdPerAlternateId.getAll(_) >>
                { args -> args[0].stream().filter(altId -> altAlreadyInDb == altId).collect(Collectors.toMap(altId->altId, altId->'dont matter')) }
        and: 'the database already contains a cm handle with alternate id: #altAlreadyInDb'
            mockInventoryPersistenceService.getYangModelCmHandle(_) >> new YangModelCmHandle(alternateId: altAlreadyInDb)
        when: 'the batch of cm handle updates is checked'
            def result = objectUnderTest.getIdsOfCmHandlesWithRejectedAlternateId(batch, AlternateIdChecker.Operation.UPDATE)
        then: 'the result contains ids of the rejected cm handles'
            assert result == expectedRejectedCmHandleIds
        where: 'the following parameters are used'
            scenario                      | proposedAlt | altAlreadyInDb || expectedRejectedCmHandleIds
            'no alternate id'             | 'fdn1'      | ''             || []
            'used the same alternate id'  | 'fdn1'      | 'fdn1'         || []
            'used different alternate id' | 'otherFdn'  | 'fdn1'         || ['ch-1']
    }

    def 'Check update of non-existing cm handle.'() {
        given: 'a batch of 1 non-existing cm handle to update alternate id'
            def batch = [new NcmpServiceCmHandle(cmHandleId: 'non-existing', alternateId: 'altId')]
        and: 'the alternate ID map returns some cm handle ids per alternate id '
            mockCmHandleIdPerAlternateId.getAll(_) >> [:]
        and: 'the database does not contain any cm handles'
            mockInventoryPersistenceService.getYangModelCmHandle(_) >> { throwDataNodeNotFoundException() }
        when: 'the batch of cm handle updates is checked'
            def result = objectUnderTest.getIdsOfCmHandlesWithRejectedAlternateId(batch, AlternateIdChecker.Operation.UPDATE)
        then: 'the result has no rejected cm handles'
            assert result.empty
    }

    static throwDataNodeNotFoundException() {
        throw new DataNodeNotFoundException('', '')
    }

}
