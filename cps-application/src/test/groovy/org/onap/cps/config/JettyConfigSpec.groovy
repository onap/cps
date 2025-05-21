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

package org.onap.cps.config

import org.eclipse.jetty.server.ConnectionFactory
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.Server
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory
import spock.lang.Specification

class JettyConfigSpec extends Specification {

    def objectUnderTest = new JettyConfig()
    def server = Mock(Server)
    def connector = Mock(Connector)

    def 'Enable support for ambiguous path separators in Jetty Http configuration'() {
        given: 'a Jetty server factory'
            def jettyServletWebServerFactory = new JettyServletWebServerFactory()
        and: 'a mocked connection factory (Http or Non-Http)'
            def connectionFactory = connectionFactoryType == 'http' ? Mock(HttpConnectionFactory) : Mock(ConnectionFactory)
        and: 'optional mock for HttpConfiguration if applicable'
            def httpConfig = null
            if (connectionFactory instanceof HttpConnectionFactory) {
                httpConfig = Mock(HttpConfiguration)
                connectionFactory.getHttpConfiguration() >> httpConfig
            }
        and: 'mocked components return expected values'
            connector.getConnectionFactories() >> [connectionFactory]
            server.getConnectors() >> [connector]
        when: 'JettyConfig customization is triggered on the server factory'
            objectUnderTest.customize(jettyServletWebServerFactory)
        and: 'a server customizer is extracted from the configured factory'
            def serverCustomizer = jettyServletWebServerFactory.serverCustomizers.first()
        and: 'the customizer is applied to the mocked Jetty server'
            serverCustomizer.customize(server)
        then: 'the HTTP configuration is updated to allow ambiguous path separators'
            expectedCalls * httpConfig.setUriCompliance({ it.toString().contains('AMBIGUOUS_PATH_SEPARATOR') })
        where: 'type of connection factories'
            description                                              | connectionFactoryType || expectedCalls
            'HttpConnectionFactory - should configure UriCompliance' | 'http'                || 1
            'Non-HttpConnectionFactory - should do nothing'          | 'non-http'            || 0
    }
}
