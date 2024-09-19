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

package org.onap.cps.integration.functional.ncmp

import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.onap.cps.integration.KafkaTestContainer
import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.api.NcmpResponseStatus
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

class CmHandleCreateSpec extends CpsIntegrationSpecBase {

    NetworkCmProxyInventoryFacade objectUnderTest

    def kafkaConsumer = KafkaTestContainer.getConsumer('ncmp-group', StringDeserializer.class)

    def setup() {
        objectUnderTest = networkCmProxyInventoryFacade
    }

    def 'CM Handle registration is successful.'() {
        given: 'DMI will return modules when requested'
            dmiDispatcher1.moduleNamesPerCmHandleId['ch-1'] = ['M1', 'M2']

        and: 'consumer subscribed to topic'
            kafkaConsumer.subscribe(['ncmp-events'])

        when: 'a CM-handle is registered for creation'
            def cmHandleToCreate = new NcmpServiceCmHandle(cmHandleId: 'ch-1')
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: DMI1_URL, createdCmHandles: [cmHandleToCreate])
            def dmiPluginRegistrationResponse = objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)

        then: 'registration gives successful response'
            assert dmiPluginRegistrationResponse.createdCmHandles == [CmHandleRegistrationResponse.createSuccessResponse('ch-1')]

        and: 'CM-handle is initially in ADVISED state'
            assert CmHandleState.ADVISED == objectUnderTest.getCmHandleCompositeState('ch-1').cmHandleState

        and: 'CM-handle goes to READY state after module sync'
            new PollingConditions().within(MODULE_SYNC_WAIT_TIME_IN_SECONDS, () -> {
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
            deregisterCmHandle(DMI1_URL, 'ch-1')
    }

    def 'CM Handle goes to LOCKED state when DMI gives error during module sync.'() {
        given: 'DMI is not available to handle requests'
            dmiDispatcher1.isAvailable = false

        when: 'a CM-handle is registered for creation'
            def cmHandleToCreate = new NcmpServiceCmHandle(cmHandleId: 'ch-1')
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: DMI1_URL, createdCmHandles: [cmHandleToCreate])
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)

        then: 'CM-handle goes to LOCKED state with reason MODULE_SYNC_FAILED'
            new PollingConditions().within(MODULE_SYNC_WAIT_TIME_IN_SECONDS, () -> {
                def cmHandleCompositeState = objectUnderTest.getCmHandleCompositeState('ch-1')
                assert cmHandleCompositeState.cmHandleState == CmHandleState.LOCKED
                assert cmHandleCompositeState.lockReason.lockReasonCategory == LockReasonCategory.MODULE_SYNC_FAILED
            })

        and: 'CM-handle has no modules'
            assert objectUnderTest.getYangResourcesModuleReferences('ch-1').empty

        cleanup: 'deregister CM handle'
            deregisterCmHandle(DMI1_URL, 'ch-1')
    }

    def 'Create a CM-handle with existing moduleSetTag.'() {
        given: 'DMI will return modules when requested'
            dmiDispatcher1.moduleNamesPerCmHandleId = ['ch-1': ['M1', 'M2'], 'ch-2': ['M1', 'M3']]
        and: 'existing CM-handles cm-1 with moduleSetTag "A", and cm-2 with moduleSetTag "B"'
            registerCmHandle(DMI1_URL, 'ch-1', 'A')
            registerCmHandle(DMI1_URL, 'ch-2', 'B')

        when: 'a CM-handle is registered for creation with moduleSetTag "B"'
            def cmHandleToCreate = new NcmpServiceCmHandle(cmHandleId: 'ch-3', moduleSetTag: 'B')
            objectUnderTest.updateDmiRegistrationAndSyncModule(new DmiPluginRegistration(dmiPlugin: DMI1_URL, createdCmHandles: [cmHandleToCreate]))

        then: 'the CM-handle goes to READY state'
            new PollingConditions().within(MODULE_SYNC_WAIT_TIME_IN_SECONDS, () -> {
                assert CmHandleState.READY == objectUnderTest.getCmHandleCompositeState('ch-3').cmHandleState
            })

        and: 'the CM-handle has expected moduleSetTag'
            assert objectUnderTest.getNcmpServiceCmHandle('ch-3').moduleSetTag == 'B'

        and: 'the CM-handle has expected modules from module set "B": M1 and M3'
            assert ['M1', 'M3'] == objectUnderTest.getYangResourcesModuleReferences('ch-3').moduleName.sort()

        cleanup: 'deregister CM handles'
            deregisterCmHandles(DMI1_URL, ['ch-1', 'ch-2', 'ch-3'])
    }

    def 'Create CM-handles with alternate IDs.'() {
        given: 'DMI will return modules for all CM-handles when requested'
            dmiDispatcher1.moduleNamesPerCmHandleId = (1..7).collectEntries{ ['ch-'+it, ['M1']] }
        and: 'an existing CM-handle with an alternate ID'
            registerCmHandle(DMI1_URL, 'ch-1', NO_MODULE_SET_TAG, 'existing-alt-id')
        and: 'an existing CM-handle with no alternate ID'
            registerCmHandle(DMI1_URL, 'ch-2', NO_MODULE_SET_TAG, NO_ALTERNATE_ID)

        when: 'a batch of CM-handles is registered for creation with various alternate IDs'
            def cmHandlesToCreate = [
                    new NcmpServiceCmHandle(cmHandleId: 'ch-3', alternateId: NO_ALTERNATE_ID),
                    new NcmpServiceCmHandle(cmHandleId: 'ch-4', alternateId: 'unique-alt-id'),
                    new NcmpServiceCmHandle(cmHandleId: 'ch-5', alternateId: 'existing-alt-id'),
                    new NcmpServiceCmHandle(cmHandleId: 'ch-6', alternateId: 'duplicate-alt-id'),
                    new NcmpServiceCmHandle(cmHandleId: 'ch-7', alternateId: 'duplicate-alt-id'),
            ]
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: DMI1_URL, createdCmHandles: cmHandlesToCreate)
            def dmiPluginRegistrationResponse = objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)

        then: 'registration gives expected responses'
            assert dmiPluginRegistrationResponse.createdCmHandles.sort { it.cmHandle } == [
                CmHandleRegistrationResponse.createSuccessResponse('ch-3'),
                CmHandleRegistrationResponse.createSuccessResponse('ch-4'),
                CmHandleRegistrationResponse.createFailureResponse('ch-5', NcmpResponseStatus.ALTERNATE_ID_ALREADY_ASSOCIATED),
                CmHandleRegistrationResponse.createSuccessResponse('ch-6'),
                CmHandleRegistrationResponse.createFailureResponse('ch-7', NcmpResponseStatus.ALTERNATE_ID_ALREADY_ASSOCIATED),
            ]

        cleanup: 'deregister CM handles'
            deregisterCmHandles(DMI1_URL, (1..7).collect{ 'ch-'+it })
    }

    def 'CM Handle retry after failed module sync.'() {
        given: 'DMI is not initially available to handle requests'
            dmiDispatcher1.isAvailable = false

        when: 'CM-handles are registered for creation'
            def cmHandlesToCreate = [new NcmpServiceCmHandle(cmHandleId: 'ch-1'), new NcmpServiceCmHandle(cmHandleId: 'ch-2')]
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: DMI1_URL, createdCmHandles: cmHandlesToCreate)
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'CM-handles go to LOCKED state'
            new PollingConditions().within(MODULE_SYNC_WAIT_TIME_IN_SECONDS, () -> {
                assert objectUnderTest.getCmHandleCompositeState('ch-1').cmHandleState == CmHandleState.LOCKED
            })

        when: 'DMI is available for retry'
            dmiDispatcher1.moduleNamesPerCmHandleId = ['ch-1': ['M1', 'M2'], 'ch-2': ['M1', 'M2']]
            dmiDispatcher1.isAvailable = true

        then: 'Both CM-handles go to READY state'
            new PollingConditions().within(MODULE_SYNC_WAIT_TIME_IN_SECONDS, () -> {
                ['ch-1', 'ch-2'].each { cmHandleId ->
                    assert objectUnderTest.getCmHandleCompositeState(cmHandleId).cmHandleState == CmHandleState.READY
                }
            })

        and: 'Both CM-handles have expected modules'
            ['ch-1', 'ch-2'].each { cmHandleId ->
                assert objectUnderTest.getYangResourcesModuleReferences(cmHandleId).moduleName.sort() == ['M1', 'M2']
            }

        cleanup: 'deregister CM handles'
            deregisterCmHandles(DMI1_URL, ['ch-1', 'ch-2'])
    }
}
