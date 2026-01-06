/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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

package org.onap.cps.ncmp.utils.events

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import org.slf4j.LoggerFactory
import spock.lang.Specification

class CloudEventMapperSpec extends Specification {

    def logger = Spy(ListAppender<ILoggingEvent>)

    void setup() {
        def setupLogger = ((Logger) LoggerFactory.getLogger(CloudEventMapper.class))
        setupLogger.setLevel(Level.DEBUG)
        setupLogger.addAppender(logger)
        logger.start()
    }

    void cleanup() {
        ((Logger) LoggerFactory.getLogger(CloudEventMapper.class)).detachAndStopAllAppenders()
    }

    def 'Map cloud event with runtime exception'() {
        given: 'a cloud event with invalid data'
            def cloudEvent = CloudEventBuilder.v1()
                .withId('test-id')
                .withType('test-type')
                .withSource(URI.create('test-source'))
                .withData('invalid-json-data'.bytes)
                .build()
        when: 'mapping to target event class'
            def result = CloudEventMapper.toTargetEvent(cloudEvent, String.class)
        then: 'exception is caught and logged'
            result == null
        and: 'error message is logged'
            def loggingEvents = logger.list
            assert loggingEvents.size() >= 1
            def errorEvent = loggingEvents.find { it.level == Level.ERROR }
            assert errorEvent != null
            assert errorEvent.formattedMessage.contains('Unable to map cloud event to target event class type')
            assert errorEvent.formattedMessage.contains('class java.lang.String')
    }
}