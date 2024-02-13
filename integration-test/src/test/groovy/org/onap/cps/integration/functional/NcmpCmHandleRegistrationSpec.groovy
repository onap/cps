/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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

package org.onap.cps.integration.functional

import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.api.NetworkCmProxyDataService
import org.onap.cps.ncmp.api.impl.inventory.CmHandleState
import org.onap.cps.ncmp.api.impl.inventory.sync.ModuleSyncWatchdog
import org.onap.cps.ncmp.api.models.DmiPluginRegistration
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import org.onap.cps.spi.exceptions.DataNodeNotFoundException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import spock.util.concurrent.PollingConditions

import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus

class NcmpCmHandleRegistrationSpec extends CpsIntegrationSpecBase {

    NetworkCmProxyDataService objectUnderTest

    @Autowired
    ModuleSyncWatchdog moduleSyncWatchdog

    static final DMI_URL = 'http://mock-dmi-server'
    def mockDmiServer
    def moduleReferencesResponse
    def moduleResourcesResponse

    def setup() {
        objectUnderTest = networkCmProxyDataService
        mockDmiServer = MockRestServiceServer.createServer(restTemplate)
        moduleReferencesResponse = readResourceDataFile('mock-dmi-responses/ietfYangModuleResponse.json')
        moduleResourcesResponse = readResourceDataFile('mock-dmi-responses/ietfYangModuleResourcesResponse.json')
    }

    def 'CM Handle is READY when Registration is successful.'() {
        given: 'a CM handle to create'
            def cmHandlesToCreate = [new NcmpServiceCmHandle(cmHandleId: 'cm-1')]

        and: 'DMI registration params'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: DMI_URL, createdCmHandles: cmHandlesToCreate)
            dmiPluginRegistration.validateDmiPluginRegistration()

        and: 'DMI returns modules'
            mockDmiServer.expect(requestTo("${DMI_URL}/dmi/v1/ch/cm-1/modules"))
                    .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(moduleReferencesResponse))
            mockDmiServer.expect(requestTo("${DMI_URL}/dmi/v1/ch/cm-1/moduleResources"))
                    .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body(moduleResourcesResponse))

        when: 'a CM-handle is registered'
            def dmiPluginRegistrationResponse = networkCmProxyDataService.updateDmiRegistrationAndSyncModule(dmiPluginRegistration);

        then: 'registration gives expected response'
            assert dmiPluginRegistrationResponse.createdCmHandles.size() == 1

        and: 'CM-handle is initially in ADVISED state'
            assert CmHandleState.ADVISED == objectUnderTest.getCmHandleCompositeState('cm-1').cmHandleState

        when: 'module sync runs'
            moduleSyncWatchdog.moduleSyncAdvisedCmHandles()

        then: 'CM-handle goes to READY state'
            new PollingConditions().within(3, () -> {
                assert CmHandleState.READY == objectUnderTest.getCmHandleCompositeState('cm-1').cmHandleState
            })

        and: 'DMI received expected requests'
            mockDmiServer.verify()
    }

    def 'CM Handle goes to LOCKED state when DMI gives error during module sync.'() {
        given: 'a CM handle to create'
            def cmHandlesToCreate = [new NcmpServiceCmHandle(cmHandleId: 'cm-2')]

        and: 'DMI registration params'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: DMI_URL, createdCmHandles: cmHandlesToCreate)
            dmiPluginRegistration.validateDmiPluginRegistration()

        and: 'DMI returns error code'
            mockDmiServer.expect(anything()).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE))

        when: 'a CM-handle is registered'
            def dmiPluginRegistrationResponse = networkCmProxyDataService.updateDmiRegistrationAndSyncModule(dmiPluginRegistration);

        then: 'registration gives expected response'
            assert dmiPluginRegistrationResponse.createdCmHandles.size() == 1

        and: 'CM-handle is initially in ADVISED state'
            assert CmHandleState.ADVISED == objectUnderTest.getCmHandleCompositeState('cm-2').cmHandleState

        when: 'module sync runs'
            moduleSyncWatchdog.moduleSyncAdvisedCmHandles()

        then: 'CM-handle goes to LOCKED state'
            new PollingConditions().within(3, () -> {
                assert CmHandleState.LOCKED == objectUnderTest.getCmHandleCompositeState('cm-2').cmHandleState
            })

        and: 'DMI received expected requests'
            mockDmiServer.verify()
    }

    def 'Deregister CM-handles.'() {
        given: 'a list of CM handles to remove'
            def cmHandlesToRemove = ['cm-1', 'cm-2']

        and: 'DMI registration parameters are set'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: DMI_URL, removedCmHandles: cmHandlesToRemove)
            dmiPluginRegistration.validateDmiPluginRegistration()

        when: 'the CM-handles are deregistered'
            networkCmProxyDataService.updateDmiRegistrationAndSyncModule(dmiPluginRegistration);

        then: 'the CM-handles no longer exists'
            assert !objectUnderTest.getAllCmHandleIdsByDmiPluginIdentifier(DMI_URL).contains('cm-1')
            assert !objectUnderTest.getAllCmHandleIdsByDmiPluginIdentifier(DMI_URL).contains('cm-2')
    }
}
