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

package org.onap.cps.ncmp.api.impl.events.deprecated.cmsubscription

import com.fasterxml.jackson.databind.ObjectMapper
import org.mapstruct.factory.Mappers
import org.onap.cps.ncmp.api.impl.deprecated.subscriptions.SubscriptionStatus
import org.onap.cps.ncmp.events.cmsubscription1_0_0.client_to_ncmp.CmSubscriptionNcmpInEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification


@SpringBootTest(classes = [JsonObjectMapper, ObjectMapper])
class CmSubscriptionNcmpInEventMapperSpec extends Specification {

    CmSubscriptionNcmpInEventMapper objectUnderTest = Mappers.getMapper(CmSubscriptionNcmpInEventMapper)

    @Autowired
    JsonObjectMapper jsonObjectMapper

    def 'Map subscription event to yang model subscription event where #scenario'() {
        given: 'a Subscription Event'
            def jsonData = TestUtils.getResourceFileContent('deprecatedCmSubscription/cmSubscriptionNcmpInEvent.json')
            def testEventToMap = jsonObjectMapper.convertJsonString(jsonData, CmSubscriptionNcmpInEvent.class)
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
            def testEventToMap = new CmSubscriptionNcmpInEvent()
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
}