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


class HostnameStartupDelayManagerSpec extends Specification {

    def spiedHostnameStartupDelayManager = Spy(HostnameStartupDelayManager)

    def 'delay based on digit at end of hostname'() {
        given: 'mock hostname with a digit at the end'
            spiedHostnameStartupDelayManager.getHostName() >> 'host9'
        when: 'applyHostnameBasedStartupDelay is called'
            spiedHostnameStartupDelayManager.applyHostnameBasedStartupDelay()
        then: 'doSleep is called with 9000 ms delay'
            1 * spiedHostnameStartupDelayManager.doSleep(9000)
    }

    def 'delay based on hash if non-digit in hostname'() {
        given: 'mock hostname with a non-digit at the end'
            spiedHostnameStartupDelayManager.getHostName() >> 'hostX'
        when: 'applyHostnameBasedStartupDelay is called'
            spiedHostnameStartupDelayManager.applyHostnameBasedStartupDelay()
        then: 'doSleep is called with a delay based on hash value'
            1 * spiedHostnameStartupDelayManager.doSleep(_ as Long)
    }

    def 'no exception thrown on UnknownHostException'() {
        given: 'An UnknownHostException is thrown while getting the hostname'
            spiedHostnameStartupDelayManager.getHostName() >> { throw new UnknownHostException('Unable to resolve hostname') }
        when: 'applyHostnameBasedStartupDelay is called'
            spiedHostnameStartupDelayManager.applyHostnameBasedStartupDelay()
        then: 'no exception is thrown'
            noExceptionThrown()
    }
}
