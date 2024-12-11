/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 Nordix Foundation.
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

package org.onap.cps.ncmp.impl.inventory.sync.lcm

import static org.onap.cps.ncmp.impl.inventory.models.CmHandleState.ADVISED
import static org.onap.cps.ncmp.impl.inventory.models.CmHandleState.READY

import com.hazelcast.map.IMap
import spock.lang.Specification;

class CmHandleStateMonitorSpec extends Specification {

    def cmHandlesByState = Mock(IMap<String, Integer>)
    def objectUnderTest = new CmHandleStateMonitor(cmHandlesByState)

    def 'Update metric with state change'() {
        when: 'method to update metric with state change is called'
           objectUnderTest.updateMetricWithStateChange(ADVISED, READY)
        then: 'cm handle by state cache map is called once for current and target state'
            1 * cmHandlesByState.executeOnKey('readyCmHandlesCount', _)
            1 * cmHandlesByState.executeOnKey('advisedCmHandlesCount', _)

    }
}
