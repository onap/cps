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
import org.onap.cps.ncmp.api.models.CmSubscriptionEvent
import org.onap.cps.ncmp.api.models.CmSubscriptionStatus
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification


@SpringBootTest(classes = [JsonObjectMapper, ObjectMapper])
class CmSubscriptionEventToCmSubscriptionNcmpOutEventMapperSpec extends Specification {

    CmSubscriptionEventToCmSubscriptionNcmpOutEventMapper objectUnderTest = Mappers.getMapper(CmSubscriptionEventToCmSubscriptionNcmpOutEventMapper)

    @Autowired
    JsonObjectMapper jsonObjectMapper

    def 'Map cm subscription event to ncmp out event'() {
        given: 'a cm subscription event'
            def cmSubscriptionEventJsonData = TestUtils.getResourceFileContent('deprecatedCmSubscription/cmSubscriptionEvent.json')
            def cmSubscriptionEvent = jsonObjectMapper.convertJsonString(cmSubscriptionEventJsonData, CmSubscriptionEvent.class)
        when: 'cm subscription event is mapped to ncmp out event'
            def result = objectUnderTest.toCmSubscriptionNcmpOutEvent(cmSubscriptionEvent)
        then: 'the resulting ncmp out event contains expected pending targets per details grouping'
            def pendingCmHandleTargetsPerDetails = result.getData().getAdditionalInfo().getPending()
            assert pendingCmHandleTargetsPerDetails.get(0).getDetails() == 'Some other error happened'
            assert pendingCmHandleTargetsPerDetails.get(0).getTargets() == ['CMHandle4','CMHandle5']
            assert pendingCmHandleTargetsPerDetails.get(1).getDetails() == 'Some error causes pending'
            assert pendingCmHandleTargetsPerDetails.get(1).getTargets() == ['CMHandle3']
        and: 'the resulting ncmp out event contains expected rejected targets per details grouping'
            def rejectedCmHandleTargetsPerDetails = result.getData().getAdditionalInfo().getRejected()
            assert rejectedCmHandleTargetsPerDetails.get(0).getDetails() == 'Some other error message from the DMI'
            assert rejectedCmHandleTargetsPerDetails.get(0).getTargets() == ['CMHandle2']
            assert rejectedCmHandleTargetsPerDetails.get(1).getDetails() == 'Some error message from the DMI'
            assert rejectedCmHandleTargetsPerDetails.get(1).getTargets() == ['CMHandle1']
    }

    def 'Map cm subscription event to ncmp out event with the given scenarios causes an exception'() {
        given: 'a cm subscription event'
            def cmSubscriptionEventJsonData = TestUtils.getResourceFileContent('deprecatedCmSubscription/cmSubscriptionEvent.json')
            def cmSubscriptionEvent = jsonObjectMapper.convertJsonString(cmSubscriptionEventJsonData, CmSubscriptionEvent.class)
        and: 'set cm subscription status with given scenarios'
            cmSubscriptionEvent.setCmSubscriptionStatus(subscriptionStatusList)
        when: 'cm subscription event is mapped to ncmp out event'
            objectUnderTest.toCmSubscriptionNcmpOutEvent(cmSubscriptionEvent)
        then: 'a DataValidationException is thrown with an expected exception details'
            def exception = thrown(DataValidationException)
            exception.details == 'CmSubscriptionStatus list cannot be null or empty'
        where: 'the following values are used'
            scenario                            ||     subscriptionStatusList
            'A null subscription status list'   ||     null
            'An empty subscription status list' ||     new ArrayList<CmSubscriptionStatus>()
    }

    def 'Map cm subscription event to ncmp out event without any exception'() {
        given: 'a cm subscription Event'
            def subscriptionResponseJsonData = TestUtils.getResourceFileContent('deprecatedCmSubscription/cmSubscriptionEvent.json')
            def subscriptionResponseEvent = jsonObjectMapper.convertJsonString(subscriptionResponseJsonData, CmSubscriptionEvent.class)
        when: 'cm subscription event is mapped to ncmp out event'
            objectUnderTest.toCmSubscriptionNcmpOutEvent(subscriptionResponseEvent)
        then: 'no exception thrown'
            noExceptionThrown()
    }
}