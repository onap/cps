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

    def 'should set MDC from header if present'(){
        given:
            def filter = new MdcServletFilter()
            def request = Mock(HttpServletRequest)
            def response = Mock(ServletResponse)
            def chain = Mock(FilterChain)
            request.getHeader("Request-Id") >> "test-request-id"
            request.getHeader("Client-Id") >> "test-client-id"
        when:
            filter.doFilter(request, response, chain)
        then:
            chain.doFilter(request, response)
            MDC.get("requestId") == null
            MDC.get("clientId") == null

    }

    def 'should generate UUIDs if Headers are missing'(){
        given:
            def filter = new MdcServletFilter()
            def request = Mock(HttpServletRequest)
            def response = Mock(ServletResponse)
            def chain = Mock(FilterChain)
            request.getHeader("Request-Id") >> null
            request.getHeader("Client-Id") >> ""
        when:
            filter.doFilter(request, response, chain)
        then:
            chain.doFilter(request, response)
            MDC.get("requestId") == null
            MDC.get("clientId") == null

    }
}
