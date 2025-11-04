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

import static org.onap.cps.ncmp.impl.datajobs.subscription.models.CmSubscriptionStatus.ACCEPTED
import static org.onap.cps.ncmp.impl.datajobs.subscription.models.CmSubscriptionStatus.REJECTED

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
import org.onap.cps.ncmp.impl.datajobs.subscription.utils.CmDataJobSubscriptionPersistenceService
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.slf4j.LoggerFactory
import spock.lang.Specification

class EventConsumerSpec extends Specification {

    def logger = new ListAppender<ILoggingEvent>()
    def objectMapper = new ObjectMapper()
    def jsonObjectMapper = new JsonObjectMapper(objectMapper)

    def mockCmSubscriptionHandler = Mock(CmSubscriptionHandler)
    def mockCmDataJobSubscriptionPersistenceService = Mock(CmDataJobSubscriptionPersistenceService)
    def objectUnderTest = new EventConsumer(mockCmSubscriptionHandler, mockCmDataJobSubscriptionPersistenceService)

    void setup() {
        ((Logger) LoggerFactory.getLogger(EventConsumer.class)).addAppender(logger)
        logger.start()
    }

    void cleanup() {
        ((Logger) LoggerFactory.getLogger(EventConsumer.class)).detachAndStopAllAppenders()
    }

    def 'Consume subscription CREATE response with status ACCEPTED from DMI Plugin'() {
        given: 'a response event from DMI'
            def responseEvent = getResponseEventFromDmi('sub-1#myDmi', 'myDmi', 'ACCEPTED')
            def consumerRecord = new ConsumerRecord<String, CloudEvent>('topic-name', 0, 0, 'event-key', responseEvent)
        when: 'the event is consumed'
            objectUnderTest.consumeDmiOutEvent(consumerRecord)
        then: 'an event is logged with level INFO'
            def loggingEvent = logger.list.first()
            assert loggingEvent.level == Level.INFO
        and: 'the log indicates the task completed successfully'
            assert loggingEvent.formattedMessage == 'Consumed DMI subscription response event with details: | correlationId=sub-1#myDmi | eventType=subscriptionCreateResponse'
        and:  'the subscription handler is called to update status of subscription with correct details'
            1 * mockCmSubscriptionHandler.updateCmSubscriptionStatus('sub-1', 'myDmi', ACCEPTED)
    }

    def 'Consume subscription CREATE response with status REJECTED from DMI Plugin'() {
        given: 'a response event from DMI'
            def responseEvent = getResponseEventFromDmi('sub-1#myDmi', 'myDmi', 'REJECTED')
            def consumerRecord = new ConsumerRecord<String, CloudEvent>('topic-name', 0, 0, 'event-key', responseEvent)
        and: 'persistence service returns some data node selectors'
            mockCmDataJobSubscriptionPersistenceService.getDataNodeSelectors('sub-1') >> ["/parent=1", "/parent/child=1"]
        when: 'the event is consumed'
            objectUnderTest.consumeDmiOutEvent(consumerRecord)
        then:  'the subscription handler is called to update status of subscription with correct details'
            1 * mockCmSubscriptionHandler.updateCmSubscriptionStatus('sub-1', 'myDmi', REJECTED)
        and: 'an event is logged with level INFO'
            def loggingEvent = logger.list.last()
            assert loggingEvent.level == Level.INFO
        and: 'the log shows details of rejected create request'
            assert loggingEvent.formattedMessage == '' +
                'DataJob CREATE request with the following details was rejected by DMI plugin myDmi: dataJobId=sub-1 | ' +
                'dataNodeSelector=/parent=1\n/parent/child=1'
    }

    def getResponseEventFromDmi(correlationId, dmiPluginName, status) {
        def jsonData = TestUtils.getResourceFileContent(
            'datajobs/subscription/cmNotificationSubscriptionDmiOutEvent.json')
        jsonData = jsonData.replace('#statusMessage', status)
        def testEventSent = jsonObjectMapper.convertJsonString(jsonData, DataJobSubscriptionDmiOutEvent.class)
        def testCloudEventSent = CloudEventBuilder.v1()
            .withData(objectMapper.writeValueAsBytes(testEventSent))
            .withId('random-uuid')
            .withType('subscriptionCreateResponse')
            .withSource(URI.create(dmiPluginName))
            .withExtension('correlationid', correlationId).build()
        return testCloudEventSent
    }
}
