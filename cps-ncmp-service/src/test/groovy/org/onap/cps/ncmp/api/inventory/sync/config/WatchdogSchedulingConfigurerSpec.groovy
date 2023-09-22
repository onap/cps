/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2023 Nordix Foundation
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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

package org.onap.cps.ncmp.api.inventory.sync.config

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.onap.cps.ncmp.api.impl.inventory.sync.config.WatchdogSchedulingConfigurer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@SpringBootTest
@ContextConfiguration(classes = [ConfigurableApplicationContext, WatchdogSchedulingConfigurer])
class WatchdogSchedulingConfigurerSpec extends Specification {

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    def watchdogSchedulingConfigurer;

    @BeforeEach
    void setup() {
        watchdogSchedulingConfigurer = (WatchdogSchedulingConfigurer) applicationContext.getBean("watchdogSchedulingConfigurer")
    }

    @AfterEach
    void tearDown() {
        if (applicationContext != null) {
            applicationContext.close()
        }
    }

    def 'Validate watchdog scheduling configuration'() {
        given: 'task scheduler configuration properties are loaded as map'
            def linkedHashMap = watchdogSchedulingConfigurer.taskScheduler().getProperties()
        expect: 'thread name prefix is mapped correctly'
            assert linkedHashMap.'threadNamePrefix' == 'watchdog-th-'
    }
}
