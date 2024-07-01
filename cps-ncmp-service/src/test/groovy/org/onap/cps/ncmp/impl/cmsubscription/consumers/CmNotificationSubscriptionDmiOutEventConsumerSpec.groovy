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

package org.onap.cps.ncmp.impl.cmsubscription.consumers

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.events.cmnotificationsubscription_merge1_0_0.dmi_to_ncmp.CmNotificationSubscriptionDmiOutEvent
import org.onap.cps.ncmp.events.cmnotificationsubscription_merge1_0_0.dmi_to_ncmp.Data
import org.onap.cps.ncmp.impl.cmsubscription.CmNotificationSubscriptionEventsFacade
import org.onap.cps.ncmp.impl.cmsubscription.CmNotificationSubscriptionMappersFacade
import org.onap.cps.ncmp.impl.cmsubscription.cache.DmiCacheHandler
import org.onap.cps.ncmp.impl.cmsubscription.models.CmNotificationSubscriptionStatus
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper])
class CmNotificationSubscriptionDmiOutEventConsumerSpec extends MessagingBaseSpec {

    @Autowired
    JsonObjectMapper jsonObjectMapper

    @Autowired
    ObjectMapper objectMapper

    def mockDmiCmNotificationSubscriptionCacheHandler = Mock(DmiCacheHandler)
    def mockCmNotificationSubscriptionEventsHandler = Mock(CmNotificationSubscriptionEventsFacade)
    def mockCmNotificationSubscriptionMappersHandler = Mock(CmNotificationSubscriptionMappersFacade)

    def objectUnderTest = new CmNotificationSubscriptionDmiOutEventConsumer(mockDmiCmNotificationSubscriptionCacheHandler, mockCmNotificationSubscriptionEventsHandler, mockCmNotificationSubscriptionMappersHandler)
    def logger = Spy(ListAppender<ILoggingEvent>)

    void setup() {
        ((Logger) LoggerFactory.getLogger(CmNotificationSubscriptionDmiOutEventConsumer.class)).addAppender(logger)
        logger.start()
    }

    void cleanup() {
        ((Logger) LoggerFactory.getLogger(CmNotificationSubscriptionDmiOutEventConsumer.class)).detachAndStopAllAppenders()
    }


    def 'Consume valid CM Subscription response from DMI Plugin'() {
        given: 'a cmsubscription event'
            def jsonData = TestUtils.getResourceFileContent('cmSubscription/cmNotificationSubscriptionDmiOutEvent.json')
            def testEventSent = jsonObjectMapper.convertJsonString(jsonData, CmNotificationSubscriptionDmiOutEvent.class)
            def testCloudEventSent = CloudEventBuilder.v1()
                .withData(objectMapper.writeValueAsBytes(testEventSent))
                .withId('random-uuid')
                .withType('subscriptionCreateResponse')
                .withSource(URI.create('test-dmi-plugin-name'))
                .withExtension('correlationid', 'sub-1#test-dmi-plugin-name').build()
            def consumerRecord = new ConsumerRecord<String, CloudEvent>('topic-name', 0, 0, 'event-key', testCloudEventSent)
        when: 'the valid event is consumed'
            objectUnderTest.consumeCmNotificationSubscriptionDmiOutEvent(consumerRecord)
        then: 'an event is logged with level INFO'
            def loggingEvent = getLoggingEvent()
            assert loggingEvent.level == Level.INFO
        and: 'the log indicates the task completed successfully'
            assert loggingEvent.formattedMessage == 'Cm Subscription with id : sub-1 handled by the dmi-plugin : test-dmi-plugin-name has the status : accepted'
    }

    def 'Consume a valid CM Notification Subscription Event and perform correct actions base on status'() {
        given: 'a cmNotificationSubscription event'
            def dmiOutEventData = new Data(statusCode: statusCode, statusMessage: subscriptionStatus.toString())
            def cmNotificationSubscriptionDmiOutEvent = new CmNotificationSubscriptionDmiOutEvent().withData(dmiOutEventData)
            def testCloudEventSent = CloudEventBuilder.v1()
                .withData(objectMapper.writeValueAsBytes(cmNotificationSubscriptionDmiOutEvent))
                .withId('random-uuid')
                .withType('subscriptionCreateResponse')
                .withSource(URI.create('test-dmi-plugin-name'))
                .withExtension('correlationid', 'sub-1#test-dmi-plugin-name').build()
            def consumerRecord = new ConsumerRecord<String, CloudEvent>('topic-name', 0, 0, 'event-key', testCloudEventSent)
        when: 'the event is consumed'
            objectUnderTest.consumeCmNotificationSubscriptionDmiOutEvent(consumerRecord)
        then: 'correct number of calls to cache'
            expectedCacheCalls * mockDmiCmNotificationSubscriptionCacheHandler.updateDmiCmNotificationSubscriptionStatusPerDmi('sub-1','test-dmi-plugin-name', subscriptionStatus)
        and: 'correct number of calls to persist cache'
            expectedPersistenceCalls * mockDmiCmNotificationSubscriptionCacheHandler.persistIntoDatabasePerDmi('sub-1','test-dmi-plugin-name')
        and: 'correct number of calls to map the ncmp out event'
            1 * mockCmNotificationSubscriptionMappersHandler.toCmNotificationSubscriptionNcmpOutEvent('sub-1', _)
        and: 'correct number of calls to publish the ncmp out event to client'
            1 * mockCmNotificationSubscriptionEventsHandler.publishCmNotificationSubscriptionNcmpOutEvent('sub-1', 'subscriptionCreateResponse', _, false)
        where: 'the following parameters are used'
            scenario          | subscriptionStatus                        | statusCode || expectedCacheCalls | expectedPersistenceCalls
            'Accepted Status' | CmNotificationSubscriptionStatus.ACCEPTED | '1'        || 1                  | 1
            'Rejected Status' | CmNotificationSubscriptionStatus.REJECTED | '104'      || 1                  | 0
    }

    def getLoggingEvent() {
        return logger.list[0]
    }

}
