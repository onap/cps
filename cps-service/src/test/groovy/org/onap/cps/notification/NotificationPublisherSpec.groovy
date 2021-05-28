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

import org.onap.cps.event.model.Content
import org.onap.cps.event.model.CpsDataUpdatedEvent
import org.spockframework.spring.SpringBean
import org.springframework.kafka.KafkaException
import org.springframework.kafka.core.KafkaTemplate

class NotificationPublisherSpec extends KafkaPublisherSpecBase {

    @SpringBean
    NotificationErrorHandler mockNotificationErrorHandler = Mock()

    KafkaTemplate spyKafkaTemplate
    KafkaProducerListener spyKafkaErrorHandler
    NotificationPublisher notificationPublisher

    def myAnchorName = 'my-anchor'
    def myDataspaceName = 'my-dataspace'

    def cpsDataUpdatedEvent = new CpsDataUpdatedEvent()
            .withContent(new Content()
                    .withDataspaceName(myDataspaceName)
                    .withAnchorName(myAnchorName))

    def setup() {
        spyKafkaErrorHandler = Spy(kafkaErrorHandler)
        kafkaTemplate.setProducerListener(spyKafkaErrorHandler)
        spyKafkaTemplate = Spy(kafkaTemplate)
        notificationPublisher = new NotificationPublisher(spyKafkaTemplate, cpsEventTopic);
    }

    def 'Sending event to eventbus with correct Message Key.'() {

        when: 'event is sent to publisher'
            notificationPublisher.sendNotification(cpsDataUpdatedEvent)
            kafkaTemplate.flush()

        then: 'event is sent to correct topic with the expected messageKey'
            interaction {
                def messageKey = myDataspaceName + "-" + myAnchorName
                1 * spyKafkaTemplate.send(cpsEventTopic, messageKey, cpsDataUpdatedEvent)
            }
        and: 'received a successful response'
            1 * spyKafkaErrorHandler.onSuccess(_, _)
        and: 'kafka consumer returns expected message'
            cpsDataUpdatedEvent == getEventFromKafka()
    }

    def 'All errors are handled by the NotificationErrorHandler'() {

        when: 'message to sent to invalid partition id'
            kafkaTemplate.send(cpsEventTopic, 5, myDataspaceName, cpsDataUpdatedEvent )
            kafkaTemplate.flush()

        then: 'error is thrown '
            thrown KafkaException
        and: 'error handler is called with exception details'
            1 * spyKafkaErrorHandler.onError(_, _, _ as Exception)
            1 * mockNotificationErrorHandler.onException(*_)
    }

    /* Kafka message listener is async.
     Due to it, it becomes tricky to know how much time it will take to get the messages
     Currently it waits for maximum 20 seconds.
     */
    def getEventFromKafka() {
        for (int i : 1..20) {
            if (consumedMessages.isEmpty()) {
                sleep(1000)
            } else {
                return consumedMessages.get(0)
            }
        }
    }
}
