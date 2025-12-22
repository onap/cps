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
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.impl.provmns.http

import io.micrometer.common.KeyValues
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientRequestObservationContext
import spock.lang.Specification

@SpringBootTest(classes = [ClientRequestMetricsTagCustomizer])
@TestPropertySource(properties = ["rest.api.provmns-base-path=/ProvMnS"])
class ClientRequestMetricsTagCustomizerSpec extends Specification {

    @Autowired
    ClientRequestMetricsTagCustomizer objectUnderTest

    def 'Customize metrics via client request observation'() {
        given: 'a request to a network device'
            def context = Mock(ClientRequestObservationContext)
            context.getUriTemplate() >> 'http://some-service/ProvMnS/v1/parent=A/child=E'
            context.getCarrier() >> Mock(ClientRequest)

        when: 'metrics are collected for this request'
            def result = objectUnderTest.getLowCardinalityKeyValues(context)

        then: 'the device-specific URL is masked to prevent too many metrics'
            def additionalTags = objectUnderTest.additionalTags(context)
            additionalTags.stream().anyMatch { it.key == 'uri' && it.value == '/ProvMnS/v1/{fdn}' }
    }

    def 'Mask URIs for ProvMns : #scenario'() {
        given: 'a request to a network device'
            def context = Mock(ClientRequestObservationContext)
            context.getUriTemplate() >> inputUri

        when: 'the URL is processed for metrics'
            def result = objectUnderTest.additionalTags(context)

        then: 'device-specific parts are replaced with a template'
            if (expectedUri) {
                result.stream().anyMatch { keyValue ->
                    keyValue.key == 'uri' && keyValue.value == expectedUri
                }
            } else {
                result == KeyValues.empty()
            }

        where:
            scenario          | inputUri                                                             | expectedUri
            'device path'     | 'http://some-service/ProvMnS/v1/parent=A/child=E'                    | '/ProvMnS/v1/{fdn}'
            'with filters'    | 'http://some-service/ProvMnS/v1/parent=A/child=E?filter1=1&filter2=2'| '/ProvMnS/v1/{fdn}?filter1=1&filter2=2'
            'filters only'    | 'http://some-service/ProvMnS/v1/?filter1=1'                          | '/ProvMnS/v1/{fdn}?filter1=1'
            'non-matching URI'| 'http://some-service/other-api/v1/resource'                          | null
    }
}