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

package org.onap.cps.ncmp.api.impl.event.avc

import com.fasterxml.jackson.databind.ObjectMapper
import com.hazelcast.map.IMap
import org.onap.cps.ncmp.api.impl.events.avcsubscription.SubscriptionEventForwarder
import org.onap.cps.ncmp.api.impl.events.avcsubscription.SubscriptionEventResponseMapper
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionPersistenceImpl
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.api.models.SubscriptionEventResponse
import org.onap.cps.ncmp.event.model.SubscriptionEventOutcome
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper])
class SubscriptionEventResponseConsumerSpec extends MessagingBaseSpec {

    IMap<String, Set<String>> mockForwardedSubscriptionEventCache = Mock(IMap<String, Set<String>>)
    def mockSubscriptionPersistence = Mock(SubscriptionPersistenceImpl)
    def mockSubscriptionEventResponseMapper  = Mock(SubscriptionEventResponseMapper)
    def mockSubscriptionOutcomeMapper = Mock(SubscriptionOutcomeMapper)
    def mockSubscriptionEventForwarder = Mock(SubscriptionEventForwarder)

    def objectUnderTest = new SubscriptionEventResponseConsumer(mockForwardedSubscriptionEventCache,
        mockSubscriptionPersistence, mockSubscriptionEventResponseMapper, mockSubscriptionOutcomeMapper,
        mockSubscriptionEventForwarder)


    def 'Consume Subscription Event Response where all DMIs have responded'() {
        given: 'a subscription event response with a clientId, subscriptionName, dmiName and cm handle to status map'
            def cmHandleToStatusMap = [CMHandle1: 'PENDING', CMHandle1: 'ACCEPTED'] as Map
            def testEventReceived = new SubscriptionEventResponse(clientId: 'some-client-id',
                subscriptionName: 'some-subscription-name', dmiName: 'some-dmi-name', cmHandleIdToStatus: cmHandleToStatusMap)
            mockSubscriptionOutcomeMapper.toSubscriptionEventOutcome(testEventReceived) >> new SubscriptionEventOutcome()
        and: 'notifications are enabled'
            objectUnderTest.notificationFeatureEnabled = true
        and: 'subscription model loader is enabled'
            objectUnderTest.subscriptionModelLoaderEnabled = true
        when: 'the valid event is consumed'
            objectUnderTest.consumeSubscriptionEventResponse(testEventReceived)
        then: 'the forwarded subscription event cache returns only the received dmiName existing for the subscription create event'
            1 * mockForwardedSubscriptionEventCache.containsKey('some-client-idsome-subscription-name') >> true
            1 * mockForwardedSubscriptionEventCache.get('some-client-idsome-subscription-name') >> (['some-dmi-name'] as Set)
        and: 'the forwarded subscription event cache returns an empty Map when the dmiName has been removed'
            1 * mockForwardedSubscriptionEventCache.get('some-client-idsome-subscription-name') >> ([] as Set)
        and: 'the subscription event is removed from the map'
            1 * mockForwardedSubscriptionEventCache.remove('some-client-idsome-subscription-name')
    }

    def 'Consume Subscription Event Response where another DMI has not yet responded'() {
        given: 'a subscription event response with a clientId, subscriptionName and dmiName'
            def testEventReceived = new SubscriptionEventResponse(clientId: 'some-client-id',
                subscriptionName: 'some-subscription-name', dmiName: 'some-dmi-name')
        and: 'notifications are enabled'
            objectUnderTest.notificationFeatureEnabled = true
        and: 'subscription model loader is enabled'
            objectUnderTest.subscriptionModelLoaderEnabled = true
        when: 'the valid event is consumed'
            objectUnderTest.consumeSubscriptionEventResponse(testEventReceived)
        then: 'the forwarded subscription event cache returns only the received dmiName existing for the subscription create event'
            1 * mockForwardedSubscriptionEventCache.containsKey('some-client-idsome-subscription-name') >> true
            1 * mockForwardedSubscriptionEventCache.get('some-client-idsome-subscription-name') >> (['some-dmi-name', 'non-responded-dmi'] as Set)
        and: 'the forwarded subscription event cache returns an empty Map when the dmiName has been removed'
            1 * mockForwardedSubscriptionEventCache.get('some-client-idsome-subscription-name') >> (['non-responded-dmi'] as Set)
        and: 'the subscription event is not removed from the map'
            0 * mockForwardedSubscriptionEventCache.remove(_)
    }
}
