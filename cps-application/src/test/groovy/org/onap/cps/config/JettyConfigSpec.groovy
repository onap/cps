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

    def 'Enable support for ambiguous path separators in Jetty Http configuration'() {
        given: 'a JettyConfig instance and a Jetty server factory'
            def objectUnderTest = new JettyConfig()
            def factory = new JettyServletWebServerFactory()
        and: 'mocked Jetty server and connector'
            def server = Mock(Server)
            def connector = Mock(Connector)
        and: 'a mocked connection factory (Http or Non-Http)'
            def connectionFactory = connectionFactoryType == 'http'
                    ? Mock(HttpConnectionFactory)
                    : Mock(ConnectionFactory)
        and: 'optional mock for HttpConfiguration if applicable'
            def httpConfig = connectionFactory instanceof HttpConnectionFactory ? Mock(HttpConfiguration) : null
            if (httpConfig) {
                connectionFactory.getHttpConfiguration() >> httpConfig
            }
        and: 'mocked components return expected values'
            connector.getConnectionFactories() >> [connectionFactory]
            server.getConnectors() >> [connector]
        when: 'the Jetty server customizer is applied to the mocked server'
            objectUnderTest.customize(factory)
            def serverCustomizer = factory.serverCustomizers.first()
            serverCustomizer.customize(server)
        then: 'the HTTP configuration is updated to allow ambiguous path separators'
            expectedCalls * httpConfig.setUriCompliance({ it.toString().contains('AMBIGUOUS_PATH_SEPARATOR') })
        where: 'following cases are tested'
            connectionFactoryType | expectedCalls | description
            'http'                | 1             | 'HttpConnectionFactory - should configure UriCompliance'
            'non-http'            | 0             | 'Non-HttpConnectionFactory - should do nothing'
    }
}
