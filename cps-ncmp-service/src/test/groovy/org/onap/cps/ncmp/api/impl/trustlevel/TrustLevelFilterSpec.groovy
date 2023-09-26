/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.trustlevel

import com.hazelcast.map.IMap
import spock.lang.Specification

class TrustLevelFilterSpec extends Specification {

    def targetTrustLevel = TrustLevel.COMPLETE

    def mockTrustLevelPerCmHandle = Mock(IMap<String, TrustLevel>)

    def objectUnderTest = new TrustLevelFilter(targetTrustLevel, mockTrustLevelPerCmHandle)

    def 'Obtain cm handle ids by a given trust level value'() {
        given: 'The hazelcast cache contains two cm handle with trust level complete and none'
            mockTrustLevelPerCmHandle.entrySet() >> [Map.entry('cmhandle1', TrustLevel.COMPLETE), Map.entry('cmhandle2', TrustLevel.NONE)]
        when: 'cm handles are retrieved'
            def result = objectUnderTest.getAllCmHandleIdsByTargetTrustLevel()
        then: 'the result is equal to expected result'
            result == ['cmhandle1'] as Set
    }


}
