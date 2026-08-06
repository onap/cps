/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2026 OpenInfra Foundation Europe. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.config

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.cloudevents.CloudEvent
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.onap.cps.events.EventBatchSendException
import org.slf4j.LoggerFactory
import org.spockframework.spring.EnableSharedInjection
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.transaction.KafkaTransactionManager
import org.springframework.test.context.TestPropertySource
import spock.lang.Shared
import spock.lang.Specification

@SpringBootTest(classes = [KafkaProperties, ExactlyOnceSemanticsKafkaConfig])
@EnableSharedInjection
@EnableConfigurationProperties
@TestPropertySource(properties = [
        "ncmp.kafka.eos.enabled=true",
        "ncmp.notifications.avc-event-producer.transaction-id-prefix=tx-myPrefix-",
        "ncmp.notifications.avc-event-consumer.concurrency=2",
        "ncmp.notifications.avc-event-consumer.max-poll-records=500"
])
class ExactlyOnceSemanticsKafkaConfigSpec extends Specification {

    @Shared
    @Autowired
    ConsumerFactory<String, CloudEvent> cloudEventConsumerFactoryForExactlyOnceSemantics

    @Shared
    @Autowired
    ProducerFactory<String, CloudEvent> cloudEventProducerFactoryForExactlyOnceSemantics

    @Shared
    @Autowired
    ConcurrentKafkaListenerContainerFactory<String, CloudEvent> cloudEventConcurrentKafkaListenerContainerFactoryForExactlyOnceSemantics

    def logger = new ListAppender<ILoggingEvent>()

    def setup() {
        def setupLogger = (Logger) LoggerFactory.getLogger(ExactlyOnceSemanticsKafkaConfig)
        setupLogger.setLevel(Level.DEBUG)
        setupLogger.addAppender(logger)
        logger.start()
    }

    def cleanup() {
        ((Logger) LoggerFactory.getLogger(ExactlyOnceSemanticsKafkaConfig)).detachAndStopAllAppenders()
    }

    def 'Exactly once semantics kafka configuration is as expected.'() {
        expect: 'consumer has read_committed isolation level'
            cloudEventConsumerFactoryForExactlyOnceSemantics.configurationProperties[ConsumerConfig.ISOLATION_LEVEL_CONFIG] == 'read_committed'
        and: 'consumer has auto commit disabled'
            cloudEventConsumerFactoryForExactlyOnceSemantics.configurationProperties[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] == false
        and: 'consumer has max poll records configured'
            cloudEventConsumerFactoryForExactlyOnceSemantics.configurationProperties[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] == '500'
        and: 'listener uses BATCH ack mode'
            cloudEventConcurrentKafkaListenerContainerFactoryForExactlyOnceSemantics.containerProperties.ackMode == ContainerProperties.AckMode.BATCH
        and: 'listener is batch listener'
            cloudEventConcurrentKafkaListenerContainerFactoryForExactlyOnceSemantics.isBatchListener() == true
        and: 'concurrency is configured'
            cloudEventConcurrentKafkaListenerContainerFactoryForExactlyOnceSemantics.concurrency == 2
        and: 'producer transaction ID prefix is as expected'
            cloudEventProducerFactoryForExactlyOnceSemantics.transactionIdPrefix.startsWith('cps-tx-myPrefix-')
        and: 'KafkaTransactionManager is used instead of primary transaction manager'
            cloudEventConcurrentKafkaListenerContainerFactoryForExactlyOnceSemantics.containerProperties.kafkaAwareTransactionManager instanceof KafkaTransactionManager
    }

    def 'Consuming a record with non retryable exception'() {
        given: 'a consumer record and a non-retryable exception'
            def consumerRecord = new ConsumerRecord('my-topic', 123, 456L, 'key', null)
            def nonRetryableException = new RuntimeException('some non-retryable error')
        when: 'the recoverer is invoked for the discarded record'
            def failureTracker = getErrorHandlerField('failureTracker')
            def recovererField = failureTracker.getClass().getDeclaredField('recoverer')
            recovererField.accessible = true
            recovererField.get(failureTracker).accept(consumerRecord, nonRetryableException)
        then: 'an error log message contains the topic, partition, offset and exception message'
            def errorLog = logger.list.find { it.level == Level.ERROR }
            assert errorLog.formattedMessage.contains('my-topic')
            assert errorLog.formattedMessage.contains('123')
            assert errorLog.formattedMessage.contains('456')
            assert errorLog.formattedMessage.contains('some non-retryable error')
        and: 'a debug log message contains the full stack trace'
            def debugLog = logger.list.find { it.level == Level.DEBUG }
            assert debugLog.formattedMessage.contains('my-topic')
            assert debugLog.throwableProxy.className == RuntimeException.name
            assert debugLog.throwableProxy.message == 'some non-retryable error'
            assert debugLog.throwableProxy.stackTraceElementProxyArray.length > 0
    }

    def getErrorHandlerField(def fieldName) {
        def containerFactoryClass = cloudEventConcurrentKafkaListenerContainerFactoryForExactlyOnceSemantics.getClass()
        def handlerField = containerFactoryClass.getSuperclass().getDeclaredField('commonErrorHandler')
        handlerField.accessible = true
        def errorHandler = handlerField.get(cloudEventConcurrentKafkaListenerContainerFactoryForExactlyOnceSemantics)
        def classInHierarchy = errorHandler.getClass()
        while (classInHierarchy != null) {
            try {
                def field = classInHierarchy.getDeclaredField(fieldName)
                field.accessible = true
                return field.get(errorHandler)
            } catch (NoSuchFieldException ignored) {
                classInHierarchy = classInHierarchy.getSuperclass()
            }
        }
    }
}
