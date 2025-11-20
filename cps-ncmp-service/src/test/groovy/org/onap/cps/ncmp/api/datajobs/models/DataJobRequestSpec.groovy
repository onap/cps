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

package org.onap.cps.ncmp.api.datajobs.models

import spock.lang.Specification

class DataJobRequestSpec extends Specification {

    def dataJobMetaData = new DataJobMetadata('some destination', 'some accept type', 'some content type')
    def writeOperation = new WriteOperation('some path', 'some operation', 'some id', 'some value')
    def dataJobWriteRequest = new DataJobWriteRequest([writeOperation])

    def objectUnderTest = new DataJobRequest(dataJobMetaData, dataJobWriteRequest)

    //TODO This class is only used for a test Controller. Maybe it can be removed, see https://lf-onap.atlassian.net/browse/CPS-3062
    def 'a Data Job Request.'() {
        expect: 'a data job request consisting out of meta data and a write request'
            assert objectUnderTest.dataJobMetadata == dataJobMetaData
            assert  objectUnderTest.dataJobWriteRequest == dataJobWriteRequest
    }
}
