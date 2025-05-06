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

package org.onap.cps.rest.utils;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class MdcServletFilter implements Filter {

    private static final String ONAP_REQUEST_ID_HEADER = "X-ONAP-RequestID";
    private static final String ONAP_CLEINT_ID_HEADER = "X-ClientID";

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
                         final FilterChain filterChain) throws IOException, ServletException {
        final HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        final String requestId = httpRequest.getHeader(ONAP_REQUEST_ID_HEADER);
        final String clientId = httpRequest.getHeader(ONAP_CLEINT_ID_HEADER);

        MDC.put(ONAP_REQUEST_ID_HEADER, requestId);
        MDC.put(ONAP_CLEINT_ID_HEADER, clientId);
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            MDC.remove(ONAP_REQUEST_ID_HEADER);
            MDC.remove(ONAP_CLEINT_ID_HEADER);
        }
    }
}
