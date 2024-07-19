/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024 Nordix Foundation
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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

package org.onap.cps.ncmp.config;

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest(classes = OpenTelemetryConfigProperties)
@EnableConfigurationProperties(OpenTelemetryConfigProperties.class)
class OpenTelemetryConfigPropertiesSpec extends Specification {

    @Autowired
    OpenTelemetryConfigProperties openTelemetryConfigProperties

    def 'Default values are correctly set'() {
        expect: 'validate that the default values are set correctly'
            assert openTelemetryConfigProperties.serviceId == 'cps-application'
            assert openTelemetryConfigProperties.exporter.endpoint == 'http://onap-otel-collector:4317'
            assert openTelemetryConfigProperties.sampler.jaegerRemote.endpoint == 'http://onap-otel-collector:14250'
            assert openTelemetryConfigProperties.enabled == false
            assert openTelemetryConfigProperties.scheduledTaskNames == ['some.task.name', 'another.task.name']
    }

    def "Custom values are correctly applied"() {
        given: 'custom values are set for the properties'
            openTelemetryConfigProperties.serviceId = 'custom-service-name'
            openTelemetryConfigProperties.exporter.endpoint = 'http://custom-exporter-endpoint'
            openTelemetryConfigProperties.sampler.jaegerRemote.endpoint = 'http://custom-jaeger-endpoint'
            openTelemetryConfigProperties.enabled = true
            openTelemetryConfigProperties.scheduledTaskNames = ['custom.task.name']

        expect: 'the properties should reflect the custom values'
            assert openTelemetryConfigProperties.serviceId == 'custom-service-name'
            assert openTelemetryConfigProperties.exporter.endpoint == 'http://custom-exporter-endpoint'
            assert openTelemetryConfigProperties.sampler.jaegerRemote.endpoint == 'http://custom-jaeger-endpoint'
            assert openTelemetryConfigProperties.enabled == true
            assert openTelemetryConfigProperties.scheduledTaskNames == ['custom.task.name']
    }
}

