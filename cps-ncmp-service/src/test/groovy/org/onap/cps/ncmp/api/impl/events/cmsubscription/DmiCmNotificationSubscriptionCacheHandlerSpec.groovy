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

package org.onap.cps.ncmp.api.impl.events.cmsubscription

import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.onap.cps.ncmp.api.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.events.cmnotificationsubscription_merge1_0_0.client_to_ncmp.CmNotificationSubscriptionNcmpInEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

import static org.onap.cps.ncmp.api.impl.events.mapper.CloudEventMapper.toTargetEvent

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper])
class DmiCmNotificationSubscriptionCacheHandlerSpec extends MessagingBaseSpec {

    @Autowired
    JsonObjectMapper jsonObjectMapper
    @Autowired
    ObjectMapper objectMapper
    @SpringBean
    InventoryPersistence mockInventoryPersistence = Mock(InventoryPersistence)

    def testCache = [:]
    def objectUnderTest = new DmiCmNotificationSubscriptionCacheHandler(testCache, mockInventoryPersistence)

    CmNotificationSubscriptionNcmpInEvent cmNotificationSubscriptionNcmpInEvent
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
            def subscriptionId = cmNotificationSubscriptionNcmpInEvent.getData().getSubscriptionId();
        and: 'list of predicates'
            def predicates = cmNotificationSubscriptionNcmpInEvent.getData().getPredicates();
        when: 'a valid event object loaded in cache'
            objectUnderTest.add(subscriptionId, predicates)
        then: 'the cache contains the correct entry with #subscriptionId subscription ID'
            assert testCache.containsKey(subscriptionId)
    }

    def 'Create map for DMI cm notification subscription per DMI service name'() {
        given: 'list of predicates from the create subscription event'
            def predicates = cmNotificationSubscriptionNcmpInEvent.getData().getPredicates()
        when: 'method to create map of DMI cm notification subscription per DMI service name is called'
            def result = objectUnderTest.createDmiCmNotificationSubscriptionsPerDmi(predicates)
        then: 'the result size of resulting map is correct to the number of DMIs'
            assert result.size() == 2
        and: 'the cache objects per DMI exists'
            def resultMapForDmi1 = result.get('dmi-1')
            def resultMapForDmi2 = result.get('dmi-2')
            assert resultMapForDmi1 != null
            assert resultMapForDmi2 != null
        and: 'the size of predicates in each object is correct'
            assert resultMapForDmi1.dmiCmNotificationSubscriptionPredicates.size() == 2
            assert resultMapForDmi2.dmiCmNotificationSubscriptionPredicates.size() == 2
        and: 'the subscription status in each object is correct'
            assert resultMapForDmi1.cmNotificationSubscriptionStatus.toString() == 'PENDING'
            assert resultMapForDmi2.cmNotificationSubscriptionStatus.toString() == 'PENDING'
        and: 'the target cmHandles for each predicate is correct'
            assert resultMapForDmi1.dmiCmNotificationSubscriptionPredicates[0].targetCmHandleIds == ['ch1'].toSet()
            assert resultMapForDmi1.dmiCmNotificationSubscriptionPredicates[1].targetCmHandleIds == ['ch3'].toSet()

            assert resultMapForDmi2.dmiCmNotificationSubscriptionPredicates[0].targetCmHandleIds == ['ch2'].toSet()
            assert resultMapForDmi2.dmiCmNotificationSubscriptionPredicates[1].targetCmHandleIds == ['ch4'].toSet()
        and: 'the list of xpath for each is correct'
            assert resultMapForDmi1.dmiCmNotificationSubscriptionPredicates[0].xpaths
                    && resultMapForDmi2.dmiCmNotificationSubscriptionPredicates[0].xpaths == ['/x1/y1','x2/y2'].toSet()

            assert resultMapForDmi1.dmiCmNotificationSubscriptionPredicates[1].xpaths
                    && resultMapForDmi2.dmiCmNotificationSubscriptionPredicates[1].xpaths == ['/x3/y3','x4/y4'].toSet()
    }

    def 'Get map for cm handle IDs by DMI service name'() {
        given: 'the predicate from the test request CM subscription event'
            def targetFilter = cmNotificationSubscriptionNcmpInEvent.getData().getPredicates().get(0).getTargetFilter()
        when: 'the method to group all target CM handles by DMI service name is called'
            def mapOfCMHandleIDsByDmi = objectUnderTest.groupTargetCmHandleIdsByDmi(targetFilter)
        then: 'the size of the resulting map is correct'
            assert mapOfCMHandleIDsByDmi.size() == 2
        and: 'the values in the map is as expected'
            assert mapOfCMHandleIDsByDmi.get('dmi-1') == ['ch1'].toSet()
            assert mapOfCMHandleIDsByDmi.get('dmi-2') == ['ch2'].toSet()
    }

    def setUpTestEvent(){
        def jsonData = TestUtils.getResourceFileContent('cmSubscription/cmNotificationSubscriptionNcmpInEvent.json')
        def testEventSent = jsonObjectMapper.convertJsonString(jsonData, CmNotificationSubscriptionNcmpInEvent.class)
        def testCloudEventSent = CloudEventBuilder.v1()
                .withData(objectMapper.writeValueAsBytes(testEventSent))
                .withId('subscriptionCreated')
                .withType('subscriptionCreated')
                .withSource(URI.create('some-resource'))
                .withExtension('correlationid', 'test-cmhandle1').build()
        def consumerRecord = new ConsumerRecord<String, CloudEvent>('topic-name', 0, 0, 'event-key', testCloudEventSent)
        def cloudEvent = consumerRecord.value()

        cmNotificationSubscriptionNcmpInEvent = toTargetEvent(cloudEvent, CmNotificationSubscriptionNcmpInEvent.class);
    }

    def initialiseMockInventoryPersistenceResponses(){
        mockInventoryPersistence.getYangModelCmHandles(['ch1','ch2'])
                >> [yangModelCmHandle1, yangModelCmHandle2]

        mockInventoryPersistence.getYangModelCmHandles(['ch3','ch4'])
                >> [yangModelCmHandle3, yangModelCmHandle4]
    }

}