/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025-2026 OpenInfra Foundation Europe. All rights reserved.
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
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

class EventProducerSpec extends Specification {

    def mockLegacyKafkaTemplate = Mock(KafkaTemplate)
    def mockCloudEventKafkaTemplate = Mock(KafkaTemplate)
    def mockCloudEventKafkaTemplateForExactlyOnceSemantics = Mock(KafkaTemplate)
    def mockCloudEvent = Mock(CloudEvent)
    def logger = Spy(ListAppender<ILoggingEvent>)

    void cleanup() {
        ((Logger) LoggerFactory.getLogger(EventProducer.class)).detachAndStopAllAppenders()
    }

    def objectUnderTest = new EventProducer(mockLegacyKafkaTemplate, mockCloudEventKafkaTemplate)

    void setup() {
        def setupLogger = ((Logger) LoggerFactory.getLogger(EventProducer.class))
        setupLogger.setLevel(Level.DEBUG)
        setupLogger.addAppender(logger)
        logger.start()
        objectUnderTest.setCloudEventKafkaTemplateForExactlyOnceSemantics(mockCloudEventKafkaTemplateForExactlyOnceSemantics)
    }

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
            assert verifyLastLogEntry(Level.DEBUG, 'Successfully sent event')
    }

    def 'Send Cloud Event with Exception'() {
        given: 'a failed event'
            def eventFutureWithFailure = createFailedSendResult('some exception')
            def someCloudEvent = Mock(CloudEvent)
            1 * mockCloudEventKafkaTemplate.send('some-topic', 'some-event-key', someCloudEvent) >> eventFutureWithFailure
        when: 'sending the cloud event'
            objectUnderTest.sendCloudEvent('some-topic', 'some-event-key', someCloudEvent)
        then: 'the correct error message is logged'
            assert verifyLastLogEntry(Level.ERROR, 'Unable to send event')
    }

    def 'Send Legacy Event'() {
        given: 'a successfully sent event'
            def eventFuture = createSuccessfulSendResult('some-topic', null, 'some-value')
            def someLegacyEvent = Mock(LegacyEvent)
            1 * mockLegacyKafkaTemplate.send('some-topic', 'some-event-key', someLegacyEvent) >> eventFuture
        when: 'sending the cloud event'
            objectUnderTest.sendLegacyEvent('some-topic', 'some-event-key', someLegacyEvent)
        then: 'the correct debug message is logged'
            assert verifyLastLogEntry(Level.DEBUG, 'Successfully sent event')
    }

    def 'Send Legacy Event with Headers as Map'() {
        given: 'a successfully sent event'
            def sampleEventHeaders = ['k1': 'v1', 'k2': 'v2']
            def eventFuture = createSuccessfulSendResult('some-topic', null, 'some-value')
            def someLegacyEvent = Mock(LegacyEvent)
        when: 'sending the legacy event'
            objectUnderTest.sendLegacyEvent('some-topic', 'some-event-key', sampleEventHeaders, someLegacyEvent)
        then: 'event is sent'
            1 * mockLegacyKafkaTemplate.send(_) >> eventFuture
        and: 'the correct debug message is logged'
            assert verifyLastLogEntry(Level.DEBUG, 'Successfully sent event')
    }

    def 'Handle Legacy Event Callback'() {
        given: 'an event is successfully sent'
            def eventFuture = createSuccessfulSendResult('some-topic', null, 'some-value')
        when: 'handling legacy event callback'
            objectUnderTest.handleLegacyEventCallback('some-topic', eventFuture)
        then: 'the correct debug message is logged'
            assert verifyLastLogEntry(Level.DEBUG, 'Successfully sent event')
    }

    def 'Handle Legacy Event Callback with Exception'() {
        given: 'a failure to send an event'
            def eventFutureWithFailure = createFailedSendResult('some exception')
        when: 'handling legacy event callback'
            objectUnderTest.handleLegacyEventCallback('some-topic', eventFutureWithFailure)
        then: 'the correct error message is logged'
            assert verifyLastLogEntry(Level.ERROR, 'Unable to send event')
    }

    def 'Logging of non-kafka exceptions'() {
        given: 'a runtime exception that is not KafkaException'
            def sendResult = Mock(SendResult) {
                getProducerRecord() >> Mock(ProducerRecord)
            }
            def runtimeException = new RuntimeException('some runtime exception')
            def logOutcomeMethod = EventProducer.getDeclaredMethod('logOutcome', String, SendResult, Throwable)
            logOutcomeMethod.accessible = true
        when: 'logging the outcome with throwKafkaException set to true'
            logOutcomeMethod.invoke(null, 'some-topic', sendResult, runtimeException)
        then: 'error message is logged'
            assert verifyLastLogEntry(Level.ERROR, 'Unable to send event')
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

    def 'Send Cloud Event Batch when ExactlyOnceSemantics template is not configured'() {
        given: 'an EventProducer without ExactlyOnceSemantics template'
            def objectUnderTestWithoutExactlyOnceSemantics = new EventProducer(mockLegacyKafkaTemplate, mockCloudEventKafkaTemplate)
        and: 'a batch of events'
            def events = [key1:  mockCloudEvent].collect { new MapEntry(it.key, it.value) }
        when: 'sending the batch'
            objectUnderTestWithoutExactlyOnceSemantics.sendCloudEventBatch('some-topic', events)
        then: 'an IllegalStateException is thrown'
            def exception = thrown(IllegalStateException)
            exception.message.contains('ExactlyOnceSemantics Kafka template is not configured')
    }

    def 'Send Cloud Event Batch successfully'() {
        given: 'a batch of cloud events'
            def events = [key1: mockCloudEvent, key2: mockCloudEvent].collect { new MapEntry(it.key, it.value) }
        and: 'successful futures for each event'
            def eventFuture1 = createSuccessfulSendResult('some-topic', 'key1', mockCloudEvent)
            def eventFuture2 = createSuccessfulSendResult('some-topic', 'key2', mockCloudEvent)
            1 * mockCloudEventKafkaTemplateForExactlyOnceSemantics.send('some-topic', 'key1', mockCloudEvent) >> eventFuture1
            1 * mockCloudEventKafkaTemplateForExactlyOnceSemantics.send('some-topic', 'key2', mockCloudEvent) >> eventFuture2
        when: 'sending the batch'
            objectUnderTest.sendCloudEventBatch('some-topic', events)
        then: 'the correct debug message is logged'
            assert verifyAnyLogEntry(Level.DEBUG, 'Successfully sent batch')
    }

    def 'Send Cloud Event Batch with failure'() {
        given: 'a batch of cloud events'
            def events = [key1: mockCloudEvent, key2: mockCloudEvent].collect { new MapEntry(it.key, it.value) }
        and: 'one successful and one failed future'
            def eventFuture1 = createSuccessfulSendResult('some-topic', 'key1', mockCloudEvent)
            def eventFuture2 = createFailedSendResult()
            1 * mockCloudEventKafkaTemplateForExactlyOnceSemantics.send('some-topic', 'key1', mockCloudEvent) >> eventFuture1
            1 * mockCloudEventKafkaTemplateForExactlyOnceSemantics.send('some-topic', 'key2', mockCloudEvent) >> eventFuture2
        when: 'sending the batch'
            objectUnderTest.sendCloudEventBatch('some-topic', events)
        then: 'an EventBatchSendException is thrown'
            thrown(EventBatchSendException)
        and: 'the correct error message is logged'
            assert verifyAnyLogEntry(Level.ERROR, 'Batch send failed')
    }

    def 'Send Cloud Event Batch with empty list'() {
        given: 'an empty list of events'
            def events = []
        when: 'sending the batch'
            objectUnderTest.sendCloudEventBatch('some-topic', events)
        then: 'no kafka template calls are made'
            0 * mockCloudEventKafkaTemplateForExactlyOnceSemantics._
        and: 'the correct debug message is logged'
            assert verifyAnyLogEntry(Level.DEBUG, 'No events to send')
    }

    def verifyAnyLogEntry(expectedLevel, expectedFormattedMessage) {
        logger.list.any { it.level == expectedLevel && it.formattedMessage.contains(expectedFormattedMessage) }
    }

    def verifyLastLogEntry(expectedLevel, expectedFormattedMessage) {
        def lastLoggingEvent = logger.list[0]
        lastLoggingEvent.level == expectedLevel && lastLoggingEvent.formattedMessage.contains(expectedFormattedMessage)
    }

    def createSuccessfulSendResult(topic, key = null, value = null) {
        CompletableFuture.completedFuture(
            new SendResult(
                new ProducerRecord(topic, key, value),
                new RecordMetadata(new TopicPartition(topic, 0), 0, 0, 0, 0, 0)
            )
        )
    }

    def createFailedSendResult(exceptionMessage = 'send failed') {
        def future = new CompletableFuture<SendResult<String, CloudEvent>>()
        future.completeExceptionally(new RuntimeException(exceptionMessage))
        future
    }

}