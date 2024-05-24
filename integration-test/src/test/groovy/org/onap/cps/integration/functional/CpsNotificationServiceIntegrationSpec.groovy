/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 TechMahindra Ltd.
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

package org.onap.cps.integration.functional

import org.onap.cps.api.CpsNotificationService
import org.onap.cps.integration.base.FunctionalSpecBase

class CpsNotificationServiceIntegrationSpec extends FunctionalSpecBase {
    CpsNotificationService objectUnderTest;

    def setup() { objectUnderTest = cpsNotificationService }

    def 'subscribe notification for list of anchors.'() {
        when: 'notification is subscribed for list of anchors'
            objectUnderTest.updateNotificationSubscription(FUNCTIONAL_TEST_DATASPACE_1, 'subscribe', [BOOKSTORE_ANCHOR_1, BOOKSTORE_ANCHOR_2])
        then: 'notification is enabled for the anchor'
            assert objectUnderTest.isNotificationEnabled(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1)
            assert objectUnderTest.isNotificationEnabled(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_2)
    }

    def 'unsubscribe notification for list of anchors.'() {
        when: 'notification is unsubscribed for list of anchors'
            objectUnderTest.updateNotificationSubscription(FUNCTIONAL_TEST_DATASPACE_1, 'unsubscribe', [BOOKSTORE_ANCHOR_1, BOOKSTORE_ANCHOR_2])
        then: 'notification is disabled for the anchor'
            assert !objectUnderTest.isNotificationEnabled(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1)
            assert !objectUnderTest.isNotificationEnabled(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_2)
    }

    def 'subscribe notification for all of anchors of a dataspace.'() {
        when: 'notification is subscribed for all anchors of a dataspace'
            objectUnderTest.updateNotificationSubscription(FUNCTIONAL_TEST_DATASPACE_1, 'subscribe', [])
        then: 'notification is enabled for all the anchors'
            assert objectUnderTest.isNotificationEnabled(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1)
            assert objectUnderTest.isNotificationEnabled(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_2)
    }

    def 'unsubscribe notification for all of anchors of a dataspace.'() {
        when: 'notification is unsubscribed for all anchors or the dataspace'
            objectUnderTest.updateNotificationSubscription(FUNCTIONAL_TEST_DATASPACE_1, 'unsubscribe', [])
        then: 'notification is disabled for all the anchors of the dataspace'
            assert !objectUnderTest.isNotificationEnabled(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1)
            assert !objectUnderTest.isNotificationEnabled(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_2)
    }
}
