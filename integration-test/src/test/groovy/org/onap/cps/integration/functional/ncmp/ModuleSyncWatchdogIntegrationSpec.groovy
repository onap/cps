/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 Nordix Foundation
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

import com.hazelcast.map.IMap
import io.micrometer.core.instrument.MeterRegistry
import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.impl.inventory.sync.ModuleSyncWatchdog
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.StopWatch
import spock.lang.Ignore
import spock.util.concurrent.PollingConditions

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ModuleSyncWatchdogIntegrationSpec extends CpsIntegrationSpecBase {

    ModuleSyncWatchdog objectUnderTest

    @Autowired
    MeterRegistry meterRegistry

    @Autowired
    IMap<String, Integer> cmHandlesByState

    def executorService = Executors.newFixedThreadPool(2)
    def PARALLEL_SYNC_SAMPLE_SIZE = 100

    def setup() {
        objectUnderTest = moduleSyncWatchdog
        clearCmHandleStateGauge()
    }

    def cleanup() {
        try {
            moduleSyncWorkQueue.clear()
        } finally {
            executorService.shutdownNow()
        }
    }

    def 'Watchdog is disabled for test.'() {
        given: 'some cm handles are registered'
            registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(DMI1_URL, NO_MODULE_SET_TAG, PARALLEL_SYNC_SAMPLE_SIZE, 1)
        when: 'wait a while but less then the initial delay of 10 minutes'
            Thread.sleep(3000)
        then: 'the work queue remains empty'
            assert moduleSyncWorkQueue.isEmpty()
        cleanup: 'remove advised cm handles'
            deregisterSequenceOfCmHandles(DMI1_URL, PARALLEL_SYNC_SAMPLE_SIZE, 1)
    }

    /** this test has intermittent failures, due to race conditions
     *  Ignored but left here as it might be valuable to further optimization investigations.
     **/
    @Ignore
    def 'CPS-2478 Highlight (and improve) module sync inefficiencies.'() {
        given: 'register 250 cm handles with module set tag cps-2478-A'
            def numberOfTags = 2
            def cmHandlesPerTag = 250
            def totalCmHandles = numberOfTags * cmHandlesPerTag
            def offset = 1
            registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(DMI1_URL, 'cps-2478-A', cmHandlesPerTag, offset)
        and: 'register anther 250 cm handles with module set tag cps-2478-B'
            offset += cmHandlesPerTag
            registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(DMI1_URL, 'cps-2478-B', cmHandlesPerTag, offset)
        and: 'clear any previous instrumentation'
            meterRegistry.clear()
        when: 'sync all advised cm handles'
            objectUnderTest.moduleSyncAdvisedCmHandles()
            Thread.sleep(100)
        then: 'Keep processing until there are no more LOCKED or ADVISED cm handles'
            new PollingConditions().within(10, () -> {
                def advised = cmHandlesByState.get('advisedCmHandlesCount')
                def locked = cmHandlesByState.get('lockedCmHandlesCount')
                if ( locked > 0 | advised > 0 ) {
                    println "CPS-2576 Need to retry ${locked} LOCKED / ${advised} ADVISED cm Handles"
                    objectUnderTest.moduleSyncAdvisedCmHandles()
                    Thread.sleep(100)
                }
                assert cmHandlesByState.get('lockedCmHandlesCount') + cmHandlesByState.get('advisedCmHandlesCount') == 0
            })
        and: 'log the relevant instrumentation'
            def dmiModuleRetrievalTimer = meterRegistry.get('cps.ncmp.inventory.module.references.from.dmi').timer()
            def dbSchemaSetStorageTimer = meterRegistry.get('cps.module.persistence.schemaset.store').timer()
            def dbStateUpdateTimer = meterRegistry.get('cps.ncmp.cmhandle.state.update.batch').timer()
            logInstrumentation(dmiModuleRetrievalTimer, 'get modules from DMI   ')
            logInstrumentation(dbSchemaSetStorageTimer, 'store schema sets      ')
            logInstrumentation(dbStateUpdateTimer,      'batch state updates    ')
        cleanup: 'remove all test cm handles'
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
        cleanup: 'remove all test cm handles'
            deregisterSequenceOfCmHandles(DMI1_URL, PARALLEL_SYNC_SAMPLE_SIZE, 1)
    }

    /** this test has intermittent failures, due to race conditions
     *  Ignored but left here as it might be valuable to further optimization investigations.
     **/
    @Ignore
    def 'Schema sets with overlapping modules processed at the same time (DB constraint violation).'() {
        given: 'register one batch (100) cm handles of tag A (with overlapping module names)'
            registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(DMI1_URL, 'tagA', 100, 1, ModuleNameStrategy.OVERLAPPING)
        and: 'register another batch cm handles of tag B (with overlapping module names)'
            registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(DMI1_URL, 'tagB', 100, 101, ModuleNameStrategy.OVERLAPPING)
        and: 'populate the work queue with both batches'
            objectUnderTest.populateWorkQueueIfNeeded()
        when: 'advised cm handles are processed on 2 threads (exactly one batch for each)'
            objectUnderTest.moduleSyncAdvisedCmHandles()
            executorService.execute(moduleSyncAdvisedCmHandles)
        then: 'wait till all cm handles have been processed'
            new PollingConditions().within(10, () -> {
                assert getNumberOfProcessedCmHandles() == 200
            })
        then: 'at least 1 cm handle is in state LOCKED'
            assert cmHandlesByState.get('lockedCmHandlesCount') >= 1
        cleanup: 'remove all test cm handles'
            deregisterSequenceOfCmHandles(DMI1_URL, 200, 1)
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
        cleanup: 'remove all test cm handles'
            deregisterSequenceOfCmHandles(DMI1_URL, PARALLEL_SYNC_SAMPLE_SIZE, 1)
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

    def populateQueueWithDelay = () -> {
        try {
            Thread.sleep(10)
            objectUnderTest.populateWorkQueueIfNeeded()
        } catch (InterruptedException e) {
            e.printStackTrace()
        }
    }

    def moduleSyncAdvisedCmHandles = () -> {
        try {
            objectUnderTest.moduleSyncAdvisedCmHandles()
        } catch (InterruptedException e) {
            e.printStackTrace()
        }
    }

    def clearCmHandleStateGauge() {
        cmHandlesByState.keySet().each { cmHandlesByState.put(it, 0)}
    }

    def getNumberOfProcessedCmHandles() {
        return cmHandlesByState.get('readyCmHandlesCount') + cmHandlesByState.get('lockedCmHandlesCount')
    }


}
