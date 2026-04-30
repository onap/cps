/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.integration;

import java.util.EnumSet;
import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

/**
 * Configures Jetty to allow encoded slashes (%2F) in URI path segments for integration tests.
 * Mirrors the production JettyConfig in cps-application.
 */
@Configuration
public class JettyTestConfig implements WebServerFactoryCustomizer<JettyServletWebServerFactory> {

    @Override
    public void customize(final JettyServletWebServerFactory jettyServletWebServerFactory) {
        jettyServletWebServerFactory.addServerCustomizers(server -> {
            for (final Connector connector : server.getConnectors()) {
                for (final ConnectionFactory connectionFactory : connector.getConnectionFactories()) {
                    if (connectionFactory instanceof HttpConnectionFactory) {
                        final HttpConfiguration httpConfiguration =
                                ((HttpConnectionFactory) connectionFactory).getHttpConfiguration();
                        httpConfiguration.setUriCompliance(UriCompliance.from(EnumSet.of(
                                UriCompliance.Violation.AMBIGUOUS_PATH_SEPARATOR,
                                UriCompliance.Violation.AMBIGUOUS_EMPTY_SEGMENT)));
                    }
                }
            }
        });
    }
}
