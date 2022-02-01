/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.aop

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.onap.cps.spi.exceptions.DataValidationException
import spock.lang.Specification

import java.util.logging.Level
import java.util.logging.Logger

class CpsLoggingAspectServiceSpec extends Specification {

    def mockProceedingJoinPoint = Mock(ProceedingJoinPoint)
    def mockMethodSignature = Mock(MethodSignature);
    def mockLogger = Mock(Logger)
    def objectUnderTest = new CpsLoggingAspectService(mockLogger)

    def setup() {
        mockMethodSignature.getDeclaringType() >> this.getClass()
        mockMethodSignature.getDeclaringType().getSimpleName() >> 'CpsLoggingAspectServiceSpec'
        mockMethodSignature.getName() >> 'logMethodExecutionTime'
        mockProceedingJoinPoint.getSignature() >> mockMethodSignature
    }

    def 'Log method execution time with log level trace.'() {
        given: 'mock valid arguments and log level as trace'
            mockProceedingJoinPoint.getArgs() >> 'dataspace-name'
//            Logger.getLogger('org.onap.cps').setLevel(Level.FINEST)
            mockLogger.isLoggable(Level.FINEST) >> true
        when: 'aop intercepts cps method and start calculation of time'
            objectUnderTest.logMethodExecutionTime(mockProceedingJoinPoint)
        then: 'process successfully and log details of executed method'
            1 * mockProceedingJoinPoint.proceed()
    }

    def 'Log method execution time with log level debug.'() {
        given: 'mock valid arguments and log level as debug'
            mockProceedingJoinPoint.getArgs() >> 'dataspace-name'
//            Logger.getLogger('org.onap.cps').setLevel(Level.FINE)
            mockLogger.isLoggable(Level.FINE) >> true
        when: 'aop intercepts cps method and start calculation of time'
            objectUnderTest.logMethodExecutionTime(mockProceedingJoinPoint)
        then: 'process successfully and log details of executed method'
            1 * mockProceedingJoinPoint.proceed()
    }

    def 'Creating a data validation exception for invalid args.'() {
        given: 'a data validation exception is created'
            mockProceedingJoinPoint.proceed() >> {
                throw new DataValidationException('invalid args',
                        'invalid method arg(s) is passed', new Throwable())
            }
        when: 'aop intercepts cps method and start calculation of time'
            objectUnderTest.logMethodExecutionTime(mockProceedingJoinPoint)
        then: 'data validation exception is thrown'
            thrown(DataValidationException.class)
    }
}
