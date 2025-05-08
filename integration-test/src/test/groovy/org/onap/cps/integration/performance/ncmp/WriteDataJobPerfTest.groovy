/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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
import org.onap.cps.ncmp.api.datajobs.models.DataJobMetadata
import org.onap.cps.ncmp.api.datajobs.models.DataJobWriteRequest
import org.onap.cps.ncmp.api.datajobs.models.WriteOperation
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Ignore
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

/**
 * This test does not depend on common performance test data. Hence it just extends the integration spec base.
 */
class WriteDataJobPerfTest extends CpsIntegrationSpecBase {

    def NETWORK_SIZE = 1_000  // Increase to 40_000 for more realistic tests!

    @Autowired
    DataJobService dataJobService

    def setup() {
        registerTestCmHandles(NETWORK_SIZE)
    }

    def cleanup() {
        deregisterSequenceOfCmHandles(DMI1_URL, NETWORK_SIZE, 1)
    }

    @Ignore  // CPS-2691 / CPS-2692
    def 'Performance test Large cm write data job.'() {
        given: 'a large cm write data job'
            def dataJobWriteRequest1 = populateDataJobWriteRequests(NETWORK_SIZE, 0)
            def dataJobWriteRequest2 = populateDataJobWriteRequests(NETWORK_SIZE, 0)
            def dataJobWriteRequest3 = populateDataJobWriteRequests(NETWORK_SIZE, 0)
        when: 'sending a write job to NCMP with dynamically generated write operations'
            def executionResult1 = executeWriteJob('d1', dataJobWriteRequest1)
            def executionResult2 = executeWriteJob('d1', dataJobWriteRequest2)
            def executionResult3 = executeWriteJob('d1', dataJobWriteRequest3)
        then: 'record the result (about 2-3 second). Not asserted, just recorded in See https://lf-onap.atlassian.net/browse/CPS-2691'
            println "*** CPS-2691 (L) Execution time: ${executionResult1.executionTime} seconds | Memory usage: ${executionResult1.memoryUsage} MB"
            println "*** CPS-2691 (L) Execution time: ${executionResult2.executionTime} seconds | Memory usage: ${executionResult2.memoryUsage} MB"
            println "*** CPS-2691 (L) Execution time: ${executionResult3.executionTime} seconds | Memory usage: ${executionResult3.memoryUsage} MB"
    }

    def 'Performance test Small cm write data job.'() {
        given: 'a small'
            def dataJobWriteRequest = populateDataJobWriteRequests(100, 0)
        when: 'sending a write job to NCMP with dynamically generated write operations'
            def executionResult = executeWriteJob('d1', dataJobWriteRequest)
        then: 'record the result (about 2-3 second). Not asserted, TO BE be recorded in https://lf-onap.atlassian.net/browse/CPS-2743'
            println "*** CPS-2691 (S) Execution time: ${executionResult.executionTime} seconds | Memory usage: ${executionResult.memoryUsage} MB"
    }

    @Ignore  // CPS-2692
    def 'Performance test parallel small cm write data jobs.'() {
        when: 'sending 10 parallel write jobs to NCMP'
            def executionResults1 = executeParallelWriteJobs(10, 100, 0)
            Thread.sleep(1000)
            def executionResults2 = executeParallelWriteJobs(10, 100, 200)
            Thread.sleep(1000)
            def executionResults3 = executeParallelWriteJobs(10, 100, 300)
        then: 'record execution times'
            executionResults1.eachWithIndex { result1, index1 ->
                logExecutionResults("CPS-2692 Job-${index1 + 1}", result1) }
            executionResults2.eachWithIndex { result2, index2 ->
                logExecutionResults("CPS-2692 Job-${index2 + 1}", result2) }
            executionResults3.eachWithIndex { result3, index3 ->
                logExecutionResults("CPS-2692 Job-${index3 + 1}", result3) }
    }

    def registerTestCmHandles(numberOfCmHandles) {
        registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(
                DMI1_URL, "tagA", numberOfCmHandles, 1, ModuleNameStrategy.UNIQUE,
                { "/SubNetwork=Europe/SubNetwork=Ireland/MeContext=MyRadioNode${it}/ManagedElement=MyManagedElement${it}" }
        )
    }

    def executeParallelWriteJobs(numberOfJobs, numberOfWriteOperations, offset) {
        def executorService = Executors.newFixedThreadPool(numberOfJobs)
        def futures = (0..<numberOfJobs).collect { jobId ->
            CompletableFuture.supplyAsync({ -> executeWriteJob(jobId, populateDataJobWriteRequests(numberOfWriteOperations, offset)) }, executorService)
        }
        def executionResults = futures.collect { it.join() }
        executorService.shutdown()
        return executionResults
    }

    def populateDataJobWriteRequests(numberOfWriteOperations, offset) {
        def writeOperations = []
        for (int i = 1; i <= numberOfWriteOperations; i++) {
            def basePath = "/SubNetwork=Europe/SubNetwork=Ireland/MeContext=MyRadioNode${offset + i}/ManagedElement=MyManagedElement${offset + i}"
            writeOperations.add(new WriteOperation("${basePath}/SomeChild=child-1", 'operation1', '1', null))
            writeOperations.add(new WriteOperation("${basePath}/SomeChild=child-2", 'operation2', '2', null))
            writeOperations.add(new WriteOperation(basePath, 'operation3', '3', null))
        }
        return new DataJobWriteRequest(writeOperations)
    }


    def executeWriteJob(jobId, dataJobWriteRequest) {
        def localMeter = new ResourceMeter()
        localMeter.start()
        1.times {
            dataJobService.writeDataJob('', '', new DataJobMetadata("job-${jobId}", '', ''), dataJobWriteRequest)
        }
        localMeter.stop()
        ['executionTime': localMeter.totalTimeInSeconds, 'memoryUsage': localMeter.totalMemoryUsageInMB]
    }

    def logExecutionResults(jobId, result) {
        println "*** ${jobId} Execution time: ${result.executionTime} seconds | Memory usage: ${result.memoryUsage} MB"
    }

}
