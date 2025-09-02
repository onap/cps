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

class ReadinessManagerSpec extends Specification {

    def readinessManager = new ReadinessManager()

    def 'Default readiness state'() {
        expect: 'by default, cps service is Up and running'
            assert readinessManager.isReady() == true
    }

    def 'Readiness state during model loading'() {
        when: 'some loader process is registered'
            readinessManager.registerStartupProcess('someLoader')
        then: 'readiness manager report system is NOT ready'
            assert readinessManager.isReady() == false
            assert readinessManager.getStartupProcessesAsString() == 'someLoader'
    }

    def 'Readiness state transitions'() {
        given: 'multiple loader processes are registered'
            readinessManager.registerStartupProcess('someLoader-1')
            readinessManager.registerStartupProcess('someLoader-2')
        when: 'one process completes'
            readinessManager.markStartupProcessComplete('someLoader-1')
        then: 'still system is reposted as NOT READY with active loader name'
            assert readinessManager.isReady() == false
            assert readinessManager.getStartupProcessesAsString() == 'someLoader-2'
        when: 'the last process completes'
            readinessManager.markStartupProcessComplete('someLoader-2')
        then: 'all processes completed, service is ready without any active loader'
            assert readinessManager.isReady() == true
            assert readinessManager.getStartupProcessesAsString() == ''
    }
}
