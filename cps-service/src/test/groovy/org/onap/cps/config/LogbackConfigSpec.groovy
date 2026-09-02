/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.config

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import org.slf4j.LoggerFactory
import org.springframework.boot.logging.LoggingInitializationContext
import org.springframework.boot.logging.logback.LogbackLoggingSystem
import org.springframework.mock.env.MockEnvironment
import spock.lang.Specification

class LogbackConfigSpec extends Specification {

    def loggingSystem = new LogbackLoggingSystem(getClass().getClassLoader())

    def cleanup() {
        loggingSystem.cleanUp()
        ((LoggerContext) LoggerFactory.getILoggerFactory()).reset()
    }

    def 'Root logger has an appender attached for #scenario logging format.'() {
        given: 'an environment with the logging format set'
            def environment = new MockEnvironment()
            environment.setProperty('logging.format', loggingFormat)
        and: 'a logging initialisation context for that environment'
            def initializationContext = new LoggingInitializationContext(environment)
        when: 'logging is initialised from the real logback-spring.xml'
            loggingSystem.initialize(initializationContext, 'classpath:logback-spring.xml', null)
        then: 'the root logger has at least one appender attached'
            def rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
            assert rootLogger.iteratorForAppenders().hasNext()
        where: 'the following logging formats are used'
            scenario  | loggingFormat
            'console' | 'console'
            'json'    | 'json'
    }
}
