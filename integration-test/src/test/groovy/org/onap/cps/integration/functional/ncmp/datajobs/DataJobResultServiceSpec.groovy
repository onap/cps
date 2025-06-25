/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.integration.functional.ncmp.datajobs

import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.api.datajobs.DataJobResultService
import org.springframework.beans.factory.annotation.Autowired

class DataJobResultServiceSpec extends CpsIntegrationSpecBase {

    @Autowired
    DataJobResultService dataJobResultService;

    def 'Get the status of a data job from DMI.'() {
        given: 'the required data about the data job'
            def authorization = 'my authorization header'
            def dmiServiceName = DMI1_URL
            def dataProducerId = 'some-data-producer-id'
            def dataProducerJobId = 'some-data-producer-job-id'
            def destination = 'some-destination'
        when: 'the data job status checked'
            def result = dataJobResultService.getDataJobResult(authorization, dmiServiceName, dataProducerId, dataProducerJobId, destination)
        then: 'the status is that defined in the mock service.'
            assert result == '{ "result": "some result"}'
    }
}
