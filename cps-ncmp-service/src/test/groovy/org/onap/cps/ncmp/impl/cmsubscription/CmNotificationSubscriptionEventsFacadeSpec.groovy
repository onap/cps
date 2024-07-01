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

package org.onap.cps.ncmp.impl.cmsubscription

import org.onap.cps.ncmp.events.cmnotificationsubscription_merge1_0_0.ncmp_to_dmi.CmNotificationSubscriptionDmiInEvent
import org.onap.cps.ncmp.events.cmsubscription_merge1_0_0.ncmp_to_client.CmNotificationSubscriptionNcmpOutEvent
import org.onap.cps.ncmp.impl.cmsubscription.dmi.CmNotificationSubscriptionDmiInEventProducer
import org.onap.cps.ncmp.impl.cmsubscription.ncmp.CmNotificationSubscriptionNcmpOutEventProducer
import spock.lang.Specification

class CmNotificationSubscriptionEventsFacadeSpec extends Specification {

    def mockCmNotificationSubscriptionNcmpOutEventProducer = Mock(CmNotificationSubscriptionNcmpOutEventProducer)
    def mockCmNotificationSubscriptionDmiInEventProducer = Mock(CmNotificationSubscriptionDmiInEventProducer)

    def objectUnderTest = new CmNotificationSubscriptionEventsFacade(mockCmNotificationSubscriptionNcmpOutEventProducer,
        mockCmNotificationSubscriptionDmiInEventProducer)

    def 'Publish cm notification subscription ncmp out event'() {
        given: 'an ncmp out event'
            def cmNotificationSubscriptionNcmpOutEvent = new CmNotificationSubscriptionNcmpOutEvent()
        when: 'the method to publish cm notification subscription ncmp out event is called'
            objectUnderTest.publishCmNotificationSubscriptionNcmpOutEvent("some-id",
                "some-event", cmNotificationSubscriptionNcmpOutEvent, true)
        then: 'the parameters is delegated to the correct method once'
            1 * mockCmNotificationSubscriptionNcmpOutEventProducer.publishCmNotificationSubscriptionNcmpOutEvent(
                "some-id", "some-event", cmNotificationSubscriptionNcmpOutEvent, true)
    }

    def 'Publish cm notification subscription dmi in event'() {
        given: 'a dmi in event'
            def cmNotificationSubscriptionDmiInEvent = new CmNotificationSubscriptionDmiInEvent()
        when: 'the method to publish cm notification subscription ncmp out event is called'
            objectUnderTest.publishCmNotificationSubscriptionDmiInEvent("some-id",
                "some-dmi", "some-event", cmNotificationSubscriptionDmiInEvent)
        then: 'the parameters is delegated to the correct method once'
            1 * mockCmNotificationSubscriptionDmiInEventProducer.publishCmNotificationSubscriptionDmiInEvent("some-id",
                "some-dmi", "some-event", cmNotificationSubscriptionDmiInEvent)
    }
}