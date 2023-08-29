/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.trustlevel.dmiavailability;

import com.hazelcast.map.IMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.client.DmiRestClient;
import org.onap.cps.ncmp.api.impl.exception.NcmpStartUpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class DmiAvailabilityControlService implements ApplicationListener<ApplicationReadyEvent> {

    private final IMap<String, Map<String, String>> dmiPluginTrustLevelCache;

    private final DmiRestClient dmiRestClient;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3);

    @Value("${ncmp.timers.trust-evel.dmi-availability-control-ms:30000}")
    private int dmiAvailabilityControlInMs;

    @Override
    public void onApplicationEvent(final ApplicationReadyEvent applicationReadyEvent) {
        try {
            startDmiAvailabilityControl();
        } catch (final NcmpStartUpException ncmpStartUpException) {
            log.error("Dmi availability control start is failed: {} ", ncmpStartUpException.getMessage());
            SpringApplication.exit(applicationReadyEvent.getApplicationContext(), () -> 1);
        }
    }

    private void startDmiAvailabilityControl() {
        final DmiAvailabilityControlTask dmiAvailabilityControlTask =
                new DmiAvailabilityControlTask(dmiPluginTrustLevelCache, dmiRestClient);
        scheduledExecutorService.scheduleWithFixedDelay(dmiAvailabilityControlTask, dmiAvailabilityControlInMs,
                dmiAvailabilityControlInMs, TimeUnit.MILLISECONDS);
    }
}
