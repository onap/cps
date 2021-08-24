/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2021 Bell Canada.
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

package org.onap.cps.notification

import java.time.OffsetDateTime
import org.onap.cps.event.model.CpsDataUpdatedEvent
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

@SpringBootTest
@EnableConfigurationProperties
@ContextConfiguration(classes = [NotificationProperties])
class NotificationServiceSpec extends Specification {

    @SpringBean
    NotificationPublisher mockNotificationPublisher = Mock()
    @SpringBean
    NotificationErrorHandler spyNotificationErrorHandler = Spy(new NotificationErrorHandler())
    @SpringBean
    CpsDataUpdatedEventFactory mockCpsDataUpdatedEventFactory = Mock()

    @Autowired
    NotificationProperties notificationProperties
    NotificationProperties spyNotificationProperties

    @Shared
    def myDataspacePublishedName = 'my-dataspace-published'
    def myAnchorName = 'my-anchorname'

    def 'Skip sending notification when disabled.'() {
        given: 'notification is disabled'
            def objectUnderTest = createNotificationService(false)
        when: 'dataUpdatedEvent is received'
            objectUnderTest.processDataUpdatedEvent(myDataspacePublishedName, myAnchorName, null)
        then: 'the notification is not sent'
            0 * mockNotificationPublisher.sendNotification(_)
    }

    def 'Send notification when enabled: #scenario.'() {
        given: 'notification is enabled'
            def objectUnderTest = createNotificationService(true)
        and: 'observedTimestamp is provided'
            def observedTimestamp = OffsetDateTime.now();
        and: 'event factory can create event successfully'
            def cpsDataUpdatedEvent = new CpsDataUpdatedEvent()
            mockCpsDataUpdatedEventFactory.createCpsDataUpdatedEvent(dataspaceName, myAnchorName, observedTimestamp) >>
                cpsDataUpdatedEvent
        when: 'dataUpdatedEvent is received'
            objectUnderTest.processDataUpdatedEvent(dataspaceName, myAnchorName, observedTimestamp)
        then: 'notification is sent'
            expectedSendNotificationCount * mockNotificationPublisher.sendNotification(cpsDataUpdatedEvent)
        where:
            scenario                               | dataspaceName            || expectedSendNotificationCount
            'dataspace name does not match filter' | 'does-not-match-pattern' || 0
            'dataspace name matches filter'        | myDataspacePublishedName || 1
    }

    def 'Error handling in notification service.'() {
        given: 'notification is enabled'
            def objectUnderTest = createNotificationService(true)
        and: 'event factory can not create event successfully'
            mockCpsDataUpdatedEventFactory.createCpsDataUpdatedEvent(myDataspacePublishedName, myAnchorName, null) >>
                { throw new Exception("Could not create event") }
        when: 'event is sent for processing'
            objectUnderTest.processDataUpdatedEvent(myDataspacePublishedName, myAnchorName, null)
        then: 'error is handled and not thrown to caller'
            notThrown Exception
            1 * spyNotificationErrorHandler.onException(_, _, _, _)
    }

    NotificationService createNotificationService(boolean notificationEnabled) {
        spyNotificationProperties = Spy(notificationProperties)
        spyNotificationProperties.isEnabled() >> notificationEnabled
        return new NotificationService(spyNotificationProperties, mockNotificationPublisher,
            mockCpsDataUpdatedEventFactory, spyNotificationErrorHandler)
    }
}
