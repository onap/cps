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

package org.onap.cps.notification.updatedevents

import org.apache.kafka.clients.producer.ProducerRecord
import org.onap.cps.event.model.Content
import org.onap.cps.event.model.CpsDataUpdatedEvent
import org.spockframework.spring.SpringBean
import org.springframework.kafka.KafkaException
import org.springframework.kafka.core.KafkaTemplate
import spock.util.concurrent.PollingConditions

class CpsUpdatedEventNotificationPublisherSpec extends KafkaPublisherSpecBase {

    @SpringBean
    CpsUpdatedEventNotificationErrorHandler spyNotificationErrorHandler = Spy(new CpsUpdatedEventNotificationErrorHandler())

    @SpringBean
    CpsUpdatedEventProducerListener spyKafkaProducerListener = Spy(new CpsUpdatedEventProducerListener<>(spyNotificationErrorHandler))

    KafkaTemplate spyKafkaTemplate
    CpsUpdatedEventNotificationPublisher objectUnderTest

    def myAnchorName = 'my-anchor'
    def myDataspaceName = 'my-dataspace'

    def cpsDataUpdatedEvent = new CpsDataUpdatedEvent()
            .withContent(new Content()
                    .withDataspaceName(myDataspaceName)
                    .withAnchorName(myAnchorName))

    def setup() {
        spyKafkaTemplate = Spy(kafkaTemplate)
        objectUnderTest = new CpsUpdatedEventNotificationPublisher(spyKafkaTemplate, cpsEventTopic);
    }

    def 'Sending event to message bus with correct message Key.'() {

        when: 'event is sent to publisher'
            objectUnderTest.sendNotification(cpsDataUpdatedEvent)
            kafkaTemplate.flush()

        then: 'event is sent to correct topic with the expected messageKey'
            interaction {
                def messageKey = myDataspaceName + "," + myAnchorName
                1 * spyKafkaTemplate.send(cpsEventTopic, messageKey, cpsDataUpdatedEvent)
            }
        and: 'received a successful response'
            1 * spyKafkaProducerListener.onSuccess(_ as ProducerRecord, _)
        and: 'kafka consumer returns expected message'
            def conditions = new PollingConditions(timeout: 60, initialDelay: 0, factor: 1)
            conditions.eventually {
                assert cpsDataUpdatedEvent == consumedMessages.get(0)
            }
    }

    def 'Handling of async errors from message bus.'() {
        given: 'topic does not exist'
            objectUnderTest.topicName = 'non-existing-topic'

        when: 'message to sent to a non-existing topic'
            objectUnderTest.sendNotification(cpsDataUpdatedEvent)
            kafkaTemplate.flush()

        then: 'error is thrown'
            thrown KafkaException
        and: 'error handler is called with exception details'
            1 * spyKafkaProducerListener.onError(_ as ProducerRecord, _, _ as Exception)
            1 * spyNotificationErrorHandler.onException(_ as String, _ as Exception,
                    _ as ProducerRecord, _)
    }

}
