/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2026 OpenInfra Foundation Europe. All rights reserved.
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

class ModulesRestApiSpec extends CpsIntegrationSpecBase {

    def setup() {
        dmiDispatcher1.moduleNamesPerCmHandleId['ch-1'] = ['M1', 'M2']
        dmiDispatcher1.moduleNamesPerCmHandleId['ch-2'] = ['M1', 'M3']
        dmiDispatcher1.moduleNamesPerCmHandleId['ch-3'] = ['M4']
        registerCmHandle(DMI1_URL, 'ch-1', NO_MODULE_SET_TAG, 'alt-1')
        registerCmHandle(DMI1_URL, 'ch-2', NO_MODULE_SET_TAG, 'alt-2')
        registerCmHandle(DMI1_URL, 'ch-3', 'my-module-set-tag', 'alt-3')
        registerCmHandleWithoutWaitForReady(DMI1_URL, 'not-ready-id', NO_MODULE_SET_TAG, NO_ALTERNATE_ID)
    }

    def cleanup() {
        deregisterCmHandles(DMI1_URL, ['ch-1', 'ch-2', 'ch-3', 'not-ready-id'])
    }

    def 'Get yang module references returns expected modules with #scenario.'() {
        when: 'get module references API is called'
            def response = performGet("/ncmp/v1/ch/${cmHandleReference}/modules")
        then: 'response is successful'
            assert response.statusCode.is2xxSuccessful()
        and: 'expected modules are returned'
            def modules = parseResponseBody(response) as List
            assert modules.size() == expectedModuleNames.size()
            assert modules*.moduleName.containsAll(expectedModuleNames)
            if (!modules.isEmpty()) {
                assert modules.every { it.revision == '2024-01-01' }
            }
        where: 'following scenarios are applied'
            scenario                        | cmHandleReference || expectedModuleNames
            'cm-handle id'                  | 'ch-1'            || ['M1', 'M2']
            'alternate id'                  | 'alt-2'           || ['M1', 'M3']
            'CM handle with module set tag' | 'ch-3'            || ['M4']
            'not ready CM handle'           | 'not-ready-id'    || []
            'non-existing CM handle'        | 'non-existing'    || []
    }

    def 'Get yang module definitions returns expected modules with #scenario.'() {
        when: 'get module definitions API is called'
            def response = performGet("/ncmp/v1/ch/${cmHandleReference}/modules/definitions")
        then: 'response is successful'
            assert response.statusCode.is2xxSuccessful()
        and: 'expected module definitions are returned'
            def modules = parseResponseBody(response) as List
            assert modules.size() == expectedModuleNames.size()
            assert modules*.moduleName.containsAll(expectedModuleNames)
            if (!modules.isEmpty()) {
                assert modules.every { it.revision == '2024-01-01' }
                assert modules.every { it.content }
            }
        where: 'following scenarios are applied'
            scenario                        | cmHandleReference || expectedModuleNames
            'cm-handle id'                  | 'ch-1'            || ['M1', 'M2']
            'alternate id'                  | 'alt-2'           || ['M1', 'M3']
            'CM handle with module set tag' | 'ch-3'            || ['M4']
            'not ready CM handle'           | 'not-ready-id'    || []
            'non-existing CM handle'        | 'non-existing'    || []
    }

    def 'Get yang module definition for specific module with #scenario.'() {
        when: 'get module definition API is called with specific module name and revision'
            def response = performGet("/ncmp/v1/ch/${cmHandleReference}/modules/definitions",
                    ['module-name': requestedModuleName, revision: '2024-01-01'])
        then: 'response is successful'
            assert response.statusCode.is2xxSuccessful()
        and: 'expected module definitions are returned'
            def modules = parseResponseBody(response) as List
            assert modules.size() == expectedModuleNames.size()
            assert modules*.moduleName.containsAll(expectedModuleNames)
            if (!modules.isEmpty()) {
                assert modules.every { it.revision == '2024-01-01' }
                assert modules.every { it.content }
            }
        where: 'following scenarios are applied'
            scenario                 | cmHandleReference | requestedModuleName || expectedModuleNames
            'cm-handle id'           | 'ch-1'            | 'M1'                || ['M1']
            'alternate id'           | 'alt-2'           | 'M1'                || ['M1']
            'non-existing module'    | 'ch-1'            | 'non-existing'      || []
            'not ready CM handle'    | 'not-ready-id'    | 'not-relevant'      || []
            'non-existing CM handle' | 'non-existing'    | 'not-relevant'      || []
    }

}
