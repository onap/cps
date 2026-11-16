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

package org.onap.cps.init

import com.hazelcast.map.IMap
import spock.lang.Specification

class MasterElectionSpec extends Specification {

    def mockCpsCommonLocks = Mock(IMap<String, String>)

    def objectUnderTest = new MasterElection(mockCpsCommonLocks, 'test-instance-id')

    def 'Becoming master for a lease.'() {
        given: 'the lease can be acquired'
            mockCpsCommonLocks.executeOnKey('someLease', _) >> true
        expect: 'this instance is the master'
            assert objectUnderTest.isMaster('someLease')
    }

    def 'Not becoming master for a lease.'() {
        given: 'the lease cannot be acquired (held by another instance)'
            mockCpsCommonLocks.executeOnKey('someLease', _) >> false
        expect: 'this instance is not the master'
            assert !objectUnderTest.isMaster('someLease')
    }

    def 'Heartbeat renews a lease held by this instance.'() {
        given: 'this instance is master for the lease'
            mockCpsCommonLocks.executeOnKey('someLease', _) >> true
            objectUnderTest.isMaster('someLease')
        when: 'the heartbeat runs'
            objectUnderTest.renewHeldLeases()
        then: 'the held lease is renewed'
            1 * mockCpsCommonLocks.executeOnKey('someLease', _) >> true
    }

    def 'Heartbeat does not renew a lease this instance does not hold.'() {
        given: 'this instance failed to become master for the lease'
            mockCpsCommonLocks.executeOnKey('someLease', _) >> false
            objectUnderTest.isMaster('someLease')
        when: 'the heartbeat runs'
            objectUnderTest.renewHeldLeases()
        then: 'no lease is renewed'
            0 * mockCpsCommonLocks.executeOnKey(*_)
    }

    def 'Heartbeat stops renewing a lease once it is lost.'() {
        given: 'this instance is master for the lease'
            mockCpsCommonLocks.executeOnKey('someLease', _) >> true
            objectUnderTest.isMaster('someLease')
        when: 'the heartbeat runs but the lease can no longer be renewed'
            objectUnderTest.renewHeldLeases()
        then: 'the lease renewal is attempted once and reports it is lost'
            1 * mockCpsCommonLocks.executeOnKey('someLease', _) >> false
        when: 'the heartbeat runs again'
            objectUnderTest.renewHeldLeases()
        then: 'the lost lease is no longer renewed'
            0 * mockCpsCommonLocks.executeOnKey(*_)
    }

}
