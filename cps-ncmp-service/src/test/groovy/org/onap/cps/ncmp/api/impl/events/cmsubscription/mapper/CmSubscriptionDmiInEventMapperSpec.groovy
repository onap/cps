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

import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.CmSubscriptionPredicate
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.ScopeFilter
import org.onap.cps.ncmp.api.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import spock.lang.Specification

import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.PASSTHROUGH_OPERATIONAL
import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.PASSTHROUGH_RUNNING

class CmSubscriptionDmiInEventMapperSpec extends Specification {

    def mockInventoryPersistence = Mock(InventoryPersistence)

    def objectUnderTest = new CmSubscriptionDmiInEventMapper(mockInventoryPersistence)

    def setup() {
        def yangModelCmHandles = [new YangModelCmHandle(id: 'ch-1', dmiProperties: [new YangModelCmHandle.Property('k1', 'v1')], publicProperties: []),
                                  new YangModelCmHandle(id: 'ch-2', dmiProperties: [new YangModelCmHandle.Property('k2', 'v2')], publicProperties: [])]
        mockInventoryPersistence.getYangModelCmHandles(['ch-1', 'ch-2'] as Set) >> yangModelCmHandles
    }

    def 'Check for Cm Subscription DMI In Event mapping'() {
        given: 'a collection of cm subscription predicates'
            def cmSubscriptionPredicates = [new CmSubscriptionPredicate(targetFilter: ['ch-1'], scopeFilter: new ScopeFilter(datastoreType: PASSTHROUGH_RUNNING, xpathFilters: ['/ch-1'])),
                                            new CmSubscriptionPredicate(targetFilter: ['ch-2'], scopeFilter: new ScopeFilter(datastoreType: PASSTHROUGH_OPERATIONAL, xpathFilters: ['/ch-2']))]
        when: 'we try to map the values'
            def result = objectUnderTest.toCmSubscriptionDmiInEvent(cmSubscriptionPredicates)
        then: 'we get the Cm Subscription Dmi in event'
            assert result != null
        and: 'it contains correct cm subscription cmhandle object'
            assert result.data.cmhandles.cmhandleId.containsAll(['ch-1', 'ch-2'])
            assert result.data.cmhandles.privateProperties.containsAll([['k1': 'v1'], ['k2': 'v2']])
        and: 'also has the correct cm subscription predicates'
            assert result.data.predicates.targetFilter.containsAll([['ch-1'], ['ch-2']])

    }
}
