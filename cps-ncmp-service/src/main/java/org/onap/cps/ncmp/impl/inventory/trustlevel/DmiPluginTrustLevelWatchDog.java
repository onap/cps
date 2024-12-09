/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation
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

package org.onap.cps.ncmp.impl.inventory.trustlevel;

import com.hazelcast.map.IMap;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.inventory.models.TrustLevel;
import org.onap.cps.ncmp.impl.dmi.DmiRestClient;
import org.onap.cps.ncmp.impl.inventory.CmHandleQueryService;
import org.onap.cps.ncmp.impl.utils.http.RestServiceUrlTemplateBuilder;
import org.onap.cps.ncmp.impl.utils.http.UrlTemplateParameters;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class DmiPluginTrustLevelWatchDog {

    private final DmiRestClient dmiRestClient;
    private final CmHandleQueryService cmHandleQueryService;
    private final TrustLevelManager trustLevelManager;

    @Qualifier(TrustLevelCacheConfig.TRUST_LEVEL_PER_DMI_PLUGIN)
    private final IMap<String, TrustLevel> trustLevelPerDmiPlugin;

    /**
     * This class monitors the trust level of all DMI plugin by checking the health status
     * the resulting trust level wil be stored in the relevant cache.
     * The @fixedDelayString is the time interval, in milliseconds, between consecutive checks.
     */
    @Scheduled(fixedDelayString = "${ncmp.timers.trust-level.dmi-availability-watchdog-ms:30000}")
    public void checkDmiAvailability() {
        trustLevelPerDmiPlugin.forEach((dmiServiceName, oldDmiTrustLevel) -> {
            final TrustLevel newDmiTrustLevel;
            final String dmiHealthStatus = getDmiHealthStatus(dmiServiceName);
            log.debug("The health status for dmi-plugin: {} is {}", dmiServiceName, dmiHealthStatus);

            if ("UP".equals(dmiHealthStatus)) {
                newDmiTrustLevel = TrustLevel.COMPLETE;
            } else {
                newDmiTrustLevel = TrustLevel.NONE;
            }
            if (oldDmiTrustLevel.equals(newDmiTrustLevel)) {
                log.debug("The Dmi Plugin: {} has already the same trust level: {}", dmiServiceName, newDmiTrustLevel);
            } else {
                final Collection<String> cmHandleIds =
                    cmHandleQueryService.getCmHandleReferencesByDmiPluginIdentifier(dmiServiceName, false);
                trustLevelManager.updateDmi(dmiServiceName, cmHandleIds, newDmiTrustLevel);
            }
        });
    }

    private String getDmiHealthStatus(final String dmiServiceBaseUrl) {
        final UrlTemplateParameters urlTemplateParameters = RestServiceUrlTemplateBuilder.newInstance()
                .createUrlTemplateParametersForHealthCheck(dmiServiceBaseUrl);
        return dmiRestClient.getDmiHealthStatus(urlTemplateParameters).block();
    }
}
