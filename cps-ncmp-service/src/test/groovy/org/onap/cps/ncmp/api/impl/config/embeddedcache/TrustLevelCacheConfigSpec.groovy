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
import org.onap.cps.ncmp.api.impl.trustlevel.TrustLevel
import org.onap.cps.ncmp.api.impl.trustlevel.dmiavailability.DmiPluginStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = [TrustLevelCacheConfig])
class TrustLevelCacheConfigSpec extends Specification {

    @Autowired
    private Map<String, DmiPluginStatus> healthStatusPerDmiPlugin

    @Autowired
    private Map<String, TrustLevel> trustLevelPerCmHandle

    def 'Hazelcast cache for health status per dmi plugin'() {
        expect: 'system is able to create an instance of the health status per dmi plugin cache'
            assert null != healthStatusPerDmiPlugin
        and: 'there is at least 1 instance'
            assert Hazelcast.allHazelcastInstances.size() > 0
        and: 'Dmi Plugin Trust Level Cache is present'
            assert Hazelcast.allHazelcastInstances.name.contains('hazelcastInstanceHealthStatusPerDmiPluginMap')
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
            assert mapConfig.backupCount == 3
            assert mapConfig.asyncBackupCount == 3
        where: 'the following caches are used'
            scenario         | hazelcastInstanceName                          | hazelcastMapConfigName
            'cmhandle map'   | 'hazelcastInstanceTrustLevelPerCmHandleMap'    | 'trustLevelPerCmHandleCacheConfig'
            'dmi plugin map' | 'hazelcastInstanceHealthStatusPerDmiPluginMap' | 'healthStatusPerDmiPluginCacheConfig'
    }

    def 'Network config for health status per dmi plugin'() {
        given: 'the network config'
            def networkConfigForHealthStatusPerDmi = Hazelcast.getHazelcastInstanceByName('hazelcastInstanceHealthStatusPerDmiPluginMap').config.networkConfig
        expect: 'system created instance with correct config'
            assert networkConfigForHealthStatusPerDmi.join.autoDetectionConfig.enabled
            assert !networkConfigForHealthStatusPerDmi.join.kubernetesConfig.enabled
    }

    def 'Network config for trust level per cm handle'() {
        given: 'the network config'
            def networkConfigForTrustLevelPerCmHandle = Hazelcast.getHazelcastInstanceByName('hazelcastInstanceTrustLevelPerCmHandleMap').config.networkConfig
        expect: 'system created instance with correct config'
            assert networkConfigForTrustLevelPerCmHandle.join.autoDetectionConfig.enabled
            assert !networkConfigForTrustLevelPerCmHandle.join.kubernetesConfig.enabled
    }

    def 'Configuration for kubernetes properties and discovery mode'() {
        given: 'trust level config object and test configuration'
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
