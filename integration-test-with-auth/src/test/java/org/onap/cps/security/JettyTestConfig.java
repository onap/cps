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

package org.onap.cps.security;

import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHandler;
import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.springframework.boot.jetty.servlet.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.firewall.StrictHttpFirewall;

/**
 * Test copy of the production {@code org.onap.cps.config.JettyConfig}, required because the
 * integration-test-with-auth module does not depend on cps-application (where the production config lives).
 *
 * <p>Relaxes both Jetty layers so encoded slashes (%2F) in alternate IDs are accepted:
 * the connector's URI compliance and the EE10 servlet handler's ambiguous URI decoding.
 */
@Configuration
public class JettyTestConfig implements WebServerFactoryCustomizer<JettyServletWebServerFactory> {

    @Override
    public void customize(final JettyServletWebServerFactory jettyServletWebServerFactory) {
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

    /**
     * Mirrors the production SecurityConfig firewall bean to allow encoded slashes through Spring Security.
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        final StrictHttpFirewall strictHttpFirewall = new StrictHttpFirewall();
        strictHttpFirewall.setAllowUrlEncodedSlash(true);
        strictHttpFirewall.setAllowUrlEncodedPercent(true);
        strictHttpFirewall.setAllowUrlEncodedDoubleSlash(true);
        return web -> web.httpFirewall(strictHttpFirewall);
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
