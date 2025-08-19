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
package org.onap.cps.logging;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class MdcServletFilter implements Filter {

    private static final String REQUEST_ID_HEADER = "X-CPS-Request-Id";
    private static final String CLIENT_ID_HEADER = "X-CPS-Client-Id";

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
                         final FilterChain filterChain) throws IOException, ServletException {
        final HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        putToMdc(REQUEST_ID_HEADER, httpServletRequest);
        putToMdc(CLIENT_ID_HEADER, httpServletRequest);
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            MDC.remove(REQUEST_ID_HEADER);
            MDC.remove(CLIENT_ID_HEADER);
        }
    }

    private void putToMdc(final String header, final HttpServletRequest httpServletRequest) {
        final String value = httpServletRequest.getHeader(header);
        MDC.put(header, value);
    }
}