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

package org.onap.cps.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.MDC
import spock.lang.Specification

class MdcServletFilterSpec extends Specification {
    def mockFilterChain = new MdcServletFilter()
    def mockHttpServletRequest = Mock(HttpServletRequest)
    def mockServletResponse = Mock(ServletResponse)
    def chainExecuted = false

    def 'should set Mapped Diagnostic Context from headers if present'() {
        given: 'RequestID and ClientID present in request header'
            mockHttpServletRequest.getHeader('X-CPS-Request-Id') >> 'test-request-id'
            mockHttpServletRequest.getHeader('X-CPS-Client-Id') >> 'test-client-id'
        and: 'Mapped Diagnostic Context is populated inside filterchain execution'
           FilterChain filterChain = new FilterChain() {
            @Override
            void doFilter(ServletRequest req, ServletResponse res) throws IOException, ServletException {
                assert MDC.get('X-CPS-Request-Id') == 'test-request-id'
                assert MDC.get('X-CPS-Client-Id') == 'test-client-id'
                chainExecuted = true
            }
        }
        when: 'HTTP request is filtered and Mapped Diagnostic Context set the requestID and clientID'
            mockFilterChain.doFilter(mockHttpServletRequest, mockServletResponse, filterChain)
        then: 'RequestID and ClientID set in MDC'
        chainExecuted
           MDC.get('X-CPS-Request-Id') == null
           MDC.get('X-CPS-Client-Id') == null
    }
}