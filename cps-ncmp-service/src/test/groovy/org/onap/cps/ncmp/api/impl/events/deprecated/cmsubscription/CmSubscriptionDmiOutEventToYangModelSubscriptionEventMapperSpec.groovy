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
import org.onap.cps.ncmp.events.cmsubscription1_0_0.dmi_to_ncmp.CmSubscriptionDmiOutEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification


@SpringBootTest(classes = [JsonObjectMapper, ObjectMapper])
class CmSubscriptionDmiOutEventToYangModelSubscriptionEventMapperSpec extends Specification {

    CmSubscriptionDmiOutEventToYangModelSubscriptionEventMapper objectUnderTest = Mappers.getMapper(CmSubscriptionDmiOutEventToYangModelSubscriptionEventMapper)

    @Autowired
    JsonObjectMapper jsonObjectMapper

    def 'Map dmi out event to yang model subscription event'() {
        given: 'a dmi out event'
            def jsonData = TestUtils.getResourceFileContent('deprecatedCmSubscription/cmSubscriptionDmiOutEvent.json')
            def testEventToMap = jsonObjectMapper.convertJsonString(jsonData, CmSubscriptionDmiOutEvent.class)
        when: 'the event is mapped to a yang model subscription'
            def result = objectUnderTest.toYangModelSubscriptionEvent(testEventToMap)
        then: 'the resulting yang model subscription event contains the correct clientId'
            assert result.clientId == "SCO-9989752"
        and: 'subscription name'
            assert result.subscriptionName == "cm-subscription-001"
        and: 'predicate targets cm handle size as expected'
            assert result.predicates.targetCmHandles.size() == 2
        and: 'predicate targets cm handle ids as expected'
            assert result.predicates.targetCmHandles.cmHandleId == ["CMHandle1", "CMHandle2"]
        and: 'the status for these targets is set to expected values'
            assert result.predicates.targetCmHandles.status == [SubscriptionStatus.REJECTED, SubscriptionStatus.REJECTED]
    }

}