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

package org.onap.cps.ncmp.api.impl.config.embeddedcache

import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import com.hazelcast.map.IMap
import org.onap.cps.spi.model.DataNode
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
    private BlockingQueue<DataNode> moduleSyncWorkQueue

    @Autowired
    private IMap<String, Object> moduleSyncStartedOnCmHandles

    @Autowired
    private IMap<String, Boolean> dataSyncSemaphores

    def 'Embedded (hazelcast) Caches for Module and Data Sync.'() {
        expect: 'system is able to create an instance of the Module Sync Work Queue'
            assert null != moduleSyncWorkQueue
        and: 'system is able to create an instance of a map to hold cm handles which have started (and maybe finished) module sync'
            assert null != moduleSyncStartedOnCmHandles
        and: 'system is able to create an instance of a map to hold data sync semaphores'
            assert null != dataSyncSemaphores
        and: 'there are at least 3 instances'
            assert Hazelcast.allHazelcastInstances.size() > 2
        and: 'they have the correct names (in any order)'
            assert Hazelcast.allHazelcastInstances.name.containsAll('moduleSyncWorkQueue', 'moduleSyncStartedOnCmHandles', 'dataSyncSemaphores')
    }

    def 'Verify configs for Distributed objects'(){
        given: 'the Module Sync Work Queue config'
            def moduleSyncWorkQueueConfig = Hazelcast.getHazelcastInstanceByName('moduleSyncWorkQueue').config
            def moduleSyncDefaultWorkQueueConfig =  moduleSyncWorkQueueConfig.queueConfigs.get('defaultQueueConfig')
        and: 'the Module Sync Started Cm Handle Map config'
            def moduleSyncStartedOnCmHandlesConfig =  Hazelcast.getHazelcastInstanceByName('moduleSyncStartedOnCmHandles').config
            def moduleSyncStartedOnCmHandlesMapConfig =  moduleSyncStartedOnCmHandlesConfig.mapConfigs.get('moduleSyncStartedConfig')
        and: 'the Data Sync Semaphores Map config'
            def dataSyncSemaphoresConfig =  Hazelcast.getHazelcastInstanceByName('dataSyncSemaphores').config
            def dataSyncSemaphoresMapConfig =  dataSyncSemaphoresConfig.mapConfigs.get('dataSyncSemaphoresConfig')
        expect: 'system created instance with correct config of Module Sync Work Queue'
            assert moduleSyncDefaultWorkQueueConfig.backupCount == 3
            assert moduleSyncDefaultWorkQueueConfig.asyncBackupCount == 3
        and: 'Module Sync Started Cm Handle Map has the correct settings'
            assert moduleSyncStartedOnCmHandlesMapConfig.backupCount == 3
            assert moduleSyncStartedOnCmHandlesMapConfig.asyncBackupCount == 3
        and: 'Data Sync Semaphore Map has the correct settings'
            assert dataSyncSemaphoresMapConfig.backupCount == 3
            assert dataSyncSemaphoresMapConfig.asyncBackupCount == 3
        and: 'all instances are part of same cluster'
            def testClusterName = 'cps-and-ncmp-test-caches'
            assert moduleSyncWorkQueueConfig.clusterName == testClusterName
            assert moduleSyncStartedOnCmHandlesConfig.clusterName == testClusterName
            assert dataSyncSemaphoresConfig.clusterName == testClusterName
    }

    def 'Verify deployment network configs for Distributed objects'() {
        given: 'the Module Sync Work Queue config'
            def queueNetworkConfig = Hazelcast.getHazelcastInstanceByName('moduleSyncWorkQueue').config.networkConfig
        and: 'the Module Sync Started Cm Handle Map config'
            def moduleSyncStartedOnCmHandlesNetworkConfig = Hazelcast.getHazelcastInstanceByName('moduleSyncStartedOnCmHandles').config.networkConfig
        and: 'the Data Sync Semaphores Map config'
            def dataSyncSemaphoresNetworkConfig = Hazelcast.getHazelcastInstanceByName('dataSyncSemaphores').config.networkConfig
        expect: 'system created instance with correct config of Module Sync Work Queue'
            assert queueNetworkConfig.join.autoDetectionConfig.enabled
            assert !queueNetworkConfig.join.kubernetesConfig.enabled
        and: 'Module Sync Started Cm Handle Map has the correct settings'
            assert moduleSyncStartedOnCmHandlesNetworkConfig.join.autoDetectionConfig.enabled
            assert !moduleSyncStartedOnCmHandlesNetworkConfig.join.kubernetesConfig.enabled
        and: 'Data Sync Semaphore Map has the correct settings'
            assert dataSyncSemaphoresNetworkConfig.join.autoDetectionConfig.enabled
            assert !dataSyncSemaphoresNetworkConfig.join.kubernetesConfig.enabled
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
        when: 'the key is inserted with a TTL of 2 second (Hazelcast TTL resolution is seconds!)'
            moduleSyncStartedOnCmHandles.put('testKeyModuleSync', 'toBeExpired' as Object, 2, TimeUnit.SECONDS)
        then: 'the entry is present in the map'
            assert moduleSyncStartedOnCmHandles.get('testKeyModuleSync') != null
        and: 'the entry expires within 10 seconds'
            new PollingConditions().within(10) {
                assert moduleSyncStartedOnCmHandles.get('testKeyModuleSync')== null
            }
    }

    def 'Time to Live Verify for Data Sync Semaphore'() {
        when: 'the key is inserted with a TTL of 2 second'
            dataSyncSemaphores.put('testKeyDataSync', Boolean.TRUE, 2, TimeUnit.SECONDS)
        then: 'the entry is present in the map'
            assert dataSyncSemaphores.get('testKeyDataSync') != null
        and: 'the entry expires within 10 seconds'
            new PollingConditions().within(10) {
                assert dataSyncSemaphores.get('testKeyDataSync')== null
            }
    }

}
