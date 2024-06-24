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

import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.onap.cps.integration.KafkaTestContainer
import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.api.inventory.NetworkCmProxyInventoryFacade
import org.onap.cps.ncmp.api.inventory.models.CmHandleRegistrationResponse
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistration
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.events.lcm.v1.LcmEvent
import org.onap.cps.ncmp.impl.inventory.models.CmHandleState
import org.onap.cps.ncmp.impl.inventory.models.LockReasonCategory
import spock.util.concurrent.PollingConditions

import java.time.Duration
import java.time.OffsetDateTime

class NcmpCmHandleCreateSpec extends CpsIntegrationSpecBase {

    NetworkCmProxyInventoryFacade objectUnderTest

    def kafkaConsumer = KafkaTestContainer.getConsumer('ncmp-group', StringDeserializer.class)

    def setup() {
        objectUnderTest = networkCmProxyInventoryFacade
    }

    def 'CM Handle registration is successful.'() {
        given: 'DMI will return modules when requested'
            dmiDispatcher.moduleNamesPerCmHandleId['ch-1'] = ['M1', 'M2']

        and: 'consumer subscribed to topic'
            kafkaConsumer.subscribe(['ncmp-events'])

        when: 'a CM-handle is registered for creation'
            def cmHandleToCreate = new NcmpServiceCmHandle(cmHandleId: 'ch-1')
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: DMI_URL, createdCmHandles: [cmHandleToCreate])
            def dmiPluginRegistrationResponse = objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)

        then: 'registration gives successful response'
            assert dmiPluginRegistrationResponse.createdCmHandles == [CmHandleRegistrationResponse.createSuccessResponse('ch-1')]

        and: 'CM-handle is initially in ADVISED state'
            assert CmHandleState.ADVISED == objectUnderTest.getCmHandleCompositeState('ch-1').cmHandleState

        when: 'module sync runs'
            moduleSyncWatchdog.moduleSyncAdvisedCmHandles()

        then: 'CM-handle goes to READY state'
            new PollingConditions().within(3, () -> {
                assert CmHandleState.READY == objectUnderTest.getCmHandleCompositeState('ch-1').cmHandleState
            })

        and: 'the messages is polled'
            def message = kafkaConsumer.poll(Duration.ofMillis(10000))
            def records = message.records(new TopicPartition('ncmp-events', 0))

        and: 'the newest lcm event notification is received with READY state'
            def notificationMessage = jsonObjectMapper.convertJsonString(records.last().value().toString(), LcmEvent)
            assert notificationMessage.event.newValues.cmHandleState.value() == 'READY'

        and: 'the CM-handle has expected modules'
            assert ['M1', 'M2'] == objectUnderTest.getYangResourcesModuleReferences('ch-1').moduleName.sort()

        cleanup: 'deregister CM handle'
            deregisterCmHandle(DMI_URL, 'ch-1')
    }

    def 'CM Handle goes to LOCKED state when DMI gives error during module sync.'() {
        given: 'DMI is not available to handle requests'
            dmiDispatcher.isAvailable = false

        when: 'a CM-handle is registered for creation'
            def cmHandleToCreate = new NcmpServiceCmHandle(cmHandleId: 'ch-1')
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: DMI_URL, createdCmHandles: [cmHandleToCreate])
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)

        and: 'module sync runs'
            moduleSyncWatchdog.moduleSyncAdvisedCmHandles()

        then: 'CM-handle goes to LOCKED state with reason MODULE_SYNC_FAILED'
            new PollingConditions().within(3, () -> {
                def cmHandleCompositeState = objectUnderTest.getCmHandleCompositeState('ch-1')
                assert cmHandleCompositeState.cmHandleState == CmHandleState.LOCKED
                assert cmHandleCompositeState.lockReason.lockReasonCategory == LockReasonCategory.MODULE_SYNC_FAILED
            })

        and: 'CM-handle has no modules'
            assert objectUnderTest.getYangResourcesModuleReferences('ch-1').empty

        cleanup: 'deregister CM handle'
            deregisterCmHandle(DMI_URL, 'ch-1')
    }

    def 'Create a CM-handle with existing moduleSetTag.'() {
        given: 'DMI will return modules when requested'
            dmiDispatcher.moduleNamesPerCmHandleId = ['ch-1': ['M1', 'M2'], 'ch-2': ['M1', 'M3']]
        and: 'existing CM-handles cm-1 with moduleSetTag "A", and cm-2 with moduleSetTag "B"'
            registerCmHandle(DMI_URL, 'ch-1', 'A')
            registerCmHandle(DMI_URL, 'ch-2', 'B')

        when: 'a CM-handle is registered for creation with moduleSetTag "B"'
            def cmHandleToCreate = new NcmpServiceCmHandle(cmHandleId: 'ch-3', moduleSetTag: 'B')
            objectUnderTest.updateDmiRegistrationAndSyncModule(new DmiPluginRegistration(dmiPlugin: DMI_URL, createdCmHandles: [cmHandleToCreate]))

        then: 'the CM-handle goes to READY state after module sync'
            moduleSyncWatchdog.moduleSyncAdvisedCmHandles()
            new PollingConditions().within(3, () -> {
                assert CmHandleState.READY == objectUnderTest.getCmHandleCompositeState('ch-3').cmHandleState
            })

        and: 'the CM-handle has expected moduleSetTag'
            assert objectUnderTest.getNcmpServiceCmHandle('ch-3').moduleSetTag == 'B'

        and: 'the CM-handle has expected modules from module set "B": M1 and M3'
            assert ['M1', 'M3'] == objectUnderTest.getYangResourcesModuleReferences('ch-3').moduleName.sort()

        cleanup: 'deregister CM handles'
            deregisterCmHandles(DMI_URL, ['ch-1', 'ch-2', 'ch-3'])
    }

    def 'CM Handle retry after failed module sync.'() {
        given: 'DMI is not initially available to handle requests'
            dmiDispatcher.isAvailable = false

        when: 'CM-handles are registered for creation'
            def cmHandlesToCreate = [new NcmpServiceCmHandle(cmHandleId: 'ch-1'), new NcmpServiceCmHandle(cmHandleId: 'ch-2')]
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: DMI_URL, createdCmHandles: cmHandlesToCreate)
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        and: 'module sync runs'
            moduleSyncWatchdog.moduleSyncAdvisedCmHandles()
        then: 'CM-handles go to LOCKED state'
            new PollingConditions().within(3, () -> {
                assert objectUnderTest.getCmHandleCompositeState('ch-1').cmHandleState == CmHandleState.LOCKED
                assert objectUnderTest.getCmHandleCompositeState('ch-2').cmHandleState == CmHandleState.LOCKED
            })

        when: 'we wait for LOCKED CM handle retry time (actually just subtract 3 minutes from handles lastUpdateTime)'
            overrideCmHandleLastUpdateTime('ch-1', OffsetDateTime.now().minusMinutes(3))
            overrideCmHandleLastUpdateTime('ch-2', OffsetDateTime.now().minusMinutes(3))
        and: 'failed CM handles are reset'
            moduleSyncWatchdog.resetPreviouslyFailedCmHandles()
        then: 'CM-handles are ADVISED state'
            assert objectUnderTest.getCmHandleCompositeState('ch-1').cmHandleState == CmHandleState.ADVISED
            assert objectUnderTest.getCmHandleCompositeState('ch-2').cmHandleState == CmHandleState.ADVISED

        when: 'DMI is available for retry'
            dmiDispatcher.isAvailable = true
        and: 'DMI will return expected modules'
            dmiDispatcher.moduleNamesPerCmHandleId = ['ch-1': ['M1', 'M2'], 'ch-2': ['M1', 'M3']]
        and: 'module sync runs'
            moduleSyncWatchdog.moduleSyncAdvisedCmHandles()
        then: 'CM-handles go to READY state'
            new PollingConditions().within(3, () -> {
                assert objectUnderTest.getCmHandleCompositeState('ch-1').cmHandleState == CmHandleState.READY
                assert objectUnderTest.getCmHandleCompositeState('ch-2').cmHandleState == CmHandleState.READY
            })
        and: 'CM-handles have expected modules'
            assert ['M1', 'M2'] == objectUnderTest.getYangResourcesModuleReferences('ch-1').moduleName.sort()
            assert ['M1', 'M3'] == objectUnderTest.getYangResourcesModuleReferences('ch-2').moduleName.sort()
        and: 'CM-handles have expected module set tags (blank)'
            assert objectUnderTest.getNcmpServiceCmHandle('ch-1').moduleSetTag == ''
            assert objectUnderTest.getNcmpServiceCmHandle('ch-2').moduleSetTag == ''

        cleanup: 'deregister CM handle'
            deregisterCmHandles(DMI_URL, ['ch-1', 'ch-2'])
    }
}
