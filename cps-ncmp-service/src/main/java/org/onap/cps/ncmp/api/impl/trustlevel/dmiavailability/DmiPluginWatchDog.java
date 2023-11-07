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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.client.DmiRestClient;
import org.onap.cps.ncmp.api.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.api.impl.operations.RequiredDmiService;
import org.onap.cps.ncmp.api.impl.trustlevel.TrustLevel;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.spi.model.DataNode;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class DmiPluginWatchDog implements ApplicationListener<ApplicationReadyEvent> {
    private final InventoryPersistence inventoryPersistence;
    private final DmiRestClient dmiRestClient;
    private final Map<String, TrustLevel> trustLevelPerDmiPlugin;

    /**
     * This method monitors the trust level of all DMI plugin by checking the health status
     * the resulting trust level wil be stored in the relevant cache.
     * The @fixedDelayString is the time interval, in milliseconds, between consecutive checks.
     */
    @Scheduled(fixedDelayString = "${ncmp.timers.trust-evel.dmi-availability-watchdog-ms:30000}")
    public void watchDmiPluginTrustLevel() {
        final Set<String> allDmiServiceNames = trustLevelPerDmiPlugin.keySet();
        for (final String dmiServiceName: allDmiServiceNames) {
            getDmiHealthAndPopulateTrustLevel(dmiServiceName);
        }
    }

    private void getDmiHealthAndPopulateTrustLevel(final String dmiServiceName) {
        final String dmiHealthStatus = dmiRestClient.getDmiHealthStatus(dmiServiceName);
        if ("UP".equals(dmiHealthStatus)) {
            trustLevelPerDmiPlugin.put(dmiServiceName, TrustLevel.COMPLETE);
        } else {
            trustLevelPerDmiPlugin.put(dmiServiceName, TrustLevel.NONE);
        }
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
        final Set<String> dmiServiceNames = new HashSet<>();
        final Set<String> allCmHandleIds = getAllCmHandleIds();
        final Collection<YangModelCmHandle> allYangModelCmHandles =
            inventoryPersistence.getYangModelCmHandles(allCmHandleIds);

        for (final YangModelCmHandle yangModelCmHandle: allYangModelCmHandles) {
            final String dmiServiceName = yangModelCmHandle.resolveDmiServiceName(RequiredDmiService.DATA);
            dmiServiceNames.add(dmiServiceName);
        }

        for (final String dmiServiceName: dmiServiceNames) {
            getDmiHealthAndPopulateTrustLevel(dmiServiceName);
        }
    }

    private Set<String> getAllCmHandleIds() {
        final Set<String> allCmHandleIds = new HashSet<>();
        final DataNode dmiRegistryRootDataNode = inventoryPersistence
            .getDataNode(NCMP_DMI_REGISTRY_PARENT, DIRECT_CHILDREN_ONLY).iterator().next();
        for (final DataNode childDataNode: dmiRegistryRootDataNode.getChildDataNodes()) {
            allCmHandleIds.add((String) childDataNode.getLeaves().get("id"));
        }
        return allCmHandleIds;
    }
}
