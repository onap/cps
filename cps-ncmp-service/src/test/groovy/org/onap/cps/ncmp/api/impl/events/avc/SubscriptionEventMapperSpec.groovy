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

package org.onap.cps.ncmp.api.impl.events.avc

import com.fasterxml.jackson.databind.ObjectMapper
import org.mapstruct.factory.Mappers
import org.onap.cps.ncmp.api.impl.events.avcsubscription.SubscriptionEventMapper
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionStatus
import org.onap.cps.ncmp.event.model.SubscriptionEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification


@SpringBootTest(classes = [JsonObjectMapper, ObjectMapper])
class SubscriptionEventMapperSpec extends Specification {

    SubscriptionEventMapper objectUnderTest = Mappers.getMapper(SubscriptionEventMapper)

    @Autowired
    JsonObjectMapper jsonObjectMapper

    def 'Map subscription event to yang model subscription event where #scenario'() {
        given: 'a Subscription Event'
            def jsonData = TestUtils.getResourceFileContent('avcSubscriptionCreationEvent.json')
            def testEventToMap = jsonObjectMapper.convertJsonString(jsonData, SubscriptionEvent.class)
        when: 'the event is mapped to a yang model subscription'
            def result = objectUnderTest.toYangModelSubscriptionEvent(testEventToMap)
        then: 'the resulting yang model subscription event contains the correct clientId'
            assert result.clientId == "SCO-9989752"
        and: 'subscription name'
            assert result.subscriptionName == "cm-subscription-001"
        and: 'is tagged value is false'
            assert !result.isTagged
        and: 'predicate targets '
            assert result.predicates.targetCmHandles.cmHandleId == ["CMHandle1", "CMHandle2", "CMHandle3"]
        and: 'the status for these targets is set to pending'
            assert result.predicates.targetCmHandles.status == [SubscriptionStatus.PENDING, SubscriptionStatus.PENDING, SubscriptionStatus.PENDING]
        and: 'the topic is null'
            assert result.topic == null
    }

    def 'Map empty subscription event to yang model subscription event'() {
        given: 'a new Subscription Event with no data'
            def testEventToMap = new SubscriptionEvent()
        when: 'the event is mapped to a yang model subscription'
            def result = objectUnderTest.toYangModelSubscriptionEvent(testEventToMap)
        then: 'the resulting yang model subscription event contains null clientId'
            assert result.clientId == null
        and: 'subscription name is null'
            assert result.subscriptionName == null
        and: 'is tagged value is false'
            assert result.isTagged == false
        and: 'predicates is null'
            assert result.predicates == null
        and: 'the topic is null'
            assert result.topic == null
    }

    def 'Map a null subscription event to yang model subscription event'() {
        given: 'a null subscription event'
            def testEventToMap = null
        when: 'the event is mapped to a yang model subscription'
            def result = objectUnderTest.toYangModelSubscriptionEvent(testEventToMap)
        then: 'the resulting yang model will be null'
            assert result == null
    }

    def 'Map a null subscription event to client id, subscription name, and subscription is tagged'() {
        given: 'a null subscription event response'
            def testEventToMap = null
        when: 'the event is mapped to client id, subscription name, and subscription is tagged'
            def resultClientId = objectUnderTest.subscriptionEventEventSubscriptionClientID(testEventToMap)
            def resultName = objectUnderTest.subscriptionEventEventSubscriptionName(testEventToMap)
            def resultSubscriptionIsTagged = objectUnderTest.subscriptionEventEventSubscriptionIsTagged(testEventToMap)
        then: 'the resulting objects will be null'
            assert resultClientId == null
            assert resultName == null
            assert resultSubscriptionIsTagged == null
    }

    def 'Map a null inner subscription event to predicates, datastore, and yang model predicates'() {
        given: 'a null subscription event response'
            def testEventToMap = null
        when: 'the event is mapped to predicates, datastore, and yang model predicates'
            def resultPredicates = objectUnderTest.innerSubscriptionEventPredicatesTargets(testEventToMap)
            def resultDatastore = objectUnderTest.innerSubscriptionEventPredicatesDatastore(testEventToMap)
            def resultYangModelPredicats = objectUnderTest.innerSubscriptionEventToPredicates(testEventToMap)
        then: 'the resulting objects will be null'
            assert resultPredicates == null
            assert resultDatastore == null
            assert resultYangModelPredicats == null
    }
}