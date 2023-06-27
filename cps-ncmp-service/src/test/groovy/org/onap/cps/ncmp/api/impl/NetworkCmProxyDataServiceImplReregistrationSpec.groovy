/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation
 *  Modifications Copyright (C) 2022 Bell Canada
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

package org.onap.cps.ncmp.api.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.hazelcast.map.IMap
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.ncmp.api.NetworkCmProxyCmHandleQueryService
import org.onap.cps.ncmp.api.impl.events.lcm.LcmEventsCmHandleStateHandler
import org.onap.cps.ncmp.api.impl.operations.DmiDataOperations
import org.onap.cps.ncmp.api.inventory.CmHandleQueries
import org.onap.cps.ncmp.api.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.models.DmiPluginReregistration
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Shared
import spock.lang.Specification

class NetworkCmProxyDataServiceImplReregistrationSpec extends Specification {

    @Shared
    def ncmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: 'some-cm-handle', publicProperties: ["property":"original"])

    def mockCpsModuleService = Mock(CpsModuleService)
    def spiedJsonObjectMapper = Spy(new JsonObjectMapper(new ObjectMapper()))
    def mockDmiDataOperations = Mock(DmiDataOperations)
    def mockNetworkCmProxyDataServicePropertyHandler = Mock(NetworkCmProxyDataServicePropertyHandler)
    def mockInventoryPersistence = Mock(InventoryPersistence)
    def mockCmhandleQueries = Mock(CmHandleQueries)
    def stubbedNetworkCmProxyCmHandlerQueryService = Stub(NetworkCmProxyCmHandleQueryService)
    def mockLcmEventsCmHandleStateHandler = Mock(LcmEventsCmHandleStateHandler)
    def mockCpsDataService = Mock(CpsDataService)
    def mockModuleSyncStartedOnCmHandles = Mock(IMap<String, Object>)
    def objectUnderTest = getObjectUnderTest()

    def 'CmHandle reregistration'() {
        given: 'a cmHandle'
            def reregCmHandle = new NcmpServiceCmHandle(cmHandleId: 'some-cm-handle', publicProperties: ["property":"new"])
            def newCmHandle  = new NcmpServiceCmHandle(cmHandleId: 'new-cm-handle', publicProperties: ["property":"new"])
            addPersistedYangModelCmHandles(reregCmHandle, "dataPlugin")
        and: 'a reregistration '
            def dmiPluginReregistration = new DmiPluginReregistration(dmiModelPlugin: "modelPlugin", dmiDataPlugin: "dataPlugin")
            dmiPluginReregistration.cmHandles = [reregCmHandle, newCmHandle]
        when: 'update registration and sync module is called with correct DMI plugin information'
            objectUnderTest.dmiReRegistration(dmiPluginReregistration)
        then: 'create cm handles registration and sync modules is called with the correct plugin information'
            1 * objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration -> {
                assert dmiPluginRegistration.createdCmHandles.get(0) == newCmHandle
                assert dmiPluginRegistration.updatedCmHandles == [reregCmHandle]
            })
    }

    def getObjectUnderTest() {
        return Spy(new NetworkCmProxyDataServiceImpl(spiedJsonObjectMapper, mockDmiDataOperations,
                mockNetworkCmProxyDataServicePropertyHandler, mockInventoryPersistence, mockCmhandleQueries,
                stubbedNetworkCmProxyCmHandlerQueryService, mockLcmEventsCmHandleStateHandler, mockCpsDataService,
                mockModuleSyncStartedOnCmHandles))
    }

    def addPersistedYangModelCmHandles(cmHandles, dmiPlugin) {
        def cmHandleIds = cmHandles.collect {it.getCmHandleId()}
        mockCmhandleQueries.getCmHandleIdsByDmiPluginIdentifier(dmiPlugin) >> cmHandleIds
    }
}
