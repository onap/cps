/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2024 Nordix Foundation.
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
package org.onap.cps.ncmp.impl.cmnotificationsubscription.ncmp


import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionPredicate
import org.onap.cps.ncmp.impl.cmnotificationsubscription.utils.CmSubscriptionPersistenceService
import spock.lang.Specification

import static org.onap.cps.ncmp.api.data.models.DatastoreType.PASSTHROUGH_OPERATIONAL

class CmSubscriptionComparatorSpec extends Specification {

    def mockCmSubscriptionPersistenceService = Mock(CmSubscriptionPersistenceService)
    def objectUnderTest = new CmSubscriptionComparator(mockCmSubscriptionPersistenceService)

    def 'Find Delta of given list of predicates'() {
        given: 'A list of predicates'
            def predicates = [new DmiCmSubscriptionPredicate(['ch-1', 'ch-2'].toSet(), PASSTHROUGH_OPERATIONAL, ['a/1/', 'b/2'].toSet())]
        and: '3 positive responses and 1 negative.'
            mockCmSubscriptionPersistenceService.isOngoingCmSubscription(PASSTHROUGH_OPERATIONAL, 'ch-1', 'a/1/') >>> true
            mockCmSubscriptionPersistenceService.isOngoingCmSubscription(PASSTHROUGH_OPERATIONAL, 'ch-1', 'b/2') >>> true
            mockCmSubscriptionPersistenceService.isOngoingCmSubscription(PASSTHROUGH_OPERATIONAL, 'ch-2', 'a/1/') >>> true
            mockCmSubscriptionPersistenceService.isOngoingCmSubscription(PASSTHROUGH_OPERATIONAL, 'ch-2', 'b/2') >>> false
        when: 'getDelta is called'
            def result = objectUnderTest.getNewDmiSubscriptionPredicates(predicates)
        then: 'verify correct delta is returned'
            assert result.size() == 1
            assert result[0].targetCmHandleIds[0] == 'ch-2'
            assert result[0].xpaths[0] == 'b/2'

    }

    def 'Find Delta of given list of predicates when it is an ongoing Cm Subscription'() {
        given: 'A list of predicates'
            def predicates = [new DmiCmSubscriptionPredicate(['ch-1'].toSet(), PASSTHROUGH_OPERATIONAL, ['a/1/'].toSet())]
        and: 'its already present'
            mockCmSubscriptionPersistenceService.isOngoingCmSubscription(PASSTHROUGH_OPERATIONAL, 'ch-1', 'a/1/') >>> true
        when: 'getDelta is called'
            def result = objectUnderTest.getNewDmiSubscriptionPredicates(predicates)
        then: 'verify correct delta is returned'
            assert result.size() == 0
    }

}
