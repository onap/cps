/*
 *  ============LICENSE_START=======================================================
 *  Copyright (c) 2023 Nordix Foundation.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
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

package org.onap.cps.ncmp.api.impl.events.avcsubscription

import com.fasterxml.jackson.databind.ObjectMapper
import com.hazelcast.map.IMap
import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionPersistenceImpl
import org.onap.cps.ncmp.api.impl.utils.SubscriptionEventResponseCloudMapper
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.events.avcsubscription1_0_0.dmi_to_ncmp.CmSubscriptionDmiOutEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.spi.model.DataNodeBuilder
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper])
class CmSubscriptionDmiOutEventConsumerSpec extends MessagingBaseSpec {

    @Autowired
    JsonObjectMapper jsonObjectMapper

    @Autowired
    ObjectMapper objectMapper

    IMap<String, Set<String>> mockForwardedSubscriptionEventCache = Mock(IMap<String, Set<String>>)
    def mockSubscriptionPersistence = Mock(SubscriptionPersistenceImpl)
    def mockSubscriptionEventResponseMapper  = Mock(CmSubscriptionDmiOutEventToYangModelSubscriptionEventMapper)
    def mockSubscriptionEventResponseOutcome = Mock(CmSubscriptionNcmpOutEventPublisher)
    def mockSubscriptionEventResponseCloudMapper = new SubscriptionEventResponseCloudMapper(new ObjectMapper())

    def objectUnderTest = new CmSubscriptionDmiOutEventConsumer(mockForwardedSubscriptionEventCache,
        mockSubscriptionPersistence, mockSubscriptionEventResponseMapper, mockSubscriptionEventResponseOutcome, mockSubscriptionEventResponseCloudMapper)

    def 'Consume Subscription Event Response where all DMIs have responded'() {
        given: 'a consumer record including cloud event having subscription response'
            def consumerRecordWithCloudEventAndSubscriptionResponse = getConsumerRecord()
        and: 'a subscription response event'
            def subscriptionResponseEvent = getSubscriptionResponseEvent()
        and: 'a subscription event response and notifications are enabled'
            objectUnderTest.notificationFeatureEnabled = notificationEnabled
        and: 'subscription model loader is enabled'
            objectUnderTest.subscriptionModelLoaderEnabled = modelLoaderEnabled
        and: 'subscription persistence service returns data node includes no pending cm handle'
            mockSubscriptionPersistence.getCmHandlesForSubscriptionEvent(*_) >> [getDataNode()]
        when: 'the valid event is consumed'
            objectUnderTest.consumeSubscriptionEventResponse(consumerRecordWithCloudEventAndSubscriptionResponse)
        then: 'the forwarded subscription event cache returns only the received dmiName existing for the subscription create event'
            1 * mockForwardedSubscriptionEventCache.containsKey('SCO-9989752cm-subscription-001') >> true
            1 * mockForwardedSubscriptionEventCache.get('SCO-9989752cm-subscription-001') >> (['some-dmi-name'] as Set)
        and: 'the forwarded subscription event cache returns an empty Map when the dmiName has been removed'
            1 * mockForwardedSubscriptionEventCache.get('SCO-9989752cm-subscription-001') >> ([] as Set)
        and: 'the response event is map to yang model'
            numberOfTimeToPersist * mockSubscriptionEventResponseMapper.toYangModelSubscriptionEvent(_)
        and: 'the response event is persisted into the db'
            numberOfTimeToPersist * mockSubscriptionPersistence.saveSubscriptionEvent(_)
        and: 'the subscription event is removed from the map'
            numberOfTimeToRemove * mockForwardedSubscriptionEventCache.remove('SCO-9989752cm-subscription-001')
        and: 'a response outcome has been created'
            numberOfTimeToResponse * mockSubscriptionEventResponseOutcome.sendResponse(subscriptionResponseEvent, 'subscriptionCreated')
        where: 'the following values are used'
            scenario                                              | modelLoaderEnabled  |   notificationEnabled  ||  numberOfTimeToPersist  ||  numberOfTimeToRemove  || numberOfTimeToResponse
            'Both model loader and notification are enabled'      |    true             |     true               ||   1                     ||      1                 ||       1
            'Both model loader and notification are disabled'     |    false            |     false              ||   0                     ||      0                 ||       0
            'Model loader enabled and notification  disabled'     |    true             |     false              ||   1                     ||      0                 ||       0
            'Model loader disabled and notification enabled'      |    false            |     true               ||   0                     ||      1                 ||       1
    }

    def 'Consume Subscription Event Response where another DMI has not yet responded'() {
        given: 'a subscription event response and notifications are enabled'
            objectUnderTest.notificationFeatureEnabled = notificationEnabled
        and: 'subscription model loader is enabled'
            objectUnderTest.subscriptionModelLoaderEnabled = modelLoaderEnabled
        when: 'the valid event is consumed'
            objectUnderTest.consumeSubscriptionEventResponse(getConsumerRecord())
        then: 'the forwarded subscription event cache returns only the received dmiName existing for the subscription create event'
            1 * mockForwardedSubscriptionEventCache.containsKey('SCO-9989752cm-subscription-001') >> true
            1 * mockForwardedSubscriptionEventCache.get('SCO-9989752cm-subscription-001') >> (['responded-dmi', 'non-responded-dmi'] as Set)
        and: 'the forwarded subscription event cache returns an empty Map when the dmiName has been removed'
            1 * mockForwardedSubscriptionEventCache.get('SCO-9989752cm-subscription-001') >> (['non-responded-dmi'] as Set)
        and: 'the response event is map to yang model'
            numberOfTimeToPersist * mockSubscriptionEventResponseMapper.toYangModelSubscriptionEvent(_)
        and: 'the response event is persisted into the db'
            numberOfTimeToPersist * mockSubscriptionPersistence.saveSubscriptionEvent(_)
        and: 'the subscription event is removed from the map'
        and: 'the subscription event is not removed from the map'
            0 * mockForwardedSubscriptionEventCache.remove(_)
        and: 'a response outcome has not been created'
            0 * mockSubscriptionEventResponseOutcome.sendResponse(*_)
        where: 'the following values are used'
            scenario                                              | modelLoaderEnabled  |   notificationEnabled  ||  numberOfTimeToPersist
            'Both model loader and notification are enabled'      |    true             |     true               ||   1
            'Both model loader and notification are disabled'     |    false            |     false              ||   0
            'Model loader enabled and notification  disabled'     |    true             |     false              ||   1
            'Model loader disabled and notification enabled'      |    false            |     true               ||   0
    }

    def getSubscriptionResponseEvent() {
        def subscriptionResponseJsonData = TestUtils.getResourceFileContent('cmSubscriptionDmiOutEvent.json')
        return jsonObjectMapper.convertJsonString(subscriptionResponseJsonData, CmSubscriptionDmiOutEvent.class)
    }

    def getCloudEventHavingSubscriptionResponseEvent() {
        return CloudEventBuilder.v1()
            .withData(objectMapper.writeValueAsBytes(getSubscriptionResponseEvent()))
            .withId('some-id')
            .withType('subscriptionCreated')
            .withSource(URI.create('NCMP')).build()
    }

    def getConsumerRecord() {
        return new ConsumerRecord<String, CloudEvent>('topic-name', 0, 0, 'event-key', getCloudEventHavingSubscriptionResponseEvent())
    }

    def getDataNode() {
        def leaves = [status:'ACCEPTED', cmHandleId:'cmhandle1'] as Map
        return new DataNodeBuilder().withDataspace('NCMP-Admin')
            .withAnchor('AVC-Subscriptions').withXpath('/subscription-registry/subscription')
            .withLeaves(leaves).build()
    }
}
