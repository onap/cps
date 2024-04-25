package org.onap.cps.ncmp.api.impl.config

import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.extension.trace.jaeger.sampler.JaegerRemoteSampler
import org.spockframework.spring.SpringBean
import org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryCustomizer
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files

class OpenTelemetryConfigSpec extends Specification{

    @Shared
    @SpringBean
    OpenTelemetryConfig openTelemetryConfig = new OpenTelemetryConfig()

    def setupSpec() {
        openTelemetryConfig.tracingExporterEndpointUrl="http://tracingExporterEndpointUrl"
        openTelemetryConfig.jaegerRemoteSamplerUrl="http://jaegerRemoteSamplerUrl"
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
    }

    def  'Jaeger Remote Sampler Creation'(){
        when: 'an OTLP exporter is created'
            def result = openTelemetryConfig.createJaegerRemoteSampler()
        then: 'an OTLP Exporter is created'
            assert result instanceof JaegerRemoteSampler
    }

    def  'Skipping Acutator endpoints'(){
        when: 'an OTLP exporter is created'
            def result = openTelemetryConfig.skipActuatorEndpointsFromObservation()
        then: 'an OTLP Exporter is created'
            assert result instanceof ObservationRegistryCustomizer
    }
}
