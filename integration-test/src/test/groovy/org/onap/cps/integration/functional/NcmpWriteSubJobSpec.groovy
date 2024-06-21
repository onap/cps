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

package org.onap.cps.integration.functional

import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.api.datajobs.DataJobService
import org.onap.cps.ncmp.api.datajobs.models.DataJobMetadata
import org.onap.cps.ncmp.api.datajobs.models.DataJobWriteRequest
import org.onap.cps.ncmp.api.datajobs.models.SubJobWriteResponse
import org.onap.cps.ncmp.api.datajobs.models.WriteOperation
import org.springframework.beans.factory.annotation.Autowired

class NcmpWriteSubJobSpec extends CpsIntegrationSpecBase {

    @Autowired
    DataJobService dataJobService

    def setup() {
        dmiDispatcher1.moduleNamesPerCmHandleId['ch-1'] = ['M1']
        dmiDispatcher1.moduleNamesPerCmHandleId['ch-2'] = ['M2']
        dmiDispatcher2.moduleNamesPerCmHandleId['ch-3'] = ['M3']
        registerCmHandle(DMI1_URL, 'ch-1', NO_MODULE_SET_TAG, '/a/b')
        registerCmHandle(DMI1_URL, 'ch-2', NO_MODULE_SET_TAG, '/a/b/c')
        registerCmHandle(DMI2_URL, 'ch-3', NO_MODULE_SET_TAG, '/a/b/c/d')
    }

    def cleanup() {
        deregisterCmHandle(DMI1_URL, 'ch-1')
        deregisterCmHandle(DMI1_URL, 'ch-2')
        deregisterCmHandle(DMI2_URL, 'ch-3')
    }

    def 'Create a sub-job write request.'() {
        given: 'the required input data for the write job'
            def dataJobWriteRequest = new DataJobWriteRequest([new WriteOperation('/a/b', '', '', null), new WriteOperation('/a/b/c', '', '', null), new WriteOperation('/a/b/c/d', '', '', null)])
            def myDataJobMetadata = new DataJobMetadata('', '', '')
            def dataJobId = 'my-data-job-id'
        when: 'sending a write job to NCMP with 2 sub-jobs for DMI 1 and 1 subjob for DMI 2'
            def response = dataJobService.writeDataJob(dataJobId, myDataJobMetadata, dataJobWriteRequest)
        then: 'each DMI received the expected sub-jobs'
            assert response.size() == 2
            assert response[0].class == SubJobWriteResponse.class
        and: 'dmi 1 received the correct job details'
            def receivedSubJobsForDispatcher1 = dmiDispatcher1.receivedSubJobs['my-data-job-id']['data'].collect()
            assert receivedSubJobsForDispatcher1.size() == 2
            assert receivedSubJobsForDispatcher1[0]['path'] == '/a/b'
            assert receivedSubJobsForDispatcher1[1]['path'] == '/a/b/c'
        and: 'dmi 2 received the correct job details'
            def receivedSubJobsForDispatcher2 = dmiDispatcher2.receivedSubJobs['my-data-job-id']['data'].collect()
            assert receivedSubJobsForDispatcher2.size() == 1
            assert receivedSubJobsForDispatcher2[0]['path'] == '/a/b/c/d'
    }
}
