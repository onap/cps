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

package org.onap.cps.ncmp.api.impl.events.cmsubscription

import com.fasterxml.jackson.databind.ObjectMapper
import com.hazelcast.config.Config
import com.hazelcast.instance.impl.HazelcastInstanceFactory
import com.hazelcast.map.IMap
import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.DmiCmNotificationSubscriptionDetails
import org.onap.cps.ncmp.api.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.events.cmnotificationsubscription_merge1_0_0.client_to_ncmp.CmNotificationSubscriptionNcmpInEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

import static org.onap.cps.ncmp.api.impl.events.mapper.CloudEventMapper.toTargetEvent

class DmiCmNotificationSubscriptionCacheHandlerSpec extends Specification {

    def spiedObjectMapper = Spy(new ObjectMapper(new ObjectMapper()))
    def spiedJsonObjectMapper = Spy(new JsonObjectMapper(new ObjectMapper()))
    def mockInventoryPersistence = Mock(InventoryPersistence)
    IMap<String, Map<String, DmiCmNotificationSubscriptionDetails>> cmNotificationSubscriptionCache = HazelcastInstanceFactory
            .getOrCreateHazelcastInstance(new Config('hazelcastInstanceName'))
            .getMap('CmNotificationSubscriptionCache')

    def objectUnderTest = Spy(new DmiCmNotificationSubscriptionCacheHandler(cmNotificationSubscriptionCache, mockInventoryPersistence))

    CmNotificationSubscriptionNcmpInEvent cmNotificationSubscriptionNcmpInEvent
    def yangModelCmHandle1 = new YangModelCmHandle()
    def yangModelCmHandle2 = new YangModelCmHandle()
    def yangModelCmHandle3 = new YangModelCmHandle()
    def yangModelCmHandle4 = new YangModelCmHandle()

    def setup() {
        def jsonData = TestUtils.getResourceFileContent('cmSubscription/cmNotificationSubscriptionNcmpInEvent.json')
        def testEventSent = spiedJsonObjectMapper.convertJsonString(jsonData, CmNotificationSubscriptionNcmpInEvent.class)
        def testCloudEventSent = CloudEventBuilder.v1()
                .withData(spiedObjectMapper.writeValueAsBytes(testEventSent))
                .withId('subscriptionCreated')
                .withType('subscriptionCreated')
                .withSource(URI.create('some-resource'))
                .withExtension('correlationid', 'test-cmhandle1').build()
        def consumerRecord = new ConsumerRecord<String, CloudEvent>('topic-name', 0, 0, 'event-key', testCloudEventSent)
        def cloudEvent = consumerRecord.value();

        cmNotificationSubscriptionNcmpInEvent = toTargetEvent(cloudEvent, CmNotificationSubscriptionNcmpInEvent.class);

        yangModelCmHandle1.setId("ch1")
        yangModelCmHandle1.setDmiServiceName("dmi-1")

        yangModelCmHandle2.setId("ch2")
        yangModelCmHandle2.setDmiServiceName("dmi-2")

        yangModelCmHandle3.setId("ch3")
        yangModelCmHandle3.setDmiServiceName("dmi-1")

        yangModelCmHandle4.setId("ch4")
        yangModelCmHandle4.setDmiServiceName("dmi-2")

        mockInventoryPersistence.getYangModelCmHandles(['ch1','ch2'])
                >> [yangModelCmHandle1, yangModelCmHandle2]

        mockInventoryPersistence.getYangModelCmHandles(['ch3','ch4'])
                >> [yangModelCmHandle3, yangModelCmHandle4]
    }

    def 'Load CM subscription event to cache'() {
        given: 'a valid subscription event with ID'
            def subscriptionID = cmNotificationSubscriptionNcmpInEvent.getData().getSubscriptionId();
        and: 'list of predicates'
            def predicates = cmNotificationSubscriptionNcmpInEvent.getData().getPredicates();
        when: 'a valid event object loaded in cache'
            objectUnderTest.add(subscriptionID, predicates)
        then: 'the cache contains the correct entry with #subscriptionID subscription ID'
            cmNotificationSubscriptionCache.containsKey(subscriptionID)
    }

    def 'Create map for cache object by DMI service name'() {
        given: 'list of predicates'
            def predicates = cmNotificationSubscriptionNcmpInEvent.getData().getPredicates()
        when: 'method to get all cache object by DMI service name is called'
            def mapOfCacheObjectByDmi =
                    objectUnderTest.createDmiCmNotificationSubscriptionsPerDmi(predicates)
        then: 'the method to group cm handle Ids by DMI is called the same amount of times as the size of the predicate'
            predicates.size() * objectUnderTest.groupTargetCmHandleIdsByDmi(_)
        and: 'the result size of resulting map is correct to the number of DMIs'
            assert mapOfCacheObjectByDmi.size() == 2
        and: 'the cache objects per DMI exists'
            def cacheObjectForDmi1 = mapOfCacheObjectByDmi.get("dmi-1")
            def cacheObjectForDmi2 = mapOfCacheObjectByDmi.get("dmi-2")
            assert cacheObjectForDmi1 != null
            assert cacheObjectForDmi2 != null
        and: 'the size of predicates in each object is correct'
            assert cacheObjectForDmi1.dmiCmNotificationSubscriptionPredicates.size() == 2
            assert cacheObjectForDmi2.dmiCmNotificationSubscriptionPredicates.size() == 2
        and: 'the subscription status in each object is correct'
            assert cacheObjectForDmi1.cmNotificationSubscriptionStatus.toString() == "PENDING"
            assert cacheObjectForDmi2.cmNotificationSubscriptionStatus.toString() == "PENDING"
        and: 'the target cmHandles for each predicate is correct'
            assert cacheObjectForDmi1.dmiCmNotificationSubscriptionPredicates.get(0).targetCmHandleIds == ["ch1"]
            assert cacheObjectForDmi1.dmiCmNotificationSubscriptionPredicates.get(1).targetCmHandleIds == ["ch3"]

            assert cacheObjectForDmi2.dmiCmNotificationSubscriptionPredicates.get(0).targetCmHandleIds == ["ch2"]
            assert cacheObjectForDmi2.dmiCmNotificationSubscriptionPredicates.get(1).targetCmHandleIds == ["ch4"]
        and: 'the list of xpath for each is correct'
            assert cacheObjectForDmi1.dmiCmNotificationSubscriptionPredicates.get(0).xpaths
                    && cacheObjectForDmi2.dmiCmNotificationSubscriptionPredicates.get(0).xpaths == ["/x1/y1","x2/y2"]

            assert cacheObjectForDmi1.dmiCmNotificationSubscriptionPredicates.get(1).xpaths
                    && cacheObjectForDmi2.dmiCmNotificationSubscriptionPredicates.get(1).xpaths == ["/x3/y3","x4/y4"]
    }

    def 'Get map for cm handle IDs by DMI service name'() {
        given: 'the predicate from the test request CM subscription event'
            def targetFilter = cmNotificationSubscriptionNcmpInEvent.getData().getPredicates().get(0).getTargetFilter()
        when: 'the method to group all target CM handles by DMI service name is called'
            def mapOfCMHandleIDsByDmi = objectUnderTest.groupTargetCmHandleIdsByDmi(targetFilter)
        then: 'the size of the resulting map is correct'
            assert mapOfCMHandleIDsByDmi.size() == 2
        and: 'the values in the map is as expected'
            assert mapOfCMHandleIDsByDmi.get("dmi-1") == ["ch1"]
            assert mapOfCMHandleIDsByDmi.get("dmi-2") == ["ch2"]
    }
}