/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation
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

package org.onap.cps.cache

import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import com.hazelcast.map.IMap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@SpringBootTest
@ContextConfiguration(classes = [AnchorDataCacheConfig])
class AnchorDataCacheConfigSpec extends Specification {

    @Autowired
    private IMap<String, AnchorDataCacheEntry> anchorDataCache

    def 'Embedded (hazelcast) cache for Anchor Data.'() {
        expect: 'system is able to create an instance of the Anchor data cache'
            assert null != anchorDataCache
        and: 'there is at least 1 instance'
            assert Hazelcast.allHazelcastInstances.size() > 0
        and: 'anchorDataCache is present'
            assert Hazelcast.allHazelcastInstances.name.contains('hazelCastInstanceCpsCore')
    }

    def 'Verify configs for Distributed Caches'(){
        given: 'the Anchor Data Cache config'
            def anchorDataCacheConfig =  Hazelcast.getHazelcastInstanceByName('hazelCastInstanceCpsCore').config
            def anchorDataCacheMapConfig =  anchorDataCacheConfig.mapConfigs.get('anchorDataCacheMapConfig')
        expect: 'system created instance with correct config'
            assert anchorDataCacheConfig.clusterName == 'cps-and-ncmp-test-caches'
            assert anchorDataCacheMapConfig.backupCount == 1
            assert anchorDataCacheMapConfig.asyncBackupCount == 0
    }

    def 'Verify deployment network configs for Distributed Caches'() {
        given: 'the Anchor Data Cache config'
            def anchorDataCacheNetworkConfig = Hazelcast.getHazelcastInstanceByName('hazelCastInstanceCpsCore').config.networkConfig
        expect: 'system created instance with correct config'
            assert anchorDataCacheNetworkConfig.join.autoDetectionConfig.enabled
            assert !anchorDataCacheNetworkConfig.join.kubernetesConfig.enabled
    }

    def 'Verify network config'() {
        given: 'Synchronization config object and test configuration'
            def objectUnderTest = new AnchorDataCacheConfig()
            def testConfig = new Config()
        when: 'kubernetes properties are enabled'
            objectUnderTest.kubernetesDiscoveryEnabled = true
            objectUnderTest.cacheKubernetesServiceName = 'test-service-name'
        and: 'method called to update the discovery mode'
            objectUnderTest.updateDiscoveryMode(testConfig)
        then: 'applied properties are reflected'
            assert testConfig.networkConfig.join.kubernetesConfig.enabled
            assert testConfig.networkConfig.join.kubernetesConfig.properties.get('service-name') == 'test-service-name'

    }

    def 'Verify docker network config'() {
        given: 'Synchronization config object and test configuration'
            def objectUnderTest = new AnchorDataCacheConfig()
            def testConfig = new Config()
        when: 'docker properties are enabled'
            objectUnderTest.kubernetesDiscoveryEnabled = false
            objectUnderTest.dockerDiscoveryEnabled = true
        and: 'method called to update the discovery mode'
            objectUnderTest.updateDiscoveryMode(testConfig)
        then: 'applied properties are reflected'
            assert testConfig.networkConfig.join.autoDetectionEnabled
    }

}
