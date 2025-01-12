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
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MicroMeterConfig {

    private static final String TAG = "state";
    private static final String CMHANDLE_STATE_GAUGE = "cmHandlesByState";
    final IMap<String, Integer> cmHandlesByState;

    @Bean
    public TimedAspect timedAspect(final MeterRegistry meterRegistry) {
        return new TimedAspect(meterRegistry);
    }

    /**
     * Register gauge metric for cm handles with state 'advised'.
     *
     * @param meterRegistry meter registry
     * @return cm handle state gauge
     */
    @Bean
    public Gauge advisedCmHandles(final MeterRegistry meterRegistry) {
        return Gauge.builder(CMHANDLE_STATE_GAUGE, cmHandlesByState,
                        value -> cmHandlesByState.get("advisedCmHandlesCount"))
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
        return Gauge.builder(CMHANDLE_STATE_GAUGE, cmHandlesByState,
                        value -> cmHandlesByState.get("readyCmHandlesCount"))
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
        return Gauge.builder(CMHANDLE_STATE_GAUGE, cmHandlesByState,
                value -> cmHandlesByState.get("lockedCmHandlesCount"))
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
        return Gauge.builder(CMHANDLE_STATE_GAUGE, cmHandlesByState,
                        value -> cmHandlesByState.get("deletingCmHandlesCount"))
                .tag(TAG, "DELETING")
                .description("Current number of cmhandles in deleting state")
                .register(meterRegistry);
    }

}
