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

package org.onap.cps.ncmp.impl.datajobs

import org.onap.cps.ncmp.impl.dmi.DmiProperties
import org.onap.cps.ncmp.impl.dmi.DmiRestClient
import org.onap.cps.ncmp.impl.dmi.UrlTemplateParameters
import reactor.core.publisher.Mono
import spock.lang.Specification

class DmiSubJobStatusRequestHandlerSpec extends Specification {

    def mockDmiRestClient = Mock(DmiRestClient)
    def mockDmiProperties = Mock(DmiProperties)
    def objectUnderTest = new DmiSubJobStatusRequestHandler(mockDmiRestClient, mockDmiProperties)

    def NO_AUTH = null

    def setup() {
        mockDmiProperties.dmiBasePath >> 'dmi'
    }

    def 'Get the status of a data job.' () {
        given: 'the required parameters to get the status of the data job'
            def dmiServiceName = 'some-dmi-service'
            def requestId = 'some-request-id'
            def dataProducerJobId = 'some-data-producer-job-id'
            def dataJobId = 'some-data-job-id'
            def urlParams = new UrlTemplateParameters('some-dmi-service/dmi/v1/dataJob/{requestId}/dataProducerJob/{dataProducerJobId}/status?dataProducerId={dataProducerId}', ['dataProducerJobId':'some-data-producer-job-id', 'dataProducerId':'some-data-job-id', 'requestId':'some-request-id'])
            mockDmiRestClient.getDataJobStatus(urlParams, NO_AUTH) >> Mono.just('some status')
        when: 'the request made towards DMI'
            def response = objectUnderTest.getDataJobStatusFromDmi(dmiServiceName, requestId, dataProducerJobId, dataJobId)
        then: 'result contains the expected status'
            assert response != null
            assert  response == 'some status'
    }

}
