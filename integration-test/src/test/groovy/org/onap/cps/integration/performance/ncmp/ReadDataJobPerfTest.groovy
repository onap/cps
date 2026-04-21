/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.integration.performance.ncmp

import org.onap.cps.integration.ResourceMeter
import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.api.datajobs.DataJobService
import org.onap.cps.ncmp.api.datajobs.models.DataJobReadRequest
import org.onap.cps.ncmp.api.datajobs.models.ReadProperties
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistration
import org.springframework.beans.factory.annotation.Autowired

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

/**
 * Performance test for read data job classification.
 *
 * Production characteristics:
 *   - 20 parallel requests at 20 req/sec
 *   - Average 10 selectors per request
 *   - 99% broadcast selectors
 *   - Up to 5 distinct FDNs and DMIs per request
 *   - Target: under 1 second per request
 *
 * This test does not depend on common performance test data. Hence it just extends the integration spec base.
 */
class ReadDataJobPerfTest extends CpsIntegrationSpecBase {

    def NETWORK_SIZE = 1_000
    def READY_CM_HANDLES_PER_DMI = 50
    def NETWORK_OFFSET = 10_001
    def PARALLEL_REQUESTS = 20

    @Autowired
    DataJobService dataJobService

    def setup() {
        // Register READY handles first across both DMIs, then bulk non-READY handles.
        // Order matters: moduleSyncAdvisedCmHandles() syncs ALL advised handles,
        // so bulk handles must be registered after to ensure they stay non-READY.
        registerReadyCmHandles(DMI1_URL, dmiDispatcher1, 'dmi1', READY_CM_HANDLES_PER_DMI)
        registerReadyCmHandles(DMI2_URL, dmiDispatcher2, 'dmi2', READY_CM_HANDLES_PER_DMI)
        registerTestCmHandles(NETWORK_SIZE)
    }

    def cleanup() {
        deregisterSequenceOfCmHandles(DMI1_URL, NETWORK_SIZE, NETWORK_OFFSET)
        (1..READY_CM_HANDLES_PER_DMI).each {
            deregisterCmHandle(DMI1_URL, 'ch-dmi1-' + it)
            deregisterCmHandle(DMI2_URL, 'ch-dmi2-' + it)
        }
    }

    def 'Performance test single read data job classification'() {
        given: 'a read request with 10 selectors (99% broadcast)'
            def dataJobReadRequest = createReadRequest('job-single')
        when: 'classifying the selectors 3 times'
            def executionResult1 = executeReadJob(dataJobReadRequest)
            def executionResult2 = executeReadJob(dataJobReadRequest)
            def executionResult3 = executeReadJob(dataJobReadRequest)
        then: 'record the results'
            logExecutionResults('Single run 1', executionResult1)
            logExecutionResults('Single run 2', executionResult2)
            logExecutionResults('Single run 3', executionResult3)
        and: 'each run completes under 1 second'
            assert executionResult1.executionTime < 1
            assert executionResult2.executionTime < 1
            assert executionResult3.executionTime < 1
    }

    def 'Performance test 20 parallel read data job classifications'() {
        when: 'sending 20 parallel read jobs, execute 3 times with delay'
            def batchResult1 = executeParallelReadJobs(PARALLEL_REQUESTS)
            Thread.sleep(500)
            def batchResult2 = executeParallelReadJobs(PARALLEL_REQUESTS)
            Thread.sleep(500)
            def batchResult3 = executeParallelReadJobs(PARALLEL_REQUESTS)
        then: 'record execution times'
            logBatchResults('Parallel run 1', batchResult1)
            logBatchResults('Parallel run 2', batchResult2)
            logBatchResults('Parallel run 3', batchResult3)
        and: 'each batch of 20 jobs completes under 1 second'
            assert batchResult1.executionTime < 1
            assert batchResult2.executionTime < 1
            assert batchResult3.executionTime < 1
    }

    def registerTestCmHandles(numberOfCmHandles) {
        registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(
                DMI1_URL, 'tagA', numberOfCmHandles, NETWORK_OFFSET, ModuleNameStrategy.UNIQUE,
                { "/SubNetwork=Europe/SubNetwork=Ireland/MeContext=PerfRadioNode${it}/ManagedElement=PerfME${it}" }
        )
    }

