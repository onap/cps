/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025-2026 OpenInfra Foundation Europe. All rights reserved.
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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHandler;
import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the embedded Jetty server to allow encoded slashes (%2F) within URI path segments.
 *
 * <p>Alternate IDs can be hierarchical paths (e.g. {@code /SubNetwork=Europe/ManagedElement=X1}) that
 * clients URL-encode. Accepting these requires relaxing TWO independent Jetty layers:
 * <ol>
 *     <li><b>HTTP parser</b> - the connector's {@code HttpConfiguration} URI compliance. Without
 *     {@code UriCompliance.UNSAFE} Jetty rejects the raw request with "Ambiguous URI path separator".</li>
 *     <li><b>Servlet dispatch</b> - the EE10 {@code ServletHandler}. Without
 *     {@code setDecodeAmbiguousURIs(true)} Jetty wraps the request as an {@code AmbiguousURI} and
 *     returns 400 when Spring reads the servlet path, even though the parser accepted it.</li>
 * </ol>
 *
 * <p>Note: Spring Security's firewall is a THIRD layer that must also allow encoded slashes; see
 * {@link SecurityConfig#webSecurityCustomizer()}.
 *
 * <p><b>Spring Boot 4 / Jetty upgrade warning:</b> This class depends on Jetty 12 EE10 internals
 * ({@code org.eclipse.jetty.ee10.servlet.*}) and the {@code UriCompliance} model, both of which have
 * changed across Jetty major versions. When migrating to Spring Boot 4 (newer Jetty / likely {@code ee11}):
 * <ul>
 *     <li>The {@code ee10} package imports will no longer resolve - update to the new EE namespace.</li>
 *     <li>Verify {@code ServletHandler.setDecodeAmbiguousURIs} still exists / has the same semantics.</li>
 *     <li>Verify {@code UriCompliance.UNSAFE} is still the correct mode.</li>
 *     <li>The integration test covering encoded-slash paths is the safety net - if it fails after the
 *     upgrade, this class needs revisiting.</li>
 * </ul>
 *
 * @see <a href="https://jetty.org/docs/jetty/12/programming-guide/server/compliance.html">Jetty Compliance</a>
 */
@Configuration
@Slf4j
public class JettyConfig implements WebServerFactoryCustomizer<JettyServletWebServerFactory> {

    @Override
    public void customize(final JettyServletWebServerFactory jettyServletWebServerFactory) {
        log.info("JettyConfig: allowing encoded slashes (UriCompliance.UNSAFE + decodeAmbiguousURIs)");
        jettyServletWebServerFactory.addServerCustomizers(server -> {
            for (final Connector connector : server.getConnectors()) {
                for (final ConnectionFactory connectionFactory : connector.getConnectionFactories()) {
                    if (connectionFactory instanceof HttpConnectionFactory httpConnectionFactory) {
                        httpConnectionFactory.getHttpConfiguration().setUriCompliance(UriCompliance.UNSAFE);
                    }
                }
            }
            enableAmbiguousUriDecodingOnServletHandlers(server.getHandler());
        });
    }

    private void enableAmbiguousUriDecodingOnServletHandlers(final Handler handler) {
        if (handler instanceof ServletContextHandler servletContextHandler) {
            final ServletHandler servletHandler = servletContextHandler.getServletHandler();
            if (servletHandler != null) {
                servletHandler.setDecodeAmbiguousURIs(true);
            }
        }
        if (handler instanceof Handler.Container handlerContainer) {
            for (final Handler childHandler : handlerContainer.getHandlers()) {
                enableAmbiguousUriDecodingOnServletHandlers(childHandler);
            }
        }
    }
}
