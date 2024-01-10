/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.events

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.cloudevents.CloudEvent
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

class EventsPublisherSpec extends Specification {

    def legacyKafkaTemplateStub = Stub(KafkaTemplate)
    def mockCloudEventKafkaTemplate = Mock(KafkaTemplate)
    def logger = Spy(ListAppender<ILoggingEvent>)

    @BeforeEach
    void setup() {
        def setupLogger = ((Logger) LoggerFactory.getLogger(EventsPublisher.class))
        setupLogger.setLevel(Level.DEBUG)
        setupLogger.addAppender(logger)
        logger.start()
    }

    @AfterEach
    void teardown() {
        ((Logger) LoggerFactory.getLogger(EventsPublisher.class)).detachAndStopAllAppenders()
    }

    def objectUnderTest = new EventsPublisher(legacyKafkaTemplateStub, mockCloudEventKafkaTemplate)

    def 'Publish Cloud Event'() {
        given: 'a successfully published event'
            def eventFuture = CompletableFuture.completedFuture(
                new SendResult(
                    new ProducerRecord('some-topic', 'some-value'),
                    new RecordMetadata(new TopicPartition('some-topic', 0), 0, 0, 0, 0, 0)
                )
            )
            def someCloudEvent = Mock(CloudEvent)
            1 * mockCloudEventKafkaTemplate.send('some-topic', 'some-event-key', someCloudEvent) >> eventFuture
        when: 'publishing the cloud event'
            objectUnderTest.publishCloudEvent('some-topic', 'some-event-key', someCloudEvent)
        then: 'the correct debug message is logged'
            def lastLoggingEvent = logger.list[0]
            assert lastLoggingEvent.level == Level.DEBUG
            assert lastLoggingEvent.formattedMessage.contains('Successfully published event')
    }

    def 'Publish Cloud Event with Exception'() {
        given: 'a failed event'
            def eventFuture = new CompletableFuture<SendResult<String, String>>()
            eventFuture.completeExceptionally(new RuntimeException('some exception'))
            def someCloudEvent = Mock(CloudEvent)
            1 * mockCloudEventKafkaTemplate.send('some-topic', 'some-event-key', someCloudEvent) >> eventFuture
        when: 'publishing the cloud event'
            objectUnderTest.publishCloudEvent('some-topic', 'some-event-key', someCloudEvent)
        then: 'the correct error message is logged'
            def lastLoggingEvent = logger.list[0]
            assert lastLoggingEvent.level == Level.ERROR
            assert lastLoggingEvent.formattedMessage.contains('Unable to publish event')
    }

    def 'Handle Legacy Event Callback'() {
        given: 'an event is successfully published'
            def eventFuture = CompletableFuture.completedFuture(
                new SendResult(
                    new ProducerRecord('some-topic', 'some-value'),
                    new RecordMetadata(new TopicPartition('some-topic', 0), 0, 0, 0, 0, 0)
                )
            )
        when: 'handling legacy event callback'
            objectUnderTest.handleLegacyEventCallback('some-topic', eventFuture)
        then: 'the correct debug message is logged'
            def lastLoggingEvent = logger.list[0]
            assert lastLoggingEvent.level == Level.DEBUG
            assert lastLoggingEvent.formattedMessage.contains('Successfully published event')
    }

    def 'Handle Legacy Event Callback with Exception'() {
        given: 'a failure to publish an event'
            def eventFuture = new CompletableFuture<SendResult<String, String>>()
            eventFuture.completeExceptionally(new RuntimeException('some exception'))
        when: 'handling legacy event callback'
            objectUnderTest.handleLegacyEventCallback('some-topic', eventFuture)
        then: 'the correct error message is logged'
            def lastLoggingEvent = logger.list[0]
            assert lastLoggingEvent.level == Level.ERROR
            assert lastLoggingEvent.formattedMessage.contains('Unable to publish event')
    }

}