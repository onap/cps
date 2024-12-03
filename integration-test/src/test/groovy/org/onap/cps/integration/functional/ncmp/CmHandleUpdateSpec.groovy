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

import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.api.NcmpResponseStatus
import org.onap.cps.ncmp.impl.NetworkCmProxyInventoryFacadeImpl
import org.onap.cps.ncmp.api.inventory.models.CmHandleRegistrationResponse
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistration
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle

class CmHandleUpdateSpec extends CpsIntegrationSpecBase {

    NetworkCmProxyInventoryFacadeImpl objectUnderTest

    def setup() {
        objectUnderTest = networkCmProxyInventoryFacade
    }

    def 'Update of CM-handle with new or unchanged alternate ID succeeds.'() {
        given: 'DMI will return modules when requested'
            dmiDispatcher1.moduleNamesPerCmHandleId = ['ch-1': ['M1', 'M2']]
        and: "existing CM-handle with alternate ID: $oldAlternateId"
            registerCmHandle(DMI1_URL, 'ch-1', NO_MODULE_SET_TAG, oldAlternateId)

        when: "CM-handle is registered for update with new alternate ID: $newAlternateId"
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

        then: 'registration gives failure response, due to alternate ID being already associated'
            assert dmiPluginRegistrationResponse.updatedCmHandles == [CmHandleRegistrationResponse.createFailureResponse('ch-1', NcmpResponseStatus.ALTERNATE_ID_ALREADY_ASSOCIATED)]

        and: 'the CM-handle still has the old alternate ID'
            assert objectUnderTest.getNcmpServiceCmHandle('ch-1').alternateId == 'original'

        cleanup: 'deregister CM handles'
            deregisterCmHandle(DMI1_URL, 'ch-1')
    }

}
