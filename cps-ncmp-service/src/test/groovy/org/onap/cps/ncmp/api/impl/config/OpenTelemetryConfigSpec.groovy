package org.onap.cps.ncmp.api.impl.config

import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.extension.trace.jaeger.sampler.JaegerRemoteSampler
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.MediaType
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate
import spock.lang.Specification

class OpenTelemetryConfigSpec extends Specification{

    @SpringBean
    OpenTelemetryConfig openTelemetryConfig = new OpenTelemetryConfig()

    def mockRestTemplateBuilder = new RestTemplateBuilder()

    def 'OpenTelemetryConfig Construction.'() {
        expect: 'the system can create an instance'
        new OpenTelemetryConfig() != null
    }

    def  'OTLP Exporter creation with Grpc protocol'(){
        given :
        openTelemetryConfig.serviceId ="cps-application"
        openTelemetryConfig.tracingExporterEndpointUrl="http://tracingExporterEndpointUrl"
        openTelemetryConfig.jaegerRemoteSamplerUrl="http://jaegerRemoteSamplerUrl"
        when: 'an OTLP exporter is created'
        def result = openTelemetryConfig.createOtlpExporterGrpc()
        then: 'an OTLP Exporter is created'
        assert result instanceof OtlpGrpcSpanExporter
    }

    def  'OTLP Exporter creation with HTTP protocol'(){
        given :
        openTelemetryConfig.serviceId ="cps-application"
        openTelemetryConfig.tracingExporterEndpointUrl="http://tracingExporterEndpointUrl"
        openTelemetryConfig.jaegerRemoteSamplerUrl="http://jaegerRemoteSamplerUrl"
        when: 'an OTLP exporter is created'
        def result = openTelemetryConfig.createOtlpExporterHttp()
        then: 'an OTLP Exporter is created'
        assert result instanceof OtlpHttpSpanExporter
    }

    def  'Jaeger Remote Sampler Creation'(){
        given :
        openTelemetryConfig.serviceId ="cps-application"
        openTelemetryConfig.tracingExporterEndpointUrl="http://tracingExporterEndpointUrl"
        openTelemetryConfig.jaegerRemoteSamplerUrl="http://jaegerRemoteSamplerUrl"
        when: 'an OTLP exporter is created'
        def result = openTelemetryConfig.createJaegerRemoteSampler()
        then: 'an OTLP Exporter is created'
        assert result instanceof JaegerRemoteSampler
    }
}
