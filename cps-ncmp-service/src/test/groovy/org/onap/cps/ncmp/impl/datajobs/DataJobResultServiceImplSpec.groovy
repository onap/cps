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

package org.onap.cps.ncmp.impl.datajobs

import org.onap.cps.ncmp.impl.dmi.DmiRestClient
import org.onap.cps.ncmp.impl.dmi.DmiServiceAuthenticationProperties
import org.onap.cps.ncmp.impl.utils.http.UrlTemplateParameters
import reactor.core.publisher.Mono
import spock.lang.Specification

class DataJobResultServiceImplSpec extends Specification {

    def mockDmiRestClient = Mock(DmiRestClient)
    def mockDmiServiceAuthenticationProperties = Mock(DmiServiceAuthenticationProperties)
    def objectUnderTest = new DataJobResultServiceImpl(mockDmiRestClient, mockDmiServiceAuthenticationProperties)

    def setup() {
        mockDmiServiceAuthenticationProperties.dmiBasePath >> 'dmi'
    }

    def 'Retrieve data job result.'() {
        given: 'the required parameters for querying'
            def dmiServiceName = 'some-dmi-service'
            def dataProducerJobId = 'some-data-producer-job-id'
            def dataProducerId = 'some-data-producer-id'
            def authorization = 'my authorization header'
            def destination = 'some-destination'
            def urlParams = new UrlTemplateParameters('some-dmi-service/dmi/v1/cmwriteJob/dataProducer/{dataProducerId}/dataProducerJob/{dataProducerJobId}/result?destination={destination}', ['dataProducerJobId':'some-data-producer-job-id', 'dataProducerId':'some-data-producer-id', 'destination': 'some-destination'])
        and: 'the rest client returns the result for the given parameters'
            mockDmiRestClient.asynchronousDmiDataRequest(urlParams, authorization) >> Mono.just('some result')
        when: 'the job status is queried'
            def result = objectUnderTest.getDataJobResult(authorization, dmiServiceName,dataProducerId,  dataProducerJobId, destination)
        then: 'the result from the rest client is returned'
            assert result != null
            assert  result == 'some result'
    }
}
