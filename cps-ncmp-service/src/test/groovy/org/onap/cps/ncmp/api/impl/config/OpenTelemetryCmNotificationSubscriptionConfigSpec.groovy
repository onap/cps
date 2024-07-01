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

package org.onap.cps.ncmp.api.impl.config

import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.extension.trace.jaeger.sampler.JaegerRemoteSampler
import org.spockframework.spring.SpringBean
import org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryCustomizer
import spock.lang.Shared
import spock.lang.Specification

class OpenTelemetryConfigSpec extends Specification{

    @Shared
    @SpringBean
    OpenTelemetryConfig openTelemetryConfig = new OpenTelemetryConfig()

    def setupSpec() {
        openTelemetryConfig.tracingExporterEndpointUrl="http://tracingExporterEndpointUrl"
        openTelemetryConfig.jaegerRemoteSamplerUrl="http://jaegerremotesamplerurl"
        openTelemetryConfig.serviceId ="cps-application"
    }

    def 'OpenTelemetryConfig Construction.'() {
        expect: 'the system can create an instance'
        new OpenTelemetryConfig() != null
    }

    def  'OTLP Exporter creation with Grpc protocol'(){
        when: 'an OTLP exporter is created'
            def result = openTelemetryConfig.createOtlpExporterGrpc()
        then: 'an OTLP Exporter is created'
            assert result instanceof OtlpGrpcSpanExporter
    }

    def  'OTLP Exporter creation with HTTP protocol'(){
        when: 'an OTLP exporter is created'
            def result = openTelemetryConfig.createOtlpExporterHttp()
        then: 'an OTLP Exporter is created'
            assert result instanceof OtlpHttpSpanExporter
        and:
            assert result.builder.endpoint=="http://tracingExporterEndpointUrl"
    }

    def  'Jaeger Remote Sampler Creation'(){
        when: 'an OTLP exporter is created'
            def result = openTelemetryConfig.createJaegerRemoteSampler()
        then: 'an OTLP Exporter is created'
            assert result instanceof JaegerRemoteSampler
        and:
            assert result.delegate.type=="remoteSampling"
        and:
            assert result.delegate.url.toString().startsWith("http://jaegerremotesamplerurl")
    }

    def  'Skipping Acutator endpoints'(){
        when: 'an OTLP exporter is created'
            def result = openTelemetryConfig.skipActuatorEndpointsFromObservation()
        then: 'an OTLP Exporter is created'
            assert result instanceof ObservationRegistryCustomizer
    }
}
