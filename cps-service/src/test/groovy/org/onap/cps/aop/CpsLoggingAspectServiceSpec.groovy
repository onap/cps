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
    def objectUnderTest = new CpsLoggingAspectService()

    def setup() {
        mockMethodSignature.getDeclaringType() >> this.getClass()
        mockMethodSignature.getDeclaringType().getSimpleName() >> 'CpsLoggingAspectServiceSpec'
        mockMethodSignature.getName() >> 'logMethodExecutionTime'
        mockProceedingJoinPoint.getSignature() >> mockMethodSignature
        Logger.getLogger('org.onap.cps').setLevel(Level.FINE)
    }

    def 'Log method execution time.'() {
        given: 'mock valid arguments'
            mockProceedingJoinPoint.getArgs() >> 'dataspace-name'
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
