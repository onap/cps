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
import org.springframework.util.StopWatch
import spock.util.concurrent.PollingConditions

import java.util.concurrent.Executors
import java.util.concurrent.Future
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

    def 'CPS-2478 Highlight (and improve) module sync inefficiencies.'() {
        given: 'register 250 cm handles with module set tag cps-2478-A'
            def numberOfTags = 2
            def cmHandlesPerTag = 250
            def totalCmHandles = numberOfTags * cmHandlesPerTag
            def offset = 1
            def minimumBatches = totalCmHandles / 100
            registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(DMI1_URL, 'cps-2478-A', cmHandlesPerTag, offset)
        and: 'register anther 250 cm handles with module set tag cps-2478-B'
            offset += cmHandlesPerTag
            registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(DMI1_URL, 'cps-2478-B', cmHandlesPerTag, offset)
        and: 'clear any previous instrumentation'
            meterRegistry.clear()
        when: 'sync all advised cm handles'
            objectUnderTest.moduleSyncAdvisedCmHandles()
            Thread.sleep(100)
        then: 'retry until all schema sets are stored in db (1 schema set  for each cm handle)'
            def dbSchemaSetStorageTimer = meterRegistry.get('cps.module.persistence.schemaset.store').timer()
            new PollingConditions().within(10, () -> {
                objectUnderTest.moduleSyncAdvisedCmHandles()
                Thread.sleep(100)
                assert dbSchemaSetStorageTimer.count() >= 500
            })
        then: 'wait till at least 5 batches of state updates are done (often more because of retries of locked cm handles)'
            def dbStateUpdateTimer = meterRegistry.get('cps.ncmp.cmhandle.state.update.batch').timer()
            new PollingConditions().within(10, () -> {
                assert dbStateUpdateTimer.count() >= minimumBatches
            })
        and: 'the db has been queried for tags exactly 2 times.'
            def dbModuleQueriesTimer = meterRegistry.get('cps.module.service.module.reference.query.by.attribute').timer()
            assert dbModuleQueriesTimer.count() == 2
        and: 'exactly 2 calls to DMI to get module references'
            def dmiModuleRetrievalTimer = meterRegistry.get('cps.ncmp.inventory.module.references.from.dmi').timer()
            assert dmiModuleRetrievalTimer.count() == 2
        and: 'log the relevant instrumentation'
            logInstrumentation(dbModuleQueriesTimer,    'query module references')
            logInstrumentation(dmiModuleRetrievalTimer, 'get modules from DMI   ')
            logInstrumentation(dbSchemaSetStorageTimer, 'store schema sets      ')
            logInstrumentation(dbStateUpdateTimer,      'batch state updates    ')
        cleanup: 'remove all cm handles'
            // To properly measure performance the sample-size should be increased to 20,000 cm handles or higher (10,000 per tag)
            def stopWatch = new StopWatch()
            stopWatch.start()
            deregisterSequenceOfCmHandles(DMI1_URL, totalCmHandles, 1)
            stopWatch.stop()
            println "*** CPS-2478, Deletion of $totalCmHandles cm handles took ${stopWatch.getTotalTimeMillis()} milliseconds"
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

    def 'Populate module sync work queue simultaneously on two parallel threads.'() {
        given: 'the queue is empty at the start'
            registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(DMI1_URL, NO_MODULE_SET_TAG, PARALLEL_SYNC_SAMPLE_SIZE, 1)
            assert moduleSyncWorkQueue.isEmpty()
        and: 'no lock is acquired'
            assert !cpsAndNcmpLock.isLocked("workQueueLock")
        when: 'attempt to populate the queue using 2 threads'
            def submittedTasks = new ArrayList()
            for (int i = 0; i < 2; i++) {
                submittedTasks.addAll(executorService.submit(populateQueueWithoutDelayCallable))
            }
        then: 'the lock is acquired by a thread'
            for (def task : submittedTasks) {
                if (task.get() == "locked") {
                    assert cpsAndNcmpLock.isLocked("workQueueLock")
                }
            }
        then: 'the queue size is exactly the sample size'
            assert moduleSyncWorkQueue.size() == PARALLEL_SYNC_SAMPLE_SIZE
        and: 'after processing the lock is released'
            assert !cpsAndNcmpLock.isLocked("workQueueLock")
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

    def logInstrumentation(timer, description) {
        println "*** CPS-2478, $description : Invoked ${timer.count()} times, Total Time: ${timer.totalTime(TimeUnit.MILLISECONDS)} ms, Mean Time: ${timer.mean(TimeUnit.MILLISECONDS)} ms"
        return true
    }

    def populateQueueWithoutDelay = () -> {
        try {
            objectUnderTest.populateWorkQueueIfNeeded()
        } catch (InterruptedException e) {
            e.printStackTrace()
        }
    }

    def populateQueueWithoutDelayCallable = () -> {
        try {
            objectUnderTest.populateWorkQueueIfNeeded()
            return "locked"
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
