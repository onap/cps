/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the 'License');
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.integration.functional.ncmp

import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.impl.inventory.sync.ModuleSyncWatchdog

import java.util.concurrent.Executors

class ModuleSyncWatchdogSpec extends CpsIntegrationSpecBase {

    ModuleSyncWatchdog objectUnderTest

    def executorService = Executors.newFixedThreadPool(2)
    def SYNC_SAMPLE_SIZE = 100

    def setup() {
        objectUnderTest = moduleSyncWatchdog
        registerSequenceOfCmHandlesWithoutWaitForReady(DMI1_URL, NO_MODULE_SET_TAG, SYNC_SAMPLE_SIZE)
    }

    def cleanup() {
        deregisterSequenceOfCmHandles(DMI1_URL, SYNC_SAMPLE_SIZE)
        moduleSyncWorkQueue.clear()
    }

    def 'Watchdog is delayed for test.'() {
        when: 'wait a while but less then the initial delay of 10 minutes'
            Thread.sleep(3000)
        then: 'the work queue remains empty'
            assert moduleSyncWorkQueue.size() == 0
    }

    def 'Populate module sync work queue simultaneously on two parallel threads (CPS-2403).'() {
        // This test failed before bug https://lf-onap.atlassian.net/browse/CPS-2403 was fixed
        given: 'the queue is empty at the start'
            assert moduleSyncWorkQueue.isEmpty()
        when: 'attempt to populate the queue on the main (test) and another parallel thread at the same time'
            objectUnderTest.populateWorkQueueIfNeeded()
            executorService.execute(populateQueueWithoutDelay)
        and: 'wait a little (to give all threads time to complete their task)'
            Thread.sleep(50)
        then: 'the queue size is exactly the sample size'
            assert moduleSyncWorkQueue.size() == SYNC_SAMPLE_SIZE
    }

    def 'Populate module sync work queue on two parallel threads with a slight difference in start time.'() {
        // This test proved that the issue in CPS-2403 did not arise if the the queue was populated and given time to be distributed
        given: 'the queue is empty at the start'
            assert moduleSyncWorkQueue.isEmpty()
        when: 'attempt to populate the queue on the main (test) and another parallel thread a little later'
            objectUnderTest.populateWorkQueueIfNeeded()
            executorService.execute(populateQueueWithDelay)
        and: 'wait a little (to give all threads time to complete their task)'
            Thread.sleep(50)
        then: 'the queue size is exactly the sample size'
            assert moduleSyncWorkQueue.size() == SYNC_SAMPLE_SIZE
    }

    def populateQueueWithoutDelay = () -> {
        try {
            objectUnderTest.populateWorkQueueIfNeeded()
        } catch (InterruptedException e) {
            e.printStackTrace()
        }
    }

    def populateQueueWithDelay = () -> {
        try {
            Thread.sleep(10)
            objectUnderTest.populateWorkQueueIfNeeded()
        } catch (InterruptedException e) {
            e.printStackTrace()
        }
    }

}
