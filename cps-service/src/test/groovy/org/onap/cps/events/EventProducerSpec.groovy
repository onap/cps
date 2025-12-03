/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.events

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.cloudevents.CloudEvent
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.header.Headers
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.util.SerializationUtils
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

class EventProducerSpec extends Specification {

    def mockLegacyKafkaTemplate = Mock(KafkaTemplate)
    def mockCloudEventKafkaTemplate = Mock(KafkaTemplate)
    def mockCloudEventKafkaTemplateForEos = Mock(KafkaTemplate)
    def logger = Spy(ListAppender<ILoggingEvent>)

    void setup() {
        def setupLogger = ((Logger) LoggerFactory.getLogger(EventProducer.class))
        setupLogger.setLevel(Level.DEBUG)
        setupLogger.addAppender(logger)
        logger.start()
    }

    void cleanup() {
        ((Logger) LoggerFactory.getLogger(EventProducer.class)).detachAndStopAllAppenders()
    }

    def objectUnderTest = new EventProducer(mockLegacyKafkaTemplate, mockCloudEventKafkaTemplate, mockCloudEventKafkaTemplateForEos)

    def 'Send Cloud Event'() {
        given: 'a successfully sent event'
            def eventFuture = CompletableFuture.completedFuture(
                new SendResult(
                    new ProducerRecord('some-topic', 'some-value'),
                    new RecordMetadata(new TopicPartition('some-topic', 0), 0, 0, 0, 0, 0)
                )
            )
            def someCloudEvent = Mock(CloudEvent)
            1 * mockCloudEventKafkaTemplate.send('some-topic', 'some-event-key', someCloudEvent) >> eventFuture
        when: 'sending the cloud event'
            objectUnderTest.sendCloudEvent('some-topic', 'some-event-key', someCloudEvent)
        then: 'the correct debug message is logged'
            assert verifyLoggingEvent(Level.DEBUG, 'Successfully sent event')
    }

    def 'Send Cloud Event Using EOS'() {
        given: 'a successfull result from send event with EOS semantics'
            def eventFuture = CompletableFuture.completedFuture(
                new SendResult(
                    new ProducerRecord('eos-topic', 'some-value'),
                    new RecordMetadata(new TopicPartition('eos-topic', 0), 0, 0, 0, 0, 0)
                )
            )
            def someCloudEvent = Mock(CloudEvent)
            1 * mockCloudEventKafkaTemplateForEos.send('eos-topic', 'some-event-key', someCloudEvent) >> eventFuture
        when: 'sending the cloud event using EOS'
            objectUnderTest.sendCloudEventUsingEos('eos-topic', 'some-event-key', someCloudEvent)
        then: 'the correct debug message is logged'
            assert verifyLoggingEvent(Level.DEBUG, 'Successfully sent event') == true
    }

    def 'Send Cloud Event with Exception'() {
        given: 'a failed event'
            def eventFutureWithFailure = new CompletableFuture<SendResult<String, String>>()
            eventFutureWithFailure.completeExceptionally(new RuntimeException('some exception'))
            def someCloudEvent = Mock(CloudEvent)
            1 * mockCloudEventKafkaTemplate.send('some-topic', 'some-event-key', someCloudEvent) >> eventFutureWithFailure
        when: 'sending the cloud event'
            objectUnderTest.sendCloudEvent('some-topic', 'some-event-key', someCloudEvent)
        then: 'the correct error message is logged'
            assert verifyLoggingEvent(Level.ERROR, 'Unable to send event') == true
    }

    def 'Send Cloud Event Using EOS with KafkaException'() {
        given: 'an event fails with KafkaException'
            def kafkaException = new org.springframework.kafka.KafkaException('some kafka exception')
            def eventFutureWithFailure = new CompletableFuture<SendResult<String, CloudEvent>>()
            eventFutureWithFailure.completeExceptionally(kafkaException)
            def someCloudEvent = Mock(CloudEvent)
            1 * mockCloudEventKafkaTemplateForEos.send('eos-topic', 'some-event-key', someCloudEvent) >> eventFutureWithFailure
        when: 'sending the cloud event using EOS'
            objectUnderTest.sendCloudEventUsingEos('eos-topic', 'some-event-key', someCloudEvent)
        then: 'the correct error message is logged'
            assert verifyLoggingEvent(Level.ERROR, 'Unable to send event') == true
    }

