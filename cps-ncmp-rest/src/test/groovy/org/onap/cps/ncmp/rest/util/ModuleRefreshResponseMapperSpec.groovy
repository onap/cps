/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.rest.util

import org.onap.cps.ncmp.api.inventory.models.RefreshCmHandle
import spock.lang.Specification

class ModuleRefreshResponseMapperSpec extends Specification {

    def objectUnderTest = new ModuleRefreshResponseMapper()

    def 'Map grouped cm handles to a rest module refresh response.'() {
        given: 'refresh cm handles grouped by module set tag, with state and the sample flagged'
            def refreshCmHandlesByModuleSetTag = [
                'tag-A': [refreshCmHandle('alt-1', 'LOCKED', false), refreshCmHandle('ch-2', 'READY', true)],
                'tag-B': [refreshCmHandle('alt-3', 'READY', true)]
            ] as LinkedHashMap
        when: 'mapping to the rest response'
            def result = objectUnderTest.toRestModuleRefreshResponse(refreshCmHandlesByModuleSetTag)
        then: 'the response contains an entry per module set tag'
            assert result.cmHandlesByModuleSetTag.size() == 2
        and: 'the first entry contains the correct tag, references and states'
            def tagA = result.cmHandlesByModuleSetTag.find { it.moduleSetTag == 'tag-A' }
            assert tagA.cmHandles.cmHandleReference == ['alt-1', 'ch-2']
            assert tagA.cmHandles.cmHandleState == ['LOCKED', 'READY']
        and: 'only one sample (first READY) is selected for refresh'
            assert tagA.cmHandles.find { it.cmHandleReference == 'ch-2' }.selectedForRefresh
            assert tagA.cmHandles.find { it.cmHandleReference == 'alt-1' }.selectedForRefresh == false
        and: 'the second entry contains the correct tag and reference'
            def tagB = result.cmHandlesByModuleSetTag.find { it.moduleSetTag == 'tag-B' }
            assert tagB.cmHandles.cmHandleReference == ['alt-3']
    }

    def 'Map an empty grouping.'() {
        when: 'mapping an empty map'
            def result = objectUnderTest.toRestModuleRefreshResponse([:])
        then: 'the response contains an empty list'
            assert result.cmHandlesByModuleSetTag.isEmpty()
    }

    def refreshCmHandle(reference, state, selected) {
        return new RefreshCmHandle('some-cm-handle-id', reference, state, selected)
    }
}
