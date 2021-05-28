/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Bell Canada. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.notification

import org.onap.cps.event.model.CpsDataUpdatedEvent
import spock.lang.Specification

class NotificationServiceSpec extends Specification {

    def mockNotificationPublisher = Mock(NotificationPublisher)
    def spyNotificationErrorHandler = Spy(new NotificationErrorHandler())
    def mockCpsDataUpdatedEventFactory = Mock(CpsDataUpdatedEventFactory)

    def notificationService = new NotificationService(true, mockNotificationPublisher,
            mockCpsDataUpdatedEventFactory, spyNotificationErrorHandler)

    def myDataspaceName = 'my-dataspace'
    def myAnchorName = 'my-anchorname'

    def 'Skip sending notification when disabled.'() {

        given: 'notification is disabled'
            notificationService.dataUpdatedEventNotificationEnabled = false

        when: 'dataUpdatedEvent is received'
            notificationService.processDataUpdatedEvent(myDataspaceName, myAnchorName)

        then: 'the notification is not sent'
            0 * mockNotificationPublisher.sendNotification(_)
    }

    def 'Send notification when enabled.'() {

        given: 'notification is enabled'
            notificationService.dataUpdatedEventNotificationEnabled = true
        and: 'event factory can create event successfully'
            def cpsDataUpdatedEvent = new CpsDataUpdatedEvent()
            mockCpsDataUpdatedEventFactory.createCpsDataUpdatedEvent(myDataspaceName, myAnchorName) >> cpsDataUpdatedEvent

        when: 'dataUpdatedEvent is received'
            notificationService.processDataUpdatedEvent(myDataspaceName, myAnchorName)

        then: 'notification is sent with correct event'
            1 * mockNotificationPublisher.sendNotification(cpsDataUpdatedEvent)
    }

    def 'No exception is thrown to ensure that cps operation does not fail because of notification service'() {
        given: 'Event factory can not create event successfully'
            mockCpsDataUpdatedEventFactory.createCpsDataUpdatedEvent(myDataspaceName, myAnchorName) >>
                    { throw new Exception("Could not create event") }

        when: 'event is sent for processing'
            notificationService.processDataUpdatedEvent(myDataspaceName, myAnchorName)

        then: 'Error is handled and not thrown to caller'
            notThrown Exception
            1 * spyNotificationErrorHandler.onException(_,_,_,_)

    }

}
