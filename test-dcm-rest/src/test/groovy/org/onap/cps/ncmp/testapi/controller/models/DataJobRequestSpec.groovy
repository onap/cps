/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.testapi.controller.models

import org.onap.cps.ncmp.api.datajobs.models.DataJobMetadata
import org.onap.cps.ncmp.api.datajobs.models.DataJobWriteRequest
import spock.lang.Specification

class DataJobRequestSpec extends Specification {

    def 'Create DataJobRequest'() {
        given: 'metadata and write request'
            def metadata = new DataJobMetadata('destination', 'accept', 'content')
            def writeRequest = new DataJobWriteRequest([])
        when: 'DataJobRequest is created'
            def result = new DataJobRequest(metadata, writeRequest)
        then: 'the record contains the correct values'
            result.dataJobMetadata() == metadata
            result.dataJobWriteRequest() == writeRequest
    }
}
