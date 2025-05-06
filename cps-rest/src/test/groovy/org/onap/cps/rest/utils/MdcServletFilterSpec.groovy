/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 TechMahindra Ltd.
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

package org.onap.cps.rest.utils

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.MDC
import spock.lang.Specification

class MdcServletFilterSpec extends Specification {
    def mdcServletFilter = new MdcServletFilter()
    def mockHttpServletRequest = Mock(HttpServletRequest)
    def mockServletResponse = Mock(ServletResponse)
    def mockFilterChain = Mock(FilterChain)

    def 'Should set Mapped Diagnostic Context from header if present'() {
        given: 'Request ID and client ID present in request header'
             mockHttpServletRequest.getHeader('X-ONAP-RequestID') >> "test-request-id"
             mockHttpServletRequest.getHeader('X-ClientID') >> "test-client-id"

        when: 'HTTP request is filtered and Mapped Diagnostic Context set the requestID and clientID'
            mdcServletFilter.doFilter(mockHttpServletRequest, mockServletResponse, mockFilterChain)

        then: 'RequestID and clientID set in MDC'
            mockFilterChain.doFilter(mockHttpServletRequest, mockServletResponse) >> {
            assert MDC.get("X-ONAP-RequestID") == "test-request-id"
            assert MDC.get("X-ClientID") == "test-client-id"
        }
    }
}


