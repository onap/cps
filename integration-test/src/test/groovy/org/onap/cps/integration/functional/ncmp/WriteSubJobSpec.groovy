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

import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.api.datajobs.DataJobService
import org.onap.cps.ncmp.api.datajobs.models.DataJobMetadata
import org.onap.cps.ncmp.api.datajobs.models.DataJobWriteRequest
import org.onap.cps.ncmp.api.datajobs.models.SubJobWriteResponse
import org.onap.cps.ncmp.api.datajobs.models.WriteOperation
import org.springframework.beans.factory.annotation.Autowired

class WriteSubJobSpec extends CpsIntegrationSpecBase {

    @Autowired
    DataJobService dataJobService

    def setup() {
        dmiDispatcher1.moduleNamesPerCmHandleId['ch-1'] = ['M1']
        dmiDispatcher1.moduleNamesPerCmHandleId['ch-2'] = ['M2']
        dmiDispatcher2.moduleNamesPerCmHandleId['ch-3'] = ['M3']
        registerCmHandle(DMI1_URL, 'ch-1', NO_MODULE_SET_TAG, 'p1')
        registerCmHandle(DMI1_URL, 'ch-2', NO_MODULE_SET_TAG, 'p2')
        registerCmHandle(DMI2_URL, 'ch-3', NO_MODULE_SET_TAG, 'p3')
    }

    def cleanup() {
        deregisterCmHandle(DMI1_URL, 'ch-1')
        deregisterCmHandle(DMI1_URL, 'ch-2')
        deregisterCmHandle(DMI2_URL, 'ch-3')
    }

    def 'Create a sub-job write request.'() {
        given: 'the required input data for the write job'
            def authorization = 'my authorization header'
            def dataJobWriteRequest = new DataJobWriteRequest([new WriteOperation('p1', '', '', null), new WriteOperation('p2', '', '', null), new WriteOperation('p3', '', '', null)])
            def myDataJobMetadata = new DataJobMetadata('d1', '', '')
            def dataJobId = 'my-data-job-id'
        when: 'sending a write job to NCMP with 2 sub-jobs for DMI 1 and 1 sub-job for DMI 2'
            def response = dataJobService.writeDataJob(authorization, dataJobId, myDataJobMetadata, dataJobWriteRequest)
        then: 'each DMI received the expected sub-jobs and the response has the expected values'
            assert response.size() == 2
            assert response[0].class == SubJobWriteResponse.class
            assert response[0].subJobId == 'some sub job id'
            assert response[0].dmiServiceName.startsWith('http://localhost:')
            assert response[0].dataProducerId == 'some data producer id'
        and: 'dmi 1 received the correct job details'
            def receivedSubJobsForDispatcher1 = dmiDispatcher1.receivedSubJobs['?destination=d1']['data'].collect()
            assert receivedSubJobsForDispatcher1.size() == 2
            assert receivedSubJobsForDispatcher1[0]['path'] == 'p1'
            assert receivedSubJobsForDispatcher1[1]['path'] == 'p2'
        and: 'dmi 2 received the correct job details'
            def receivedSubJobsForDispatcher2 = dmiDispatcher2.receivedSubJobs['?destination=d1']['data'].collect()
            assert receivedSubJobsForDispatcher2.size() == 1
            assert receivedSubJobsForDispatcher2[0]['path'] == 'p3'
    }
}
