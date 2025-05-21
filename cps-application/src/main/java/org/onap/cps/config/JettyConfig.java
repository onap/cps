/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.config;

import java.util.EnumSet;
import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

/**
 * Configures the Jetty server to allow encoded slashes (%2F) within URI path segments.

 * This customization is essential when path parameters may include encoded slashes,
 * such as hierarchical identifiers (e.g., {@code SubNetwork=Europe/SubNetwork=Ireland}).
 * By permitting the {@code AMBIGUOUS_PATH_SEPARATOR} violation, Jetty accepts these
 * encoded slashes without rejecting the request.
 *
 * @see <a href="https://jetty.org/docs/jetty/12/programming-guide/server/compliance.html">Jetty Server Compliance Modes</a>
 * @see <a href="https://javadoc.jetty.org/jetty-12/org/eclipse/jetty/http/UriCompliance.Violation.html">UriCompliance.Violation</a>
 */
@Component
public class JettyConfig implements WebServerFactoryCustomizer<JettyServletWebServerFactory> {

    /**
     * Customizes the Jetty server factory to allow encoded slashes in URI paths.
     *
     * @param jettyServletWebServerFactory the Jetty servlet web server factory to customize
     */
    @Override
    public void customize(final JettyServletWebServerFactory jettyServletWebServerFactory) {
        jettyServletWebServerFactory.addServerCustomizers(server -> {
            for (final Connector connector : server.getConnectors()) {
                for (final ConnectionFactory connectionFactory : connector.getConnectionFactories()) {
                    if (connectionFactory instanceof HttpConnectionFactory) {
                        final HttpConfiguration httpConfiguration
                                = ((HttpConnectionFactory) connectionFactory).getHttpConfiguration();
                        httpConfiguration.setUriCompliance(UriCompliance.from(EnumSet.of(
                                UriCompliance.Violation.AMBIGUOUS_PATH_SEPARATOR)));
                    }
                }
            }
        });
    }
}

