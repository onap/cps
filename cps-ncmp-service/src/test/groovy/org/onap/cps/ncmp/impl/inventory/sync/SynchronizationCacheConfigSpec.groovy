/*
 * ============LICENSE_START========================================================
 *  Copyright (C) 2022-2024 Nordix Foundation
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

import com.hazelcast.collection.ISet
import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import com.hazelcast.map.IMap
import org.onap.cps.spi.model.DataNode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
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
    BlockingQueue<DataNode> moduleSyncWorkQueue

    @Autowired
    @Qualifier("moduleSyncStartedOnCmHandles")
    IMap<String, Object> moduleSyncStartedOnCmHandles

    @Autowired
    IMap<String, Boolean> dataSyncSemaphores

    @Autowired
    ISet<String> moduleSetTagsBeingProcessed

    def cleanupSpec() {
        Hazelcast.getHazelcastInstanceByName('cps-and-ncmp-hazelcast-instance-test-config').shutdown()
    }

    def 'Embedded (hazelcast) Caches for Module and Data Sync.'() {
        expect: 'system is able to create an instance of the Module Sync Work Queue'
            assert null != moduleSyncWorkQueue
        and: 'system is able to create an instance of a map to hold cm handles which have started (and maybe finished) module sync'
            assert null != moduleSyncStartedOnCmHandles
        and: 'system is able to create an instance of a map to hold data sync semaphores'
            assert null != dataSyncSemaphores
        and: 'system is able to create an instance of a set to hold module set tags being processed'
            assert null != moduleSetTagsBeingProcessed
        and: 'there is only one instance with the correct name'
            assert Hazelcast.allHazelcastInstances.size() == 1
            assert Hazelcast.allHazelcastInstances.name[0] == 'cps-and-ncmp-hazelcast-instance-test-config'
    }

    def 'Verify configs for Distributed objects'(){
        given: 'hazelcast common config'
            def hzConfig = Hazelcast.getHazelcastInstanceByName('cps-and-ncmp-hazelcast-instance-test-config').config
        and: 'the Module Sync Work Queue config'
            def moduleSyncDefaultWorkQueueConfig =  hzConfig.queueConfigs.get('defaultQueueConfig')
        and: 'the Module Sync Started Cm Handle Map config'
            def moduleSyncStartedOnCmHandlesMapConfig =  hzConfig.mapConfigs.get('moduleSyncStartedConfig')
        and: 'the Data Sync Semaphores Map config'
            def dataSyncSemaphoresMapConfig =  hzConfig.mapConfigs.get('dataSyncSemaphoresConfig')
        expect: 'system created instance with correct config of Module Sync Work Queue'
            assert moduleSyncDefaultWorkQueueConfig.backupCount == 1
            assert moduleSyncDefaultWorkQueueConfig.asyncBackupCount == 0
        and: 'Module Sync Started Cm Handle Map has the correct settings'
            assert moduleSyncStartedOnCmHandlesMapConfig.backupCount == 1
            assert moduleSyncStartedOnCmHandlesMapConfig.asyncBackupCount == 0
        and: 'Data Sync Semaphore Map has the correct settings'
            assert dataSyncSemaphoresMapConfig.backupCount == 1
            assert dataSyncSemaphoresMapConfig.asyncBackupCount == 0
        and: 'all instances are part of same cluster'
            assert hzConfig.clusterName == 'cps-and-ncmp-test-caches'
    }

    def 'Verify deployment network configs for Distributed objects'() {
        given: 'common hazelcast network config'
            def hzConfig = Hazelcast.getHazelcastInstanceByName('cps-and-ncmp-hazelcast-instance-test-config').config.networkConfig
        and: 'all configs has the correct settings'
            assert hzConfig.join.autoDetectionConfig.enabled
            assert !hzConfig.join.kubernetesConfig.enabled
    }

    def 'Verify network config'() {
        given: 'Synchronization config object and test configuration'
            def objectUnderTest = new SynchronizationCacheConfig()
            def testConfig = new Config()
        when: 'kubernetes properties are enabled'
            objectUnderTest.cacheKubernetesEnabled = true
            objectUnderTest.cacheKubernetesServiceName = 'test-service-name'
        and: 'method called to update the discovery mode'
            objectUnderTest.updateDiscoveryMode(testConfig)
        then: 'applied properties are reflected'
            assert testConfig.networkConfig.join.kubernetesConfig.enabled
            assert testConfig.networkConfig.join.kubernetesConfig.properties.get('service-name') == 'test-service-name'
    }

    def 'Time to Live Verify for Module Sync Semaphore'() {
        when: 'the key is inserted with a TTL of 1 second (Hazelcast TTL resolution is seconds!)'
            moduleSyncStartedOnCmHandles.put('testKeyModuleSync', 'toBeExpired' as Object, 1, TimeUnit.SECONDS)
        then: 'the entry is present in the map'
            assert moduleSyncStartedOnCmHandles.get('testKeyModuleSync') != null
        and: 'the entry expires'
            new PollingConditions().within(10) {
                assert moduleSyncStartedOnCmHandles.get('testKeyModuleSync') == null
            }
    }

    def 'Time to Live Verify for Data Sync Semaphore'() {
        when: 'the key is inserted with a TTL of 1 second'
            dataSyncSemaphores.put('testKeyDataSync', Boolean.TRUE, 1, TimeUnit.SECONDS)
        then: 'the entry is present in the map'
            assert dataSyncSemaphores.get('testKeyDataSync') != null
        and: 'the entry expires'
            new PollingConditions().within(10) {
                assert dataSyncSemaphores.get('testKeyDataSync') == null
            }
    }

}
