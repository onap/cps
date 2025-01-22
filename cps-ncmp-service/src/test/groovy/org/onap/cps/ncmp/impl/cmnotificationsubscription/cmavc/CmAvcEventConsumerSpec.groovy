/*
 *  ============LICENSE_START=======================================================
 *  Copyright (c) 2023-2024 Nordix Foundation.
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

package org.onap.cps.ncmp.impl.cmnotificationsubscription.cmavc

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import io.cloudevents.kafka.CloudEventDeserializer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.onap.cps.ncmp.events.avc1_0_0.AvcEvent
import org.onap.cps.ncmp.impl.cmnotificationsubscription.ncmp.NcmpInEventConsumer
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.ncmp.utils.events.CmAvcEventPublisher
import org.onap.cps.ncmp.utils.events.MessagingBaseSpec
import org.onap.cps.utils.JsonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

import java.time.Duration

import static org.onap.cps.ncmp.utils.events.CloudEventMapper.toTargetEvent

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper])
class CmAvcEventConsumerSpec extends MessagingBaseSpec {

    def mockCmAvcEventPublisher = Mock(CmAvcEventPublisher)
    def objectUnderTest = new CmAvcEventConsumer(mockCmAvcEventPublisher)
    def logger = Spy(ListAppender<ILoggingEvent>)

    @Autowired
    JsonObjectMapper jsonObjectMapper

    @Autowired
    ObjectMapper objectMapper

    void setup() {

        ((Logger) LoggerFactory.getLogger(CmAvcEventConsumer.class)).addAppender(logger)
        logger.start()
    }

    void cleanup() {
        ((Logger) LoggerFactory.getLogger(CmAvcEventConsumer.class)).detachAndStopAllAppenders()
    }

    def 'Consume and forward valid message'() {
        given: 'avc events destination topic is set'
            objectUnderTest.cmEventsTopicName = 'cm-events'
        and: 'an avc event'
            def jsonData = TestUtils.getResourceFileContent('sampleAvcInputEvent.json')
            def testEventKey = 'sample-eventid-key'
            def testEventSent = jsonObjectMapper.convertJsonString(jsonData, AvcEvent.class)
            def testCloudEventSent = CloudEventBuilder.v1()
                .withData(jsonObjectMapper.asJsonBytes(testEventSent))
                .withId('sample-eventid')
                .withType('sample-test-type')
                .withSource(URI.create('sample-test-source'))
                .withExtension('correlationid', 'test-cmhandle1').build()
        and: 'avc event has header information'
            def consumerRecord = new ConsumerRecord<String, CloudEvent>('someTopic', 0, 0, testEventKey, testCloudEventSent)
        when: 'the event is consumed and forwarded to target topic'
            objectUnderTest.consumeAndForward(consumerRecord)
        then: 'the avc events publisher service is called once with the correct details '
            1 * mockCmAvcEventPublisher.publishAvcEvent('cm-events', testEventKey, testCloudEventSent)
    }
}
