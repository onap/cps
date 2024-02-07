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
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.CmNotificationSubscriptionStatus
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.DmiCmNotificationSubscriptionDetails
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.DmiCmNotificationSubscriptionPredicate
import org.onap.cps.ncmp.api.impl.operations.DatastoreType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = [CmNotificationSubscriptionCacheConfig])
class CmNotificationSubscriptionCacheConfigSpec extends Specification {

    @Autowired
    IMap<String, Map<String, DmiCmNotificationSubscriptionDetails>> cmNotificationSubscriptionCache;

    def 'Embedded (hazelcast) cache for Cm Notification Subscription Cache.'() {
        expect: 'system is able to create an instance of the Cm Notification Subscription Cache'
            assert null != cmNotificationSubscriptionCache
        and: 'there is at least 1 instance'
            assert Hazelcast.allHazelcastInstances.size() > 0
        and: 'Cm Notification Subscription Cache is present'
            assert Hazelcast.allHazelcastInstances.name.contains('hazelCastInstanceCmNotificationSubscription')
    }

    def 'Provided CM Subscription data'() {
        given: 'a cm subscription properties'
            def subscriptionId = 'sub123'
            def dmiPluginName = 'dummydmi'
            def cmSubscriptionPredicate = new DmiCmNotificationSubscriptionPredicate(targetCmHandleIds: ['cmhandle1', 'cmhandle2'], datastoreType: DatastoreType.PASSTHROUGH_RUNNING, xpaths: ['/a/b/c'])
            def cmSubscriptionCacheObject = new DmiCmNotificationSubscriptionDetails(dmiCmNotificationSubscriptionPredicates: [cmSubscriptionPredicate], cmNotificationSubscriptionStatus: CmNotificationSubscriptionStatus.PENDING)
        when: 'the cache is populated'
            cmNotificationSubscriptionCache.put(subscriptionId, [(dmiPluginName): cmSubscriptionCacheObject])
        then: 'the values are present in memory'
            assert cmNotificationSubscriptionCache.get(subscriptionId) != null
        and: 'properties match'
            assert dmiPluginName == cmNotificationSubscriptionCache.get(subscriptionId).keySet()[0]
            assert cmSubscriptionCacheObject.cmNotificationSubscriptionStatus == cmNotificationSubscriptionCache.get(subscriptionId).values().cmNotificationSubscriptionStatus[0]
            assert cmSubscriptionCacheObject.dmiCmNotificationSubscriptionPredicates[0].targetCmHandleIds == cmNotificationSubscriptionCache.get(subscriptionId).values().dmiCmNotificationSubscriptionPredicates[0].targetCmHandleIds[0]
    }
}
