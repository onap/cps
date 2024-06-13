/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021-2022 Bell Canada
 *  Modifications Copyright (C) 2023 TechMahindra Ltd.
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


import org.onap.cps.ncmp.api.ParameterizedCmHandleQueryService
import org.onap.cps.ncmp.api.impl.inventory.CmHandleQueryService
import org.onap.cps.ncmp.api.models.CmHandleQueryServiceParameters
import org.onap.cps.spi.model.ConditionProperties
import spock.lang.Specification
import org.onap.cps.ncmp.api.models.DmiPluginRegistration

class NetworkCmProxyInventoryFacadeSpec extends Specification {

    def mockCmHandleRegistrationService = Mock(CmHandleRegistrationService)
    def mockCmHandleQueryService = Mock(CmHandleQueryService)
    def mockParameterizedCmHandleQueryService = Mock(ParameterizedCmHandleQueryService)

    def objectUnderTest = new NetworkCmProxyInventoryFacade(mockCmHandleRegistrationService, mockCmHandleQueryService, mockParameterizedCmHandleQueryService)

    def 'Update DMI Registration'() {
        given: 'an (updated) dmi plugin registration'
            def dmiPluginRegistration = Mock(DmiPluginRegistration)
        when: 'the registration is submitted '
           objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'the call is delegated to the cm handle registration service'
            1 * mockCmHandleRegistrationService.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
    }

    def 'Execute cm handle id search for inventory'() {
        given: 'a ConditionApiProperties object'
            def conditionProperties = new ConditionProperties()
            conditionProperties.conditionName = 'hasAllProperties'
            conditionProperties.conditionParameters = [ [ 'some-key' : 'some-value' ] ]
            def cmHandleQueryServiceParameters = new CmHandleQueryServiceParameters()
            cmHandleQueryServiceParameters.cmHandleQueryParameters = [conditionProperties] as List<ConditionProperties>
        and: 'the system returns an set of cmHandle ids'
            mockParameterizedCmHandleQueryService.queryCmHandleIdsForInventory(*_) >> [ 'cmHandle1', 'cmHandle2' ]
        when: 'executing the search'
            def result = objectUnderTest.executeParameterizedCmHandleIdSearch(cmHandleQueryServiceParameters)
        then: 'the result returns the correct 2 elements'
            assert result.size() == 2
            assert result.contains('cmHandle1')
            assert result.contains('cmHandle2')
    }

    def 'Get all cm handle IDs by DMI plugin identifier.' () {
        given: 'cm handle queries service returns cm handles'
            1 * mockCmHandleQueryService.getCmHandleIdsByDmiPluginIdentifier('some-dmi-plugin-identifier') >> ['cm-handle-1','cm-handle-2']
        when: 'cm handle Ids are requested with dmi plugin identifier'
            def result = objectUnderTest.getAllCmHandleIdsByDmiPluginIdentifier('some-dmi-plugin-identifier')
        then: 'the result size is correct'
            assert result.size() == 2
        and: 'the result returns the correct details'
            assert result.containsAll('cm-handle-1','cm-handle-2')
    }

}
