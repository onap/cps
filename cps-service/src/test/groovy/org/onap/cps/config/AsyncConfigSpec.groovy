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

package org.onap.cps.config

import spock.lang.Specification

class AsyncConfigSpec extends Specification {

    def objectUnderTest = new AsyncConfig()

    def 'Create Async Config and validate it'() {
        when: 'we set some test properties to tune taskexecutor'
            objectUnderTest.setCorePoolSize(5)
            objectUnderTest.setMaxPoolSize(50)
            objectUnderTest.setQueueCapacity(100)
            objectUnderTest.setThreadNamePrefix('Test-')
            objectUnderTest.setWaitForTasksToCompleteOnShutdown(true)
        then: 'we can instantiate a Async Config object'
            objectUnderTest != null
        and: 'taskexector is configured with correct properties'
            def tasExecutor = objectUnderTest.getThreadAsyncExecutorForNotification()
            assert tasExecutor.properties['corePoolSize'] == 5
            assert tasExecutor.properties['maxPoolSize'] == 50
            assert tasExecutor.properties['queueCapacity'] == 100
            assert tasExecutor.properties['keepAliveSeconds'] == 60
            assert tasExecutor.properties['threadNamePrefix'] == 'Test-'
    }
}
