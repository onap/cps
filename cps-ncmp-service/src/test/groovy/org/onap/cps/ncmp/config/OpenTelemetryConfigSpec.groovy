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
        openTelemetryConfigProperties.exporter.endpoint = 'http://exporter-test-url'
        openTelemetryConfigProperties.sampler.jaegerRemote.endpoint = 'http://jaeger-Remote-test-url'
        openTelemetryConfigProperties.serviceId = 'sample-app'
        openTelemetryConfigProperties.excludedTaskNames = ['excluded-task-name']
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
            assert result.builder.endpoint == 'http://exporter-test-url'
    }

    def 'Jaeger Remote Sampler Creation'() {
        when: 'a Jaeger remote sampler is created'
            def result = openTelemetryConfig.createJaegerRemoteSampler()
        then: 'a Jaeger remote sampler is created'
            assert result instanceof JaegerRemoteSampler
        and: 'the sampler type is correct'
            assert result.delegate.type == 'remoteSampling'
        and: 'the sampler endpoint is correctly set'
            assert result.delegate.url.toString().startsWith('http://jaeger-remote-test-url')
    }

    def 'Skipping actuator endpoints'() {
        given: 'a mocked observation registry and config'
            def observationRegistry = Mock(ObservationRegistry.class)
            def observationConfig = Mock(ObservationConfig.class)
            observationRegistry.observationConfig() >> observationConfig
        when: 'an observation registry customizer is created and applied'
            def result = openTelemetryConfig.skipActuatorEndpointsFromObservation()
            result.customize(observationRegistry)
        then: 'the observation predicate is set correctly'
            1 * observationConfig.observationPredicate(_) >> { ObservationPredicate observationPredicate ->
                    def mockedHttpServletRequest = new MockHttpServletRequest(_ as String, requestUrl)
                    def serverRequestObservationContext = new ServerRequestObservationContext(mockedHttpServletRequest, null)
                and: 'expected predicate for endpoint'
                    assert observationPredicate.test('some-name', serverRequestObservationContext) == expectedPredicate
            }
        where: 'the following parameters are used'
            scenario         | requestUrl  || expectedPredicate
            'an actuator'    | '/actuator' || false
            'a non actuator' | '/some-api' || true
    }

    def 'Observation predicate is configured to filter out excluded tasks by name'() {
        when: 'a path matcher and observation predicate'
            def observationPredicate = openTelemetryConfig.observationPredicate(new AntPathMatcher('/'))
        then: 'a task name is provided'
            assert observationPredicate.test(taskName, null) == expectedPredicate
        where: 'the following parameters are used'
            taskName                 || expectedPredicate
            'excluded-task-name'     || false
            'non-excluded-task-name' || true
    }
}
