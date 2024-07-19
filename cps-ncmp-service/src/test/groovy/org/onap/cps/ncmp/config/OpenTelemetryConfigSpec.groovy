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

package org.onap.cps.ncmp.config

import io.micrometer.observation.ObservationPredicate
import io.micrometer.observation.ObservationRegistry
import io.micrometer.observation.ObservationRegistry.ObservationConfig
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.extension.trace.jaeger.sampler.JaegerRemoteSampler
import org.spockframework.spring.SpringBean
import org.springframework.http.server.observation.ServerRequestObservationContext
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.util.AntPathMatcher
import spock.lang.Shared
import spock.lang.Specification

class OpenTelemetryConfigSpec extends Specification {

    @Shared
    def openTelemetryConfigProperties = new OpenTelemetryConfigProperties()

    @Shared
    @SpringBean
    def openTelemetryConfig = new OpenTelemetryConfig(openTelemetryConfigProperties)

    def setup() {
        openTelemetryConfigProperties.exporter.endpoint = 'http://exporter-endpoint'
        openTelemetryConfigProperties.sampler.jaegerRemote.endpoint = 'http://jaeger-remote-endpoint'
        openTelemetryConfigProperties.serviceId = 'sample-app'
        openTelemetryConfigProperties.scheduledTaskNames = ['non-monitored-task']
    }

    def 'Open telemetry config construction.'() {
        expect: 'the system can create an instance'
            new OpenTelemetryConfig(openTelemetryConfigProperties) != null
    }

    def 'OTLP exporter creation with Grpc protocol'() {
        when: 'an OTLP exporter is created'
            def result = openTelemetryConfig.createOtlpExporterGrpc()
        then: 'expected an instance of OtlpGrpcSpanExporter'
            assert result instanceof OtlpGrpcSpanExporter
    }

    def 'OTLP exporter creation with HTTP protocol'() {
        when: 'an OTLP exporter is created'
            def result = openTelemetryConfig.createOtlpExporterHttp()
        then: 'an OTLP Exporter is created'
            assert result instanceof OtlpHttpSpanExporter
        and: 'the endpoint is correctly set'
            assert result.builder.endpoint == 'http://exporter-endpoint'
    }

    def 'Jaeger Remote Sampler Creation'() {
        when: 'a Jaeger remote sampler is created'
            def result = openTelemetryConfig.createJaegerRemoteSampler()
        then: 'a Jaeger remote sampler is created'
            assert result instanceof JaegerRemoteSampler
        and: 'the sampler type is correct'
            assert result.delegate.type == 'remoteSampling'
        and: 'the sampler endpoint is correctly set'
            assert result.delegate.url.toString().startsWith('http://jaeger-remote-endpoint')
    }

    def 'Skipping actuator endpoints'() {
        given: 'a mocked observation registry and config'
            def registry = Mock(ObservationRegistry.class)
            def config = Mock(ObservationConfig.class)
            registry.observationConfig() >> config
        when: 'an observation registry customizer is created and applied'
            def result = openTelemetryConfig.skipActuatorEndpointsFromObservation()
            result.customize(registry)
        then: 'the observation predicate is set correctly'
            1 * config.observationPredicate(_) >> { ObservationPredicate predicate ->
                    def mockedRequest = new MockHttpServletRequest(_ as String, requestUrl)
                    def requestContext = new ServerRequestObservationContext(mockedRequest, null)
                and: 'expected predicate for endpoint'
                    assert predicate.test('some-name', requestContext) == expectedPredicate
            }
        where: 'the following parameters are used'
            scenario         | requestUrl  || expectedPredicate
            'an actuator'    | '/actuator' || false
            'a non actuator' | '/some-api' || true
    }

    def 'Observation predicate is configured to filter request url'() {
        given: 'a path matcher and observation predicate'
            def predicate = openTelemetryConfig.observationPredicate(new AntPathMatcher('/'))
        when: 'a server request observation context for #scenario endpoint is provided'
            def mockedRequest = new MockHttpServletRequest(_ as String, requestUrl)
            def requestContext = new ServerRequestObservationContext(mockedRequest, null)
        then: 'expected predicate for endpoint'
            assert predicate.test('some-name', requestContext) == expectedPredicate
        where: 'the following parameters are used'
            scenario         | requestUrl  || expectedPredicate
            'an actuator'    | '/actuator' || false
            'a non actuator' | '/some-api' || true
    }

    def 'Observation predicate is configured to filter scheduled tasks by name'() {
        when: 'a path matcher and observation predicate'
            def predicate = openTelemetryConfig.observationPredicate( new AntPathMatcher('/'))
        then: 'a scheduled task name is provided'
            assert predicate.test(scheduledTaskName, null) == expectedPredicate
        where: 'the following parameters are used'
            scenario             | scheduledTaskName    || expectedPredicate
            'not monitored task' | 'non-monitored-task' || false
            'monitored task'     | 'monitored-task'     || true
    }
}
