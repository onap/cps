/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl

import org.onap.cps.ncmp.api.models.datajob.DataJobReadRequest
import org.onap.cps.ncmp.api.models.datajob.DataJobWriteRequest
import org.onap.cps.ncmp.api.models.datajob.OutputParameters
import org.onap.cps.ncmp.api.models.datajob.ReadOperation
import org.onap.cps.ncmp.api.models.datajob.WriteOperation
import spock.lang.Specification

class DataJobServiceImplSpec extends Specification{

    def objectUnderTest = new DataJobServiceImpl()

    def 'Read data job #operation request.'() {
        given: 'list of read operations'
            def outputParameters = new OutputParameters('client-topic', 'application/vnd.3gpp.object-tree-hierarchical+json', 'application/3gpp-json-patch+json')
            def dataJobReadRequest = new DataJobReadRequest([getWriteOrReadOperationRequest('read')])
        when: 'data job request is processed'
            def response = objectUnderTest.readDataJob(outputParameters, 'some-job-id', dataJobReadRequest)
        then: 'the job id is correctly populated for response'
            assert response.jobId() == 'some-job-id'
    }

    def 'Write data job #operation request.'() {
        given: 'list of write operations'
            def outputParameters = new OutputParameters('client-topic', 'application/vnd.3gpp.object-tree-hierarchical+json', 'application/3gpp-json-patch+json')
            def dataJobWriteRequest = new DataJobWriteRequest([getWriteOrReadOperationRequest('write')])
        when: 'data job request is processed'
            def response = objectUnderTest.writeDataJob(outputParameters, 'some-job-id', dataJobWriteRequest, [:])
        then: 'the job id is correctly populated for response'
            assert response.jobId() == 'some-job-id'
    }

    def getWriteOrReadOperationRequest(operation) {
        if (operation == 'write') {
            return new WriteOperation('some/write/path', 'add', 'some-operation-id', 'some-value')
        }
        return new ReadOperation('some/read/path', 'read', 'some-operation-id', ['some-attrib-1'], ['some-field-1'], 'some-scope-type', 1)
    }
}