    def 'Send Legacy Event'() {
        given: 'a successfully sent event'
            def eventFuture = CompletableFuture.completedFuture(
                new SendResult(
                    new ProducerRecord('some-topic', 'some-value'),
                    new RecordMetadata(new TopicPartition('some-topic', 0), 0, 0, 0, 0, 0)
                )
            )
            def someLegacyEvent = Mock(LegacyEvent)
            1 * mockLegacyKafkaTemplate.send('some-topic', 'some-event-key', someLegacyEvent) >> eventFuture
        when: 'sending the cloud event'
            objectUnderTest.sendLegacyEvent('some-topic', 'some-event-key', someLegacyEvent)
        then: 'the correct debug message is logged'
            assert verifyLoggingEvent(Level.DEBUG, 'Successfully sent event') == true
    }

    def 'Send Legacy Event with Headers as Map'() {
        given: 'a successfully sent event'
            def sampleEventHeaders = ['k1': SerializationUtils.serialize('v1')]
            def eventFuture = CompletableFuture.completedFuture(
                new SendResult(
                    new ProducerRecord('some-topic', 'some-value'),
                    new RecordMetadata(new TopicPartition('some-topic', 0), 0, 0, 0, 0, 0)
                )
            )
            def someLegacyEvent = Mock(LegacyEvent)
        when: 'sending the legacy event'
            objectUnderTest.sendLegacyEvent('some-topic', 'some-event-key', sampleEventHeaders, someLegacyEvent)
        then: 'event is sent'
            1 * mockLegacyKafkaTemplate.send(_) >> eventFuture
        and: 'the correct debug message is logged'
            assert verifyLoggingEvent(Level.DEBUG, 'Successfully sent event') == true
    }

    def 'Handle Legacy Event Callback'() {
        given: 'an event is successfully sent'
            def eventFuture = CompletableFuture.completedFuture(
                new SendResult(
                    new ProducerRecord('some-topic', 'some-value'),
                    new RecordMetadata(new TopicPartition('some-topic', 0), 0, 0, 0, 0, 0)
                )
            )
        when: 'handling legacy event callback'
            objectUnderTest.handleLegacyEventCallback('some-topic', eventFuture)
        then: 'the correct debug message is logged'
            assert verifyLoggingEvent(Level.DEBUG, 'Successfully sent event') == true
    }

    def 'Handle Legacy Event Callback with Exception'() {
        given: 'a failure to send an event'
            def eventFutureWithFailure = new CompletableFuture<SendResult<String, String>>()
            eventFutureWithFailure.completeExceptionally(new RuntimeException('some exception'))
        when: 'handling legacy event callback'
            objectUnderTest.handleLegacyEventCallback('some-topic', eventFutureWithFailure)
        then: 'the correct error message is logged'
            assert verifyLoggingEvent(Level.ERROR, 'Unable to send event') == true
    }

    def 'Logging of non-kafka exceptions'() {
        given: 'a runtime exception that is not KafkaException'
            def sendResult = Mock(SendResult) {
                getProducerRecord() >> Mock(ProducerRecord)
            }
            def runtimeException = new RuntimeException('some runtime exception')
            def logOutcomeMethod = EventProducer.getDeclaredMethod('logOutcome', String, SendResult, Throwable, boolean)
            logOutcomeMethod.accessible = true
        when: 'logging the outcome with throwKafkaException set to true'
            logOutcomeMethod.invoke(null, 'some-topic', sendResult, runtimeException, true)
        then: 'error message is logged'
            assert verifyLoggingEvent(Level.ERROR, 'Unable to send event') == true
    }

    def 'Convert to kafka headers'() {
        given: 'Few key value pairs'
            def someKeyValue = ['key1': 'value1', 'key2': 'value2']
        when: 'we convert to headers'
            def headers = objectUnderTest.convertToKafkaHeaders(someKeyValue)
        then: 'it is correctly converted'
            assert headers instanceof Headers
        and: 'also has correct values'
            assert headers[0].key() == 'key1'
            assert headers[1].key() == 'key2'
    }

    def verifyLoggingEvent(expectedLevel, expectedFormattedMessage) {
        def lastLoggingEvent = logger.list[0]
        lastLoggingEvent.level == expectedLevel && lastLoggingEvent.formattedMessage.contains(expectedFormattedMessage)
    }

}
