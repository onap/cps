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

package org.onap.cps.ncmp.impl.cmnotificationsubscription.ncmp

import org.onap.cps.ncmp.api.data.models.DatastoreType
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.CmSubscriptionStatus
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionDetails
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionPredicate
import spock.lang.Specification

import static org.onap.cps.ncmp.api.data.models.DatastoreType.PASSTHROUGH_OPERATIONAL
import static org.onap.cps.ncmp.api.data.models.DatastoreType.PASSTHROUGH_RUNNING
import static org.onap.cps.ncmp.impl.cmnotificationsubscription.models.CmSubscriptionStatus.ACCEPTED
import static org.onap.cps.ncmp.impl.cmnotificationsubscription.models.CmSubscriptionStatus.PENDING
import static org.onap.cps.ncmp.impl.cmnotificationsubscription.models.CmSubscriptionStatus.REJECTED

class NcmpOutEventMapperSpec extends Specification {

    static Map<String, DmiCmSubscriptionDetails> dmiSubscriptionDetailsMap

    def objectUnderTest = new NcmpOutEventMapper()

    def setup() {
        def dmiSubscriptionPredicateA = new DmiCmSubscriptionPredicate(['ch-A'] as Set, PASSTHROUGH_RUNNING, ['/a'] as Set)
        def dmiSubscriptionPredicateB = new DmiCmSubscriptionPredicate(['ch-B'] as Set, PASSTHROUGH_OPERATIONAL, ['/b'] as Set)
        def dmiSubscriptionPredicateC = new DmiCmSubscriptionPredicate(['ch-C'] as Set, PASSTHROUGH_OPERATIONAL, ['/c'] as Set)
        dmiSubscriptionDetailsMap = ['dmi-1': new DmiCmSubscriptionDetails([dmiSubscriptionPredicateA], PENDING),
                                     'dmi-2': new DmiCmSubscriptionDetails([dmiSubscriptionPredicateB], ACCEPTED),
                                     'dmi-3': new DmiCmSubscriptionDetails([dmiSubscriptionPredicateC], REJECTED)
        ]
    }

    def 'Check for Cm Notification Subscription Outgoing event mapping'() {
        when: 'we try to map the event to send it to client'
            def result = objectUnderTest.toNcmpOutEvent('test-subscription', dmiSubscriptionDetailsMap)
        then: 'event is mapped correctly for the subscription'
            result.data.subscriptionId == 'test-subscription'
        and: 'the cm handle ids are part of correct list'
            result.data.pendingTargets == ['ch-A']
            result.data.acceptedTargets == ['ch-B']
            result.data.rejectedTargets == ['ch-C']
    }

    def 'Check for Cm Notification Rejected Subscription Outgoing event mapping'() {
        when: 'we try to map the event to send it to client'
            def result = objectUnderTest.toNcmpOutEventForRejectedRequest('test-subscription', ['ch-1', 'ch-2'])
        then: 'event is mapped correctly for the subscription id'
            result.data.subscriptionId == 'test-subscription'
        and: 'the cm handle ids are part of correct list'
            result.data.withRejectedTargets(['ch-1', 'ch-2'])
    }
}
