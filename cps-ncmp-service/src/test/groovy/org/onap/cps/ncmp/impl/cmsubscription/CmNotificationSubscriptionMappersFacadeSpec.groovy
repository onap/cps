/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.impl.cmsubscription


import org.onap.cps.ncmp.impl.cmsubscription.utils.CmNotificationSubscriptionDmiInEventMapper
import org.onap.cps.ncmp.impl.cmsubscription.utils.CmNotificationSubscriptionNcmpOutEventMapper
import spock.lang.Specification

class CmNotificationSubscriptionMappersFacadeSpec extends Specification{

    def mockCmNotificationDmiInEventMapper = Mock(CmNotificationSubscriptionDmiInEventMapper)
    def mockCmNotificationNcmpOutEventMapper = Mock(CmNotificationSubscriptionNcmpOutEventMapper)

    def objectUnderTest = new CmNotificationSubscriptionMappersFacade(mockCmNotificationDmiInEventMapper,
        mockCmNotificationNcmpOutEventMapper)

    def 'Get cm notification subscription DMI in event'() {
        given: 'a list of predicates'
            def testListOfPredicates = []
        when: 'method to create a cm notification subscription dmi in event is called with predicates'
            objectUnderTest.toCmNotificationSubscriptionDmiInEvent(testListOfPredicates)
        then: 'the parameters is delegated to the correct dmi in event mapper method'
            1 * mockCmNotificationDmiInEventMapper.toCmNotificationSubscriptionDmiInEvent(testListOfPredicates)
    }

    def 'Get cm notification subscription ncmp out event'() {
        given: 'a subscription details map'
            def testSubscriptionDetailsMap = [:]
        when: 'method to create cm notification subscription ncmp out event is called with the following parameters'
            objectUnderTest.toCmNotificationSubscriptionNcmpOutEvent("test-id", testSubscriptionDetailsMap)
        then: 'the parameters is delegated to the correct ncmp out event mapper method'
            1 * mockCmNotificationNcmpOutEventMapper.toCmNotificationSubscriptionNcmpOutEvent("test-id",
            testSubscriptionDetailsMap)
    }

    def 'Get cm notification subscription ncmp out event for a rejected request'() {
        given: 'a list of target filters'
            def testRejectedTargetFilters = []
        when: 'method to create cm notification subscription ncmp out event is called with the following parameters'
            objectUnderTest.toCmNotificationSubscriptionNcmpOutEventForRejectedRequest(
                "test-id", testRejectedTargetFilters)
        then: 'the parameters is delegated to the correct ncmp out event mapper method'
            1 * mockCmNotificationNcmpOutEventMapper.toCmNotificationSubscriptionNcmpOutEventForRejectedRequest(
                "test-id", testRejectedTargetFilters)
    }
}
