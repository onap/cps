/*
 * ============LICENSE_START========================================================
 * Copyright (c) 2023 Nordix Foundation.
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

package org.onap.cps.ncmp.api.impl.events

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.core.read.ListAppender
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.TopicPartition
import org.onap.cps.ncmp.init.SubscriptionModelLoader
import org.slf4j.LoggerFactory
import org.springframework.kafka.support.SendResult
import spock.lang.Specification

class EventPublisherSpec extends Specification {

    def objectUnderTest = new EventsPublisher(null, null)
    def logger = (Logger) LoggerFactory.getLogger(objectUnderTest.getClass())
    def loggingListAppender

    void setup() {
        logger.setLevel(Level.DEBUG)
        loggingListAppender = new ListAppender()
        logger.addAppender(loggingListAppender)
        loggingListAppender.start()
    }

    void cleanup() {
        ((Logger) LoggerFactory.getLogger(SubscriptionModelLoader.class)).detachAndStopAllAppenders()
    }

    def 'Callback handling on success.'() {
        given: 'a send result'
            def producerRecord = new ProducerRecord('topic-1', 'my value')
            def topicPartition = new TopicPartition('topic-2', 0)
            def recordMetadata = new RecordMetadata(topicPartition, 0, 0, 0, 0, 0)
            def sendResult = new SendResult(producerRecord, recordMetadata)
        when: 'the callback handler processes success'
            def callbackHandler = objectUnderTest.handleCallback('topic-3')
            callbackHandler.onSuccess(sendResult)
        then: 'an event is logged with level DEBUG'
            def loggingEvent = getLoggingEvent()
            loggingEvent.level == Level.DEBUG
        and: 'it contains the topic (from the record metadata) and the "value" (from the producer record)'
            loggingEvent.formattedMessage.contains('topic-2')
            loggingEvent.formattedMessage.contains('my value')
    }


    def 'Callback handling on failure.'() {
        when: 'the callback handler processes a failure'
            def callbackHandler = objectUnderTest.handleCallback('my topic')
            callbackHandler.onFailure(new Exception('my exception'))
        then: 'an event is logged with level ERROR'
            def loggingEvent = getLoggingEvent()
            loggingEvent.level == Level.ERROR
        and: 'it contains the topic and exception message'
            loggingEvent.formattedMessage.contains('my topic')
            loggingEvent.formattedMessage.contains('my exception')
    }

    def getLoggingEvent() {
        return loggingListAppender.list[0]
    }


}
