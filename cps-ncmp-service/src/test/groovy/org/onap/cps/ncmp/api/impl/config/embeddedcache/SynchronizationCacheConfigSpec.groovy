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

import com.hazelcast.core.Hazelcast
import com.hazelcast.map.IMap
import org.onap.cps.spi.model.DataNode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import java.util.concurrent.BlockingQueue

@SpringBootTest
@ContextConfiguration(classes = [SynchronizationCacheConfig])
class SynchronizationCacheConfigSpec extends Specification {

    @Autowired
    private BlockingQueue<DataNode> moduleSyncWorkQueue

    @Autowired
    private IMap<String, Object> moduleSyncStartedOnCmHandles

    @Autowired
    private Map<String, Boolean> dataSyncSemaphores

    def 'Embedded (hazelcast) Caches for Module and Data Sync.'() {
        expect: 'system is able to create an instance of the Module Sync Work Queue'
            assert null != moduleSyncWorkQueue
        and: 'system is able to create an instance of a map to hold cm handles which have started (and maybe finished) module sync'
            assert null != moduleSyncStartedOnCmHandles
        and: 'system is able to create an instance of a map to hold data sync semaphores'
            assert null != dataSyncSemaphores
        and: 'there 3 instances'
            assert Hazelcast.allHazelcastInstances.size() == 3
        and: 'they have the correct names (in any order)'
            assert Hazelcast.allHazelcastInstances.name.containsAll('moduleSyncWorkQueue', 'moduleSyncStartedOnCmHandles', 'dataSyncSemaphores' )
    }
}
