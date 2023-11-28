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

import java.util.Collection;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.api.impl.client.DmiRestClient;
import org.onap.cps.ncmp.api.impl.trustlevel.TrustLevel;
import org.onap.cps.ncmp.api.impl.trustlevel.TrustLevelManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class DmiPluginWatchDog {

    private final DmiRestClient dmiRestClient;
    private final NetworkCmProxyDataService networkCmProxyDataService;
    private final TrustLevelManager trustLevelManager;
    private final Map<String, TrustLevel> trustLevelPerDmiPlugin;

    /**
     * This class monitors the trust level of all DMI plugin by checking the health status
     * the resulting trustlevel wil be stored in the relevant cache.
     * The @fixedDelayString is the time interval, in milliseconds, between consecutive checks.
     */
    @Scheduled(fixedDelayString = "${ncmp.timers.trust-evel.dmi-availability-watchdog-ms:30000}")
    public void checkDmiAvailability() {
        trustLevelPerDmiPlugin.entrySet().forEach(entry -> {
            final TrustLevel newDmiTrustLevel;
            final TrustLevel oldDmiTrustLevel = entry.getValue();
            final String dmiServiceName = entry.getKey();
            final String dmiHealthStatus = getDmiHealthStatus(dmiServiceName);
            log.debug("The health status for dmi-plugin: {} is {}", dmiServiceName, dmiHealthStatus);

            if ("UP".equals(dmiHealthStatus)) {
                newDmiTrustLevel = TrustLevel.COMPLETE;
            } else {
                newDmiTrustLevel = TrustLevel.NONE;
            }

            if (oldDmiTrustLevel.equals(newDmiTrustLevel)) {
                log.debug("The Dmi Plugin: {} has already the same trust level: {}", dmiServiceName,
                        newDmiTrustLevel);
            } else {
                trustLevelPerDmiPlugin.put(dmiServiceName, newDmiTrustLevel);

                final Collection<String> notificationCandidateCmHandleIds =
                    networkCmProxyDataService.getAllCmHandleIdsByDmiPluginIdentifier(dmiServiceName);
                for (final String cmHandleId: notificationCandidateCmHandleIds) {
                    trustLevelManager.handleUpdateOfTrustLevels(cmHandleId, newDmiTrustLevel.name());
                }
            }
        });
    }

    private String getDmiHealthStatus(final String dmiServiceName) {
        return dmiRestClient.getDmiHealthStatus(dmiServiceName);
    }
}
