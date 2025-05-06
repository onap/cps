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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class MDCServletFilter implements jakarta.servlet.Filter {

    private static final Logger LOG = LoggerFactory.getLogger("MDCServletFilter");

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        String requestId = httpRequest.getHeader("Request-Id");
        String clientId = httpRequest.getHeader("Client-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        if (clientId == null || clientId.isBlank()) {
            clientId = UUID.randomUUID().toString();
        }

        MDC.put("requestId", requestId);
        MDC.put("clientId", clientId);
        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            MDC.clear();
        }
    }
}
