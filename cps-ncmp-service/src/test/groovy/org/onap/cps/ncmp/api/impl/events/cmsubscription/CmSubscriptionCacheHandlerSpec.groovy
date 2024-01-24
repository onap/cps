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
import com.hazelcast.map.IMap
import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.CmSubscriptionCacheObject
import org.onap.cps.ncmp.api.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.events.cmsubscription_merge1_0_0.client_to_ncmp.CmSubscriptionNcmpInEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

import static org.onap.cps.ncmp.api.impl.events.mapper.CloudEventMapper.toTargetEvent

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper, CmSubscriptionCacheHandler])
class CmSubscriptionCacheHandlerSpec extends MessagingBaseSpec {

    @Autowired
    JsonObjectMapper jsonObjectMapper
    @Autowired
    ObjectMapper objectMapper
    @SpringBean
    InventoryPersistence mockInventoryPersistence = Mock(InventoryPersistence)
    @SpringBean
    IMap<String, Map<String, CmSubscriptionCacheObject>> mockCmSubscriptionEventCache = Mock(IMap<String, Map<String, CmSubscriptionCacheObject>>)

    def objectUnderTest = Spy(new CmSubscriptionCacheHandler(mockCmSubscriptionEventCache, mockInventoryPersistence))

    CmSubscriptionNcmpInEvent cmSubscriptionNcmpInEvent
    def yangModelCmHandle1 = new YangModelCmHandle()
    def yangModelCmHandle2 = new YangModelCmHandle()
    def yangModelCmHandle3 = new YangModelCmHandle()
    def yangModelCmHandle4 = new YangModelCmHandle()

    def setup() {
        def jsonData = TestUtils.getResourceFileContent('cmSubscription/cmSubscriptionNcmpInEvent.json')
        def testEventSent = jsonObjectMapper.convertJsonString(jsonData, CmSubscriptionNcmpInEvent.class)
        def testCloudEventSent = CloudEventBuilder.v1()
                .withData(objectMapper.writeValueAsBytes(testEventSent))
                .withId('subscriptionCreated')
                .withType('subscriptionCreated')
                .withSource(URI.create('some-resource'))
                .withExtension('correlationid', 'test-cmhandle1').build()
        def consumerRecord = new ConsumerRecord<String, CloudEvent>('topic-name', 0, 0, 'event-key', testCloudEventSent)
        def cloudEvent = consumerRecord.value();

        cmSubscriptionNcmpInEvent = toTargetEvent(cloudEvent, CmSubscriptionNcmpInEvent.class);

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
            def subscriptionID = cmSubscriptionNcmpInEvent.getData().getSubscriptionId();
        when: 'a valid event object loaded in cache'
            objectUnderTest.loadCmSubscriptionEventToCache(cmSubscriptionNcmpInEvent)
        then: 'the cache contains the correct entry with #subscriptionID subscription ID'
            1 * mockCmSubscriptionEventCache.put(subscriptionID, _)
        and: 'the cache contains the correct entry with #subscriptionID subscription ID'
            mockCmSubscriptionEventCache.containsKey(subscriptionID) >> true
    }

    def 'Create map for cache object by DMI service name'() {
        when: 'method to get all cache object by DMI service name is called'
            def mapOfCacheObjectByDmi = objectUnderTest.getAllCmSubscriptionCacheObjectByDmiMap(cmSubscriptionNcmpInEvent)
        then: 'the method to group cm handles by DMI service name is called twice'
            2 * objectUnderTest.groupTargetCmHandlesByDmi(_)
        and: 'the result size of resulting map is correct to the number of DMIs'
            mapOfCacheObjectByDmi.size() == 2
        and: 'the cache objects per DMI exists'
            def cacheObjectForDmi1 = mapOfCacheObjectByDmi.get("dmi-1")
            def cacheObjectForDmi2 = mapOfCacheObjectByDmi.get("dmi-2")
            cacheObjectForDmi1 != null
            cacheObjectForDmi2 != null
        and: 'the size of predicates in each object is correct'
            cacheObjectForDmi1.cmSubscriptionPredicates.size() == 2
            cacheObjectForDmi2.cmSubscriptionPredicates.size() == 2
        and: 'the subscription status in each object is correct'
            cacheObjectForDmi1.cmSubscriptionStatus.toString() == "PENDING"
            cacheObjectForDmi2.cmSubscriptionStatus.toString() == "PENDING"
        and: 'the target cmHandles for each predicate is correct'
            cacheObjectForDmi1.cmSubscriptionPredicates.get(0).getTargetFilter() == ["ch1"]
            cacheObjectForDmi1.cmSubscriptionPredicates.get(1).getTargetFilter() == ["ch3"]

            cacheObjectForDmi2.cmSubscriptionPredicates.get(0).getTargetFilter() == ["ch2"]
            cacheObjectForDmi2.cmSubscriptionPredicates.get(1).getTargetFilter() == ["ch4"]
        and: 'the list of xpath for each is correct'
            cacheObjectForDmi1.cmSubscriptionPredicates.get(0).getScopeFilter()
                    .getXpathFilters() && cacheObjectForDmi2.cmSubscriptionPredicates.get(0).getScopeFilter()
                    .getXpathFilters() == ["/x1/y1","x2/y2"]

            cacheObjectForDmi1.cmSubscriptionPredicates.get(1).getScopeFilter()
                    .getXpathFilters() && cacheObjectForDmi2.cmSubscriptionPredicates.get(1).getScopeFilter()
                    .getXpathFilters() == ["/x3/y3","x4/y4"]
    }

    def 'Get map for cm handle IDs by DMI service name'() {
        given: 'the predicate from the test request CM subscription event'
            def requestPredicate = cmSubscriptionNcmpInEvent.getData().getPredicates().get(0)
        when: 'the method to group all target CM handles by DMI service name is called'
            def mapOfCMHandleIDsByDmi = objectUnderTest.groupTargetCmHandlesByDmi(requestPredicate)
        then: 'the size of the resulting map is correct'
            mapOfCMHandleIDsByDmi.size() == 2
        and: 'the values in the map is as expected'
            mapOfCMHandleIDsByDmi.get("dmi-1") == ["ch1"]
            mapOfCMHandleIDsByDmi.get("dmi-2") == ["ch2"]
    }
}