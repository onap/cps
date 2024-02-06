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
package org.onap.cps.ncmp.api.impl.events.cmsubscription

import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.CmSubscriptionPredicate
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.ScopeFilter
import org.onap.cps.ncmp.api.impl.events.cmsubscription.service.CmSubscriptionService
import org.onap.cps.ncmp.api.impl.operations.DatastoreType
import spock.lang.Specification

class CmSubscriptionDeltaSpec extends Specification {

    def mockCmSubscriptionService = Mock(CmSubscriptionService)
    def objectUnderTest = new CmSubscriptionDelta(mockCmSubscriptionService)

    def 'Find Delta of given list of predicates'() {
        given: 'A list of predicates'
            def predicateList = [new CmSubscriptionPredicate(targetFilter: ['ch-1','ch-2'], scopeFilter: new ScopeFilter(datastoreType: DatastoreType.PASSTHROUGH_OPERATIONAL, xpathFilters: ['a/1/','b/2']))]
            mockCmSubscriptionService.isOngoingCmSubscription(DatastoreType.PASSTHROUGH_OPERATIONAL, 'ch-1', 'a/1/') >>> true
            mockCmSubscriptionService.isOngoingCmSubscription(DatastoreType.PASSTHROUGH_OPERATIONAL, 'ch-1', 'b/2') >>> true
            mockCmSubscriptionService.isOngoingCmSubscription(DatastoreType.PASSTHROUGH_OPERATIONAL, 'ch-2', 'a/1/') >>> true
            mockCmSubscriptionService.isOngoingCmSubscription(DatastoreType.PASSTHROUGH_OPERATIONAL, 'ch-2', 'b/2') >>> false
        when: 'getDelta is called'
            def result = objectUnderTest.getDelta(predicateList)
        then: 'verify correct delta is returned'
            assert result.size() == 1
            assert result.get(0).targetFilter.get(0) == 'ch-2'
            assert result.get(0).scopeFilter.xpathFilters.get(0) == 'b/2'

    }

}
