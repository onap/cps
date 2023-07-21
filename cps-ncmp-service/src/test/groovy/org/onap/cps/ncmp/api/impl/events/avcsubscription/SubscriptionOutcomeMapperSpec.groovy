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
import org.onap.cps.ncmp.events.avcsubscription1_0_0.dmi_to_ncmp.SubscriptionEventResponse
import org.onap.cps.ncmp.events.avcsubscription1_0_0.dmi_to_ncmp.SubscriptionStatus
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.spi.exceptions.DataValidationException
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
        when: 'the subscription response event is mapped to a subscription event outcome'
            def result = objectUnderTest.toSubscriptionEventOutcome(subscriptionResponseEvent)
        then: 'the resulting subscription event outcome contains expected pending targets per details grouping'
            def pendingCmHandleTargetsPerDetails = result.getData().getAdditionalInfo().getPending()
            assert pendingCmHandleTargetsPerDetails.get(0).getDetails() == 'EMS or node connectivity issues, retrying'
            assert pendingCmHandleTargetsPerDetails.get(0).getTargets() == ['CMHandle5', 'CMHandle6','CMHandle7']
        and: 'the resulting subscription event outcome contains expected rejected targets per details grouping'
            def rejectedCmHandleTargetsPerDetails = result.getData().getAdditionalInfo().getRejected()
            assert rejectedCmHandleTargetsPerDetails.get(0).getDetails() == 'Target(s) do not exist'
            assert rejectedCmHandleTargetsPerDetails.get(0).getTargets() == ['CMHandle4']
            assert rejectedCmHandleTargetsPerDetails.get(1).getDetails() == 'Faulty subscription format for target(s)'
            assert rejectedCmHandleTargetsPerDetails.get(1).getTargets() == ['CMHandle1', 'CMHandle2','CMHandle3']
    }

    def 'Map subscription event response with null of subscription status list to subscription event outcome causes an exception'() {
        given: 'a Subscription Response Event'
            def subscriptionResponseJsonData = TestUtils.getResourceFileContent('avcSubscriptionEventResponse.json')
            def subscriptionResponseEvent = jsonObjectMapper.convertJsonString(subscriptionResponseJsonData, SubscriptionEventResponse.class)
        and: 'set subscription status list to null'
            subscriptionResponseEvent.getData().setSubscriptionStatus(subscriptionStatusList)
        when: 'the subscription response event is mapped to a subscription event outcome'
            objectUnderTest.toSubscriptionEventOutcome(subscriptionResponseEvent)
        then: 'a DataValidationException is thrown with an expected exception details'
            def exception = thrown(DataValidationException)
            exception.details == 'SubscriptionStatus list cannot be null or empty'
        where: 'the following values are used'
            scenario                            ||     subscriptionStatusList
            'A null subscription status list'   ||      null
            'An empty subscription status list' ||      new ArrayList<SubscriptionStatus>()
    }

    def 'Map subscription event response with subscription status list to subscription event outcome without any exception'() {
        given: 'a Subscription Response Event'
            def subscriptionResponseJsonData = TestUtils.getResourceFileContent('avcSubscriptionEventResponse.json')
            def subscriptionResponseEvent = jsonObjectMapper.convertJsonString(subscriptionResponseJsonData, SubscriptionEventResponse.class)
        when: 'the subscription response event is mapped to a subscription event outcome'
            objectUnderTest.toSubscriptionEventOutcome(subscriptionResponseEvent)
        then: 'no exception thrown'
            noExceptionThrown()
    }
}