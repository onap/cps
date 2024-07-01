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

package org.onap.cps.ncmp.impl.cmsubscription.utils

import org.onap.cps.ncmp.impl.cmsubscription.models.DmiCmNotificationSubscriptionPredicate
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import spock.lang.Specification

import static org.onap.cps.ncmp.api.data.models.DatastoreType.PASSTHROUGH_OPERATIONAL
import static org.onap.cps.ncmp.api.data.models.DatastoreType.PASSTHROUGH_RUNNING

class CmNotificationSubscriptionDmiInEventMapperSpec extends Specification {

    def mockInventoryPersistence = Mock(InventoryPersistence)

    def objectUnderTest = new CmNotificationSubscriptionDmiInEventMapper(mockInventoryPersistence)

    def setup() {
        def yangModelCmHandles = [new YangModelCmHandle(id: 'ch-1', dmiProperties: [new YangModelCmHandle.Property('k1', 'v1')], publicProperties: []),
                                  new YangModelCmHandle(id: 'ch-2', dmiProperties: [new YangModelCmHandle.Property('k2', 'v2')], publicProperties: [])]
        mockInventoryPersistence.getYangModelCmHandles(['ch-1', 'ch-2'] as Set) >> yangModelCmHandles
    }

    def 'Check for Cm Notification Subscription DMI In Event mapping'() {
        given: 'a collection of cm subscription predicates'
            def dmiCmNotificationSubscriptionPredicates = [new DmiCmNotificationSubscriptionPredicate(['ch-1'].toSet(), PASSTHROUGH_RUNNING, ['/ch-1'].toSet()),
                                                           new DmiCmNotificationSubscriptionPredicate(['ch-2'].toSet(), PASSTHROUGH_OPERATIONAL, ['/ch-2'].toSet())]
        when: 'we try to map the values'
            def result = objectUnderTest.toCmNotificationSubscriptionDmiInEvent(dmiCmNotificationSubscriptionPredicates)
        then: 'it contains correct cm notification subscription cmhandle object'
            assert result.data.cmHandles.cmhandleId.containsAll(['ch-1', 'ch-2'])
            assert result.data.cmHandles.privateProperties.containsAll([['k1': 'v1'], ['k2': 'v2']])
        and: 'also has the correct dmi cm notification subscription predicates'
            assert result.data.predicates.targetFilter.containsAll([['ch-1'], ['ch-2']])

    }
}
