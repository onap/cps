/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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

package org.onap.cps.ncmp.impl.cmnotificationsubscription.cache

import com.hazelcast.core.Hazelcast
import com.hazelcast.map.IMap
import org.onap.cps.ncmp.api.data.models.DatastoreType
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.CmSubscriptionStatus
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionDetails
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionPredicate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = [CmSubscriptionConfig])
class CmSubscriptionConfigSpec extends Specification {

    @Autowired
    IMap<String, Map<String, DmiCmSubscriptionDetails>> cmNotificationSubscriptionCache;

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
            def cmSubscriptionPredicates = new DmiCmSubscriptionPredicate(['cmhandle1', 'cmhandle2'].toSet(), DatastoreType.PASSTHROUGH_RUNNING, ['/a/b/c'].toSet())
            def cmSubscriptionCacheObject = new DmiCmSubscriptionDetails([cmSubscriptionPredicates], CmSubscriptionStatus.PENDING)
        when: 'the cache is populated'
            cmNotificationSubscriptionCache.put(subscriptionId, [(dmiPluginName): cmSubscriptionCacheObject])
        then: 'the values are present in memory'
            assert cmNotificationSubscriptionCache.get(subscriptionId) != null
        and: 'properties match'
            assert dmiPluginName == cmNotificationSubscriptionCache.get(subscriptionId).keySet()[0]
            assert cmSubscriptionCacheObject.cmSubscriptionStatus == cmNotificationSubscriptionCache.get(subscriptionId).values().cmSubscriptionStatus[0]
            assert cmSubscriptionCacheObject.dmiCmSubscriptionPredicates[0].targetCmHandleIds == cmNotificationSubscriptionCache.get(subscriptionId).values().dmiCmSubscriptionPredicates[0].targetCmHandleIds[0]
    }
}
