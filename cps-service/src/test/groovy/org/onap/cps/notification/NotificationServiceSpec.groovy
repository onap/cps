/*
 * ============LICENSE_START=======================================================
 *  Copyright (c) 2021-2022 Bell Canada.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
import org.onap.cps.config.AsyncConfig
import org.onap.cps.event.model.CpsDataUpdatedEvent
import org.onap.cps.spi.model.Anchor
import org.spockframework.spring.SpringBean
import org.spockframework.spring.SpringSpy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.TimeUnit

@SpringBootTest
@EnableConfigurationProperties
@ContextConfiguration(classes = [NotificationProperties, NotificationService, NotificationErrorHandler, AsyncConfig])
class NotificationServiceSpec extends Specification {

    @SpringBean
    NotificationPublisher mockNotificationPublisher = Mock()
    @SpringBean
    CpsDataUpdatedEventFactory mockCpsDataUpdatedEventFactory = Mock()
    @SpringSpy
    NotificationErrorHandler spyNotificationErrorHandler
    @SpringSpy
    NotificationProperties spyNotificationProperties

    @Autowired
    NotificationService objectUnderTest

    @Shared
    def anchor = new Anchor('my-anchorname', 'my-dataspace-published', 'my-schemaset-name')
    def myObservedTimestamp = OffsetDateTime.now()

    def 'Skip sending notification when disabled.'() {
        given: 'notification is disabled'
            spyNotificationProperties.isEnabled() >> false
        when: 'dataUpdatedEvent is received'
            objectUnderTest.processDataUpdatedEvent(anchor, myObservedTimestamp, '/', Operation.CREATE)
        then: 'the notification is not sent'
            0 * mockNotificationPublisher.sendNotification(_)
    }

    def 'Send notification when enabled: #scenario.'() {
        given: 'notification is enabled'
            spyNotificationProperties.isEnabled() >> true
        and: 'an anchor is in dataspace where #scenario'
            def anchor = new Anchor('my-anchorname', dataspaceName, 'my-schemaset-name')
        and: 'event factory can create event successfully'
            def cpsDataUpdatedEvent = new CpsDataUpdatedEvent()
            mockCpsDataUpdatedEventFactory.createCpsDataUpdatedEvent(anchor, myObservedTimestamp, Operation.CREATE) >>
                    cpsDataUpdatedEvent
        when: 'dataUpdatedEvent is received'
            def future = objectUnderTest.processDataUpdatedEvent(anchor, myObservedTimestamp,
                    '/', Operation.CREATE)
        and: 'wait for async processing to complete'
            future.get(10, TimeUnit.SECONDS)
        then: 'async process completed successfully'
            future.isDone()
        and: 'notification is sent'
            expectedSendNotificationCount * mockNotificationPublisher.sendNotification(cpsDataUpdatedEvent)
        where:
            scenario                               | dataspaceName            || expectedSendNotificationCount
            'dataspace name does not match filter' | 'does-not-match-pattern' || 0
            'dataspace name matches filter'        | 'my-dataspace-published' || 1
    }

    def '#scenario are changed with xpath #xpath and operation #operation'() {
        given: 'notification is enabled'
            spyNotificationProperties.isEnabled() >> true
        and: 'event factory creates event if operation is #operation'
            def cpsDataUpdatedEvent = new CpsDataUpdatedEvent()
            mockCpsDataUpdatedEventFactory.createCpsDataUpdatedEvent(anchor, myObservedTimestamp, expectedOperationInEvent) >>
                    cpsDataUpdatedEvent
        when: 'dataUpdatedEvent is received for root xpath'
            def future = objectUnderTest.processDataUpdatedEvent(anchor, myObservedTimestamp, xpath, operation)
        and: 'wait for async processing to complete'
            future.get(10, TimeUnit.SECONDS)
        then: 'async process completed successfully'
            future.isDone()
        and: 'notification is sent'
            1 * mockNotificationPublisher.sendNotification(cpsDataUpdatedEvent)
        where:
            scenario                                   | xpath           | operation            || expectedOperationInEvent
            'Same event is sent when root nodes'       | ''              | Operation.CREATE     || Operation.CREATE
            'Same event is sent when root nodes'       | ''              | Operation.UPDATE     || Operation.UPDATE
            'Same event is sent when root nodes'       | ''              | Operation.DELETE     || Operation.DELETE
            'Same event is sent when root nodes'       | '/'             | Operation.CREATE     || Operation.CREATE
            'Same event is sent when root nodes'       | '/'             | Operation.UPDATE     || Operation.UPDATE
            'Same event is sent when root nodes'       | '/'             | Operation.DELETE     || Operation.DELETE
            'UPDATE event is sent when non root nodes' | '/parent/child' | Operation.CREATE     || Operation.UPDATE
            'UPDATE event is sent when non root nodes' | '/parent/child' | Operation.UPDATE     || Operation.UPDATE
            'UPDATE event is sent when non root nodes' | '/parent/child' | Operation.DELETE     || Operation.UPDATE
            'Same event is sent when container nodes'  | '/parent'       | Operation.CREATE     || Operation.CREATE
            'Same event is sent when container nodes'  | '/parent'       | Operation.UPDATE     || Operation.UPDATE
            'Same event is sent when container nodes'  | '/parent'       | Operation.DELETE     || Operation.DELETE
    }

    def 'Error handling in notification service.'() {
        given: 'notification is enabled'
            spyNotificationProperties.isEnabled() >> true
        and: 'event factory can not create event successfully'
            mockCpsDataUpdatedEventFactory.createCpsDataUpdatedEvent(anchor, myObservedTimestamp, Operation.CREATE) >>
                    { throw new Exception("Could not create event") }
        when: 'event is sent for processing'
            def future = objectUnderTest.processDataUpdatedEvent(anchor, myObservedTimestamp, '/', Operation.CREATE)
        and: 'wait for async processing to complete'
            future.get(10, TimeUnit.SECONDS)
        then: 'async process completed successfully'
            future.isDone()
        and: 'error is handled and not thrown to caller'
            notThrown Exception
            1 * spyNotificationErrorHandler.onException(_, _, _, '/', Operation.CREATE)
    }
}
