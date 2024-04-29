/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

@Getter
@Setter
@ConfigurationProperties(prefix = "ncmp.dmi.httpclient", ignoreUnknownFields = true)
public class HttpClientConfiguration {

    /**
     * The maximum time to establish a connection.
     */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration connectionTimeoutInSeconds = Duration.ofSeconds(180);

    /**
     * The maximum number of open connections per route.
     */
    private int maximumConnectionsPerRoute = 50;

    /**
     * The maximum total number of open connections.
     */
    private int maximumConnectionsTotal = maximumConnectionsPerRoute * 2;

    /**
     * The duration after which idle connections are evicted.
     */
    @DurationUnit(ChronoUnit.SECONDS)
    private Duration idleConnectionEvictionThresholdInSeconds = Duration.ofSeconds(5);

}
