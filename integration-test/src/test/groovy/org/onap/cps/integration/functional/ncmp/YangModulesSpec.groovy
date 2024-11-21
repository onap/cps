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

import static org.hamcrest.Matchers.containsInAnyOrder
import static org.hamcrest.Matchers.emptyString
import static org.hamcrest.Matchers.everyItem
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.hasSize
import static org.hamcrest.Matchers.is
import static org.hamcrest.Matchers.not
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class YangModulesSpec extends CpsIntegrationSpecBase {

    def setup() {
        dmiDispatcher1.moduleNamesPerCmHandleId['ch-1'] = ['M1', 'M2']
        dmiDispatcher1.moduleNamesPerCmHandleId['ch-2'] = ['M1', 'M3']
        registerCmHandle(DMI1_URL, 'ch-1', NO_MODULE_SET_TAG, 'alt-1')
        registerCmHandle(DMI1_URL, 'ch-2', NO_MODULE_SET_TAG, 'alt-2')
        // Note DMI dispatcher is not configured to return modules for this handle, so module sync will fail
        registerCmHandleWithoutWaitForReady(DMI1_URL, 'not-ready-id', NO_MODULE_SET_TAG, NO_ALTERNATE_ID)
    }

    def cleanup() {
        deregisterCmHandles(DMI1_URL, ['ch-1', 'ch-2', 'not-ready-id'])
    }

    def 'Get yang module references returns expected modules with #scenario.'() {
        expect: 'get module references API to return expected modules'
            mvc.perform(get("/ncmp/v1/ch/${cmHandleReference}/modules"))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(jsonPath('$', hasSize(expectedModuleNames.size())))
                    .andExpect(jsonPath('$[*].moduleName', containsInAnyOrder(expectedModuleNames.toArray())))
                    .andExpect(jsonPath('$[*].revision', everyItem(equalTo('2024-01-01'))))
        where: 'following scenarios are applied'
            scenario                 | cmHandleReference || expectedModuleNames
            'cm-handle id'           | 'ch-1'            || ['M1', 'M2']
            'alternate id'           | 'alt-2'           || ['M1', 'M3']
            'not ready CM handle'    | 'not-ready-id'    || []
            'non-existing CM handle' | 'non-existing'    || []
    }

    def 'Get yang module definitions returns expected modules with #scenario.'() {
        expect: 'get module definitions API to return expected module definitions'
            mvc.perform(get("/ncmp/v1/ch/${cmHandleReference}/modules/definitions"))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(jsonPath('$', hasSize(expectedModuleNames.size())))
                    .andExpect(jsonPath('$[*].moduleName', containsInAnyOrder(expectedModuleNames.toArray())))
                    .andExpect(jsonPath('$[*].revision', everyItem(equalTo('2024-01-01'))))
                    .andExpect(jsonPath('$[*].content', not(is(emptyString()))))
        where: 'following scenarios are applied'
            scenario                 | cmHandleReference || expectedModuleNames
            'cm-handle id'           | 'ch-1'            || ['M1', 'M2']
            'alternate id'           | 'alt-2'           || ['M1', 'M3']
            'not ready CM handle'    | 'not-ready-id'    || []
            'non-existing CM handle' | 'non-existing'    || []
    }

    def 'Get yang module definition for specific module with #scenario.'() {
        expect: 'get module definition API to return definition of requested module name and revision'
            mvc.perform(get("/ncmp/v1/ch/${cmHandleReference}/modules/definitions")
                    .queryParam('module-name', requestedModuleName)
                    .queryParam('revision', '2024-01-01'))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(jsonPath('$', hasSize(expectedModuleNames.size())))
                    .andExpect(jsonPath('$[*].moduleName', containsInAnyOrder(expectedModuleNames.toArray())))
                    .andExpect(jsonPath('$[*].revision', everyItem(equalTo('2024-01-01'))))
                    .andExpect(jsonPath('$[*].content', not(is(emptyString()))))
        where: 'following scenarios are applied'
            scenario                 | cmHandleReference | requestedModuleName || expectedModuleNames
            'cm-handle id'           | 'ch-1'            | 'M1'                || ['M1']
            'alternate id'           | 'alt-2'           | 'M1'                || ['M1']
            'non-existing module'    | 'ch-1'            | 'non-existing'      || []
            'not ready CM handle'    | 'not-ready-id'    | 'not-relevant'      || []
            'non-existing CM handle' | 'non-existing'    | 'not-relevant'      || []
    }

}
