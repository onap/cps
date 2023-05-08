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
import org.mapstruct.factory.Mappers
import org.onap.cps.ncmp.api.impl.event.avc.SubscriptionOutcomeMapper
import org.onap.cps.ncmp.api.impl.events.EventsPublisher
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionPersistence
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.events.avc.subscription.v1.SubscriptionEventOutcome
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper, SubscriptionOutcomeMapper, SubscriptionEventResponseOutcome])
class SubscriptionEventResponseOutcomeSpec extends MessagingBaseSpec {

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

    def 'Form subscription outcome message with a list of cm handle id to status mapping'() {
        given: 'a list of collection including cm handle id to status'
            def cmHandleIdToStatus = [['PENDING', 'CMHandle5'], ['PENDING', 'CMHandle4'], ['ACCEPTED', 'CMHandle1'], ['REJECTED', 'CMHandle3']]
        and: 'an outcome event'
            def jsonData = TestUtils.getResourceFileContent('avcSubscriptionOutcomeEvent.json')
            def eventOutcome = jsonObjectMapper.convertJsonString(jsonData, SubscriptionEventOutcome.class)
        when: 'a subscription outcome message formed'
            def result = objectUnderTest.formSubscriptionOutcomeMessage(cmHandleIdToStatus, 'SCO-9989752',
                'cm-subscription-001', false)
            result.getEvent().getPredicates().getPendingTargets().sort()
        then: 'the result will be equal to event outcome'
            result == eventOutcome
    }
}
