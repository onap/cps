/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Deutsche Telekom AG
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
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.MDC
import spock.lang.Specification

class MdcServletFilterSpec extends Specification {
    def mockFilterChain = Mock(FilterChain)
    def mockHttpServletRequest = Mock(HttpServletRequest)
    def mockServletResponse = Mock(ServletResponse)

    def objectUnderTest = new MdcServletFilter()

    def 'Filter chain and Mapped Diagnostic Context (MDC).'() {
        given:'the mocked request returns a value for any header'
               mockHttpServletRequest.getHeader(_) >> { args -> { args[0]+' TEST VALUE'} }
        when: 'HTTP request is filtered'
               objectUnderTest.doFilter(mockHttpServletRequest, mockServletResponse, mockFilterChain)
        then: 'the filter chain is called once MDC only has the expected headers at that moment in time'
               1 * mockFilterChain.doFilter(mockHttpServletRequest, mockServletResponse) >> {
             assert MDC.get('X-CPS-Request-Id') == 'X-CPS-Request-Id TEST VALUE'
             assert MDC.get('X-CPS-Client-Id') == 'X-CPS-Client-Id TEST VALUE'
             assert MDC.get('Other Header') == null
        }
        and: 'MDC is cleaned up after filter chain execution'
              assert MDC.get('X-CPS-Request-Id') == null
              assert MDC.get('X-CPS-Client-Id') == null
    }
}