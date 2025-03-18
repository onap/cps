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

    @Autowired
    DataJobService dataJobService

    def populateDataJobWriteRequests(int numberOfWriteOperations) {
        def writeOperations = []
        for (int i = 1; i <= numberOfWriteOperations; i++) {
            def basePath = "/SubNetwork=Europe/SubNetwork=Ireland/MeContext=MyRadioNode${i}/ManagedElement=MyManagedElement${i}"
            writeOperations.add(new WriteOperation("${basePath}/SomeChild=child-1", 'operation1', '1', null))
            writeOperations.add(new WriteOperation("${basePath}/SomeChild=child-2", 'operation2', '2', null))
            writeOperations.add(new WriteOperation(basePath, 'operation3', '3', null))
        }
        return new DataJobWriteRequest(writeOperations)
    }

    @Ignore  // CPS-2691
    def 'Performance test for writeDataJob method'() {
        given: 'register 10_000 cm handles (with alternative ids)'
            registerTestCmHandles(10_000)
            def dataJobWriteRequest = populateDataJobWriteRequests(10_000)
        when: 'sending a write job to NCMP with dynamically generated write operations'
            def executionResult = executeWriteJob('d1', dataJobWriteRequest)
        then: 'record the result. Not asserted, just recorded in See https://lf-onap.atlassian.net/browse/CPS-2691'
            println "*** CPS-2691 Execution time: ${executionResult.executionTime} seconds | Memory usage: ${executionResult.memoryUsage} MB"
        cleanup: 'deregister test cm handles'
            deregisterTestCmHandles(10_000)
    }

    @Ignore  // CPS-2692
    def 'Performance test for writeDataJob method with 10 parallel requests'() {
        given: 'register 10_000 cm handles (with alternative ids)'
            registerTestCmHandles(1_000)
        when: 'sending 10 parallel write jobs to NCMP'
            def executionResults = executeParallelWriteJobs(10, 1_000)
        then: 'record execution times'
            executionResults.eachWithIndex { result, index ->
                logExecutionResults("CPS-2691 Job-${index + 1}", result)
            }
        cleanup: 'deregister test cm handles'
            deregisterSequenceOfCmHandles(DMI1_URL, 1_000, 1)
    }

    def registerTestCmHandles(numberOfCmHandles) {
        registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(
                DMI1_URL, "tagA", numberOfCmHandles, 1, ModuleNameStrategy.UNIQUE,
                { "/SubNetwork=Europe/SubNetwork=Ireland/MeContext=MyRadioNode${it}/ManagedElement=MyManagedElement${it}" }
        )
    }

    def executeParallelWriteJobs(numberOfJobs, numberOfWriteOperations) {
        def executorService = Executors.newFixedThreadPool(numberOfJobs)
        def futures = (0..<numberOfJobs).collect { jobId ->
            CompletableFuture.supplyAsync({ -> executeWriteJob(jobId, populateDataJobWriteRequests(numberOfWriteOperations)) }, executorService)
        }
        def executionResults = futures.collect { it.join() }
        executorService.shutdown()
        return executionResults
    }

    def executeWriteJob(jobId, dataJobWriteRequest) {
        def localMeter = new ResourceMeter()
        localMeter.start()
        dataJobService.writeDataJob('', '', new DataJobMetadata("job-${jobId}", '', ''), dataJobWriteRequest)
        localMeter.stop()
        ['executionTime': localMeter.totalTimeInSeconds, 'memoryUsage': localMeter.totalMemoryUsageInMB]
    }

    def logExecutionResults(jobId, result) {
        println "*** ${jobId} Execution time: ${result.executionTime} seconds | Memory usage: ${result.memoryUsage} MB"
    }

    def deregisterTestCmHandles(numberOfCmHandles) {
        deregisterSequenceOfCmHandles(DMI1_URL, numberOfCmHandles, 1)
    }
}
