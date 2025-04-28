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

    def 'Startup delay with sequenced host name with #scenario'() {
        given: 'a sequenced host name'
            objectUnderTest.getHostName() >> hostName
        and: 'the expected delay is based on the sequence number'
            def expectedDelay = expectedDelayInSeconds * 1_000;
        when: 'startup delay is called'
            objectUnderTest.applyHostNameBasedStartupDelay()
        then: 'the system will sleep for expected number of seconds as defined by sequence number'
            1 * objectUnderTest.haveALittleSleepInMs(expectedDelay) >> { /* don't sleep for testing purposes */  }
        where: ' following sequenced host names are used'
            scenario                      | hostName           || expectedDelayInSeconds
            'our usual host-name'         | 'cps-and-ncmp-0'   || 0
            'dash and 1 digit at end'     | 'host-1'           || 1
            'dash and 2 digits at end'    | 'host-23'          || 23
            'digits in name'              | 'host-2-34'        || 34
            'weird name ending in digits' | 't@st : - { " -56' || 56
    }

    def 'Startup delay with un-sequenced host name.'() {
        given: 'a un-sequenced host name: #hostName'
            objectUnderTest.getHostName() >> hostName
        when: 'startup delay is called'
            objectUnderTest.applyHostNameBasedStartupDelay()
        then: 'the system will sleep for an expected time based on the hash'
            1 * objectUnderTest.haveALittleSleepInMs(expectedDelayBasedOnHashInMs) >> { /* don't sleep for testing purposes */  }
        where: ' following un-sequenced host names are used'
            hostName                                  || expectedDelayBasedOnHashInMs
            'no_digits_at_all'                        || 784
            'digits-12-in-the-middle'                 || 1484
            'non-digit-after-digit-1a'                || 753
            'dash-after-digit-1-'                     || 7256
            'three-digits-at-end-is-not-accepted-123' || 9941
    }

    def 'Startup delay when host name cannot be resolved.'() {
        given: 'an exception is thrown while getting the host name'
            objectUnderTest.getHostName() >> { throw new Exception('some message') }
        when: 'startup delay is called'
            objectUnderTest.applyHostNameBasedStartupDelay()
        then: 'system will not sleep'
            0 * objectUnderTest.haveALittleSleepInMs(_)
    }

    def 'Startup delay when sleep is interrupted'() {
        given: 'sleep method throws InterruptedException'
            objectUnderTest.haveALittleSleepInMs(_) >> { throw new InterruptedException('some message') }
        when: 'startup delay is called'
            objectUnderTest.applyHostNameBasedStartupDelay()
        then: 'interrupt exception is ignored'
            noExceptionThrown()
    }

}

