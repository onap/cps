/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023-2025 Nordix Foundation.
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

package org.onap.cps.config

import com.hazelcast.map.IMap
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import spock.lang.Specification

class MicroMeterConfigSpec extends Specification {

    def cmHandlesByState = Mock(IMap)
    def objectUnderTest = new MicroMeterConfig(cmHandlesByState)
    def simpleMeterRegistry = new SimpleMeterRegistry()

    def 'Creating a timed aspect.'() {
        expect: ' a timed aspect can be created'
            assert objectUnderTest.timedAspect(simpleMeterRegistry) != null
    }

    def 'Creating gauges for cm handle states.'() {
        given: 'cache returns value for each state'
            cmHandlesByState.get(_) >> 1
        when: 'gauges for each state are created'
             objectUnderTest.advisedCmHandles(simpleMeterRegistry)
             objectUnderTest.readyCmHandles(simpleMeterRegistry)
             objectUnderTest.lockedCmHandles(simpleMeterRegistry)
             objectUnderTest.deletingCmHandles(simpleMeterRegistry)
             objectUnderTest.deletedCmHandles(simpleMeterRegistry)
        then: 'each state has the correct value when queried'
            ['ADVISED', 'READY', 'LOCKED', 'DELETING', 'DELETED'].each { state ->
                def gaugeValue = simpleMeterRegistry.get('cmHandlesByState').tag('state',state).gauge().value()
                assert gaugeValue == 1
            }
    }

}
