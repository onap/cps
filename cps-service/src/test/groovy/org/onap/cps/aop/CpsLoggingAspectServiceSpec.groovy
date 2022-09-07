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

    private static final Logger logger = Logger.getLogger('org.onap.cps')

    def mockProceedingJoinPoint = Mock(ProceedingJoinPoint)
    def mockMethodSignature = Mock(MethodSignature)
    def objectUnderTest = Spy(CpsLoggingAspectService)

    def setup() {
        mockMethodSignature.getDeclaringType() >> this.getClass()
        mockProceedingJoinPoint.getSignature() >> mockMethodSignature
    }

    def 'Log method execution time for log level : #logLevel.'() {
        given: 'normal method and log level of #logLevel'
            mockMethodSignature.getName() >> 'some method'
            logger.setLevel(logLevel)
        when: 'cps method is intercepted'
            objectUnderTest.interceptMethodCall(mockProceedingJoinPoint)
        then: 'logging is only done for correct levels'
            expectedNumberOfMethodExecution * objectUnderTest.logMethodCall(*_)
        where: 'the following log levels are used'
            logLevel     || expectedNumberOfMethodExecution
            Level.INFO   || 0
            Level.FINE   || 1
            Level.FINEST || 1
    }

    def 'Exception thrown during method execution.'() {
        given: 'some exception is thrown'
            def originalException = new Exception('some exception')
            mockProceedingJoinPoint.proceed() >> {
                throw originalException
            }
        when: 'cps method is intercepted'
            objectUnderTest.interceptMethodCall(mockProceedingJoinPoint)
        then: 'the same exception is still thrown'
            def thrownException = thrown(Exception)
            assert thrownException == originalException
    }

    def 'Masking sensitive data.'() {
        given: 'method named #methodName returns some value'
            mockMethodSignature.getName() >> methodName
            mockProceedingJoinPoint.proceed() >> 'original return value'
        and: 'the logger level is set to FINE'
            logger.setLevel(Level.FINE)
        when: 'cps method is intercepted'
           objectUnderTest.interceptMethodCall(mockProceedingJoinPoint)
        then: 'the expected value is being logged'
            1 * objectUnderTest.logMethodCall(_, _, _, expectedLogValue)
        where: 'the following method names are used'
            methodName        || expectedLogValue
            'normalMethod'    || 'original return value'
            'getAuthPassword' || '***********'
    }

}
