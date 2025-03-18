/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Nordix Foundation
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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.stream.Collectors

/**
 * This test does not depend on common performance test data. Hence it just extends the integration spec base.
 */
class WriteDataJobPerfTest extends CpsIntegrationSpecBase {

    @Autowired
    DataJobService dataJobService

    def resourceMeter = new ResourceMeter()

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
            registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(DMI1_URL, 'tagA', 10_000, 1, ModuleNameStrategy.UNIQUE,
                { it -> "/SubNetwork=Europe/SubNetwork=Ireland/MeContext=MyRadioNode${it}/ManagedElement=MyManagedElement${it}" })
            def dataJobWriteRequest = populateDataJobWriteRequests(10_000)
        when: 'sending a write job to NCMP with dynamically generated write operations'
            resourceMeter.start()
            dataJobService.writeDataJob('', '', new DataJobMetadata('d1', '', ''), dataJobWriteRequest)
            resourceMeter.stop()
        then: 'record the result. Not asserted, just recorded in See https://lf-onap.atlassian.net/browse/CPS-2691'
            println "*** CPS-2691 Execution time: ${resourceMeter.totalTimeInSeconds} seconds"
            println "*** CPS-2691 Memory usage: ${resourceMeter.totalMemoryUsageInMB} MB"
        cleanup: 'deregister test cm handles'
            deregisterSequenceOfCmHandles(DMI1_URL, 10_000, 1)
    }

     @Ignore  // CPS-2692
    def 'Performance test for writeDataJob method with 10 parallel requests'() {
        given: 'register 10_000 cm handles (with alternative ids)'
        registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(DMI1_URL, "tagA", 1_000, 1, ModuleNameStrategy.UNIQUE,
                { it -> "/SubNetwork=Europe/SubNetwork=Ireland/MeContext=MyRadioNode${it}/ManagedElement=MyManagedElement${it}" })

        when: 'sending 10 parallel write jobs to NCMP'
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        List<CompletableFuture<Map<String, Double>>> futures = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            int jobId = i;
            CompletableFuture<Map<String, Double>> future = CompletableFuture.supplyAsync(() -> {
                DataJobWriteRequest dataJobWriteRequest = populateDataJobWriteRequests(1_000);

                ResourceMeter localMeter = new ResourceMeter();
                localMeter.start();
                dataJobService.writeDataJob('', '', new DataJobMetadata("job-${jobId}", '', ''), dataJobWriteRequest);
                localMeter.stop();

                Map<String, Double> result = new HashMap<>();
                result.put('executionTime', localMeter.totalTimeInSeconds);
                result.put('memoryUsage', localMeter.totalMemoryUsageInMB);

                return result;
            }, executorService) as CompletableFuture<Map<String, Double>>;
            futures.add(future);
        }
        // Wait for all requests to complete and gather execution times
        List<Map<String, Double>> executionResults = futures.stream()
                .map(future -> (Map<String, Double>) future.join())
                .collect(Collectors.toList());
        then: 'record execution times'
        for (int i = 0; i < executionResults.size(); i++) {
            Map<String, Double> result = executionResults.get(i)
            println "*** CPS-2691 Job-${(i + 1)} Execution time: ${result.get('executionTime')} seconds"
            println "*** CPS-2691 Job-${(i + 1)} Execution time: ${result.get('memoryUsage')} MB"
        }
        cleanup: 'deregister test cm handles'
        executorService.shutdown()
        deregisterSequenceOfCmHandles(DMI1_URL, 1_000, 1)
    }
}