    def registerReadyCmHandles(dmiUrl, dmiDispatcher, dmiPrefix, count) {
        def cmHandles = []
        def moduleReferences = (1..200).collect { 'tagAModule' + it }
        (1..count).each {
            def cmHandleId = 'ch-' + dmiPrefix + '-' + it
            def alternateId = '/SubNetwork=Europe/SubNetwork=Ireland/MeContext=' + dmiPrefix + 'Node' + it + '/ManagedElement=' + dmiPrefix + 'ME' + it
            def cmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, moduleSetTag: 'tagA', alternateId: alternateId, dataProducerIdentifier: dmiPrefix)
            cmHandles.add(cmHandle)
            dmiDispatcher.moduleNamesPerCmHandleId[cmHandleId] = moduleReferences
        }
        networkCmProxyInventoryFacade.updateDmiRegistration(new DmiPluginRegistration(dmiPlugin: dmiUrl, createdCmHandles: cmHandles))
        moduleSyncWatchdog.moduleSyncAdvisedCmHandles()
    }

    def createReadRequest(jobId) {
        // 10 selectors reflecting production characteristics (99% broadcast):
        //   9 broadcast:    deep search and SubNetwork selectors without specific ManagedElement/MeContext id
        //   1 dmiSelectors: exact id selector matching a READY CM handle on DMI1
        def selectors = [
            '//ManagedElement',
            '//ManagedElement/SomeChild',
            '//MeContext',
            '/SubNetwork[id="Europe"]',
            '/SubNetwork[id="Europe"]/SubNetwork[id="Ireland"]',
            '/SubNetwork[id="Europe"]/SubNetwork[id="Ireland"]/ManagedElement',
            '/SubNetwork[id="Asia"]',
            '/SubNetwork[id="Europe"]/SubNetwork[id="Ireland"]/MeContext',
            '//ManagedElement/attributes',
            '/SubNetwork[id="Europe"]/SubNetwork[id="Ireland"]/MeContext[id="dmi1Node1"]/ManagedElement[id="dmi1ME1"]',
        ].join('\n')
        return new DataJobReadRequest('perf-test', jobId, 'performance test',
                new ReadProperties(selectors, 'JSON'), [:])
    }

    def executeParallelReadJobs(numberOfJobs) {
        def localMeter = new ResourceMeter()
        def executorService = Executors.newFixedThreadPool(numberOfJobs)
        localMeter.start()
        def futures = (1..numberOfJobs).collect { jobId ->
            CompletableFuture.supplyAsync({ -> dataJobService.readDataJob(createReadRequest('job-' + jobId)) }, executorService)
        }
        futures.each { it.join() }
        localMeter.stop()
        executorService.shutdown()
        [
            'executionTime': localMeter.totalTimeInSeconds,
            'memoryUsage'  : localMeter.totalMemoryUsageInMB,
            'jobCount'     : numberOfJobs
        ]
    }

    def executeReadJob(dataJobReadRequest) {
        def localMeter = new ResourceMeter()
        localMeter.start()
        def result = dataJobService.readDataJob(dataJobReadRequest)
        localMeter.stop()
        def dmiSelectorCount = result.dmiSelectors().values().collectMany { it.values() }.flatten().size()
        [
            'executionTime'    : localMeter.totalTimeInSeconds,
            'memoryUsage'      : localMeter.totalMemoryUsageInMB,
            'broadcast'        : result.broadcastSelectors().size(),
            'dmiGroups'        : result.dmiSelectors().size(),
            'dmiSelectorCount' : dmiSelectorCount,
            'notReady'         : result.notReadySelectors().size(),
            'errors'           : result.errorSelectors().size()
        ]
    }

    def logExecutionResults(label, result) {
        println "*** ReadJob ${label}: ${result.executionTime}s | ${result.memoryUsage}MB | broadcast:${result.broadcast} dmi:${result.dmiGroups}(${result.dmiSelectorCount} selectors) notReady:${result.notReady} errors:${result.errors}"
        return true
    }

    def logBatchResults(label, result) {
        println "*** ReadJob ${label}: ${result.executionTime}s | ${result.memoryUsage}MB | ${result.jobCount} parallel jobs"
        return true
    }
}
