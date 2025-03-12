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

    def 'Performance test for writeDataJob method'() {
        given: 'register 1,000 cm handles (with alternative ids)'
        registerSequenceOfCmHandlesWithManyModuleReferencesButDoNotWaitForReady(DMI1_URL, 'tagA', 1000, 1, ModuleNameStrategy.UNIQUE, { it -> "/SubNetwork=Europe/SubNetwork=Ireland/MeContext=MyRadioNode${it}/ManagedElement=MyManagedElement${it}" })
            def authorization = 'my authorization header'
            def numberOfWriteOperations = 1000
            def dataJobWriteRequest = populateDataJobWriteRequests(numberOfWriteOperations)
            def myDataJobMetadata = new DataJobMetadata('d1', '', '')
            def dataJobId = 'my-data-job-id'
        when: 'sending a write job to NCMP with dynamically generated write operations'
            resourceMeter.start()
            dataJobService.writeDataJob(authorization, dataJobId, myDataJobMetadata, dataJobWriteRequest)
            resourceMeter.stop()
        then: 'record the result. Not asserted, just recorded in See https://lf-onap.atlassian.net/browse/CPS-2691'
            println "*** CPS-2691 Execution time: ${resourceMeter.totalTimeInSeconds} seconds"
        cleanup: 'deregister test cm handles'
            deregisterSequenceOfCmHandles(DMI1_URL, 1000, 1)
    }
}
