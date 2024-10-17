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
import spock.util.concurrent.PollingConditions

import java.util.concurrent.Executors

class ModuleSyncSpec extends CpsIntegrationSpecBase {

    ModuleSyncWatchdog objectUnderTest

    def executorService = Executors.newFixedThreadPool(2)
    def SYNC_SAMPLE_SIZE = 100

    def setup() {
        objectUnderTest = moduleSyncWatchdog
        objectUnderTest.enabled = false
        registerBatchOfCmHandlesWithoutWaitForReady(DMI1_URL, NO_MODULE_SET_TAG, SYNC_SAMPLE_SIZE)
    }

    def cleanup() {
        deregisterBatchOfCmHandles(DMI1_URL, SYNC_SAMPLE_SIZE)
        objectUnderTest.moduleSyncWorkQueue.clear()
    }

    def 'Watchdog enabled.'() {
        given: 'enable the watchdog'
            objectUnderTest.enabled = true
        expect: 'the watchdog will put advised cm handles in the work queue within a few seconds'
            new PollingConditions().within(3) {
                assert objectUnderTest.moduleSyncWorkQueue.size() > 0
            }
    }

    def 'Watchdog disabled.'() {
        given: 'disable the watchdog'
            objectUnderTest.enabled = false
        when: 'wait a while'
            Thread.sleep(3000)
        then: 'the work queue remains empty'
            assert objectUnderTest.moduleSyncWorkQueue.size() == 0
    }

    def 'Populate module sync work queue simultaneously on two parallel threads.'() {
        given: 'the queue is empty at the start'
            assert objectUnderTest.moduleSyncWorkQueue.isEmpty()
        when: 'attempt to populate the queue on the main (test) and another parallel thread at the same time'
            executorService.execute(populateQueueWithoutDelay)
            objectUnderTest.populateWorkQueueIfNeeded()
        then: 'the queue size it greater then the original sample size i.e. there is duplication'
            // THis is the main cause for the bug https://lf-onap.atlassian.net/browse/CPS-2403
            assert objectUnderTest.moduleSyncWorkQueue.size() > SYNC_SAMPLE_SIZE
    }

    def 'Populate module sync work queue on two parallel threads with a slight difference in start time.'() {
        given: 'the queue is empty at the start'
            assert objectUnderTest.moduleSyncWorkQueue.isEmpty()
        when: 'attempt to populate the queue on the main (test) and another parallel thread at the same time'
            executorService.execute(populateQueueWithDelay)
            objectUnderTest.populateWorkQueueIfNeeded()
        then: 'the queue size is exactly teh sample size'
            assert objectUnderTest.moduleSyncWorkQueue.size() == SYNC_SAMPLE_SIZE
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
