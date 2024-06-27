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

import java.util.Collection;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.events.avc.ncmptoclient.AvcEventPublisher;
import org.onap.cps.ncmp.api.inventory.models.TrustLevel;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.models.RequiredDmiService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrustLevelManager {

    @Qualifier(TrustLevelCacheConfig.TRUST_LEVEL_PER_CM_HANDLE)
    private final Map<String, TrustLevel> trustLevelPerCmHandle;

    @Qualifier(TrustLevelCacheConfig.TRUST_LEVEL_PER_DMI_PLUGIN)
    private final Map<String, TrustLevel> trustLevelPerDmiPlugin;

    private final InventoryPersistence inventoryPersistence;
    private final AvcEventPublisher avcEventPublisher;
    private static final String AVC_CHANGED_ATTRIBUTE_NAME = "trustLevel";
    private static final String AVC_NO_OLD_VALUE = null;

    /**
     * Add cmHandles to the cache and publish notification for initial trust level of cmHandles if it is NONE.
     *
     * @param cmHandlesToBeCreated a list of cmHandles being created
     */
    public void handleInitialRegistrationOfTrustLevels(final Map<String, TrustLevel> cmHandlesToBeCreated) {
        for (final Map.Entry<String, TrustLevel> entry : cmHandlesToBeCreated.entrySet()) {
            final String cmHandleId = entry.getKey();
            if (trustLevelPerCmHandle.containsKey(cmHandleId)) {
                log.warn("Cm handle: {} already registered", cmHandleId);
            } else {
                TrustLevel initialTrustLevel = entry.getValue();
                if (initialTrustLevel == null) {
                    initialTrustLevel = TrustLevel.COMPLETE;
                }
                trustLevelPerCmHandle.put(cmHandleId, initialTrustLevel);
                if (TrustLevel.NONE.equals(initialTrustLevel)) {
                    avcEventPublisher.publishAvcEvent(cmHandleId,
                        AVC_CHANGED_ATTRIBUTE_NAME,
                        AVC_NO_OLD_VALUE,
                        initialTrustLevel.name());
                }
            }
        }
    }

    /**
     * Updates trust level of dmi plugin in the cache and publish notification for trust level of cmHandles if it
     * has changed.
     *
     * @param dmiServiceName        dmi service name
     * @param affectedCmHandleIds   cm handle ids belonging to dmi service name
     * @param newDmiTrustLevel      new trust level of the dmi plugin
     */
    public void handleUpdateOfDmiTrustLevel(final String dmiServiceName,
                                            final Collection<String> affectedCmHandleIds,
                                            final TrustLevel newDmiTrustLevel) {
        final TrustLevel oldDmiTrustLevel  = trustLevelPerDmiPlugin.get(dmiServiceName);
        trustLevelPerDmiPlugin.put(dmiServiceName, newDmiTrustLevel);
        for (final String affectedCmHandleId : affectedCmHandleIds) {
            final TrustLevel deviceTrustLevel = trustLevelPerCmHandle.get(affectedCmHandleId);
            final TrustLevel oldEffectiveTrustLevel = deviceTrustLevel.getEffectiveTrustLevel(oldDmiTrustLevel);
            final TrustLevel newEffectiveTrustLevel = deviceTrustLevel.getEffectiveTrustLevel(newDmiTrustLevel);
            sendAvcNotificationIfRequired(affectedCmHandleId, oldEffectiveTrustLevel, newEffectiveTrustLevel);
        }
    }

    /**
     * Updates trust level of device in the cache and publish notification for trust level of device if it has
     * changed.
     *
     * @param cmHandleId            cm handle id
     * @param newDeviceTrustLevel   new trust level of the device
     */
    public void handleUpdateOfDeviceTrustLevel(final String cmHandleId,
                                               final TrustLevel newDeviceTrustLevel) {
        final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(cmHandleId);
        final String dmiServiceName = yangModelCmHandle.resolveDmiServiceName(RequiredDmiService.DATA);

        final TrustLevel dmiTrustLevel = trustLevelPerDmiPlugin.get(dmiServiceName);
        final TrustLevel oldDeviceTrustLevel = trustLevelPerCmHandle.get(cmHandleId);

        final TrustLevel oldEffectiveTrustLevel = oldDeviceTrustLevel.getEffectiveTrustLevel(dmiTrustLevel);
        final TrustLevel newEffectiveTrustLevel = newDeviceTrustLevel.getEffectiveTrustLevel(dmiTrustLevel);

        trustLevelPerCmHandle.put(cmHandleId, newDeviceTrustLevel);
        sendAvcNotificationIfRequired(cmHandleId, oldEffectiveTrustLevel, newEffectiveTrustLevel);
    }

    private void sendAvcNotificationIfRequired(final String notificationCandidateCmHandleId,
                                               final TrustLevel oldEffectiveTrustLevel,
                                               final TrustLevel newEffectiveTrustLevel) {
        if (oldEffectiveTrustLevel.equals(newEffectiveTrustLevel)) {
            log.debug("The Cm Handle: {} has already the same trust level: {}", notificationCandidateCmHandleId,
                newEffectiveTrustLevel);
        } else {
            log.info("The trust level for Cm Handle: {} is now: {} ", notificationCandidateCmHandleId,
                newEffectiveTrustLevel);
            avcEventPublisher.publishAvcEvent(notificationCandidateCmHandleId,
                AVC_CHANGED_ATTRIBUTE_NAME,
                oldEffectiveTrustLevel.name(),
                newEffectiveTrustLevel.name());
        }
    }

}
