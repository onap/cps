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
            def jsonData = TestUtils.getResourceFileContent('avcSubscriptionEventResponse.json')
            def testEventToMap = jsonObjectMapper.convertJsonString(jsonData, SubscriptionEventResponse.class)
        and: 'a Subscription Outcome Event'
            def jsonDataOutcome = TestUtils.getResourceFileContent('avcSubscriptionOutcomeEvent.json')
            def testEventTarget = jsonObjectMapper.convertJsonString(jsonDataOutcome, SubscriptionEventOutcome.class)
        when: 'the subscription response event is mapped to a subscription event outcome'
            def result = objectUnderTest.toSubscriptionEventOutcome(testEventToMap)
            result.setEventType(SubscriptionEventOutcome.EventType.PARTIAL_OUTCOME)
        then: 'the resulting subscription event outcome contains the correct clientId'
            assert result == testEventTarget
    }
}