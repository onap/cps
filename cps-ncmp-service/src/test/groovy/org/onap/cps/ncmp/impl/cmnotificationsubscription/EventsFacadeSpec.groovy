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

package org.onap.cps.ncmp.impl.cmnotificationsubscription

import org.onap.cps.ncmp.impl.cmnotificationsubscription.dmi.DmiInEventProducer
import org.onap.cps.ncmp.impl.cmnotificationsubscription.ncmp.NcmpOutEventProducer
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.ncmp_to_client.NcmpOutEvent
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.ncmp_to_dmi.DmiInEvent
import spock.lang.Specification

class EventsFacadeSpec extends Specification {

    def mockCmNotificationSubscriptionNcmpOutEventProducer = Mock(NcmpOutEventProducer)
    def mockCmNotificationSubscriptionDmiInEventProducer = Mock(DmiInEventProducer)

    def objectUnderTest = new EventsFacade(mockCmNotificationSubscriptionNcmpOutEventProducer,
        mockCmNotificationSubscriptionDmiInEventProducer)

    def 'Publish cm notification subscription ncmp out event'() {
        given: 'an ncmp out event'
            def ncmpOutEvent = new NcmpOutEvent()
        when: 'the method to publish cm notification subscription ncmp out event is called'
            objectUnderTest.publishNcmpOutEvent("some-id",
                "some-event", ncmpOutEvent, true)
        then: 'the parameters is delegated to the correct method once'
            1 * mockCmNotificationSubscriptionNcmpOutEventProducer.publishNcmpOutEvent(
                "some-id", "some-event", ncmpOutEvent, true)
    }

    def 'Publish cm notification subscription dmi in event'() {
        given: 'a dmi in event'
            def dmiInEvent = new DmiInEvent()
        when: 'the method to publish cm notification subscription ncmp out event is called'
            objectUnderTest.publishDmiInEvent("some-id",
                "some-dmi", "some-event", dmiInEvent)
        then: 'the parameters is delegated to the correct method once'
            1 * mockCmNotificationSubscriptionDmiInEventProducer.publishDmiInEvent("some-id",
                "some-dmi", "some-event", dmiInEvent)
    }
}