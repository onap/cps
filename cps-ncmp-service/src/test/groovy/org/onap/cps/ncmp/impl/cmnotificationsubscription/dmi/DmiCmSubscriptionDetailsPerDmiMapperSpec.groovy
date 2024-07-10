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

package org.onap.cps.ncmp.impl.cmnotificationsubscription.dmi

import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionKey
import spock.lang.Specification

class DmiCmSubscriptionDetailsPerDmiMapperSpec extends Specification {

    def objectUnderTest = new DmiCmSubscriptionDetailsPerDmiMapper()

    def 'Check for grouping of Dmi Subscription Details'() {
        given: 'details in the form of datastore , cmhandle and xpath'
            def subscribersPerDmi = [
                'dmi-1': [
                    new DmiCmSubscriptionKey('ncmp-datastore:passthrough-operational', 'ch-1', '/a/b'),
                    new DmiCmSubscriptionKey('ncmp-datastore:passthrough-operational', 'ch-2', '/a/b')
                ],
                'dmi-2': [
                    new DmiCmSubscriptionKey('ncmp-datastore:passthrough-running', 'ch-3', '/c/d'),
                    new DmiCmSubscriptionKey('ncmp-datastore:passthrough-running', 'ch-3', '/e/f')
                ]
            ]
        when: 'we try to map the values based on datastore and xpath'
            def result = objectUnderTest.toDmiCmSubscriptionsPerDmi(subscribersPerDmi)
        then: 'the mapped values are grouped as expected for dmi-1'
            assert result['dmi-1'].dmiCmSubscriptionPredicates.size() == 1
            assert result['dmi-1'].dmiCmSubscriptionPredicates[0].targetCmHandleIds.containsAll(['ch-1', 'ch-2'])
        and: 'similarly for dmi-2'
            assert result['dmi-2'].dmiCmSubscriptionPredicates.size() == 2
            assert result['dmi-2'].dmiCmSubscriptionPredicates[0].targetCmHandleIds.contains('ch-3')
            assert result['dmi-2'].dmiCmSubscriptionPredicates[1].targetCmHandleIds.contains('ch-3')
    }
}
