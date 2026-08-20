/*
 * ============LICENSE_START========================================================
 *  Copyright (C) 2022-2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.inventory.sync

import com.hazelcast.collection.impl.queue.QueueProxyImpl
import com.hazelcast.core.Hazelcast
import com.hazelcast.map.IMap
import com.hazelcast.map.impl.proxy.MapProxyImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit

@SpringBootTest
@ContextConfiguration(classes = [SynchronizationCacheConfig])
class SynchronizationCacheConfigSpec extends Specification {

    @Autowired
    BlockingQueue<String> moduleSyncWorkQueue

    @Autowired
    IMap<String, Object> moduleSyncStartedOnCmHandles

    @Autowired
    IMap<String, Boolean> dataSyncSemaphores

    def cleanupSpec() {
        Hazelcast.getHazelcastInstanceByName('cps-and-ncmp-hazelcast-instance-test-config').shutdown()
    }

    def 'Cache map configurations for Module Sync.'() {
        given: 'get each map'
            def hazelcastMap = mapName == 'moduleSyncStartedOnCmHandles' ?  moduleSyncStartedOnCmHandles : dataSyncSemaphores
        when: 'retrieving the map config for #mapName'
            def hazelcastInstance = ((MapProxyImpl) hazelcastMap).getNodeEngine().getHazelcastInstance()
            def config = hazelcastInstance.getConfig().findMapConfig(hazelcastMap.getName())
        then: 'the expected configuration is used'
            assert config.name == expectedConfigName
        and: 'the map config has the correct (default) backup counts'
            assert config.backupCount == 1
            assert config.asyncBackupCount == 0
        and: 'time to live is only set for module sync started on cm handles'
            assert config.getTimeToLiveSeconds() == expectedTimeToLiveSeconds
        where: 'the following caches are used'
            mapName                        || expectedConfigName             | expectedTimeToLiveSeconds
            'moduleSyncStartedOnCmHandles' || 'moduleSyncStartedOnCmHandles' | 600
            'dataSyncSemaphores'           || '*'                            |  0
    }

    def 'Cache queue configuration for Module Sync.'() {
        given: 'the queue config'
            def hazelcastInstance = ((QueueProxyImpl) moduleSyncWorkQueue).getNodeEngine().getHazelcastInstance()
            def config = hazelcastInstance.getConfig().findQueueConfig('moduleSyncWorkQueue')
        expect: 'the default configuration is used'
            assert config.name == '*'
        and: 'the configuration has the correct (default) backup counts'
            assert config.backupCount == 1
            assert config.asyncBackupCount == 0
    }

    def 'Time to Live on Module Started on Cm Handle.'() {
        when: 'the key is inserted with a TTL of 100ms'
            moduleSyncStartedOnCmHandles.put('testKeyModuleSync', 'toBeExpired' as Object, 100, TimeUnit.MILLISECONDS)
        then: 'the entry expires within a second'
            new PollingConditions().within(1) {
                assert moduleSyncStartedOnCmHandles.get('testKeyModuleSync') == null
            }
    }

    def 'Time to Live on Data Sync Semaphore'() {
        when: 'the key is inserted with a TTL of 100ms'
            dataSyncSemaphores.put('testKeyDataSync', Boolean.TRUE, 100, TimeUnit.MILLISECONDS)
        then: 'the entry expires within a second'
            new PollingConditions().within(1) {
                assert dataSyncSemaphores.get('testKeyDataSync') == null
            }
    }

}
