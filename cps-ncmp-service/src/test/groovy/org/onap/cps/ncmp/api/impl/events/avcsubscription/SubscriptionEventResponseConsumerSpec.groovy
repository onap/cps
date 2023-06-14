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
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionPersistenceImpl
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.api.models.SubscriptionEventResponse
import org.onap.cps.spi.model.DataNodeBuilder
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper])
class SubscriptionEventResponseConsumerSpec extends MessagingBaseSpec {

    IMap<String, Set<String>> mockForwardedSubscriptionEventCache = Mock(IMap<String, Set<String>>)
    def mockSubscriptionPersistence = Mock(SubscriptionPersistenceImpl)
    def mockSubscriptionEventResponseMapper  = Mock(SubscriptionEventResponseMapper)
    def mockSubscriptionEventResponseOutcome = Mock(SubscriptionEventResponseOutcome)

    def objectUnderTest = new SubscriptionEventResponseConsumer(mockForwardedSubscriptionEventCache,
        mockSubscriptionPersistence, mockSubscriptionEventResponseMapper, mockSubscriptionEventResponseOutcome)

    def cmHandleToStatusMap = [CMHandle1: 'PENDING', CMHandle2: 'ACCEPTED'] as Map
    def testEventReceived = new SubscriptionEventResponse(clientId: 'some-client-id',
        subscriptionName: 'some-subscription-name', dmiName: 'some-dmi-name', cmHandleIdToStatus: cmHandleToStatusMap)
    def consumerRecord = new ConsumerRecord<String, SubscriptionEventResponse>('topic-name', 0, 0, 'event-key', testEventReceived)

    def 'Consume Subscription Event Response where all DMIs have responded'() {
        given: 'a subscription event response and notifications are enabled'
            objectUnderTest.notificationFeatureEnabled = isNotificationFeatureEnabled
        and: 'subscription model loader is enabled'
            objectUnderTest.subscriptionModelLoaderEnabled = true
        and: 'a data node exist in db'
            def leaves1 = [status:'ACCEPTED', cmHandleId:'cmhandle1'] as Map
            def dataNode = new DataNodeBuilder().withDataspace('NCMP-Admin')
                .withAnchor('AVC-Subscriptions').withXpath('/subscription-registry/subscription')
                .withLeaves(leaves1).build()
        and: 'subscription persistence service returns data node'
            mockSubscriptionPersistence.getCmHandlesForSubscriptionEvent(*_) >> [dataNode]
        when: 'the valid event is consumed'
            objectUnderTest.consumeSubscriptionEventResponse(consumerRecord)
        then: 'the forwarded subscription event cache returns only the received dmiName existing for the subscription create event'
            1 * mockForwardedSubscriptionEventCache.containsKey('some-client-idsome-subscription-name') >> true
            1 * mockForwardedSubscriptionEventCache.get('some-client-idsome-subscription-name') >> (['some-dmi-name'] as Set)
        and: 'the forwarded subscription event cache returns an empty Map when the dmiName has been removed'
            1 * mockForwardedSubscriptionEventCache.get('some-client-idsome-subscription-name') >> ([] as Set)
        and: 'the subscription event is removed from the map'
            numberOfExpectedCallToRemove * mockForwardedSubscriptionEventCache.remove('some-client-idsome-subscription-name')
        and: 'a response outcome has been created'
            numberOfExpectedCallToSendResponse * mockSubscriptionEventResponseOutcome.sendResponse('some-client-id', 'some-subscription-name')
        where: 'the following values are used'
            scenario             | isNotificationFeatureEnabled  ||  numberOfExpectedCallToRemove  || numberOfExpectedCallToSendResponse
            'Response sent'      | true                          ||   1                            || 1
            'Response not sent'  | false                         ||   0                            || 0
    }

    def 'Consume Subscription Event Response where another DMI has not yet responded'() {
        given: 'a subscription event response and notifications are enabled'
            objectUnderTest.notificationFeatureEnabled = true
        and: 'subscription model loader is enabled'
            objectUnderTest.subscriptionModelLoaderEnabled = true
        when: 'the valid event is consumed'
            objectUnderTest.consumeSubscriptionEventResponse(consumerRecord)
        then: 'the forwarded subscription event cache returns only the received dmiName existing for the subscription create event'
            1 * mockForwardedSubscriptionEventCache.containsKey('some-client-idsome-subscription-name') >> true
            1 * mockForwardedSubscriptionEventCache.get('some-client-idsome-subscription-name') >> (['some-dmi-name', 'non-responded-dmi'] as Set)
        and: 'the forwarded subscription event cache returns an empty Map when the dmiName has been removed'
            1 * mockForwardedSubscriptionEventCache.get('some-client-idsome-subscription-name') >> (['non-responded-dmi'] as Set)
        and: 'the subscription event is not removed from the map'
            0 * mockForwardedSubscriptionEventCache.remove(_)
        and: 'a response outcome has not been created'
            0 * mockSubscriptionEventResponseOutcome.sendResponse(*_)
    }

    def 'Update subscription event when the model loader flag is enabled'() {
        given: 'subscription model loader is enabled as per #scenario'
            objectUnderTest.subscriptionModelLoaderEnabled = isSubscriptionModelLoaderEnabled
        when: 'the valid event is consumed'
            objectUnderTest.consumeSubscriptionEventResponse(consumerRecord)
        then: 'the forwarded subscription event cache does not return dmiName for the subscription create event'
            1 * mockForwardedSubscriptionEventCache.containsKey('some-client-idsome-subscription-name') >> false
        and: 'the mapper returns yang model subscription event with #numberOfExpectedCall'
            numberOfExpectedCall * mockSubscriptionEventResponseMapper.toYangModelSubscriptionEvent(_)
        and: 'subscription event has been updated into DB with #numberOfExpectedCall'
            numberOfExpectedCall * mockSubscriptionPersistence.saveSubscriptionEvent(_)
        where: 'the following values are used'
            scenario                   | isSubscriptionModelLoaderEnabled || numberOfExpectedCall
            'The event is updated'     | true                             || 1
            'The event is not updated' | false                            || 0
    }
}
