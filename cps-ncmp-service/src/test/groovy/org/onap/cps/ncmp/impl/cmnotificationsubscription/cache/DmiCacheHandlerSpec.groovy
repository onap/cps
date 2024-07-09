/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the 'License');
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.impl.cmnotificationsubscription.cache

import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.CmSubscriptionStatus
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionDetails
import org.onap.cps.ncmp.impl.cmnotificationsubscription.utils.CmSubscriptionPersistenceService
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.client_to_ncmp.NcmpInEvent
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.ncmp.utils.events.MessagingBaseSpec
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

import static org.onap.cps.ncmp.utils.events.CloudEventMapper.toTargetEvent

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper])
class DmiCacheHandlerSpec extends MessagingBaseSpec {

    @Autowired
    JsonObjectMapper jsonObjectMapper
    @Autowired
    ObjectMapper objectMapper
    @SpringBean
    InventoryPersistence mockInventoryPersistence = Mock(InventoryPersistence)
    @SpringBean
    CmSubscriptionPersistenceService mockCmSubscriptionPersistenceService = Mock(CmSubscriptionPersistenceService)

    def testCache = [:]
    def objectUnderTest = new DmiCacheHandler(mockCmSubscriptionPersistenceService, testCache, mockInventoryPersistence)

    NcmpInEvent ncmpInEvent
    def yangModelCmHandle1 = new YangModelCmHandle(id:'ch1',dmiServiceName:'dmi-1')
    def yangModelCmHandle2 = new YangModelCmHandle(id:'ch2',dmiServiceName:'dmi-2')
    def yangModelCmHandle3 = new YangModelCmHandle(id:'ch3',dmiServiceName:'dmi-1')
    def yangModelCmHandle4 = new YangModelCmHandle(id:'ch4',dmiServiceName:'dmi-2')

    def setup() {
        setUpTestEvent()
        initialiseMockInventoryPersistenceResponses()
    }

    def 'Load CM subscription event to cache'() {
        given: 'a valid subscription event with Id'
            def subscriptionId = ncmpInEvent.getData().getSubscriptionId()
        and: 'list of predicates'
            def predicates = ncmpInEvent.getData().getPredicates()
        when: 'a valid event object loaded in cache'
            objectUnderTest.add(subscriptionId, predicates)
        then: 'the cache contains the correct entry with #subscriptionId subscription ID'
            assert testCache.containsKey(subscriptionId)
    }

    def 'Get cache entry via subscription id'() {
        given: 'the cache contains value for some-id'
            testCache.put('some-id',[:])
        when: 'the get method is called'
            def result = objectUnderTest.get('some-id')
        then: 'correct value is returned as expected'
            assert result == [:]
    }

    def 'Remove accepted and rejected entries from cache via subscription id'() {
        given: 'a map as the value for cache entry for some-id'
            def testMap = [:]
            testMap.put("dmi-1",
                new DmiCmSubscriptionDetails([],CmSubscriptionStatus.ACCEPTED))
            testMap.put("dmi-2",
                new DmiCmSubscriptionDetails([],CmSubscriptionStatus.REJECTED))
            testMap.put("dmi-3",
                new DmiCmSubscriptionDetails([],CmSubscriptionStatus.PENDING))
            testCache.put("test-id", testMap)
            assert testCache.get("test-id").size() == 3
        when: 'the method to remove accepted and rejected entries for test-id is called'
            objectUnderTest.removeAcceptedAndRejectedDmiSubscriptionEntries("test-id")
        then: 'all entries with status accepted/rejected are no longer present for test-id'
            testCache.get("test-id").each { key, testResultMap ->
                assert testResultMap.cmSubscriptionStatus != CmSubscriptionStatus.ACCEPTED
                    || testResultMap.cmSubscriptionStatus != CmSubscriptionStatus.REJECTED
            }
        and: 'the size of the map for cache entry test-id is as expected'
            assert testCache.get("test-id").size() == 1
    }

