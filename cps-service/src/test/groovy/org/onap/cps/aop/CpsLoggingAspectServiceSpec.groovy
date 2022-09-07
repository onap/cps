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
import spock.lang.Specification
import java.util.logging.Level
import java.util.logging.Logger

class CpsLoggingAspectServiceSpec extends Specification {

    private static final Logger logger = Logger.getLogger("org.onap.cps")

    def mockProceedingJoinPoint = Mock(ProceedingJoinPoint)
    def mockMethodSignature = Mock(MethodSignature);
    def objectUnderTest = new CpsLoggingAspectService()

    def setup() {
        mockMethodSignature.getDeclaringType() >> this.getClass()
        mockMethodSignature.getDeclaringType().getSimpleName() >> 'CpsLoggingAspectServiceSpec'
        mockMethodSignature.getName() >> 'getAuthPassword()'
        mockProceedingJoinPoint.getSignature() >> mockMethodSignature
    }

    def 'Log method execution time for log level : #logLevel.'() {
        given: 'log level is set to #logLevel'
            logger.setLevel(logLevel)
        when: 'aop intercepts cps method'
            objectUnderTest.logMethodExecutionTime(mockProceedingJoinPoint)
        then: 'expected number of method execution'
            expectedNumberOfMethodExecution * mockProceedingJoinPoint.getArgs() >> 'dataspace-name'
        where: 'the following log levels are used'
            logLevel     || expectedNumberOfMethodExecution
            Level.INFO   || 0
            Level.FINE   || 1
            Level.FINEST || 1
    }

    def 'Exception thrown during method execution.'() {
        given: 'some exception is created'
            mockProceedingJoinPoint.proceed() >> { throw new Exception("some exception") }
        when: 'aop intercepts cps method and start calculation of time'
            objectUnderTest.logMethodExecutionTime(mockProceedingJoinPoint)
        then: 'some exception is thrown'
            thrown Exception
    }

    def 'Password blurred for DmiProperties getAuthPassword.'() {
        given: 'password is returned for call to method getAuthPassword()'
            mockProceedingJoinPoint.proceed() >> 'CPS_PASSWORD'
        and: 'the logger level is set to FINE'
            logger.setLevel(Level.FINE)
        when: 'logging intercepts cps method getAuthPassword()'
            def password = objectUnderTest.logMethodExecutionTime(mockProceedingJoinPoint)
        then: 'the password has been replaced with asterisks'
            assert password == '***********'
    }

}
