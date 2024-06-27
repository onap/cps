/*
 * ============LICENSE_START=======================================================
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

package org.onap.cps.ncmp.api.impl.events.cmsubscription.mapper

import org.onap.cps.ncmp.api.data.models.DatastoreType
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.CmNotificationSubscriptionStatus
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.DmiCmNotificationSubscriptionDetails
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.DmiCmNotificationSubscriptionPredicate
import spock.lang.Specification

class CmNotificationSubscriptionNcmpOutEventMapperSpec extends Specification {

    static Map<String, DmiCmNotificationSubscriptionDetails> dmiCmNotificationSubscriptionDetailsMap

    def objectUnderTest = new CmNotificationSubscriptionNcmpOutEventMapper()

    def setup() {
        def dmiCmNotificationSubscriptionPredicateA = new DmiCmNotificationSubscriptionPredicate(['ch-A'] as Set, DatastoreType.PASSTHROUGH_RUNNING, ['/a'] as Set)
        def dmiCmNotificationSubscriptionPredicateB = new DmiCmNotificationSubscriptionPredicate(['ch-B'] as Set, DatastoreType.PASSTHROUGH_OPERATIONAL, ['/b'] as Set)
        def dmiCmNotificationSubscriptionPredicateC = new DmiCmNotificationSubscriptionPredicate(['ch-C'] as Set, DatastoreType.PASSTHROUGH_OPERATIONAL, ['/c'] as Set)
        dmiCmNotificationSubscriptionDetailsMap = ['dmi-1': new DmiCmNotificationSubscriptionDetails([dmiCmNotificationSubscriptionPredicateA], CmNotificationSubscriptionStatus.PENDING),
                                                   'dmi-2': new DmiCmNotificationSubscriptionDetails([dmiCmNotificationSubscriptionPredicateB], CmNotificationSubscriptionStatus.ACCEPTED),
                                                   'dmi-3': new DmiCmNotificationSubscriptionDetails([dmiCmNotificationSubscriptionPredicateC], CmNotificationSubscriptionStatus.REJECTED)
        ]
    }

    def 'Check for Cm Notification Subscription Outgoing event mapping'() {
        when: 'we try to map the event to send it to client'
            def result = objectUnderTest.toCmNotificationSubscriptionNcmpOutEvent('test-subscription', dmiCmNotificationSubscriptionDetailsMap)
        then: 'event is mapped correctly for the subscription'
            result.data.subscriptionId == 'test-subscription'
        and: 'the cm handle ids are part of correct list'
            result.data.pendingTargets == ['ch-A']
            result.data.acceptedTargets == ['ch-B']
            result.data.rejectedTargets == ['ch-C']
    }

    def 'Check for Cm Notification Rejected Subscription Outgoing event mapping'() {
        when: 'we try to map the event to send it to client'
            def result = objectUnderTest.toCmNotificationSubscriptionNcmpOutEventForRejectedRequest('test-subscription', ['ch-1', 'ch-2'])
        then: 'event is mapped correctly for the subscription id'
            result.data.subscriptionId == 'test-subscription'
        and: 'the cm handle ids are part of correct list'
            result.data.withRejectedTargets(['ch-1', 'ch-2'])
    }
}
