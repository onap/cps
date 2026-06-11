/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.integration.functional.ncmp.security

import org.onap.cps.api.exceptions.DataValidationException
import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistration
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.impl.dmi.DmiPluginUrlValidator

class DmiPluginUrlValidationIntegrationSpec extends CpsIntegrationSpecBase {

    def objectUnderTest

    def setup() {
        objectUnderTest = networkCmProxyInventoryFacade
    }

    def 'DMI registration with valid http URL.'() {
        given: 'a registration with the mock DMI server URL'
            dmiDispatcher1.moduleNamesPerCmHandleId['ch-valid-url'] = ['M1', 'M2']
            def cmHandleToCreate = new NcmpServiceCmHandle(cmHandleId: 'ch-valid-url')
            def registration = new DmiPluginRegistration(dmiPlugin: DMI1_URL, createdCmHandles: [cmHandleToCreate])
        when: 'the registration is submitted'
            def result = objectUnderTest.updateDmiRegistration(registration)
        then: 'registration is successful'
            assert result.createdCmHandles[0].status.name() == 'SUCCESS'
        cleanup:
            deregisterCmHandle(DMI1_URL, 'ch-valid-url')
    }

    def 'DMI registration with invalid scheme.'() {
        given: 'a registration with an ftp URL'
            def cmHandleToCreate = new NcmpServiceCmHandle(cmHandleId: 'ch-bad-scheme')
            def registration = new DmiPluginRegistration(dmiPlugin: 'ftp://some-host:21', createdCmHandles: [cmHandleToCreate])
        when: 'the registration is submitted'
            objectUnderTest.updateDmiRegistration(registration)
        then: 'a DataValidationException is thrown'
            thrown(DataValidationException)
    }

    def 'DMI registration with missing host.'() {
        given: 'a registration with a URL that has no valid host'
            def cmHandleToCreate = new NcmpServiceCmHandle(cmHandleId: 'ch-no-host')
            def registration = new DmiPluginRegistration(dmiPlugin: 'http://:8080', createdCmHandles: [cmHandleToCreate])
        when: 'the registration is submitted'
            objectUnderTest.updateDmiRegistration(registration)
        then: 'a DataValidationException is thrown'
            thrown(DataValidationException)
    }

    def 'DMI registration not adhering to allowed URL pattern.'() {
        given: 'a validator configured with a restrictive pattern'
            def restrictedValidator = new DmiPluginUrlValidator('.dmi')
        and: 'a registration with a URL not matching the pattern'
            def registration = new DmiPluginRegistration(dmiPlugin: 'http://not-allowed-host:8080')
        when: 'validation is performed directly'
            restrictedValidator.validateDmiPluginUrls(registration)
        then: 'a DataValidationException is thrown'
            thrown(DataValidationException)
    }
}
