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

package org.onap.cps.ncmp.api.impl.config.embeddedcache

import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import com.hazelcast.map.IMap
import org.onap.cps.ncmp.api.impl.trustlevel.TrustLevel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = [DmiPluginTrustLevelCacheConfig])
class DmiPluginTrustLevelCacheConfigSpec extends Specification {

    @Autowired
    private IMap<String, Map<String, String>> dmiPluginTrustLevelCache

    def 'Hazelcast cache for Dmi Plugin Trust Level Cache'() {
        expect: 'system is able to create an instance of the Dmi Plugin Trust Level Cache'
            assert null != dmiPluginTrustLevelCache
        and: 'there is at least 1 instance'
            assert Hazelcast.allHazelcastInstances.size() > 0
        and: 'Dmi Plugin Trust Level Cache is present'
            assert Hazelcast.allHazelcastInstances.name.contains('hazelcastInstanceDmiPluginTrustLevelMap')
    }

    def 'Verify Dmi Plugin Trust Level Cache for basic hazelcast map operations'() {
        when: 'the key inserted into Dmi Plugin Trust Level Cache'
            dmiPluginTrustLevelCache.put('some-dmi', ['trustLevel': TrustLevel.COMPLETE, 'healthCheckUrl':'http://localhost:8080/actuator/health'] as Map)
        then: 'the key is being get from Dmi Plugin Trust Level Cache'
            def getResult = dmiPluginTrustLevelCache.get('some-dmi')
        and: 'the entry is present in the map as expected'
            assert TrustLevel.COMPLETE == getResult.get('trustLevel')
            assert 'http://localhost:8080/actuator/health' == getResult.get('healthCheckUrl')
    }

    def 'Verify configs for Distributed Caches'(){
        given: 'the Dmi Plugin Trust Level Cache config'
            def dmiPluginTrustLevelCacheConfig =  Hazelcast.getHazelcastInstanceByName('hazelcastInstanceDmiPluginTrustLevelMap').config
            def dmiPluginTrustLevelCacheMapConfig =  dmiPluginTrustLevelCacheConfig.mapConfigs.get('dmiPluginTrustLevelCacheConfig')
        expect: 'system created instance with correct config'
            assert dmiPluginTrustLevelCacheConfig.clusterName == 'cps-and-ncmp-test-caches'
            assert dmiPluginTrustLevelCacheMapConfig.backupCount == 3
            assert dmiPluginTrustLevelCacheMapConfig.asyncBackupCount == 3
    }

    def 'Verify deployment network configs for Distributed Caches'() {
        given: 'the Dmi Plugin Trust Level Cache config'
            def dmiPluginTrustLevelCacheNetworkConfig = Hazelcast.getHazelcastInstanceByName('hazelcastInstanceDmiPluginTrustLevelMap').config.networkConfig
        expect: 'system created instance with correct config'
            assert dmiPluginTrustLevelCacheNetworkConfig.join.autoDetectionConfig.enabled
            assert !dmiPluginTrustLevelCacheNetworkConfig.join.kubernetesConfig.enabled
    }

    def 'Verify network config'() {
        given: 'Synchronization config object and test configuration'
            def objectUnderTest = new DmiPluginTrustLevelCacheConfig()
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

}
