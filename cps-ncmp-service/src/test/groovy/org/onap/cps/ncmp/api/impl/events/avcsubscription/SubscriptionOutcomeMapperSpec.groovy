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
import org.mapstruct.factory.Mappers
import org.onap.cps.ncmp.api.models.SubscriptionEventResponse
import org.onap.cps.ncmp.events.avc.subscription.v1.SubscriptionEventOutcome
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification


@SpringBootTest(classes = [JsonObjectMapper, ObjectMapper])
class SubscriptionOutcomeMapperSpec extends Specification {

    SubscriptionOutcomeMapper objectUnderTest = Mappers.getMapper(SubscriptionOutcomeMapper)

    @Autowired
    JsonObjectMapper jsonObjectMapper

    def 'Map subscription event response to subscription event outcome'() {
        given: 'a Subscription Response Event'
            def subscriptionResponseJsonData = TestUtils.getResourceFileContent('avcSubscriptionEventResponse.json')
            def subscriptionResponseEvent = jsonObjectMapper.convertJsonString(subscriptionResponseJsonData, SubscriptionEventResponse.class)
        and: 'a Subscription Outcome Event'
            def jsonDataOutcome = TestUtils.getResourceFileContent('avcSubscriptionOutcomeEvent.json')
            def expectedEventOutcome = jsonObjectMapper.convertJsonString(jsonDataOutcome, SubscriptionEventOutcome.class)
            expectedEventOutcome.setEventType(expectedEventType)
        when: 'the subscription response event is mapped to a subscription event outcome'
            def result = objectUnderTest.toSubscriptionEventOutcome(subscriptionResponseEvent)
            result.setEventType(expectedEventType)
        then: 'the resulting subscription event outcome contains the correct clientId'
            assert result == expectedEventOutcome
        where: 'the following values are used'
            scenario              || expectedEventType
            'is full outcome'     || SubscriptionEventOutcome.EventType.COMPLETE_OUTCOME
            'is partial outcome'  || SubscriptionEventOutcome.EventType.PARTIAL_OUTCOME
    }

    def 'Map a null subscription response event to subscription event outcome'() {
        given: 'a null subscription event response'
            def testEventToMap = null
        when: 'the event is mapped to a subscription event outcome'
            def result = objectUnderTest.toSubscriptionEventOutcome(testEventToMap)
        then: 'the resulting subscription outcome will be null'
            assert result == null
    }

    def 'Map a null subscription response event to subscription, predicates and inner outcome'() {
        given: 'a null subscription event response'
            def testEventToMap = null
        when: 'the event is mapped to to subscription, predicates and inner outcome'
            def resultSubscription = objectUnderTest.subscriptionEventResponseToSubscription(testEventToMap)
            def resultPredicates = objectUnderTest.subscriptionEventResponseToPredicates(testEventToMap)
            def resultInnerOutcome = objectUnderTest.subscriptionEventResponseToInnerSubscriptionEventOutcome(testEventToMap)
        then: 'the resulting objects will be null'
            assert resultSubscription == null
            assert resultPredicates == null
            assert resultInnerOutcome == null
    }
}