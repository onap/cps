/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 Nordix Foundation
 *  Modifications Copyright (C) 2022 Bell Canada
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

package org.onap.cps.ncmp.impl.dmi

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.http.HttpHeaders
import spock.lang.Specification

class DmiRestClientSpec extends Specification {

    static NO_AUTH_HEADER = null
    static BASIC_AUTH_HEADER = 'Basic c29tZSB1c2VyOnNvbWUgcGFzc3dvcmQ='
    static BEARER_AUTH_HEADER = 'Bearer my-bearer-token'

    def mockDmiServiceAuthenticationProperties = Mock(DmiServiceAuthenticationProperties)

    JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

    DmiRestClient objectUnderTest = new DmiRestClient(mockDmiServiceAuthenticationProperties, jsonObjectMapper, null, null, null)

    def 'DMI auth header #scenario.'() {
        when: 'Specific dmi properties are provided'
            mockDmiServiceAuthenticationProperties.dmiBasicAuthEnabled >> authEnabled
            mockDmiServiceAuthenticationProperties.authUsername >> 'some user'
            mockDmiServiceAuthenticationProperties.authPassword >> 'some password'
        then: 'http headers to conditionally have Authorization header'
            def httpHeaders = new HttpHeaders()
            objectUnderTest.configureHttpHeaders(httpHeaders, ncmpAuthHeader)
            def outputAuthHeader = (httpHeaders.Authorization == null ? null : httpHeaders.Authorization[0])
            assert outputAuthHeader == expectedAuthHeader
        where: 'the following configurations are used'
            scenario                                          | authEnabled | ncmpAuthHeader     || expectedAuthHeader
            'DMI basic auth enabled, no NCMP bearer token'    | true        | NO_AUTH_HEADER     || BASIC_AUTH_HEADER
            'DMI basic auth enabled, with NCMP bearer token'  | true        | BEARER_AUTH_HEADER || BASIC_AUTH_HEADER
            'DMI basic auth disabled, no NCMP bearer token'   | false       | NO_AUTH_HEADER     || NO_AUTH_HEADER
            'DMI basic auth disabled, with NCMP bearer token' | false       | BEARER_AUTH_HEADER || BEARER_AUTH_HEADER
            'DMI basic auth disabled, with NCMP basic auth'   | false       | BASIC_AUTH_HEADER  || NO_AUTH_HEADER
    }
}
