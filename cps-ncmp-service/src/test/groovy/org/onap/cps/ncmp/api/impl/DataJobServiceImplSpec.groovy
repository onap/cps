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

import org.onap.cps.ncmp.api.models.datajob.DataJobRequest
import org.onap.cps.ncmp.api.models.datajob.OutputParameters
import org.onap.cps.ncmp.api.models.datajob.ReadOperation
import org.onap.cps.ncmp.api.models.datajob.ScopeType
import org.onap.cps.ncmp.api.models.datajob.WriteOperation
import spock.lang.Specification

class DataJobServiceImplSpec extends Specification{

    def objectUnderTest = new DataJobServiceImpl()

    def 'Read data job #operation request.'() {
        given: 'list of read operations'
        def outputParameters = new OutputParameters(destination: 'client-topic', dataAcceptType: ' application/vnd.3gpp.object-tree-hierarchical+json', dataContentType: 'application/3gpp-json-patch+json')
        def dataJobRequest = new DataJobRequest(List.of(getWriteOrReadOperationRequest(operation)))
        when: 'data job request is processed'
        def response = objectUnderTest.readDataJob(outputParameters, operation + '_some-job-id', dataJobRequest)
        then: 'the job id is correctly populated for response'
        assert response.jobId() == operation+'_some-job-id'
        where: 'the following operation are used'
        operation << ['read']
    }

    def 'Write data job #operation request.'() {
        given: 'list of write operations'
        def outputParameters = new OutputParameters(destination: 'client-topic', dataAcceptType: ' application/vnd.3gpp.object-tree-hierarchical+json', dataContentType: 'application/3gpp-json-patch+json')
        def dataJobRequest = new DataJobRequest(List.of(getWriteOrReadOperationRequest(operation)))
        Map<String, String> metaData = new HashMap<String, String>()
        when: 'data job request is processed'
        def response = objectUnderTest.writeDataJob(outputParameters, operation + '_some-job-id', dataJobRequest, metaData)
        then: 'the job id is correctly populated for response'
        assert response.jobId() == operation+'_some-job-id'
        where: 'the following operation are used'
        operation << ['write']
    }

    def getWriteOrReadOperationRequest(operation) {
        if (operation == 'write') {
            return new WriteOperation(path: 'some/write/path', op: 'add', operationId: 1, value: 'some-value')
        }
        return new ReadOperation(path: 'some/read/path', op: 'read', operationId: 2, attributes: ['some-attrib-1', 'some-attrib-2'], fields: ['some-field-1', 'some-field-2'], scopeType: ScopeType.BASE_NTH_LEVEL, scopeLevel: 4)
    }
}
