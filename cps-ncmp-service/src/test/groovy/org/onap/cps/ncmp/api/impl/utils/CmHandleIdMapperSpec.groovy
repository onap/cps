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

import org.onap.cps.ncmp.api.NetworkCmProxyCmHandleQueryService
import org.onap.cps.ncmp.api.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import org.onap.cps.spi.exceptions.DataNodeNotFoundException
import org.onap.cps.spi.model.DataNodeBuilder
import spock.lang.Specification

class CmHandleIdMapperSpec extends Specification {

    def alternateIdPerCmHandle = new HashMap<String, String>()
    def cmHandlePerAlternateId = new HashMap<String, String>()
    def mockCpsCmHandlerQueryService = Mock(NetworkCmProxyCmHandleQueryService)
    def mockInventoryPersistenceService = Mock(InventoryPersistence)
    def someDataNode = new DataNodeBuilder().build()
    def dataNodeFoundException = new DataNodeNotFoundException('', '')

    def objectUnderTest = new CmHandleIdMapper(alternateIdPerCmHandle, cmHandlePerAlternateId, mockCpsCmHandlerQueryService, mockInventoryPersistenceService)

    def 'Add mapping for new cm handle with new alternate id'() {
        given: 'inventory persistence can not find cm handle id'
            mockInventoryPersistenceService.getYangModelCmHandle('ch 1') >> {throw dataNodeFoundException}
        and: 'inventory persistence can not find alternate id'
            mockInventoryPersistenceService.getCmHandleDataNodeByAlternateId('alternate id') >> {throw dataNodeFoundException}
        expect: 'mapping can be added'
             assert objectUnderTest.addMapping('ch 1', 'alternate id')
    }

    def 'Add mapping for new cm handle with used alternate id'() {
        given: 'inventory persistence can not find cm handle id'
            mockInventoryPersistenceService.getYangModelCmHandle('ch 1') >> {throw dataNodeFoundException}
        and: 'inventory persistence can find alternate id'
            mockInventoryPersistenceService.getCmHandleDataNodeByAlternateId('alternate id') >> { someDataNode }
        expect: 'mapping can not be added'
            assert objectUnderTest.addMapping('ch 1', 'alternate id') == false
    }

    def 'Add mapping for cm handle with #currentAlternateId'() {
        given: 'a cm handle with the #currentAlternateId'
            def yangModelCmHandle = new YangModelCmHandle(alternateId: currentAlternateId)
        and: 'inventory service finds the cm handle'
            mockInventoryPersistenceService.getYangModelCmHandle('my cm handle') >> yangModelCmHandle
        expect: 'add mapping returns expected result'
            assert canAdd == objectUnderTest.addMapping('my cm handle', 'same alternate id')
        where: 'following alternate ids is used'
            currentAlternateId   ||  canAdd
            'same alternate id'  ||  true
            'other alternate id' ||  false
    }

    def 'Initializing the cache'() {
        given: 'cache is not initialized'
            objectUnderTest.cacheIsInitialized = false
        and: 'query service finds all cm handles'
            mockCpsCmHandlerQueryService.getAllCmHandles() >> [new NcmpServiceCmHandle(cmHandleId: 'ch-1', alternateId: 'alt-1')]
        and: 'inventory service finds the cm handle'
            mockInventoryPersistenceService.getYangModelCmHandle('ch-1') >> new YangModelCmHandle()
        when: 'cache is (re-)initialized'
            objectUnderTest.initializeCache()
        then: 'cache initialized'
            assert objectUnderTest.cacheIsInitialized == true
    }

}