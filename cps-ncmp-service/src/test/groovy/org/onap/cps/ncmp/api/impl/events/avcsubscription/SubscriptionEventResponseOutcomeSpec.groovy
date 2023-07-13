/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2023 Nordix Foundation.
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
import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import org.mapstruct.factory.Mappers
import org.onap.cps.ncmp.api.impl.events.EventsPublisher
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionPersistence
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionStatus
import org.onap.cps.ncmp.api.impl.utils.DataNodeBaseSpec
import org.onap.cps.ncmp.api.impl.utils.SubscriptionOutcomeCloudMapper
import org.onap.cps.ncmp.events.avcsubscription1_0_0.dmi_to_ncmp.SubscriptionEventResponse
import org.onap.cps.ncmp.events.avcsubscription1_0_0.ncmp_to_client.SubscriptionEventOutcome;
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper, SubscriptionOutcomeMapper, SubscriptionEventResponseOutcome])
class SubscriptionEventResponseOutcomeSpec extends DataNodeBaseSpec {

    @Autowired
    SubscriptionEventResponseOutcome objectUnderTest

    @SpringBean
    SubscriptionPersistence mockSubscriptionPersistence = Mock(SubscriptionPersistence)
    @SpringBean
    EventsPublisher<CloudEvent> mockSubscriptionEventOutcomePublisher = Mock(EventsPublisher<CloudEvent>)
    @SpringBean
    SubscriptionOutcomeMapper subscriptionOutcomeMapper = Mappers.getMapper(SubscriptionOutcomeMapper)

    @Autowired
    JsonObjectMapper jsonObjectMapper

    @Autowired
    ObjectMapper objectMapper

    def 'Send response to the client apps successfully'() {
        given: 'a subscription response event'
            def subscriptionResponseJsonData = TestUtils.getResourceFileContent('avcSubscriptionEventResponse.json')
            def subscriptionResponseEvent = jsonObjectMapper.convertJsonString(subscriptionResponseJsonData, SubscriptionEventResponse.class)
        and: 'a subscription outcome event'
            def subscriptionOutcomeJsonData = TestUtils.getResourceFileContent('avcSubscriptionOutcomeEvent.json')
            def subscriptionOutcomeEvent = jsonObjectMapper.convertJsonString(subscriptionOutcomeJsonData, SubscriptionEventOutcome.class)
        and: 'a random id for the cloud event'
            SubscriptionOutcomeCloudMapper.randomId = 'some-id'
        and: 'a cloud event containing the outcome event'
            def testCloudEventSent = CloudEventBuilder.v1()
                .withData(objectMapper.writeValueAsBytes(subscriptionOutcomeEvent))
                .withId('some-id')
                .withType('CREATE')
                .withSource(URI.create('NCMP')).build()
        and: 'the persistence service return a data node that includes pending cm handles that makes it partial success'
            mockSubscriptionPersistence.getCmHandlesForSubscriptionEvent(*_) >> [dataNode4]
        when: 'the response is being sent'
            objectUnderTest.sendResponse(subscriptionResponseEvent)
        then: 'the publisher publish the cloud event with itself and expected parameters'
            1 * mockSubscriptionEventOutcomePublisher.publishCloudEvent('cm-avc-subscription-response', 'SCO-9989752cm-subscription-001', testCloudEventSent)
    }

    def 'Create subscription outcome message as expected'() {
        given: 'a subscription response event'
            def subscriptionResponseJsonData = TestUtils.getResourceFileContent('avcSubscriptionEventResponse.json')
            def subscriptionResponseEvent = jsonObjectMapper.convertJsonString(subscriptionResponseJsonData, SubscriptionEventResponse.class)
        and: 'a subscription outcome event'
            def subscriptionOutcomeJsonData = TestUtils.getResourceFileContent('avcSubscriptionOutcomeEvent.json')
            def subscriptionOutcomeEvent = jsonObjectMapper.convertJsonString(subscriptionOutcomeJsonData, SubscriptionEventOutcome.class)
        and: 'a status code and status message a per #scenarios'
            subscriptionOutcomeEvent.getData().setStatusCode(statusCode)
            subscriptionOutcomeEvent.getData().setStatusMessage(statusMessage)
        when: 'a subscription event outcome message is being formed'
            def result = objectUnderTest.fromSubscriptionEventResponse(subscriptionResponseEvent, isFullOutcomeResponse)
        then: 'the result will be equal to event outcome'
            result == subscriptionOutcomeEvent
        where: 'the following values are used'
            scenario             | isFullOutcomeResponse  || statusMessage                      ||  statusCode
            'is full outcome'    | true                   || 'Fully applied subscription'       ||  600
            'is partial outcome' | false                  || 'Partially Applied Subscription'   ||  601
    }

    def 'Check cm handle id to status map to see if it is a full outcome response'() {
        when: 'is full outcome response evaluated'
            def response = objectUnderTest.isFullOutcomeResponse(cmHandleIdToStatusMap)
        then: 'the result will be as expected'
            response == expectedResult
        where: 'the following values are used'
            scenario                                          | cmHandleIdToStatusMap                                                                       || expectedResult
            'The map contains PENDING status'                 | ['CMHandle1': SubscriptionStatus.PENDING] as Map                                            || false
            'The map contains ACCEPTED status'                | ['CMHandle1': SubscriptionStatus.ACCEPTED] as Map                                           || true
            'The map contains REJECTED status'                | ['CMHandle1': SubscriptionStatus.REJECTED] as Map                                           || true
            'The map contains PENDING and ACCEPTED statuses'  | ['CMHandle1': SubscriptionStatus.PENDING,'CMHandle2': SubscriptionStatus.ACCEPTED] as Map   || false
            'The map contains REJECTED and ACCEPTED statuses' | ['CMHandle1': SubscriptionStatus.REJECTED,'CMHandle2': SubscriptionStatus.ACCEPTED] as Map  || true
            'The map contains PENDING and REJECTED statuses'  | ['CMHandle1': SubscriptionStatus.PENDING,'CMHandle2': SubscriptionStatus.REJECTED] as Map   || false
    }
}
