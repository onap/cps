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

package org.onap.cps.ncmp.impl.inventory.sync

import spock.lang.Specification

class MasterLeaseEntryProcessorSpec extends Specification {

    def objectUnderTest = new MasterLeaseEntryProcessor('test-instance-id', 'test-instance-id:9999999999999')

    def 'Master lease evaluation with #scenario.'() {
        given: 'an entry holding the current lease value'
            def entry = new AbstractMap.SimpleEntry<String, String>('moduleSyncMasterLease', currentLease)
        when: 'the processor evaluates the entry'
            def becomesMaster = objectUnderTest.process(entry)
        then: 'the outcome is as expected'
            assert becomesMaster == expectedToBecomeMaster
        and: 'the resulting lease value is as expected'
            assert entry.value == expectedLeaseAfter
        where: 'the following lease states are used'
            scenario                         | currentLease                   || expectedToBecomeMaster | expectedLeaseAfter
            'no existing lease'              | null                           || true                   | 'test-instance-id:9999999999999'
            'lease already owned (renew)'    | 'test-instance-id:1'           || true                   | 'test-instance-id:9999999999999'
            'another instance lease expired' | 'other-instance:1'             || true                   | 'test-instance-id:9999999999999'
            'another instance lease valid'   | 'other-instance:9999999999999' || false                  | 'other-instance:9999999999999'
    }

}
