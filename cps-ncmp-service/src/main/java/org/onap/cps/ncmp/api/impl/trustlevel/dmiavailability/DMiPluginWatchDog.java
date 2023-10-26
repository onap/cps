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

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.client.DmiRestClient;
import org.onap.cps.ncmp.api.impl.trustlevel.TrustLevel;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class DMiPluginWatchDog {

    private final DmiRestClient dmiRestClient;
    private final Map<String, TrustLevel> trustLevelPerDmiPlugin;

    /**
     * Monitors the aliveness of DMI plugins by this watchdog.
     * This method periodically checks the health and status of each DMI plugin to ensure that
     * they are functioning properly. If a plugin is found to be unresponsive or in an
     * unhealthy state, the caches will be updated with the latest status.
     * The @fixedDelayString is the time interval, in milliseconds, between consecutive aliveness checks.
     */
    @Scheduled(fixedDelayString = "${ncmp.timers.trust-evel.dmi-availability-watchdog-ms:30000}")
    public void watchDmiPluginTrustLevel() {
        trustLevelPerDmiPlugin.keySet().forEach(dmiKey -> {
            final TrustLevel dmiTrustLevel = dmiRestClient.getDmiPluginTrustLevel(dmiKey);
            log.debug("Trust level for dmi-plugin: {} is {}", dmiKey, dmiTrustLevel.toString());
            trustLevelPerDmiPlugin.put(dmiKey, dmiTrustLevel);
        });
    }

}
