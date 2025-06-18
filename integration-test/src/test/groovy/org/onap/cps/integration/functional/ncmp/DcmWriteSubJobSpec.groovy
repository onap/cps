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

package org.onap.cps.integration.functional.ncmp

import groovy.json.JsonSlurper
import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.springframework.http.MediaType

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class DcmWriteSubJobSpec extends CpsIntegrationSpecBase {

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

    def 'Create write data job and validate sub-job routing'() {
        given: 'a valid writeDataJob JSON request containing paths for multiple CM handles'
            def writeJobRequest = """
            {
                "dataJobMetadata": {
                "destination": "d1",
                "dataAcceptType": "some-acceptance-type",
                "dataContentType": "some-content-type"
            },
                "dataJobWriteRequest": {
                    "data": [
                        {
                            "path": "p1",
                            "op": "add",
                            "operationId": "some-op-id",
                            "value": { "key": "some-value" }
                        },
                        {
                            "path": "p2",
                            "op": "add",
                            "operationId": "some-op-id",
                            "value": { "key": "some-value" }
                        },
                        {
                            "path": "p3",
                            "op": "add",
                            "operationId": "some-op-id",
                            "value": { "key": "some-value" }
                        }
                    ]
                }
            }
            """
        when: 'a POST request is made to the write job test endpoint'
            def mvcResult = mvc.perform(
                    post('/do-not-use/dataJobs/some-job-id/write')
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(writeJobRequest)
            ).andExpect(status().is2xxSuccessful())
            .andReturn()
        then: 'the sub jobs contains 2 sub-jobs, grouped by their DMI plugin'
            def subJobs = parseJsonResponse(mvcResult.response.contentAsString)
            assert subJobs.size() == 2
            subJobs.every {
                assert it.subJobId == 'some sub job id'
                assert it.dataProducerId == 'some data producer id'
                assert (it.dmiServiceName.startsWith('http://kubernetes') || it.dmiServiceName.startsWith('http://localhost'))
            }
        and: 'DMI 1 received the correct job details'
            def dmi1SubJobs = dmiDispatcher1.receivedSubJobs['?destination=d1']?.data
            assert dmi1SubJobs.size() == 2
            def dmi1Paths = dmi1SubJobs*.path
            assert dmi1Paths.containsAll(['p1', 'p2'])
        and: 'DMI 2 received the correct job details'
            def dmi2SubJobs = dmiDispatcher2.receivedSubJobs['?destination=d1']?.data
            assert dmi2SubJobs.size() == 1
            assert dmi2SubJobs[0].path == 'p3'
    }

    def parseJsonResponse(jsonResponse) {
        new JsonSlurper().parseText(jsonResponse) as List<Map<String, Object>>
    }
}
