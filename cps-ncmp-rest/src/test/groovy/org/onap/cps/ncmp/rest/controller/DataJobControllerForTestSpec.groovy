/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.rest.controller

import org.onap.cps.ncmp.api.datajobs.DataJobService
import org.onap.cps.ncmp.api.datajobs.models.DataJobMetadata
import org.onap.cps.ncmp.api.datajobs.models.DataJobRequest
import org.onap.cps.ncmp.api.datajobs.models.DataJobWriteRequest
import org.onap.cps.ncmp.api.datajobs.models.WriteOperation
import org.springframework.http.HttpStatus
import spock.lang.Specification

class DataJobControllerForTestSpec extends Specification {

    DataJobService mockDataJobService = Mock()

    def objectUnderTest = new DataJobControllerForTest(mockDataJobService)

    def 'Write Data Job request'() {
        given: 'a valid datajob write request'
            def dataJobMetadata = new DataJobMetadata('some destination', 'some accept type', 'some content type', 'some job execution policy')
            def writeOperations = [ new WriteOperation('/path/to/node', 'create', 'op123', 'value1') ]
            def dataJobWriteRequest = new DataJobWriteRequest(writeOperations)
            def dataJobRequest = new DataJobRequest(dataJobMetadata, dataJobWriteRequest)
        when: 'write data job is called'
            def result = objectUnderTest.writeDataJob('my authorization', 'my job', dataJobRequest)
        then: 'response is 200 OK'
            assert result.statusCode == HttpStatus.OK
        and: 'the service method is called once with expected parameters'
            1 * mockDataJobService.writeDataJob('my authorization', 'my job', dataJobMetadata, dataJobWriteRequest)
    }
}
