/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation
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

package org.onap.cps.ncmp.rest.executor

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.slf4j.LoggerFactory
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class CpsNcmpTaskExecutorSpec extends Specification {

    def objectUnderTest = new CpsNcmpTaskExecutor()
    def logger = Spy(ListAppender<ILoggingEvent>)
    def enoughTime = 100

    @BeforeEach
    void setup() {
        ((Logger) LoggerFactory.getLogger(CpsNcmpTaskExecutor.class)).addAppender(logger)
        logger.start()
    }

    @AfterEach
    void teardown() {
        ((Logger) LoggerFactory.getLogger(CpsNcmpTaskExecutor.class)).detachAndStopAllAppenders()
    }

    def 'Execute successful task.'() {
        when: 'task is executed'
            objectUnderTest.executeTask(taskSupplier(), enoughTime)
        then: 'an event is logged with level INFO'
            new PollingConditions().within(1) {
                def loggingEvent = getLoggingEvent()
                assert loggingEvent.level == Level.INFO
            }
        and: 'the log indicates the task completed successfully'
            assert loggingEvent.formattedMessage == 'Async task completed successfully.'
    }

    def 'Execute failing task.'() {
        when: 'task is executed'
            objectUnderTest.executeTask(taskSupplierForFailingTask(), enoughTime)
        then: 'an event is logged with level ERROR'
            new PollingConditions().within(1) {
                def loggingEvent = getLoggingEvent()
                assert loggingEvent.level == Level.ERROR
            }
        and: 'the original error message is logged'
            assert loggingEvent.formattedMessage.contains('original exception message')
    }

    def taskSupplier() {
        return () -> 'hello world'
    }

    def taskSupplierForFailingTask() {
        return () -> { throw new RuntimeException('original exception message') }
    }

    def getLoggingEvent() {
        return logger.list[0]
    }

}
