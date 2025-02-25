/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023-2025 Nordix Foundation.
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

import com.hazelcast.map.IMap;
import io.github.mweirauch.micrometer.jvm.extras.ProcessMemoryMetrics;
import io.github.mweirauch.micrometer.jvm.extras.ProcessThreadMetrics;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MicroMeterConfig {

    private static final String STATE_TAG = "state";
    private static final String CM_HANDLE_STATE_GAUGE = "cps_ncmp_inventory_cm_handles_by_state";
    final IMap<String, Integer> cmHandlesByState;

    @Bean
    public TimedAspect timedAspect(final MeterRegistry meterRegistry) {
        return new TimedAspect(meterRegistry);
    }

    @Bean
    @ConditionalOnProperty("cps.monitoring.micrometer-jvm-extras")
    public MeterBinder processMemoryMetrics() {
        return new ProcessMemoryMetrics();
    }

    @Bean
    @ConditionalOnProperty("cps.monitoring.micrometer-jvm-extras")
    public MeterBinder processThreadMetrics() {
        return new ProcessThreadMetrics();
    }

    /**
     * Register gauge metric for cm handles with state 'advised'.
     *
     * @param meterRegistry meter registry
     * @return cm handle state gauge
     */
    @Bean
    public Gauge advisedCmHandles(final MeterRegistry meterRegistry) {
        return Gauge.builder(CM_HANDLE_STATE_GAUGE, cmHandlesByState,
                        value -> cmHandlesByState.get("advisedCmHandlesCount"))
                .tag(STATE_TAG, "ADVISED")
                .description("Current number of cm handles in advised state")
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
        return Gauge.builder(CM_HANDLE_STATE_GAUGE, cmHandlesByState,
                        value -> cmHandlesByState.get("readyCmHandlesCount"))
                .tag(STATE_TAG, "READY")
                .description("Current number of cm handles in ready state")
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
        return Gauge.builder(CM_HANDLE_STATE_GAUGE, cmHandlesByState,
                value -> cmHandlesByState.get("lockedCmHandlesCount"))
                .tag(STATE_TAG, "LOCKED")
                .description("Current number of cm handles in locked state")
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
        return Gauge.builder(CM_HANDLE_STATE_GAUGE, cmHandlesByState,
                        value -> cmHandlesByState.get("deletingCmHandlesCount"))
                .tag(STATE_TAG, "DELETING")
                .description("Current number of cm handles in deleting state")
                .register(meterRegistry);
    }

    /**
     * Register gauge metric for cm handles with state 'deleted'.
     *
     * @param meterRegistry meter registry
     * @return cm handle state gauge
     */
    @Bean
    public Gauge deletedCmHandles(final MeterRegistry meterRegistry) {
        return Gauge.builder(CM_HANDLE_STATE_GAUGE, cmHandlesByState,
                        value -> cmHandlesByState.get("deletedCmHandlesCount"))
                .tag(STATE_TAG, "DELETED")
                .description("Number of cm handles that have been deleted since the application started")
                .register(meterRegistry);
    }

}
