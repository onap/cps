/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.integration.functional.ncmp.inventory


import org.apache.kafka.clients.consumer.KafkaConsumer
import org.onap.cps.events.LegacyEvent
import org.onap.cps.integration.KafkaTestContainer
import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.api.NcmpResponseStatus
import org.onap.cps.ncmp.api.inventory.models.CmHandleRegistrationResponse
import org.onap.cps.ncmp.api.inventory.models.CmHandleState
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistration
import org.onap.cps.ncmp.api.inventory.models.LockReasonCategory
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.events.lcm.v1.LcmEvent
import org.onap.cps.ncmp.impl.NetworkCmProxyInventoryFacadeImpl
import spock.util.concurrent.PollingConditions

import java.time.Duration

class CmHandleCreateSpec extends CpsIntegrationSpecBase {

    NetworkCmProxyInventoryFacadeImpl objectUnderTest
    def uniqueId = 'my-new-cm-handle'

    KafkaConsumer<String, LegacyEvent> kafkaConsumer

    def setup() {
        objectUnderTest = networkCmProxyInventoryFacade
        subscribeAndClearPreviousMessages('test-group', 'ncmp-events')
        clearPreviousInstrumentation()
    }

    def cleanup() {
        kafkaConsumer.unsubscribe()
        kafkaConsumer.close()
    }

