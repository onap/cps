/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023 Nordix Foundation.
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

package org.onap.cps.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.impl.inventory.sync.lcm.CmHandleStateMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MicroMeterConfig {

    private static final String TAG = "state";
    private static final String CMHANDLE_STATE_GAUGE = "cmHandlesByState";
    private final CmHandleStateMetrics cmHandleStateMetrics;

    @Bean
    public TimedAspect timedAspect(final MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    /**
     * Register gauge metric for cm handles with state 'advised'.
     *
     * @param meterRegistry meter registry
     * @return cm handle state gauge
     */
    @Bean
    public Gauge advisedCmHandles(final MeterRegistry meterRegistry) {
        return Gauge.builder(CMHANDLE_STATE_GAUGE, cmHandleStateMetrics,
                        lcmMetric -> cmHandleStateMetrics.getAdvisedCmHandlesCount().get())
                .tag(TAG, "ADVISED")
                .description("Current number of cmhandles in advised state")
                .register(meterRegistry);
    }

    /**
     * Register gauge metric for cm handles with state 'ready'.
     *
     * @param meterRegistry meter registry
     * @return cm handle state gauge
     */
    @Bean
    public Gauge readyCmHandles(final MeterRegistry meterRegistry) {
        return Gauge.builder(CMHANDLE_STATE_GAUGE, cmHandleStateMetrics,
                        lcmMetric -> cmHandleStateMetrics.getReadyCmHandlesCount().get())
                .tag(TAG, "READY")
                .description("Current number of cmhandles in ready state")
                .register(meterRegistry);
    }

    /**
     * Register gauge metric for cm handles with state 'locked'.
     *
     * @param meterRegistry meter registry
     * @return cm handle state gauge
     */
    @Bean
    public Gauge lockedCmHandles(final MeterRegistry meterRegistry) {
        return Gauge.builder(CMHANDLE_STATE_GAUGE, cmHandleStateMetrics,
                        lcmMetric -> cmHandleStateMetrics.getLockedCmHandlesCount().get())
                .tag(TAG, "LOCKED")
                .description("Current number of cmhandles in locked state")
                .register(meterRegistry);
    }

    /**
     * Register gauge metric for cm handles with state 'deleting'.
     *
     * @param meterRegistry meter registry
     * @return cm handle state gauge
     */
    @Bean
    public Gauge deletingCmHandles(final MeterRegistry meterRegistry) {
        return Gauge.builder(CMHANDLE_STATE_GAUGE, cmHandleStateMetrics,
                        lcmMetric -> cmHandleStateMetrics.getDeletingCmHandlesCount().get())
                .tag(TAG, "DELETING")
                .description("Current number of cmhandles in deleting state")
                .register(meterRegistry);
    }

}
