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

import com.hazelcast.core.Hazelcast
import com.hazelcast.map.IMap
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.CmSubscriptionCacheObject
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.CmSubscriptionStatus
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.ScopeFilter
import org.onap.cps.ncmp.api.impl.operations.DatastoreType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = [CmSubscriptionEventCacheConfig])
class CmSubscriptionEventCacheConfigSpec extends Specification {

    @Autowired
    IMap<String, Map<String, CmSubscriptionCacheObject>> cmSubscriptionEventCache;

    def 'Embedded (hazelcast) cache for Cm Subscription Event Cache.'() {
        expect: 'system is able to create an instance of the Forwarded Subscription Event Cache'
            assert null != cmSubscriptionEventCache
        and: 'there is at least 1 instance'
            assert Hazelcast.allHazelcastInstances.size() > 0
        and: 'Forwarded Subscription Event Cache is present'
            assert Hazelcast.allHazelcastInstances.name.contains('hazelCastInstanceCmSubscriptionEvents')
    }

    def 'Provided CM Subscription data'() {
        given: 'a cm subscription properties'
            def subscriptionId = 'sub123'
            def dmiPluginName = 'dummydmi'
            def cmSubscriptionCacheObject = new CmSubscriptionCacheObject(targetFilter: ['cmhandle1', 'cmhandle2'], cmSubscriptionStatus: CmSubscriptionStatus.PENDING, scopeFilter: new ScopeFilter(datastoreType: DatastoreType.PASSTHROUGH_RUNNING, xpathFilter: ['/a/b/c']))
            def cmSubscriptionValue = [(dmiPluginName): cmSubscriptionCacheObject]
        when: 'the cache is populated'
            cmSubscriptionEventCache.put(subscriptionId, cmSubscriptionValue)
        then: 'the values are present in memory'
            assert cmSubscriptionEventCache.get(subscriptionId) != null
        and: 'properties match'
            assert dmiPluginName == cmSubscriptionEventCache.get(subscriptionId).keySet()[0]
            assert cmSubscriptionCacheObject.cmSubscriptionStatus == cmSubscriptionEventCache.get(subscriptionId).values()[0].cmSubscriptionStatus
            assert cmSubscriptionCacheObject.targetFilter == cmSubscriptionEventCache.get(subscriptionId).values()[0].targetFilter
    }
}
