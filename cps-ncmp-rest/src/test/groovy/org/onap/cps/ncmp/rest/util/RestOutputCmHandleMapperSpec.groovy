/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.api.inventory.models.TrustLevel
import org.onap.cps.ncmp.rest.model.CmHandleCompositeState
import spock.lang.Specification

class RestOutputCmHandleMapperSpec extends Specification {

    CmHandleStateMapper mockCmHandleStateMapper = Mock()
    RestOutputCmHandleMapper objectUnderTest = new RestOutputCmHandleMapper(mockCmHandleStateMapper)

    def 'Map cm handles to rest output #scenario.'() {
        given: 'a cm handle with different states'
            def ncmpServiceCmHandle = createNcmpServiceCmHandle(trustLevel)
        and: 'the state mapper returns a composite state'
            mockCmHandleStateMapper.toCmHandleCompositeStateExternalLockReason(ncmpServiceCmHandle.getCompositeState()) >> new CmHandleCompositeState(cmHandleState: 'ADVISED')
        when: 'the mapper function is called'
            def result = objectUnderTest.toRestOutputCmHandle(ncmpServiceCmHandle, includeAdditionalProperties)
        then: 'result has the expected properties'
            if (includeAdditionalProperties) {
                assert result.cmHandleProperties.containsKey('additional property key')
            }
            if (trustLevel != null) {
                assert result.trustLevel == trustLevel.toString()
            }
            assert result.publicCmHandleProperties[0].containsKey('public property key')
            assert result.alternateId == 'alt-1'
            assert result.cmHandle == 'ch-1'
        where:
            scenario                        | includeAdditionalProperties || trustLevel
            'without additional properties' | false                       || null
            'with additional properties'    | true                        || TrustLevel.NONE
            'with trust level'              | false                       || TrustLevel.COMPLETE
    }

    def createNcmpServiceCmHandle(trustLevel) {
        return new NcmpServiceCmHandle(cmHandleId: 'ch-1', additionalProperties: ['additional property key': 'some value'],
                currentTrustLevel: trustLevel,
                publicProperties: ['public property key': 'public property value'],
                alternateId: 'alt-1', compositeState: new CompositeState(cmHandleState: 'ADVISED'))
    }
}
