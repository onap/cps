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

package org.onap.cps.ncmp.config

import com.hazelcast.map.IMap
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.onap.cps.ncmp.impl.cache.AdminCacheConfig
import org.onap.cps.ncmp.impl.inventory.CmHandleQueryService
import org.onap.cps.ncmp.impl.inventory.sync.lcm.CmHandleStateMonitor
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import spock.lang.Specification

@SpringBootTest(classes = [CmHandleStateGaugeConfig, CmHandleStateMonitor, AdminCacheConfig])
@ContextConfiguration(classes = [CpsApplicationContext])
@TestPropertySource(properties = ["hazelcast.mode.kubernetes.enabled=false"])
class CmHandleStateGaugeConfigSpec extends Specification {

    @Autowired
    CpsApplicationContext cpsApplicationContext
    @SpringBean
    CmHandleQueryService cmHandleQueryService = Mock()
    @SpringBean
    MeterRegistry meterRegistry = Mock()

    def cmHandlesByState = Mock(IMap)
    def objectUnderTest = new CmHandleStateGaugeConfig(cmHandlesByState)
    def simpleMeterRegistry = new SimpleMeterRegistry()

    def 'Creating gauges for cm handle states.'() {
        given: 'cache returns a test value (123) for each state'
            cmHandlesByState.get(_) >> 123
        when: 'gauges for each state are created'
            objectUnderTest.advisedCmHandles(simpleMeterRegistry)
            objectUnderTest.readyCmHandles(simpleMeterRegistry)
            objectUnderTest.lockedCmHandles(simpleMeterRegistry)
            objectUnderTest.deletingCmHandles(simpleMeterRegistry)
            objectUnderTest.deletedCmHandles(simpleMeterRegistry)
        then: 'each state has the correct value when queried'
            ['ADVISED', 'READY', 'LOCKED', 'DELETING', 'DELETED'].each { state ->
                def gaugeValue = simpleMeterRegistry.get(objectUnderTest.CM_HANDLE_STATE_GAUGE).tag('state',state).gauge().value()
                assert gaugeValue == 123
            }
    }

    def 'Controlling order of bean initialization'() {
        when: 'cm handle state gauge config is retrieved'
            cpsApplicationContext.getCpsBean(CmHandleStateGaugeConfig.class)
        then: 'cm handle state monitor should already be available'
            cpsApplicationContext.getCpsBean(CmHandleStateMonitor.class) != null
    }

}
