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
import com.hazelcast.core.Hazelcast
import com.hazelcast.map.IMap
import org.onap.cps.cache.AnchorDataCacheConfig
import org.onap.cps.cache.AnchorDataCacheEntry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
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
            def forwardedSubscriptionEventCacheConfig =  Hazelcast.getHazelcastInstanceByName('hazelCastInstanceSubscriptionEvents').config.mapConfigs.get('forwardedSubscriptionEventCacheMapConfig')
        expect: 'system created instance with correct config'
            assert forwardedSubscriptionEventCacheConfig.backupCount == 3
            assert forwardedSubscriptionEventCacheConfig.asyncBackupCount == 3
    }
}
