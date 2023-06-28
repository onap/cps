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
import org.apache.kafka.common.header.internals.RecordHeaders
import org.mapstruct.factory.Mappers
import org.onap.cps.ncmp.api.impl.events.EventsPublisher
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionPersistence
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionStatus
import org.onap.cps.ncmp.api.impl.utils.DataNodeBaseSpec
import org.onap.cps.ncmp.events.avc.subscription.v1.SubscriptionEventOutcome
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
    EventsPublisher<SubscriptionEventOutcome> mockSubscriptionEventOutcomePublisher = Mock(EventsPublisher<SubscriptionEventOutcome>)
    @SpringBean
    SubscriptionOutcomeMapper subscriptionOutcomeMapper = Mappers.getMapper(SubscriptionOutcomeMapper)

    @Autowired
    JsonObjectMapper jsonObjectMapper

    def 'Send response to the client apps successfully'() {
        given: 'a subscription client id and subscription name'
            def clientId = 'some-client-id'
            def subscriptionName = 'some-subscription-name'
        and: 'the persistence service return a data node'
            mockSubscriptionPersistence.getCmHandlesForSubscriptionEvent(*_) >> [dataNode4]
        and: 'the response is being generated from the db'
            def eventOutcome = objectUnderTest.generateResponse(clientId, subscriptionName)
        when: 'the response is being sent'
            objectUnderTest.sendResponse(clientId, subscriptionName)
        then: 'the publisher publish the response with expected parameters'
            1 * mockSubscriptionEventOutcomePublisher.publishEvent('cm-avc-subscription-response', clientId + subscriptionName, new RecordHeaders(), eventOutcome)
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

    def 'Generate response via fetching data nodes from database.'() {
        given: 'a db call to get data nodes for subscription event'
            1 * mockSubscriptionPersistence.getCmHandlesForSubscriptionEvent(*_) >> [dataNode4]
        when: 'a response is generated'
            def result = objectUnderTest.generateResponse('some-client-id', 'some-subscription-name')
        then: 'the result will have the same values as same as in dataNode4'
            result.eventType == SubscriptionEventOutcome.EventType.PARTIAL_OUTCOME
            result.getEvent().getSubscription().getClientID() == 'some-client-id'
            result.getEvent().getSubscription().getName() == 'some-subscription-name'
            result.getEvent().getPredicates().getPendingTargets() == ['CMHandle3']
            result.getEvent().getPredicates().getRejectedTargets() == ['CMHandle1']
            result.getEvent().getPredicates().getAcceptedTargets() == ['CMHandle2']
    }

    def 'Form subscription outcome message with a list of cm handle id to status mapping'() {
        given: 'a list of collection including cm handle id to status'
            def cmHandleIdToStatus = [['PENDING', 'CMHandle5'], ['PENDING', 'CMHandle4'], ['ACCEPTED', 'CMHandle1'], ['REJECTED', 'CMHandle3']]
        and: 'an outcome event'
            def jsonData = TestUtils.getResourceFileContent('avcSubscriptionOutcomeEvent.json')
            def eventOutcome = jsonObjectMapper.convertJsonString(jsonData, SubscriptionEventOutcome.class)
            eventOutcome.setEventType(expectedEventType)
        when: 'a subscription outcome message formed'
            def result = objectUnderTest.formSubscriptionOutcomeMessage(cmHandleIdToStatus, 'SCO-9989752',
                'cm-subscription-001', isFullOutcomeResponse)
            result.getEvent().getPredicates().getPendingTargets().sort()
        then: 'the result will be equal to event outcome'
            result == eventOutcome
        where: 'the following values are used'
            scenario             | isFullOutcomeResponse || expectedEventType
            'is full outcome'    | true                  || SubscriptionEventOutcome.EventType.COMPLETE_OUTCOME
            'is partial outcome' | false                 || SubscriptionEventOutcome.EventType.PARTIAL_OUTCOME
    }
}
