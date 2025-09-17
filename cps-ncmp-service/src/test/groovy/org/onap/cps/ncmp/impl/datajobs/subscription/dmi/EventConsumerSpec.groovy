/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.datajobs.subscription.dmi

import org.springframework.test.context.ContextConfiguration

import static org.onap.cps.ncmp.impl.datajobs.subscription.models.CmSubscriptionStatus.ACCEPTED

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.onap.cps.ncmp.impl.datajobs.subscription.dmi_to_ncmp.DataJobSubscriptionDmiOutEvent
import org.onap.cps.ncmp.impl.datajobs.subscription.ncmp.CmSubscriptionHandler
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@ContextConfiguration(classes = [ObjectMapper, JsonObjectMapper])
class EventConsumerSpec extends Specification {

    def mockCmSubscriptionHandler = Mock(CmSubscriptionHandler)
    def objectUnderTest = new EventConsumer(mockCmSubscriptionHandler)
    def logger = new ListAppender<ILoggingEvent>()

    @Autowired
    JsonObjectMapper jsonObjectMapper

    @Autowired
    ObjectMapper objectMapper

    void setup() {
        ((Logger) LoggerFactory.getLogger(EventConsumer.class)).addAppender(logger)
        logger.start()
    }

    void cleanup() {
        ((Logger) LoggerFactory.getLogger(EventConsumer.class)).detachAndStopAllAppenders()
    }

    def 'Consume subscription CREATE response with status ACCEPTED from DMI Plugin'() {
        given: 'a response event from DMI'
            def jsonData = TestUtils.getResourceFileContent(
                'datajobs/subscription/cmNotificationSubscriptionDmiOutEvent.json')
            def testEventSent = jsonObjectMapper.convertJsonString(jsonData, DataJobSubscriptionDmiOutEvent.class)
            def testCloudEventSent = CloudEventBuilder.v1()
                .withData(objectMapper.writeValueAsBytes(testEventSent))
                .withId('random-uuid')
                .withType('subscriptionCreateResponse')
                .withSource(URI.create('myDmi'))
                .withExtension('correlationid', 'sub-1#myDmi').build()
            def consumerRecord = new ConsumerRecord<String, CloudEvent>('topic-name', 0, 0, 'event-key', testCloudEventSent)
        when: 'the event is consumed'
            objectUnderTest.consumeDmiOutEvent(consumerRecord)
        then: 'an event is logged with level INFO'
            def loggingEvent = getLoggingEvent()
            assert loggingEvent.level == Level.INFO
        and: 'the log indicates the task completed successfully'
            assert loggingEvent.formattedMessage == 'Consumed DMI subscription response event with details: | correlationId=sub-1#myDmi | eventType=subscriptionCreateResponse'
        and:  'the subscription handler is called to update status of subscription with correct details'
            1 * mockCmSubscriptionHandler.updateCmSubscriptionStatus('sub-1', 'myDmi', ACCEPTED)
    }

    def getLoggingEvent() {
        return logger.list[0]
    }
}
