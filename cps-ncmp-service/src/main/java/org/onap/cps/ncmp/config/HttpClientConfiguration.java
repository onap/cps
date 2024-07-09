/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation.
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

package org.onap.cps.ncmp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ncmp.dmi.httpclient")
public class HttpClientConfiguration {

    private final DataServices dataServices = new DataServices();
    private final ModelServices modelServices = new ModelServices();
    private final HealthCheckServices healthCheckServices = new HealthCheckServices();

    @Getter
    @Setter
    public static class DataServices extends ServiceConfig {
        private String connectionProviderName = "dataConnectionPool";
    }

    @Getter
    @Setter
    public static class ModelServices extends ServiceConfig {
        private String connectionProviderName = "modelConnectionPool";
    }

    @Getter
    @Setter
    public static class HealthCheckServices extends ServiceConfig {
        private String connectionProviderName = "healthConnectionPool";
        private int maximumConnectionsTotal = 10;
        private int pendingAcquireMaxCount = 5;
    }

    /**
     * Base configuration properties for all services.
     */
    @Getter
    @Setter
    public static class ServiceConfig {
        private String connectionProviderName = "cpsConnectionPool";
        private int maximumConnectionsTotal = 100;
        private int pendingAcquireMaxCount = 50;
        private Integer connectionTimeoutInSeconds = 30;
        private long readTimeoutInSeconds = 30;
        private long writeTimeoutInSeconds = 30;
        private int maximumInMemorySizeInMegabytes = 1;
    }
}
