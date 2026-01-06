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

import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.api.NcmpResponseStatus
import org.onap.cps.ncmp.api.inventory.models.CmHandleRegistrationResponse
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistration
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.events.lcm.v1.LcmEvent
import org.onap.cps.ncmp.impl.NetworkCmProxyInventoryFacadeImpl

class CmHandleUpdateSpec extends CpsIntegrationSpecBase {

    NetworkCmProxyInventoryFacadeImpl objectUnderTest

    def setup() {
        objectUnderTest = networkCmProxyInventoryFacade
        subscribeAndClearPreviousMessages('test-group-for-update', 'ncmp-events')
    }

    def cleanup() {
        kafkaConsumer.unsubscribe()
        kafkaConsumer.close()
    }

    def 'Update of CM-handle with new or unchanged alternate ID succeeds.'() {
        given: 'DMI will return modules when requested'
            dmiDispatcher1.moduleNamesPerCmHandleId = ['ch-1': ['M1', 'M2']]
        and: 'existing CM-handle with alternate ID: $oldAlternateId'
            registerCmHandle(DMI1_URL, 'ch-1', NO_MODULE_SET_TAG, oldAlternateId)
        when: 'CM-handle is registered for update with new alternate ID: $newAlternateId'
            def cmHandleToUpdate = new NcmpServiceCmHandle(cmHandleId: 'ch-1', alternateId: newAlternateId)
            def dmiPluginRegistrationResponse =
                    objectUnderTest.updateDmiRegistration(new DmiPluginRegistration(dmiPlugin: DMI1_URL, updatedCmHandles: [cmHandleToUpdate]))
        then: 'registration gives successful response'
            assert dmiPluginRegistrationResponse.updatedCmHandles == [CmHandleRegistrationResponse.createSuccessResponse('ch-1')]
        and: 'the CM-handle has expected alternate ID'
            assert objectUnderTest.getNcmpServiceCmHandle('ch-1').alternateId == expectedAlternateId
        cleanup: 'deregister CM handles'
            deregisterCmHandle(DMI1_URL, 'ch-1')
        where:
            oldAlternateId | newAlternateId || expectedAlternateId
            ''             | ''             || ''
            ''             | 'new'          || 'new'
            'old'          | 'old'          || 'old'
            'old'          | null           || 'old'
            'old'          | ''             || 'old'
            'old'          | '  '           || 'old'
    }

    def 'Update of CM-handle with previously set alternate ID fails.'() {
        given: 'DMI will return modules when requested'
            dmiDispatcher1.moduleNamesPerCmHandleId = ['ch-1': ['M1', 'M2']]
        and: 'existing CM-handle with alternate ID'
            registerCmHandle(DMI1_URL, 'ch-1', NO_MODULE_SET_TAG, 'original')
        when: 'a CM-handle is registered for update with new alternate ID'
            def cmHandleToUpdate = new NcmpServiceCmHandle(cmHandleId: 'ch-1', alternateId: 'new')
            def dmiPluginRegistrationResponse =
                    objectUnderTest.updateDmiRegistration(new DmiPluginRegistration(dmiPlugin: DMI1_URL, updatedCmHandles: [cmHandleToUpdate]))
        then: 'registration gives failure response, due to cm-handle already existing'
            assert dmiPluginRegistrationResponse.updatedCmHandles == [CmHandleRegistrationResponse.createFailureResponse('ch-1', NcmpResponseStatus.CM_HANDLE_ALREADY_EXIST)]
        and: 'the CM-handle still has the old alternate ID'
            assert objectUnderTest.getNcmpServiceCmHandle('ch-1').alternateId == 'original'
        cleanup: 'deregister CM handles'
            deregisterCmHandle(DMI1_URL, 'ch-1')
    }

    def 'CM Handle registration to verify changes in data producer identifier'() {
        given: 'DMI will return modules when requested'
            def cmHandleId = 'ch-id-for-update'
            dmiDispatcher1.moduleNamesPerCmHandleId[cmHandleId] = ['M1', 'M2']
        when: 'a CM-handle is registered for creation'

            def cmHandleToCreate = new NcmpServiceCmHandle(cmHandleId: cmHandleId)
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: DMI1_URL, createdCmHandles: [cmHandleToCreate])
            def dmiPluginRegistrationResponse = objectUnderTest.updateDmiRegistration(dmiPluginRegistration)
        then: 'registration gives successful response'
            assert dmiPluginRegistrationResponse.createdCmHandles == [CmHandleRegistrationResponse.createSuccessResponse(cmHandleId)]
        then: 'the module sync watchdog is triggered'
            moduleSyncWatchdog.moduleSyncAdvisedCmHandles()
        and: 'flush the latest cm handle registration events( state transition from NONE to ADVISED and ADVISED to READY)'
            getLatestConsumerRecordsWithMaxPollOf1Second(2)
        and: 'cm handle updated with the data producer identifier'
            def cmHandleToUpdate = new NcmpServiceCmHandle(cmHandleId: cmHandleId, dataProducerIdentifier: 'my-data-producer-id')
            def dmiPluginRegistrationForUpdate = new DmiPluginRegistration(dmiPlugin: DMI1_URL, updatedCmHandles: [cmHandleToUpdate])
            def dmiPluginRegistrationResponseForUpdate = objectUnderTest.updateDmiRegistration(dmiPluginRegistrationForUpdate)
        then: 'registration gives successful response'
            assert dmiPluginRegistrationResponseForUpdate.updatedCmHandles == [CmHandleRegistrationResponse.createSuccessResponse(cmHandleId)]
        and: 'get the latest message'
            def consumerRecords = getLatestConsumerRecordsWithMaxPollOf1Second(1)
        and: 'the message has the updated data producer identifier'
            def notificationMessages = []
            for (def consumerRecord : consumerRecords) {
                notificationMessages.add(jsonObjectMapper.convertJsonString(consumerRecord.value().toString(), LcmEvent))
            }
            assert notificationMessages[0].event.cmHandleId.contains(cmHandleId)
            assert notificationMessages[0].event.dataProducerIdentifier == 'my-data-producer-id'
        cleanup: 'deregister CM handle'
            deregisterCmHandle(DMI1_URL, cmHandleId)
    }

}
