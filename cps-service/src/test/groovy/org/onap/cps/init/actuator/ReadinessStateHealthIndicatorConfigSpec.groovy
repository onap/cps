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

package org.onap.cps.init.actuator

import spock.lang.Specification;

class ReadinessStateHealthIndicatorConfigSpec extends Specification {

    def readinessManager = new ReadinessManager()
    def objectUnderTest = new ReadinessStateHealthIndicatorConfig(readinessManager)

    def 'CPS service UP when all loaders are completed'() {
        given: 'no loaders are in progress'
        when: 'cps health check is invoked'
            def cpsHealth = objectUnderTest.readinessStateHealthIndicator().getHealth(true)
        then: 'health status is UP with following message'
            assert cpsHealth.status.code == 'UP'
            assert cpsHealth.details['Startup Processes'] == 'All startup processes completed'
    }

    def 'CPS service is DOWN when any loader is in progress'() {
        given: 'any module loader is still running'
            readinessManager.registerStartupProcess('someLoader')
        when: 'cps health check is invoked'
            def cpsHealth = objectUnderTest.readinessStateHealthIndicator().getHealth(true)
        then: 'cps service is DOWN with loaders listed'
            assert cpsHealth.status.code == 'DOWN'
            def busyLoaders = cpsHealth.details['Startup Processes active']
            assert busyLoaders.contains('someLoader')
    }

    def 'CPS service is UP after loaders complete'() {
        given: 'a loader in progress'
            readinessManager.registerStartupProcess('someLoader')
        when: 'module loader completes'
            readinessManager.markStartupProcessComplete('someLoader')
            def health = objectUnderTest.readinessStateHealthIndicator().getHealth(true)
        then: 'cps health status flips to UP'
            assert health.status.code == 'UP'
            assert health.details['Startup Processes'] == 'All startup processes completed'
    }
}