    def 'Create map for DMI cm notification subscription per DMI service name'() {
        given: 'list of predicates from the create subscription event'
            def predicates = ncmpInEvent.getData().getPredicates()
        when: 'method to create map of DMI cm notification subscription per DMI service name is called'
            def result = objectUnderTest.createDmiSubscriptionsPerDmi(predicates)
        then: 'the result size of resulting map is correct to the number of DMIs'
            assert result.size() == 2
        and: 'the cache objects per DMI exists'
            def resultMapForDmi1 = result.get('dmi-1')
            def resultMapForDmi2 = result.get('dmi-2')
            assert resultMapForDmi1 != null
            assert resultMapForDmi2 != null
        and: 'the size of predicates in each object is correct'
            assert resultMapForDmi1.dmiCmSubscriptionPredicates.size() == 2
            assert resultMapForDmi2.dmiCmSubscriptionPredicates.size() == 2
        and: 'the subscription status in each object is correct'
            assert resultMapForDmi1.cmSubscriptionStatus.toString() == 'PENDING'
            assert resultMapForDmi2.cmSubscriptionStatus.toString() == 'PENDING'
        and: 'the target cmHandles for each predicate is correct'
            assert resultMapForDmi1.dmiCmSubscriptionPredicates[0].targetCmHandleIds == ['ch1'].toSet()
            assert resultMapForDmi1.dmiCmSubscriptionPredicates[1].targetCmHandleIds == ['ch3'].toSet()

            assert resultMapForDmi2.dmiCmSubscriptionPredicates[0].targetCmHandleIds == ['ch2'].toSet()
            assert resultMapForDmi2.dmiCmSubscriptionPredicates[1].targetCmHandleIds == ['ch4'].toSet()
        and: 'the list of xpath for each is correct'
            assert resultMapForDmi1.dmiCmSubscriptionPredicates[0].xpaths
                && resultMapForDmi2.dmiCmSubscriptionPredicates[0].xpaths == ['/x1/y1', 'x2/y2'].toSet()

            assert resultMapForDmi1.dmiCmSubscriptionPredicates[1].xpaths
                && resultMapForDmi2.dmiCmSubscriptionPredicates[1].xpaths == ['/x3/y3', 'x4/y4'].toSet()
    }

    def 'Get map for cm handle IDs by DMI service name'() {
        given: 'the predicate from the test request CM subscription event'
            def targetFilter = ncmpInEvent.getData().getPredicates().get(0).getTargetFilter()
        when: 'the method to group all target CM handles by DMI service name is called'
            def mapOfCMHandleIDsByDmi = objectUnderTest.groupTargetCmHandleIdsByDmi(targetFilter)
        then: 'the size of the resulting map is correct'
            assert mapOfCMHandleIDsByDmi.size() == 2
        and: 'the values in the map is as expected'
            assert mapOfCMHandleIDsByDmi.get('dmi-1') == ['ch1'].toSet()
            assert mapOfCMHandleIDsByDmi.get('dmi-2') == ['ch2'].toSet()
    }

    def 'Update subscription status in cache per DMI service name'() {
        given: 'populated cache'
            def predicates = ncmpInEvent.getData().getPredicates()
            def subscriptionId = ncmpInEvent.getData().getSubscriptionId()
            objectUnderTest.add(subscriptionId, predicates)
        when: 'subscription status per dmi is updated in cache'
            objectUnderTest.updateDmiSubscriptionStatusPerDmi(subscriptionId,'dmi-1', CmSubscriptionStatus.ACCEPTED)
        then: 'verify status has been updated in cache'
            def predicate = testCache.get(subscriptionId)
            assert predicate.get('dmi-1').cmSubscriptionStatus == CmSubscriptionStatus.ACCEPTED
    }

    def 'Persist Cache into database per dmi'() {
        given: 'populated cache'
            def predicates = ncmpInEvent.getData().getPredicates()
            def subscriptionId = ncmpInEvent.getData().getSubscriptionId()
            objectUnderTest.add(subscriptionId, predicates)
        when: 'subscription is persisted in database'
            objectUnderTest.persistIntoDatabasePerDmi(subscriptionId,'dmi-1')
        then: 'persistence service is called the correct number of times per dmi'
            4 * mockCmSubscriptionPersistenceService.addCmSubscription(_,_,_,subscriptionId)
    }

    def 'Remove subscription from database per dmi'() {
        given: 'populated cache'
            def predicates = ncmpInEvent.getData().getPredicates()
            def subscriptionId = ncmpInEvent.getData().getSubscriptionId()
            objectUnderTest.add(subscriptionId, predicates)
        when: 'subscription is persisted in database'
            objectUnderTest.removeFromDatabasePerDmi(subscriptionId,'dmi-1')
        then: 'persistence service is called the correct number of times per dmi'
            4 * mockCmSubscriptionPersistenceService.removeCmSubscription(_,_,_,subscriptionId)
    }

    def setUpTestEvent(){
        def jsonData = TestUtils.getResourceFileContent('cmSubscription/cmNotificationSubscriptionNcmpInEvent.json')
        def testEventSent = jsonObjectMapper.convertJsonString(jsonData, NcmpInEvent.class)
        def testCloudEventSent = CloudEventBuilder.v1()
            .withData(objectMapper.writeValueAsBytes(testEventSent))
            .withId('subscriptionCreated')
            .withType('subscriptionCreated')
            .withSource(URI.create('some-resource'))
            .withExtension('correlationid', 'test-cmhandle1').build()
        def consumerRecord = new ConsumerRecord<String, CloudEvent>('topic-name', 0, 0, 'event-key', testCloudEventSent)
        def cloudEvent = consumerRecord.value()

        ncmpInEvent = toTargetEvent(cloudEvent, NcmpInEvent.class);
    }

    def initialiseMockInventoryPersistenceResponses(){
        mockInventoryPersistence.getYangModelCmHandles(['ch1','ch2'])
            >> [yangModelCmHandle1, yangModelCmHandle2]

        mockInventoryPersistence.getYangModelCmHandles(['ch3','ch4'])
            >> [yangModelCmHandle3, yangModelCmHandle4]
    }

}
