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

package org.onap.cps.ncmp.api.impl.events.cmsubscription

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.events.cmnotificationsubscription_merge1_0_0.client_to_ncmp.CmNotificationSubscriptionNcmpInEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper])
class CmNotificationSubscriptionNcmpInEventConsumerSpec extends MessagingBaseSpec {

    def mockDmiCmNotificationSubscriptionCacheHandler = Mock(DmiCmNotificationSubscriptionCacheHandler)
    def objectUnderTest = new CmNotificationSubscriptionNcmpInEventConsumer(mockDmiCmNotificationSubscriptionCacheHandler)
    def logger = Spy(ListAppender<ILoggingEvent>)

    @Autowired
    JsonObjectMapper jsonObjectMapper

    @Autowired
    ObjectMapper objectMapper

    void setup() {
        ((Logger) LoggerFactory.getLogger(CmNotificationSubscriptionNcmpInEventConsumer.class)).addAppender(logger)
        logger.start()
    }

    void cleanup() {
        ((Logger) LoggerFactory.getLogger(CmNotificationSubscriptionNcmpInEventConsumer.class)).detachAndStopAllAppenders()
    }


    def 'Consume valid CMSubscription create message'() {
        given: 'a cmsubscription event'
            def jsonData = TestUtils.getResourceFileContent('cmSubscription/cmNotificationSubscriptionNcmpInEvent.json')
            def testEventSent = jsonObjectMapper.convertJsonString(jsonData, CmNotificationSubscriptionNcmpInEvent.class)
            def testCloudEventSent = CloudEventBuilder.v1()
                .withData(objectMapper.writeValueAsBytes(testEventSent))
                .withId('subscriptionCreated')
                .withType('subscriptionCreated')
                .withSource(URI.create('some-resource'))
                .withExtension('correlationid', 'test-cmhandle1').build()
            def consumerRecord = new ConsumerRecord<String, CloudEvent>('topic-name', 0, 0, 'event-key', testCloudEventSent)
        and: 'notifications are enabled'
            objectUnderTest.notificationFeatureEnabled = true
        when: 'the valid event is consumed'
            objectUnderTest.consumeSubscriptionEvent(consumerRecord)
        then: 'an event is logged with level INFO'
            def loggingEvent = getLoggingEvent()
            assert loggingEvent.level == Level.INFO
        and: 'the log indicates the task completed successfully'
            assert loggingEvent.formattedMessage == 'Subscription with name cm-subscription-001 to be mapped to hazelcast object...'
        and: 'the cache handler method is called once'
            1 * mockDmiCmNotificationSubscriptionCacheHandler.add('cm-subscription-001',_)
    }

    def getLoggingEvent() {
        return logger.list[0]
    }

}
