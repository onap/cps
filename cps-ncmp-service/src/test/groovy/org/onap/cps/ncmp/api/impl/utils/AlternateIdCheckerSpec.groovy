/*
 * ============LICENSE_START========================================================
 * Copyright (c) 2024 Nordix Foundation.
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

package org.onap.cps.ncmp.api.impl.utils


import org.onap.cps.ncmp.api.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import org.onap.cps.spi.exceptions.DataNodeNotFoundException
import org.onap.cps.spi.model.DataNode
import org.onap.cps.spi.model.DataNodeBuilder
import spock.lang.Specification

class AlternateIdCheckerSpec extends Specification {

    def mockInventoryPersistenceService = Mock(InventoryPersistence)
    def someDataNode = new DataNodeBuilder().build()
    def dataNodeFoundException = new DataNodeNotFoundException('', '')

    def objectUnderTest = new AlternateIdChecker(mockInventoryPersistenceService)

    def 'Check new cm handle with new alternate id.'() {
        given: 'inventory persistence can not find cm handle id'
            mockInventoryPersistenceService.getYangModelCmHandle('ch 1') >> {throw dataNodeFoundException}
        and: 'inventory persistence can not find alternate id'
            mockInventoryPersistenceService.getCmHandleDataNodeByAlternateId('alternate id') >> {throw dataNodeFoundException}
        expect: 'mapping can be added'
             assert objectUnderTest.canApplyAlternateId('ch 1', 'alternate id')
    }

    def 'Check new cm handle with used alternate id.'() {
        given: 'inventory persistence can not find cm handle id'
            mockInventoryPersistenceService.getYangModelCmHandle('ch 1') >> {throw dataNodeFoundException}
        and: 'inventory persistence can find alternate id'
            mockInventoryPersistenceService.getCmHandleDataNodeByAlternateId('alternate id') >> { someDataNode }
        expect: 'mapping can not be added'
            assert objectUnderTest.canApplyAlternateId('ch 1', 'alternate id') == false
    }

    def 'Check for existing cm handle with #currentAlternateId.'() {
        given: 'a cm handle with the #currentAlternateId'
            def yangModelCmHandle = new YangModelCmHandle(alternateId: currentAlternateId)
        and: 'inventory service finds the cm handle'
            mockInventoryPersistenceService.getYangModelCmHandle('my cm handle') >> yangModelCmHandle
        expect: 'add mapping returns expected result'
            assert canAdd == objectUnderTest.canApplyAlternateId('my cm handle', 'same alternate id')
        where: 'following alternate ids is used'
            currentAlternateId   || canAdd
            'same alternate id'  || true
            'other alternate id' || false
    }

    def 'Check a batch of NEW cm handles with #scenario.'() {
        given: 'a batch of 2 new cm handles alternate id ids #alt1 and #alt2'
            def batch = [new NcmpServiceCmHandle(cmHandleId: 'ch-1', alternateId: alt1),
                         new NcmpServiceCmHandle(cmHandleId: 'ch-2', alternateId: alt2)]
        and: 'the database already contains cm handle(s) with these alternate ids: #alreadyinDb'
            mockInventoryPersistenceService.getCmHandleDataNodeByAlternateId(_) >>
                {  args -> altAlreadyInDb.contains(args[0]) ? new DataNode() : throwDataNodeNotFoundException() }
        when: 'the batch of new cm handles is checked'
            def result = objectUnderTest.getIdsOfCmHandlesWithAcceptableAlternateId(batch)
        then: 'the result only contains the ids of the acceptable cm handles'
            assert result.contains('ch-1') == acceptCh1
            assert result.contains('ch-2') == acceptCh2
        where: 'the following alternate ids are used'
            scenario                          | alt1   | alt2   | altAlreadyInDb  || acceptCh1 | acceptCh2
            'no alternate ids'                | ''     | ''     | ['dont matter'] || true      | true
            'new alternate ids'               | 'fdn1' | 'fdn2' | ['other fdn']   || true      | true
            'one already used alternate id'   | 'fdn1' | 'fdn2' | ['fdn1']        || false     | true
            'two already used alternate ids'  | 'fdn1' | 'fdn2' | ['fdn1','fdn2'] || false     | false
            'duplicate alternate id in batch' | 'fdn1' | 'fdn1' | ['dont matter'] || true      | false
    }

    def throwDataNodeNotFoundException() {
        // cannot 'return' an exception in conditional stub behavior, so hence a method call that will always throw this exception
        throw dataNodeFoundException
    }

}
