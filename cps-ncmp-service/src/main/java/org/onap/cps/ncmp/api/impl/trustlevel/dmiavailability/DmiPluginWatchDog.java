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

import static org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence.NCMP_DMI_REGISTRY_PARENT;
import static org.onap.cps.spi.FetchDescendantsOption.DIRECT_CHILDREN_ONLY;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.api.impl.client.DmiRestClient;
import org.onap.cps.ncmp.api.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.api.impl.trustlevel.TrustLevel;
import org.onap.cps.ncmp.api.impl.trustlevel.TrustLevelManager;
import org.onap.cps.spi.model.DataNode;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class DmiPluginWatchDog implements ApplicationListener<ApplicationReadyEvent> {

    private final DmiRestClient dmiRestClient;
    private final NetworkCmProxyDataService networkCmProxyDataService;
    private final InventoryPersistence inventoryPersistence;
    private final TrustLevelManager trustLevelManager;
    private final Map<String, TrustLevel> trustLevelPerDmiPlugin;

    /**
     * This class monitors the trust level of all DMI plugin by checking the health status
     * the resulting trustlevel wil be stored in the relevant cache.
     * The @fixedDelayString is the time interval, in milliseconds, between consecutive checks.
     */
    @Scheduled(fixedDelayString = "${ncmp.timers.trust-evel.dmi-availability-watchdog-ms:30000}")
    public void checkDmiAvailability() {
        trustLevelPerDmiPlugin.entrySet().forEach(dmiTrustLevel -> {
            TrustLevel newDmiTrustLevel = TrustLevel.NONE;
            final TrustLevel oldDmiTrustLevel = dmiTrustLevel.getValue();
            final String dmiHealthStatus = getDmiHealthStatus(dmiTrustLevel.getKey());
            log.debug("The health status for dmi-plugin: {} is {}", dmiTrustLevel.getKey(), dmiHealthStatus);

            if ("UP".equals(dmiHealthStatus)) {
                newDmiTrustLevel = TrustLevel.COMPLETE;
            }

            if (oldDmiTrustLevel.equals(newDmiTrustLevel)) {
                log.debug("The Dmi Plugin: {} has already the same trust level: {}", dmiTrustLevel.getKey(),
                        newDmiTrustLevel);
            } else {
                dmiTrustLevel.setValue(newDmiTrustLevel);

                final Collection<String> notificationCandidateCmHandleIds =
                    networkCmProxyDataService.getAllCmHandleIdsByDmiPluginIdentifier(dmiTrustLevel.getKey());
                for (final String cmHandleId: notificationCandidateCmHandleIds) {
                    trustLevelManager.handleUpdateOfTrustLevels(cmHandleId, newDmiTrustLevel.name());
                }
            }
        });
    }

    /**
     * This method listens to ApplicationReadyEvent, which is triggered when the
     * CPS application has fully started and is ready. Upon receiving this,
     * it initialises dmi cache from database. This should not only depend on initial registration.
     *
     * @param applicationReadyEvent the event to respond to
     */
    @Override
    public void onApplicationEvent(final ApplicationReadyEvent applicationReadyEvent) {
        final Map<String, Set<String>> cmHandleIdsPerDmi = getAllCmHandleIdsPerDmiPlugin();
        final Set<Map.Entry<String, Set<String>>> cmHandleIdsPerDmiEntries = cmHandleIdsPerDmi.entrySet();
        for (final Map.Entry<String, Set<String>> cmHandleIdsPerDmiEntry: cmHandleIdsPerDmiEntries) {
            final String dmiServiceName = cmHandleIdsPerDmiEntry.getKey();
            final Set<String> cmHandleIds = cmHandleIdsPerDmiEntry.getValue();
            final TrustLevel effectiveTrustLevel;
            final String dmiHealthStatus = getDmiHealthStatus(dmiServiceName);
            if ("UP".equals(dmiHealthStatus)) {
                effectiveTrustLevel = TrustLevel.COMPLETE.getEffectiveTrustLevel(TrustLevel.COMPLETE);
            } else {
                effectiveTrustLevel = TrustLevel.NONE.getEffectiveTrustLevel(TrustLevel.COMPLETE);
            }
            trustLevelPerDmiPlugin.put(dmiServiceName, effectiveTrustLevel);
            for (final String cmHandleId: cmHandleIds) {
                trustLevelManager.handleRestartCpsNcmpApplication(cmHandleId, effectiveTrustLevel.name());
            }
        }
    }

    private Map<String, Set<String>> getAllCmHandleIdsPerDmiPlugin() {
        final Map<String, Set<String>> cmHandleIdsPerDmi = new HashMap<>();
        final DataNode dmiRegistryRootDataNode = inventoryPersistence
            .getDataNode(NCMP_DMI_REGISTRY_PARENT, DIRECT_CHILDREN_ONLY).iterator().next();
        for (final DataNode childDataNode: dmiRegistryRootDataNode.getChildDataNodes()) {
            final String cmHandleId = (String) childDataNode.getLeaves().get("id");
            final String dmiServiceName = resolveDmiServiceName(childDataNode);
            if (cmHandleIdsPerDmi.containsKey(dmiServiceName)) {
                cmHandleIdsPerDmi.get(dmiServiceName).add(cmHandleId);
            } else {
                cmHandleIdsPerDmi.put(dmiServiceName, new HashSet<>(Arrays.asList(cmHandleId)));
            }
        }
        return cmHandleIdsPerDmi;
    }

    private String resolveDmiServiceName(final DataNode childDataNode) {
        final String dmiServiceName = (String) childDataNode.getLeaves().get("dmi-service-name");
        if (StringUtils.isEmpty(dmiServiceName)) {
            return  (String) childDataNode.getLeaves().get("dmi-data-service-name");
        }
        return dmiServiceName;
    }

    private String getDmiHealthStatus(final String dmiServiceName) {
        return dmiRestClient.getDmiHealthStatus(dmiServiceName);
    }
}
