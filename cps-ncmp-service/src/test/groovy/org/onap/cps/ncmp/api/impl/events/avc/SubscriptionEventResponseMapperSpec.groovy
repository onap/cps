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
import org.onap.cps.ncmp.api.impl.events.avcsubscription.SubscriptionEventResponseMapper
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionStatus
import org.onap.cps.ncmp.api.models.SubscriptionEventResponse
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification


@SpringBootTest(classes = [JsonObjectMapper, ObjectMapper])
class SubscriptionEventResponseMapperSpec extends Specification {

    SubscriptionEventResponseMapper objectUnderTest = Mappers.getMapper(SubscriptionEventResponseMapper)

    @Autowired
    JsonObjectMapper jsonObjectMapper

    def 'Map subscription response event to yang model subscription event'() {
        given: 'a Subscription Response Event'
            def jsonData = TestUtils.getResourceFileContent('avcSubscriptionEventResponse.json')
            def testEventToMap = jsonObjectMapper.convertJsonString(jsonData, SubscriptionEventResponse.class)
        when: 'the event is mapped to a yang model subscription'
            def result = objectUnderTest.toYangModelSubscriptionEvent(testEventToMap)
        then: 'the resulting yang model subscription event contains the correct clientId'
            assert result.clientId == "SCO-9989752"
        and: 'subscription name'
            assert result.subscriptionName == "cm-subscription-001"
        and: 'predicate targets '
            assert result.predicates.targetCmHandles.cmHandleId == ["CMHandle1", "CMHandle2"]
        and: 'the status for these targets is set to expected values'
            assert result.predicates.targetCmHandles.status == [SubscriptionStatus.ACCEPTED, SubscriptionStatus.REJECTED]
        and: 'the topic is null'
            assert result.topic == null
    }

}