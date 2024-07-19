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

import io.micrometer.observation.ObservationPredicate;
import io.micrometer.observation.ObservationRegistry;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.extension.trace.jaeger.sampler.JaegerRemoteSampler;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

/**
 * Configuration class for setting up OpenTelemetry tracing in a Spring Boot application.
 * This class provides beans for OTLP exporters (gRPC and HTTP), a Jaeger remote sampler,
 * and customizes the ObservationRegistry to exclude certain endpoints from being observed.
 */
@Configuration
public class OpenTelemetryConfig {

    @Value("${spring.application.name:cps-application}")
    private String serviceId;

    @Value("${cps.tracing.exporter.endpoint:http://onap-otel-collector:4317}")
    private String tracingExporterEndpointUrl;

    @Value("${cps.tracing.sampler.jaeger_remote.endpoint:http://onap-otel-collector:14250}")
    private String jaegerRemoteSamplerUrl;

    @Value("${cps.tracing.excluded-task-names:tasks.scheduled.execution}")
    private String excludedTaskNamesAsCsv;

    private static final int JAEGER_REMOTE_SAMPLER_POLLING_INTERVAL_IN_SECONDS = 30;

    private List<String> excludedTaskNames;

    /**
     * Initializes the excludedTaskNames after the bean's properties have been set.
     * This method is called by the Spring container during bean initialization.
     */
    @PostConstruct
    public void init() {
        excludedTaskNames = Arrays.stream(excludedTaskNamesAsCsv.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    /**
     * Creates an OTLP Exporter with gRPC protocol.
     *
     * @return OtlpGrpcSpanExporter bean if tracing is enabled and the exporter protocol is gRPC
     */
    @Bean
    @ConditionalOnExpression(
        "${cps.tracing.enabled} && 'grpc'.equals('${cps.tracing.exporter.protocol}')")
    public OtlpGrpcSpanExporter createOtlpExporterGrpc() {
        return OtlpGrpcSpanExporter.builder().setEndpoint(tracingExporterEndpointUrl).build();
    }

    /**
     * Creates an OTLP Exporter with HTTP protocol.
     *
     * @return OtlpHttpSpanExporter bean if tracing is enabled and the exporter protocol is HTTP
     */
    @Bean
    @ConditionalOnExpression(
        "${cps.tracing.enabled} && 'http'.equals('${cps.tracing.exporter.protocol}')")
    public OtlpHttpSpanExporter createOtlpExporterHttp() {
        return OtlpHttpSpanExporter.builder().setEndpoint(tracingExporterEndpointUrl).build();
    }

    /**
     * Creates a Jaeger Remote Sampler.
     *
     * @return JaegerRemoteSampler bean if tracing is enabled
     */
    @Bean
    @ConditionalOnProperty("cps.tracing.enabled")
    public JaegerRemoteSampler createJaegerRemoteSampler() {
        return JaegerRemoteSampler.builder()
                .setEndpoint(jaegerRemoteSamplerUrl)
                .setPollingInterval(Duration.ofSeconds(JAEGER_REMOTE_SAMPLER_POLLING_INTERVAL_IN_SECONDS))
                .setInitialSampler(Sampler.alwaysOff())
                .setServiceName(serviceId)
                .build();
    }

    /**
     * Customizes the ObservationRegistry to exclude /actuator/** endpoints from being observed.
     *
     * @return ObservationRegistryCustomizer bean if tracing is enabled
     */
    @Bean
    @ConditionalOnProperty("cps.tracing.enabled")
    public ObservationRegistryCustomizer<ObservationRegistry> skipActuatorEndpointsFromObservation() {
        final PathMatcher pathMatcher = new AntPathMatcher("/");
        return registry ->
                registry.observationConfig().observationPredicate(observationPredicate(pathMatcher));
    }

    private ObservationPredicate observationPredicate(final PathMatcher pathMatcher) {
        return (observationName, context) -> {
            if (context instanceof ServerRequestObservationContext observationContext) {
                return !pathMatcher.match("/actuator/**", observationContext.getCarrier().getRequestURI());
            } else {
                return !excludedTaskNames.contains(observationName);
            }
        };
    }
}
