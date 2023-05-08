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

package org.onap.cps.ncmp.api.impl.event.avc

import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import com.hazelcast.map.IMap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = [ForwardedSubscriptionEventCacheConfig])
class ForwardedSubscriptionEventCacheConfigSpec extends Specification {

    @Autowired
    private IMap<String, Set<String>> forwardedSubscriptionEventCache

    def 'Embedded (hazelcast) cache for Forwarded Subscription Event Cache.'() {
        expect: 'system is able to create an instance of the Forwarded Subscription Event Cache'
            assert null != forwardedSubscriptionEventCache
        and: 'there is at least 1 instance'
            assert Hazelcast.allHazelcastInstances.size() > 0
        and: 'Forwarded Subscription Event Cache is present'
            assert Hazelcast.allHazelcastInstances.name.contains('hazelCastInstanceSubscriptionEvents')
    }

    def 'Verify configs for Distributed Caches'(){
        given: 'the Forwarded Subscription Event Cache config'
            def forwardedSubscriptionEventCacheConfig =  Hazelcast.getHazelcastInstanceByName('hazelCastInstanceSubscriptionEvents').config
            def forwardedSubscriptionEventCacheMapConfig =  forwardedSubscriptionEventCacheConfig.mapConfigs.get('forwardedSubscriptionEventCacheMapConfig')
        expect: 'system created instance with correct config'
            assert forwardedSubscriptionEventCacheConfig.clusterName == 'cps-and-ncmp-test-caches'
            assert forwardedSubscriptionEventCacheMapConfig.backupCount == 3
            assert forwardedSubscriptionEventCacheMapConfig.asyncBackupCount == 3
            assert forwardedSubscriptionEventCacheMapConfig.timeToLiveSeconds == 60
    }

    def 'Verify deployment network configs for Distributed Caches'() {
        given: 'the Forwarded Subscription Event Cache config'
            def forwardedSubscriptionEventCacheNetworkConfig = Hazelcast.getHazelcastInstanceByName('hazelCastInstanceSubscriptionEvents').config.networkConfig
        expect: 'system created instance with correct config'
            assert forwardedSubscriptionEventCacheNetworkConfig.join.autoDetectionConfig.enabled
            assert !forwardedSubscriptionEventCacheNetworkConfig.join.kubernetesConfig.enabled
    }

    def 'Verify network config'() {
        given: 'Synchronization config object and test configuration'
            def objectUnderTest = new ForwardedSubscriptionEventCacheConfig()
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
