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

package org.onap.cps.ncmp.impl.cmnotificationsubscription.dmi

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.impl.cmnotificationsubscription.EventsFacade
import org.onap.cps.ncmp.impl.cmnotificationsubscription.MappersFacade
import org.onap.cps.ncmp.impl.cmnotificationsubscription.cache.DmiCacheHandler
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.CmSubscriptionStatus
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.dmi_to_ncmp.Data
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.dmi_to_ncmp.DmiOutEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper])
class DmiOutEventConsumerSpec extends MessagingBaseSpec {

    @Autowired
    JsonObjectMapper jsonObjectMapper

    @Autowired
    ObjectMapper objectMapper

    def mockDmiCmNotificationSubscriptionCacheHandler = Mock(DmiCacheHandler)
    def mockEventsHandler = Mock(EventsFacade)
    def mockMappersHandler = Mock(MappersFacade)

    def objectUnderTest = new DmiOutEventConsumer(mockDmiCmNotificationSubscriptionCacheHandler, mockEventsHandler, mockMappersHandler)
    def logger = Spy(ListAppender<ILoggingEvent>)

    void setup() {
        ((Logger) LoggerFactory.getLogger(DmiOutEventConsumer.class)).addAppender(logger)
        logger.start()
    }

    void cleanup() {
        ((Logger) LoggerFactory.getLogger(DmiOutEventConsumer.class)).detachAndStopAllAppenders()
    }


    def 'Consume valid CM Subscription response from DMI Plugin'() {
        given: 'a cmsubscription event'
            def jsonData = TestUtils.getResourceFileContent('cmSubscription/cmNotificationSubscriptionDmiOutEvent.json')
            def testEventSent = jsonObjectMapper.convertJsonString(jsonData, DmiOutEvent.class)
            def testCloudEventSent = CloudEventBuilder.v1()
                .withData(objectMapper.writeValueAsBytes(testEventSent))
                .withId('random-uuid')
                .withType('subscriptionCreateResponse')
                .withSource(URI.create('test-dmi-plugin-name'))
                .withExtension('correlationid', 'sub-1#test-dmi-plugin-name').build()
            def consumerRecord = new ConsumerRecord<String, CloudEvent>('topic-name', 0, 0, 'event-key', testCloudEventSent)
        when: 'the valid event is consumed'
            objectUnderTest.consumeDmiOutEvent(consumerRecord)
        then: 'an event is logged with level INFO'
            def loggingEvent = getLoggingEvent()
            assert loggingEvent.level == Level.INFO
        and: 'the log indicates the task completed successfully'
            assert loggingEvent.formattedMessage == 'Cm Subscription with id : sub-1 handled by the dmi-plugin : test-dmi-plugin-name has the status : accepted'
    }

    def 'Consume a valid CM Notification Subscription Event and perform correct actions base on status'() {
        given: 'a cmNotificationSubscription event'
            def dmiOutEventData = new Data(statusCode: statusCode, statusMessage: subscriptionStatus.toString())
            def dmiOutEvent = new DmiOutEvent().withData(dmiOutEventData)
            def testCloudEventSent = CloudEventBuilder.v1()
                .withData(objectMapper.writeValueAsBytes(dmiOutEvent))
                .withId('random-uuid')
                .withType('subscriptionCreateResponse')
                .withSource(URI.create('test-dmi-plugin-name'))
                .withExtension('correlationid', 'sub-1#test-dmi-plugin-name').build()
            def consumerRecord = new ConsumerRecord<String, CloudEvent>('topic-name', 0, 0, 'event-key', testCloudEventSent)
        when: 'the event is consumed'
            objectUnderTest.consumeDmiOutEvent(consumerRecord)
        then: 'correct number of calls to cache'
            expectedCacheCalls * mockDmiCmNotificationSubscriptionCacheHandler.updateDmiCmNotificationSubscriptionStatusPerDmi('sub-1','test-dmi-plugin-name', subscriptionStatus)
        and: 'correct number of calls to persist cache'
            expectedPersistenceCalls * mockDmiCmNotificationSubscriptionCacheHandler.persistIntoDatabasePerDmi('sub-1','test-dmi-plugin-name')
        and: 'correct number of calls to map the ncmp out event'
            1 * mockMappersHandler.toNcmpOutEvent('sub-1', _)
        and: 'correct number of calls to publish the ncmp out event to client'
            1 * mockEventsHandler.publishNcmpOutEvent('sub-1', 'subscriptionCreateResponse', _, false)
        where: 'the following parameters are used'
            scenario          | subscriptionStatus                        | statusCode || expectedCacheCalls | expectedPersistenceCalls
            'Accepted Status' | CmSubscriptionStatus.ACCEPTED | '1'   || 1 | 1
            'Rejected Status' | CmSubscriptionStatus.REJECTED | '104' || 1 | 0
    }

    def getLoggingEvent() {
        return logger.list[0]
    }

}
