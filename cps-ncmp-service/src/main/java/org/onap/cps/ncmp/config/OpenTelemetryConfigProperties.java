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

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "cps.tracing")
public class OpenTelemetryConfigProperties {

    @Value("${spring.application.name:cps-application}")
    private String serviceId;

    static final int JAEGER_REMOTE_SAMPLER_POLLING_INTERVAL_IN_SECOND = 30;

    private boolean enabled;
    private Exporter exporter = new Exporter();
    private Sampler sampler = new Sampler();
    private List<String> excludedTaskNames;

    @Getter
    @Setter
    public static class Exporter {
        private String protocol;
        private String endpoint = "http://onap-otel-collector:4317";
    }

    @Getter
    @Setter
    public static class Sampler {
        private JaegerRemote jaegerRemote = new JaegerRemote();

        @Getter
        @Setter
        public static class JaegerRemote {
            private String endpoint = "http://onap-otel-collector:14250";
        }
    }
}
