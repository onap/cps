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

package org.onap.cps.ncmp.api.impl.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ncmp.dmi.httpclient")
public class HttpClientConfiguration {

    private final DataServices dataServices = new DataServices();
    private final ModelServices modelServices = new ModelServices();
    private final HealthCheckServices healthCheckServices = new HealthCheckServices();

    @Getter
    @Setter
    public static class DataServices {
        private Integer maximumConnectionsTotal = 100;
        private Integer connectionTimeoutInSeconds = 30;
        private Integer readTimeoutInSeconds = 30;
        private Integer writeTimeoutInSeconds = 30;
        private Integer maximumInMemorySizeInMegabytes = 1;
    }

    @Getter
    @Setter
    public static class ModelServices {
        private Integer maximumConnectionsTotal = 100;
        private Integer connectionTimeoutInSeconds = 30;
        private Integer readTimeoutInSeconds = 30;
        private Integer writeTimeoutInSeconds = 30;
        private Integer maximumInMemorySizeInMegabytes = 1;
    }

    @Getter
    public static class HealthCheckServices {
        private final Integer maximumConnectionsTotal = 10;
        private final Integer connectionTimeoutInSeconds = 30;
        private final Integer readTimeoutInSeconds = 30;
        private final Integer writeTimeoutInSeconds = 30;
        private final Integer maximumInMemorySizeInMegabytes = 1;
    }
}
