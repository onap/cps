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

import org.onap.cps.ncmp.impl.dmi.DmiRestClient
import org.onap.cps.ncmp.impl.dmi.DmiServiceAuthenticationProperties
import org.onap.cps.ncmp.impl.utils.http.UrlTemplateParameters
import reactor.core.publisher.Mono
import spock.lang.Specification

class DataJobStatusServiceImplSpec extends Specification {

    def mockDmiRestClient = Mock(DmiRestClient)
    def mockDmiServiceAuthenticationProperties = Mock(DmiServiceAuthenticationProperties)
    def objectUnderTest = new DataJobStatusServiceImpl(mockDmiRestClient, mockDmiServiceAuthenticationProperties)

    def setup() {
        mockDmiServiceAuthenticationProperties.dmiBasePath >> 'dmi'
    }

    def 'Forward a data job status query to DMI.' () {
        given: 'the required parameters for querying'
            def dmiServiceName = 'some-dmi-service'
            def dataProducerId = 'some-data-producer-id'
            def dataProducerJobId = 'some-data-producer-job-id'
            def authorization = 'my authorization header'
            def urlParams = new UrlTemplateParameters('some-dmi-service/dmi/v1/cmwriteJob/dataProducer/{dataProducerId}/dataProducerJob/{dataProducerJobId}/status', ['dataProducerId':'some-data-producer-id', 'dataProducerJobId':'some-data-producer-job-id'])
        and: 'the rest client returns a status for the given parameters'
            mockDmiRestClient.asynchronousDmiDataRequest(urlParams, authorization) >> Mono.just('some status')
        when: 'the job status is queried'
            def status = objectUnderTest.getDataJobStatus(authorization, dmiServiceName, dataProducerId, dataProducerJobId)
        then: 'the status from the rest client is returned'
            assert status == 'some status'
    }
}
