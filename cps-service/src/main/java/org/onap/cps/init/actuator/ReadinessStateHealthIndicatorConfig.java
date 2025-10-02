/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.init.actuator;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ReadinessStateHealthIndicatorConfig {

    private final ReadinessManager readinessManager;

    /**
     * Overriding the default Readiness State Health Indicator.
     *
     * @return Health Status ( UP or DOWN )
     */
    @Bean("readinessStateHealthIndicator")
    public HealthIndicator readinessStateHealthIndicator() {

        return () -> {
            if (readinessManager.isReady()) {
                return Health.up().withDetail("Startup Processes", "All startup processes completed").build();
            }
            return Health.down().withDetail("Startup Processes active", readinessManager.getStartupProcessesAsString())
                           .build();
        };
    }
}