    def 'CM Handle registration.'() {
        given: 'DMI will return modules when requested'
            dmiDispatcher1.moduleNamesPerCmHandleId['ch-1'] = ['M1', 'M2']
            dmiDispatcher1.moduleNamesPerCmHandleId[uniqueId] = ['M1', 'M2']
        when: 'a CM-handle is registered for creation'
            def cmHandleToCreate = new NcmpServiceCmHandle(cmHandleId: uniqueId,
                                                           alternateId: 'fdn1',
                                                           moduleSetTag: 'tag1',
                                                           dataProducerIdentifier: 'prod1',
                                                           publicProperties: [color:'green'])
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: DMI1_URL, createdCmHandles: [cmHandleToCreate])
            def dmiPluginRegistrationResponse = objectUnderTest.updateDmiRegistration(dmiPluginRegistration)
        then: 'registration gives successful response'
            assert dmiPluginRegistrationResponse.createdCmHandles == [CmHandleRegistrationResponse.createSuccessResponse(uniqueId)]
        and: 'CM-handle is initially in ADVISED state'
            assert CmHandleState.ADVISED == objectUnderTest.getCmHandleCompositeState(uniqueId).cmHandleState
        then: 'the module sync watchdog is triggered'
            moduleSyncWatchdog.moduleSyncAdvisedCmHandles()
        then: 'CM-handle goes to READY state after module sync'
            assert CmHandleState.READY == objectUnderTest.getCmHandleCompositeState(uniqueId).cmHandleState
        and: 'the CM-handle has expected modules'
            assert ['M1', 'M2'] == objectUnderTest.getYangResourcesModuleReferences(uniqueId).moduleName.sort()
        then: 'get the last 2 messages and related headers'
            def consumerRecords = getLatestConsumerRecordsWithMaxPollOf1Second(kafkaConsumer, 2)
            def messages = []
            def headerMaps = []
            consumerRecords.each { consumerRecord ->
                messages.add(jsonObjectMapper.convertJsonString(consumerRecord.value().toString(), LcmEvent))
                headerMaps.add(getHeadersAsMap(consumerRecord))
            }
        and: 'both messages have the correct common attributes (that did not change)'
            assert messages.event.every {
                it.cmHandleId == uniqueId &&
                it.alternateId == 'fdn1' &&
                it.moduleSetTag == 'tag1' &&
                it.dataProducerIdentifier == 'prod1'
            }
        and: 'the header fields are populated correctly and stored as kafka headers too'
            validateEventHeaders(messages[0], headerMaps[0], 'create')
            validateEventHeaders(messages[1], headerMaps[1], 'update')
        and: 'the first lcm event has no old values and the initial attributes as new values state ADVISED'
            with(messages[0].event) {
                assert oldValues == null
                assert newValues.cmHandleState.value() == 'ADVISED'
                assert newValues.dataSyncEnabled == null
                assert newValues.cmHandleProperties[0] == [color:'green']
            }
        and: 'the next event is about update to READY state (new value), the old value for state is ADVISED'
            assert messages[1].event.oldValues.cmHandleState.value() == 'ADVISED'
            assert messages[1].event.newValues.cmHandleState.value() == 'READY'
        and: 'the cm handle (public) properties have not changed and are therefore null for old and new values'
            assert messages[1].event.oldValues.cmHandleProperties == null
            assert messages[1].event.newValues.cmHandleProperties == null
        and: 'the data sync flag goes from undefined to false'
            assert messages[1].event.oldValues.dataSyncEnabled == null
            assert messages[1].event.newValues.dataSyncEnabled == false
        and: 'there are no more messages to be read'
            assert getLatestConsumerRecordsWithMaxPollOf1Second(kafkaConsumer, 1).size() == 0
        and: 'instrumentation has recorded 2 events (null > ADVISED, ADVISED > READY)'
            new PollingConditions().within(5) {
                assert countLcmEventTimerInvocations() == 2
            }
        then: 'deregister CM handle'
            deregisterCmHandle(DMI1_URL, uniqueId)
        and: 'instrumentation has recorded 2 more events (READY > DELETING, DELETING > null)'
            new PollingConditions().within(5) {
                assert countLcmEventTimerInvocations() == 2 + 2
            }
    }

    def 'CM Handle registration with DMI error during module sync.'() {
        given: 'DMI is not available to handle requests'
            dmiDispatcher1.isAvailable = false
        when: 'a CM-handle is registered for creation'
            def cmHandleToCreate = new NcmpServiceCmHandle(cmHandleId: 'ch-1')
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: DMI1_URL, createdCmHandles: [cmHandleToCreate])
            objectUnderTest.updateDmiRegistration(dmiPluginRegistration)
        and: 'the module sync watchdog is triggered'
            moduleSyncWatchdog.moduleSyncAdvisedCmHandles()
        then: 'CM-handle goes to LOCKED state with reason MODULE_SYNC_FAILED'
            def cmHandleCompositeState = objectUnderTest.getCmHandleCompositeState('ch-1')
            assert cmHandleCompositeState.cmHandleState == CmHandleState.LOCKED
            assert cmHandleCompositeState.lockReason.lockReasonCategory == LockReasonCategory.MODULE_SYNC_FAILED
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
            objectUnderTest.updateDmiRegistration(new DmiPluginRegistration(dmiPlugin: DMI1_URL, createdCmHandles: [cmHandleToCreate]))
        and: 'the module sync watchdog is triggered'
            moduleSyncWatchdog.moduleSyncAdvisedCmHandles()
        then: 'the CM-handle goes to READY state'
            assert CmHandleState.READY == objectUnderTest.getCmHandleCompositeState('ch-3').cmHandleState
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
            def dmiPluginRegistrationResponse = objectUnderTest.updateDmiRegistration(dmiPluginRegistration)
        then: 'registration gives expected responses'
            assert dmiPluginRegistrationResponse.createdCmHandles.sort { it.cmHandle } == [
                CmHandleRegistrationResponse.createSuccessResponse('ch-3'),
                CmHandleRegistrationResponse.createSuccessResponse('ch-4'),
                CmHandleRegistrationResponse.createFailureResponse('ch-5', NcmpResponseStatus.CM_HANDLE_ALREADY_EXIST),
                CmHandleRegistrationResponse.createSuccessResponse('ch-6'),
                CmHandleRegistrationResponse.createFailureResponse('ch-7', NcmpResponseStatus.CM_HANDLE_ALREADY_EXIST),
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
            objectUnderTest.updateDmiRegistration(dmiPluginRegistration)
        and: 'the module sync watchdog is triggered'
            moduleSyncWatchdog.moduleSyncAdvisedCmHandles()
        then: 'CM-handles go to LOCKED state'
            assert objectUnderTest.getCmHandleCompositeState('ch-1').cmHandleState == CmHandleState.LOCKED
        when: 'DMI is available for retry'
            dmiDispatcher1.moduleNamesPerCmHandleId = ['ch-1': ['M1', 'M2'], 'ch-2': ['M1', 'M2']]
            dmiDispatcher1.isAvailable = true
        and: 'the module sync watchdog is triggered TWICE'
            2.times { moduleSyncWatchdog.moduleSyncAdvisedCmHandles() }
        then: 'Both CM-handles go to READY state'
            ['ch-1', 'ch-2'].each { cmHandleId ->
                assert objectUnderTest.getCmHandleCompositeState(cmHandleId).cmHandleState == CmHandleState.READY
            }
        and: 'Both CM-handles have expected modules'
            ['ch-1', 'ch-2'].each { cmHandleId ->
                assert objectUnderTest.getYangResourcesModuleReferences(cmHandleId).moduleName.sort() == ['M1', 'M2']
            }
        cleanup: 'deregister CM handles'
            deregisterCmHandles(DMI1_URL, ['ch-1', 'ch-2'])
    }

    def subscribeAndClearPreviousMessages(consumerGroupId, topicName) {
        kafkaConsumer = KafkaTestContainer.getLegacyEventConsumer(consumerGroupId)
        kafkaConsumer.subscribe([topicName])
        kafkaConsumer.poll(Duration.ofMillis(500))
    }

    def getHeadersAsMap(consumerRecord) {
        def headersAsMap = [:]
        consumerRecord.headers().each { header ->
            def value = (new String((byte[]) header.value())).substring(7) // The raw header is prefixed with encoded type
            headersAsMap.put(header.key(), value)
        }
        return headersAsMap
    }

    def validateEventHeaders(message, headerAsMap, expectedEventType) {
        with(message) {
            assert UUID.fromString(eventId) != null
            assert headerAsMap.get('eventId') == eventId
            assert eventCorrelationId == uniqueId
            assert headerAsMap.get('eventCorrelationId') == eventCorrelationId
            assert timestampIsVeryRecent(eventTime)
            assert headerAsMap.get('eventTime') == eventTime
            assert eventSource == 'org.onap.ncmp'
            assert headerAsMap.get('eventSource') == eventSource
            assert eventType == 'org.onap.ncmp.cmhandle-lcm-event.'+expectedEventType
            assert headerAsMap.get('eventType') == eventType
            assert eventSchema == 'org.onap.ncmp:cmhandle-lcm-event'
            assert headerAsMap.get('eventSchema') == eventSchema
            assert eventSchemaVersion == '1.0'
            assert headerAsMap.get('eventSchemaVersion') == eventSchemaVersion
        }
        return true
    }

    def countLcmEventTimerInvocations() {
        def totalCountForAllTagCombinations = 0
        for (def timer : meterRegistry.get('cps.ncmp.lcm.events.send').timers()) {
            totalCountForAllTagCombinations = totalCountForAllTagCombinations + timer.count()
        }
        return totalCountForAllTagCombinations
    }

}
