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
import org.onap.cps.ncmp.api.inventory.models.TrustLevel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = [TrustLevelCacheConfig])
class TrustLevelCacheConfigSpec extends Specification {

    @Autowired
    private Map<String, TrustLevel> trustLevelPerDmiPlugin

    @Autowired
    private Map<String, TrustLevel> trustLevelPerCmHandle

    def 'Hazelcast cache for trust level per dmi plugin'() {
        expect: 'system is able to create an instance of the trust level per dmi plugin cache'
            assert null != trustLevelPerDmiPlugin
        and: 'there is at least 1 instance'
            assert Hazelcast.allHazelcastInstances.size() > 0
        and: 'Dmi Plugin Trust Level Cache is present'
            assert Hazelcast.allHazelcastInstances.name.contains('hazelcastInstanceTrustLevelPerDmiPluginMap')
    }

    def 'Hazelcast cache for trust level per cm handle'() {
        expect: 'system is able to create an instance of the trust level per cm handle cache'
            assert null != trustLevelPerCmHandle
        and: 'there is at least 1 instance'
            assert Hazelcast.allHazelcastInstances.size() > 0
        and: 'Hazelcast cache instance for trust level is present'
            assert Hazelcast.allHazelcastInstances.name.contains('hazelcastInstanceTrustLevelPerCmHandleMap')
    }

    def 'Trust level cache configurations: #scenario'() {
        when: 'retrieving the cache config for trustLevel'
            def cacheConfig = Hazelcast.getHazelcastInstanceByName(hazelcastInstanceName).config
        then: 'the cache config has the right cluster'
            assert cacheConfig.clusterName == 'cps-and-ncmp-test-caches'
        when: 'retrieving the map config for trustLevel'
            def mapConfig = cacheConfig.mapConfigs.get(hazelcastMapConfigName)
        then: 'the map config has the correct backup counts'
            assert mapConfig.backupCount == 1
            assert mapConfig.asyncBackupCount == 0
        where: 'the following caches are used'
            scenario         | hazelcastInstanceName                        | hazelcastMapConfigName
            'cmhandle map'   | 'hazelcastInstanceTrustLevelPerCmHandleMap'  | 'trustLevelPerCmHandleCacheConfig'
            'dmi plugin map' | 'hazelcastInstanceTrustLevelPerDmiPluginMap' | 'trustLevelPerDmiPluginCacheConfig'
    }

    def 'Verify deployment network configs for Distributed Caches'() {
        given: 'the Trust Level Per Dmi Plugin Cache config'
            def trustLevelDmiPerPluginCacheConfig = Hazelcast.getHazelcastInstanceByName('hazelcastInstanceTrustLevelPerDmiPluginMap').config.networkConfig
        expect: 'system created instance with correct config'
            assert trustLevelDmiPerPluginCacheConfig.join.autoDetectionConfig.enabled
            assert !trustLevelDmiPerPluginCacheConfig.join.kubernetesConfig.enabled
    }

    def 'Verify deployment network configs for Cm Handle Distributed Caches'() {
        given: 'the Trust Level Per Cm Handle Cache config'
            def trustLevelPerCmHandlePluginCacheConfig = Hazelcast.getHazelcastInstanceByName('hazelcastInstanceTrustLevelPerCmHandleMap').config.networkConfig
        expect: 'system created instance with correct config'
            assert trustLevelPerCmHandlePluginCacheConfig.join.autoDetectionConfig.enabled
            assert !trustLevelPerCmHandlePluginCacheConfig.join.kubernetesConfig.enabled
    }

    def 'Verify network config'() {
        given: 'Synchronization config object and test configuration'
            def objectUnderTest = new TrustLevelCacheConfig()
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
