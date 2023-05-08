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

package org.onap.cps.ncmp.api.impl.event.avc

import com.fasterxml.jackson.databind.ObjectMapper
import com.hazelcast.map.IMap
import org.mapstruct.factory.Mappers
import org.onap.cps.ncmp.api.impl.events.avcsubscription.SubscriptionEventForwarder
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionPersistence
import org.onap.cps.ncmp.event.model.SubscriptionEventOutcome
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper])
class ResponseTimeoutTaskSpec extends Specification {

    def mockForwardedSubscriptionEventCache = Mock(IMap<String, Set<String>>)
    def mockSubscriptionPersistence = Mock(SubscriptionPersistence)
    def mockSubscriptionEventForwarder = Mock(SubscriptionEventForwarder)
    def mockSubscriptionOutcomeMapper = Mappers.getMapper(SubscriptionOutcomeMapper)
    def subscriptionClientId = "SCO-9989752"
    def subscriptionName = "cm-subscription-001"
    def subscriptionEventId = subscriptionClientId + subscriptionName
    def objectUnderTest = new ResponseTimeoutTask(mockForwardedSubscriptionEventCache, subscriptionClientId,
        subscriptionName, subscriptionEventId, mockSubscriptionPersistence, mockSubscriptionEventForwarder,
        mockSubscriptionOutcomeMapper)

    @Autowired
    JsonObjectMapper jsonObjectMapper

    def 'Form subscription outcome message with a list of cm handle id to status mapping'() {
        given: 'a list of collection including cm handle id to status'
            def cmHandleIdToStatus = [['PENDING', 'CMHandle5'], ['PENDING', 'CMHandle4'], ['ACCEPTED', 'CMHandle1'], ['REJECTED', 'CMHandle3']]
        and: 'an outcome event'
            def jsonData = TestUtils.getResourceFileContent('avcSubscriptionOutcomeEvent.json')
            def eventOutcome = jsonObjectMapper.convertJsonString(jsonData, SubscriptionEventOutcome.class)
        when: 'a subscription outcome message formed'
            def result = objectUnderTest.formSubscriptionOutcomeMessage(cmHandleIdToStatus)
        then: 'the result will be equal to event outcome by client id'
            result.getEvent().getSubscription().getClientID() == eventOutcome.getEvent().getSubscription().getClientID()
        and: 'the result will be equal to event outcome by subscription name'
            result.getEvent().getSubscription().getName() == eventOutcome.getEvent().getSubscription().getName()
        and: 'the result will be equal to event outcome by rejected targets'
            result.getEvent().getPredicates().getRejectedTargets() == eventOutcome.getEvent().getPredicates().getRejectedTargets()
        and: 'the result will be equal to event outcome by accepted targets'
            result.getEvent().getPredicates().getAcceptedTargets() == eventOutcome.getEvent().getPredicates().getAcceptedTargets()
        and: 'the result will be equal to event outcome by pending targets'
            result.getEvent().getPredicates().getPendingTargets().sort() == eventOutcome.getEvent().getPredicates().getPendingTargets()
    }
}
