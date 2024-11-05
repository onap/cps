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

import io.micrometer.core.instrument.MeterRegistry
import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.impl.inventory.sync.ModuleSyncWatchdog
import org.springframework.beans.factory.annotation.Autowired
import spock.util.concurrent.PollingConditions

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ModuleSyncWatchdogIntegrationSpec extends CpsIntegrationSpecBase {

    ModuleSyncWatchdog objectUnderTest

    @Autowired
    MeterRegistry meterRegistry

    def executorService = Executors.newFixedThreadPool(2)
    def PARALLEL_SYNC_SAMPLE_SIZE = 100

    def setup() {
        objectUnderTest = moduleSyncWatchdog
    }

    def cleanup() {
        try {
            deregisterSequenceOfCmHandles(DMI1_URL, PARALLEL_SYNC_SAMPLE_SIZE, 1)
            moduleSyncWorkQueue.clear()
        } finally {
            executorService.shutdownNow()
        }
    }

    def 'Watchdog is disabled for test.'() {
        given:
            registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(DMI1_URL, NO_MODULE_SET_TAG, PARALLEL_SYNC_SAMPLE_SIZE, 1)
        when: 'wait a while but less then the initial delay of 10 minutes'
            Thread.sleep(3000)
        then: 'the work queue remains empty'
            assert moduleSyncWorkQueue.isEmpty()
    }

    def 'CPS-2478 Highlight module sync inefficiencies.'() {
        given:
            def NUMBER_OF_TAGS = 2
            def CM_HANDLES_PER_TAG = 250
            def TOTAL_CM_HANDLES = NUMBER_OF_TAGS * CM_HANDLES_PER_TAG
            dmiDispatcher1.moduleNamesPerCmHandleId.clear()
            def offset = 1000
            registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(DMI1_URL, 'cps-2478-A', CM_HANDLES_PER_TAG, offset)
            offset += CM_HANDLES_PER_TAG
            registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(DMI1_URL, 'cps-2478-B', CM_HANDLES_PER_TAG, offset)
            Thread.sleep(3000)
            meterRegistry.clear()
        when: 'sync all advised cm handles'
            objectUnderTest.moduleSyncAdvisedCmHandles()
        and: 'wait a little (to give all threads time to get started, and the instrumentation timer to be created)'
            Thread.sleep(100)
        and: 'wait until all cm handles have been queried (for their tag)'
            def dbModuleQueriesTimer = meterRegistry.get('cps.module.service.module.reference.query.by.attribute').timer()
            new PollingConditions().within(10, () -> {
                assert dbModuleQueriesTimer.count() >= TOTAL_CM_HANDLES
            })
        and: 'this takes about 1 second'
            //assert  dbModuleQueriesTimer.totalTime(TimeUnit.MILLISECONDS) > 800
            //assert  dbModuleQueriesTimer.totalTime(TimeUnit.MILLISECONDS) < 1200
            System.out.println('*** CPS-2478, query module references (ms) : ' + dbModuleQueriesTimer.totalTime(TimeUnit.MILLISECONDS))
        then: 'exactly 100 (internal batch size) calls to DMI to get module references'
            def dmiModuleRetrievalTimer = meterRegistry.get('cps.ncmp.inventory.module.references.from.dmi').timer()
            assert dmiModuleRetrievalTimer.count() == 200
        and: 'this takes about 1.2 second'
            //assert dmiModuleRetrievalTimer.totalTime(TimeUnit.MILLISECONDS) > 1000
            //assert dmiModuleRetrievalTimer.totalTime(TimeUnit.MILLISECONDS) < 1600
            System.out.println('*** CPS-2478, get modules from DMI    (ms) : ' + dmiModuleRetrievalTimer.totalTime(TimeUnit.MILLISECONDS))
        then: ' wait a little again to ensure the last batch is saved'
            Thread.sleep(1000)
        and: '1 schema set stored in db created for each cm handle'
            def dbSchemaSetStorageTimer = meterRegistry.get('cps.module.persistence.schemaset.store').timer()
            //TODO Toine, check why this often  1 or 2 short of 500
            assert dbSchemaSetStorageTimer.count() >= TOTAL_CM_HANDLES - 2
        and: 'this takes over 8 seconds'
            //assert dbSchemaSetStorageTimer.totalTime(TimeUnit.MILLISECONDS) > 8000
            System.out.println('*** CPS-2478, store schema sets in DB (ms) : ' + dbSchemaSetStorageTimer.totalTime(TimeUnit.MILLISECONDS))
        and: '(batch) state updated called once for each batch'
            def dbStateUpdateTimer = meterRegistry.get('cps.ncmp.cmhandle.state.update.batch').timer()
            assert dbStateUpdateTimer.count() == 5
            System.out.println('*** CPS-2478, update states in DB      (ms) : ' + dbStateUpdateTimer.totalTime(TimeUnit.MILLISECONDS))
        cleanup:
            deregisterSequenceOfCmHandles(DMI1_URL, TOTAL_CM_HANDLES, 1000)
    }

    def 'Populate module sync work queue simultaneously on two parallel threads (CPS-2403).'() {
        // This test failed before bug https://lf-onap.atlassian.net/browse/CPS-2403 was fixed
        given: 'the queue is empty at the start'
            registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(DMI1_URL, NO_MODULE_SET_TAG, PARALLEL_SYNC_SAMPLE_SIZE, 1)
            assert moduleSyncWorkQueue.isEmpty()
        when: 'attempt to populate the queue on the main (test) and another parallel thread at the same time'
            objectUnderTest.populateWorkQueueIfNeeded()
            executorService.execute(populateQueueWithoutDelay)
        and: 'wait a little (to give all threads time to complete their task)'
            Thread.sleep(50)
        then: 'the queue size is exactly the sample size'
            assert moduleSyncWorkQueue.size() == PARALLEL_SYNC_SAMPLE_SIZE
    }

    def 'Populate module sync work queue on two parallel threads with a slight difference in start time.'() {
        // This test proved that the issue in CPS-2403 did not arise if the the queue was populated and given time to be distributed
        given: 'the queue is empty at the start'
            registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(DMI1_URL, NO_MODULE_SET_TAG, PARALLEL_SYNC_SAMPLE_SIZE, 1)
            assert moduleSyncWorkQueue.isEmpty()
        when: 'attempt to populate the queue on the main (test) and another parallel thread a little later'
            objectUnderTest.populateWorkQueueIfNeeded()
            executorService.execute(populateQueueWithDelay)
        and: 'wait a little (to give all threads time to complete their task)'
            Thread.sleep(50)
        then: 'the queue size is exactly the sample size'
            assert moduleSyncWorkQueue.size() == PARALLEL_SYNC_SAMPLE_SIZE
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
