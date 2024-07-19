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
    OpenTelemetryConfigProperties openTelemetryConfigProperties = new OpenTelemetryConfigProperties()

    @Shared
    @SpringBean
    OpenTelemetryConfig openTelemetryConfig = new OpenTelemetryConfig(openTelemetryConfigProperties)

    def setupSpec() {
        openTelemetryConfigProperties.exporter.endpoint = 'http://exporter-endpoint'
        openTelemetryConfigProperties.sampler.jaegerRemote.endpoint = 'http://jaeger-remote-endpoint'
        openTelemetryConfigProperties.serviceId = 'sample-app'
        openTelemetryConfigProperties.scheduledTaskNames = ['some-task']
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

    def 'Skipping Actuator endpoints'() {
        given: 'a mock observation registry and observation config'
            ObservationRegistry registry = Mock()
            ObservationConfig config = Mock()
            registry.observationConfig() >> config
        when: 'an observation registry customizer is created'
            def result = openTelemetryConfig.skipActuatorEndpointsFromObservation()
        and: 'the customizer is applied to the registry'
            result.customize(registry)
        then: 'the observation predicate is set correctly'
            1 * config.observationPredicate(_ as ObservationPredicate) >> { ObservationPredicate predicate ->
                def actuatorRequest = new MockHttpServletRequest('GET', '/actuator/health')
                def actuatorContext = new ServerRequestObservationContext(actuatorRequest, null)
        and: 'Expected predicate to return false for actuator endpoint'
                assert !predicate.test('anyName', actuatorContext)
                def nonActuatorRequest = new MockHttpServletRequest('GET', '/api/data')
                def nonActuatorContext = new ServerRequestObservationContext(nonActuatorRequest, null)
        and: 'Expected predicate to return true for non-actuator endpoint'
                assert predicate.test('anyName', nonActuatorContext)
        and: 'Expected predicate to return false for scheduled task'
                assert !predicate.test('some-task', null)
        and: 'Expected predicate to return true for non-scheduled task'
                assert predicate.test('nonScheduledTask', null)
        }
    }

    def 'Observation Predicate'() {
        given: 'a PathMatcher and ObservationPredicate'
            def pathMatcher = new AntPathMatcher('/')
            def predicate = openTelemetryConfig.observationPredicate(pathMatcher)
        when: 'a ServerRequestObservationContext for an actuator endpoint is provided'
            def actuatorRequest = new MockHttpServletRequest('GET', '/actuator/health')
            def actuatorContext = new ServerRequestObservationContext(actuatorRequest, null)
        then: 'expected predicate to return false for actuator endpoint'
            assert !predicate.test('anyName', actuatorContext)
        when: 'a ServerRequestObservationContext for a non-actuator endpoint is provided'
            def nonActuatorRequest = new MockHttpServletRequest('GET', '/api/data')
            def nonActuatorContext = new ServerRequestObservationContext(nonActuatorRequest, null)
        then: 'expected predicate to return true for non-actuator endpoint'
            assert predicate.test('anyName', nonActuatorContext)
        when: 'an observation name that matches a scheduled task name is provided'
            def taskName = 'some-task'
        then: 'expected predicate to return false for scheduled task name'
            assert !predicate.test(taskName, null)
        when: 'an observation name that does not match any scheduled task name is provided'
            def nonTaskName = 'nonScheduledTask'
        then: 'expected predicate to return true for non-scheduled task name'
            assert predicate.test(nonTaskName, null)
    }
}
