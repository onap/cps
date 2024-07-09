/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the 'License');
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

package org.onap.cps.integration.functional.ncmp

import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.impl.cmnotificationsubscription.utils.CmSubscriptionPersistenceService
import org.springframework.beans.factory.annotation.Autowired

import static org.onap.cps.ncmp.api.data.models.DatastoreType.PASSTHROUGH_RUNNING

class NcmpCmNotificationSubscriptionSpec extends CpsIntegrationSpecBase {

    @Autowired
    CmSubscriptionPersistenceService cmSubscriptionPersistenceService

    def 'Adding a new cm notification subscription'() {
        given: 'there is no ongoing cm subscription for the following'
            def datastoreType = PASSTHROUGH_RUNNING
            def cmHandleId = 'ch-1'
            def xpath = '/x/y'
            assert cmSubscriptionPersistenceService.
                getOngoingCmSubscriptionIds(datastoreType, cmHandleId, xpath).size() == 0
        when: 'we add a new cm notification subscription'
            cmSubscriptionPersistenceService.addCmSubscription(datastoreType, cmHandleId, xpath,
                'subId-1')
        then: 'there is an ongoing cm subscription for that CM handle and xpath'
            assert cmSubscriptionPersistenceService.isOngoingCmSubscription(datastoreType, cmHandleId, xpath)
        and: 'only one subscription id is related to now ongoing cm subscription'
            assert cmSubscriptionPersistenceService.getOngoingCmSubscriptionIds(datastoreType, cmHandleId, xpath).size() == 1
    }

    def 'Adding a cm notification subscription to the already existing cm handle but non existing xpath'() {
        given: 'an ongoing cm subscription with the following details'
            def datastoreType = PASSTHROUGH_RUNNING
            def cmHandleId = 'ch-1'
            def existingXpath = '/x/y'
            assert cmSubscriptionPersistenceService.isOngoingCmSubscription(datastoreType, cmHandleId, existingXpath)
        and: 'a non existing cm subscription with same datastore name and cm handle but different xpath'
            def nonExistingXpath = '/x2/y2'
            assert !cmSubscriptionPersistenceService.isOngoingCmSubscription(datastoreType, cmHandleId, nonExistingXpath)
        when: 'a new cm notification subscription is made for the existing cm handle and non existing xpath'
            cmSubscriptionPersistenceService.addCmSubscription(datastoreType, cmHandleId, nonExistingXpath,
                'subId-2')
        then: 'there is an ongoing cm subscription for that CM handle and xpath'
            assert cmSubscriptionPersistenceService.isOngoingCmSubscription(datastoreType, cmHandleId, nonExistingXpath)
        and: 'only one subscription id is related to now ongoing cm subscription'
            assert cmSubscriptionPersistenceService.getOngoingCmSubscriptionIds(datastoreType, cmHandleId, nonExistingXpath).size() == 1
    }

    def 'Adding a cm notification subscription to the already existing cm handle and xpath'() {
        given: 'an ongoing cm subscription with the following details'
            def datastoreType = PASSTHROUGH_RUNNING
            def cmHandleId = 'ch-1'
            def xpath = '/x/y'
        when: 'a new cm notification subscription is made for the SAME CM handle and xpath'
            cmSubscriptionPersistenceService.addCmSubscription(datastoreType, cmHandleId, xpath,
                'subId-3')
        then: 'it is added to the ongoing list of subscription ids'
            def subscriptionIds = cmSubscriptionPersistenceService.getOngoingCmSubscriptionIds(datastoreType, cmHandleId, xpath)
            assert subscriptionIds.size() == 2
        and: 'both subscription ids exists for the CM handle and xpath'
            assert subscriptionIds.contains("subId-1") && subscriptionIds.contains("subId-3")
    }

    def 'Removing cm notification subscriber among other subscribers'() {
        given: 'an ongoing cm subscription with the following details'
            def datastoreType = PASSTHROUGH_RUNNING
            def cmHandleId = 'ch-1'
            def xpath = '/x/y'
        and: 'the number of subscribers is as follows'
            def originalNumberOfSubscribers =
                cmSubscriptionPersistenceService.getOngoingCmSubscriptionIds(datastoreType, cmHandleId, xpath).size()
        when: 'a subscriber is removed'
            cmSubscriptionPersistenceService.removeCmSubscription(datastoreType, cmHandleId, xpath, 'subId-3')
        then: 'the number of subscribers is reduced by 1'
            def updatedNumberOfSubscribers = cmSubscriptionPersistenceService.getOngoingCmSubscriptionIds(datastoreType, cmHandleId, xpath).size()
            assert updatedNumberOfSubscribers == originalNumberOfSubscribers - 1
    }

    def 'Removing the LAST cm notification subscriber for a given cm handle, datastore and xpath'() {
        given: 'an ongoing cm subscription with the following details'
            def datastoreType = PASSTHROUGH_RUNNING
            def cmHandleId = 'ch-1'
            def xpath = '/x/y'
        and: 'there is only one subscriber'
            assert cmSubscriptionPersistenceService
                .getOngoingCmSubscriptionIds(datastoreType, cmHandleId, xpath).size() == 1
        when: 'only subscriber is removed'
            cmSubscriptionPersistenceService.removeCmSubscription(datastoreType, cmHandleId, xpath, 'subId-1')
        then: 'there are no longer any subscriptions for the cm handle, datastore and xpath'
            assert !cmSubscriptionPersistenceService.isOngoingCmSubscription(datastoreType, cmHandleId, xpath)
    }

}
