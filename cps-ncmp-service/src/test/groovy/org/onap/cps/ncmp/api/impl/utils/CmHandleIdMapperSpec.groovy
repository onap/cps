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
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import spock.lang.Specification

class CmHandleIdMapperSpec extends Specification {

    def alternateIdPerCmHandle = new HashMap<String, String>()
    def cmHandlePerAlternateId = new HashMap<String, String>()
    def mockCpsCmHandlerQueryService = Mock(NetworkCmProxyCmHandleQueryService)

    def objectUnderTest = new CmHandleIdMapper(alternateIdPerCmHandle, cmHandlePerAlternateId, mockCpsCmHandlerQueryService)

    def 'Add entries to the caches.'() {
        given: 'a Cm Handle for registration'
            def createdCmHandle = new NcmpServiceCmHandle(cmHandleId: 'cmhandle1', alternateId: 'my-alternate-id-1')
        and: 'the query service returns and entry'
            mockCpsCmHandlerQueryService.getAllCmHandles() >> [new NcmpServiceCmHandle(cmHandleId: 'someCmHandle', alternateId: 'someAlternateId')]
        when: 'adding entries to the caches'
            objectUnderTest.addMapping(createdCmHandle.cmHandleId, createdCmHandle.alternateId)
        then: 'the caches have the correct size'
            assert alternateIdPerCmHandle.size() == 2
            assert cmHandlePerAlternateId.size() == 2
    }

    def 'Remove an entry from the caches.'() {
        given: 'non-empty caches with a single entry'
            def nonEmptyAlternateIdPerCmHandleMap = ['cmhandle1': 'my-alternate-id-1']
            def nonEmptyCmHandlePerAlternateIdMap = ['my-alternate-id-1': 'cmhandle1']
            def cmHandleMapper = new CmHandleIdMapper(nonEmptyAlternateIdPerCmHandleMap, nonEmptyCmHandlePerAlternateIdMap, mockCpsCmHandlerQueryService)
        when: 'removing an entry'
            cmHandleMapper.removeMapping('cmhandle1', 'my-alternate-id-1')
        then: 'the caches have the correct size'
            assert alternateIdPerCmHandle.size() == 0
            assert cmHandlePerAlternateId.size() == 0
    }

    def 'Get entries from the caches.'() {
        given: 'caches with some values'
            mockCpsCmHandlerQueryService.getAllCmHandles() >> []
            def nonEmptyAlternateIdPerCmHandleMap = ['cmhandle1': 'my-alternate-id-1']
            def nonEmptyCmHandlePerAlternateIdMap = ['my-alternate-id-1': 'cmhandle1']
            def cmHandleMapper = new CmHandleIdMapper(nonEmptyAlternateIdPerCmHandleMap, nonEmptyCmHandlePerAlternateIdMap, mockCpsCmHandlerQueryService)
        when: 'getting entries from the cache'
            def alternateId = cmHandleMapper.cmHandleIdToAlternateId('cmhandle1')
            def cmHandleId = cmHandleMapper.alternateIdToCmHandleId('my-alternate-id-1')
        then: 'the results have the correct values'
            assert alternateId == 'my-alternate-id-1'
            assert cmHandleId == 'cmhandle1'
    }
}
