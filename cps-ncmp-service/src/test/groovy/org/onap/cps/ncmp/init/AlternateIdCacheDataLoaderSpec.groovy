/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.init

import com.hazelcast.map.IMap
import org.onap.cps.api.model.DataNode
import org.onap.cps.ncmp.impl.inventory.CmHandleRegistrationService
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.utils.events.NcmpInventoryModelOnboardingFinishedEvent
import spock.lang.Specification

class AlternateIdCacheDataLoaderSpec extends Specification {

    def mockInventoryPersistence = Mock(InventoryPersistence)
    def mockCmHandleRegistrationService = Mock(CmHandleRegistrationService)
    def mockCmHandleIdPerAlternateId = Mock(IMap)

    def objectUnderTest = new AlternateIdCacheDataLoader(mockInventoryPersistence, mockCmHandleRegistrationService, mockCmHandleIdPerAlternateId)

    def 'Populate cm handle id per alternate id cache.'() {
        given: 'cache is empty'
            mockCmHandleIdPerAlternateId.isEmpty() >> true
        and: 'inventory persistence returns some data nodes'
            def childDataNodes = [new DataNode(xpath: "", leaves: ['id': 'ch-1', 'alternate-id': 'alt-1'])]
            mockInventoryPersistence.getDataNode(_, _) >> [new DataNode(childDataNodes:childDataNodes, leaves: ['id':''])]
        when: 'the method to populate the cache is invoked by the ncmp model onboarding event'
            objectUnderTest.populateCmHandleIdPerAlternateIdMap(Mock(NcmpInventoryModelOnboardingFinishedEvent))
        then: 'the cm handle registration service is called once to add ids to cache'
            1 * mockCmHandleRegistrationService.addAlternateIdsToCache(_)
    }
}
