/*
 * ============LICENSE_START========================================================
 *  Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.config.embeddedcache

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@SpringBootTest
@ContextConfiguration(classes = [SynchronizationSemaphoresConfig])
class SynchronizationSemaphoresConfigSpec extends Specification {

    @Autowired
    @Qualifier(value = "moduleSyncSemaphore")
    private Map<String, String> moduleSyncSemaphore;

    @Autowired
    @Qualifier(value = "dataSyncSemaphore")
    private Map<String, String> dataSyncSemaphore;

    def 'Embedded Sync Semaphores'() {
        expect: 'system is able to create an instance of ModuleSyncSemaphore'
            assert null != moduleSyncSemaphore
        and: 'system is able to create an instance of DataSyncSemaphore'
            assert null != dataSyncSemaphore
    }
}
