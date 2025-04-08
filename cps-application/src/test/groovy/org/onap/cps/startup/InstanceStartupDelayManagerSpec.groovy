/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

package org.onap.cps.startup

import spock.lang.Specification

class InstanceStartupDelayManagerSpec extends Specification {

    def objectUnderTest = Spy(InstanceStartupDelayManager)

    def 'Startup delay with real hostname.'() {
        when: 'start up delay is called'
            objectUnderTest.applyHostnameBasedStartupDelay()
        then: 'the system will sleep for some time'
            1 * objectUnderTest.haveALittleSleepInMs(_ as Long) >> { /* don't really sleep */ }
    }

    def 'Startup delay for hostname that ends with digit.'() {
        given: 'a hostname with a digit at the end'
            objectUnderTest.getHostName() >> 'host' + lastDigit
        and: 'the expected delay is based on last digit'
            def expectedDelay = lastDigit * 1_000
        when: 'startup delay is called'
            objectUnderTest.applyHostnameBasedStartupDelay()
        then: 'the system will sleep for expected time'
            1 * objectUnderTest.haveALittleSleepInMs(expectedDelay)
        where: 'following last digits are used'
            lastDigit << [0, 1]
    }

    def 'Startup delay for hostname that does not end with digit.'() {
        given: 'a hostname with a non-digit at the end'
            objectUnderTest.getHostName() >> 'hostX'
        and: 'the expected delay is based on hash code with max of 3,000 ms'
            def expectedDelay =  Math.abs('hostX'.hashCode() % 3_000)
        when: 'startup delay is called'
            objectUnderTest.applyHostnameBasedStartupDelay()
        then: 'the system will sleep for expected time'
            1 * objectUnderTest.haveALittleSleepInMs(expectedDelay) >> { /* don't really sleep */ }
    }

    def 'Startup delay when hostname cannot be resolved.'() {
        given: 'an exception is thrown while getting the hostname'
            objectUnderTest.getHostName() >> { throw new Exception('some message') }
        when: 'startup delay is called'
            objectUnderTest.applyHostnameBasedStartupDelay()
        then: 'system will not sleep'
            0 * objectUnderTest.haveALittleSleepInMs(_)
    }
}
