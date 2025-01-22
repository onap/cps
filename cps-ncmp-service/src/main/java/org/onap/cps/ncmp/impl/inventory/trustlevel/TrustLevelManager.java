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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistration;
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle;
import org.onap.cps.ncmp.api.inventory.models.TrustLevel;
import org.onap.cps.ncmp.impl.dmi.DmiServiceNameResolver;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.models.RequiredDmiService;
import org.onap.cps.ncmp.utils.events.CmAvcEventPublisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrustLevelManager {

    @Qualifier(TrustLevelCacheConfig.TRUST_LEVEL_PER_CM_HANDLE)
    private final IMap<String, TrustLevel> trustLevelPerCmHandleId;

    @Qualifier(TrustLevelCacheConfig.TRUST_LEVEL_PER_DMI_PLUGIN)
    private final IMap<String, TrustLevel> trustLevelPerDmiPlugin;

    private final InventoryPersistence inventoryPersistence;
    private final CmAvcEventPublisher cmAvcEventPublisher;
    private static final String AVC_CHANGED_ATTRIBUTE_NAME = "trustLevel";
    private static final String AVC_NO_OLD_VALUE = null;

    /**
     * Add dmi plugins to the cache.
     *
     * @param dmiPluginRegistration a dmi plugin being registered
     */
    public void registerDmiPlugin(final DmiPluginRegistration dmiPluginRegistration) {
        final String dmiServiceName = DmiServiceNameResolver.resolveDmiServiceName(RequiredDmiService.DATA,
                dmiPluginRegistration);
        trustLevelPerDmiPlugin.putAsync(dmiServiceName, TrustLevel.COMPLETE);
    }

    /**
     * Add cmHandles to the cache and publish notification for initial trust level of cmHandles if it is NONE.
     *
     * @param cmHandlesToBeCreated a list of cmHandles being created
     */
    public void registerCmHandles(final Map<String, TrustLevel> cmHandlesToBeCreated) {
        final Map<String, TrustLevel> trustLevelPerCmHandleIdForCache = new HashMap<>();
        for (final Map.Entry<String, TrustLevel> entry : cmHandlesToBeCreated.entrySet()) {
            final String cmHandleId = entry.getKey();
            TrustLevel initialTrustLevel = entry.getValue();
            if (initialTrustLevel == null) {
                initialTrustLevel = TrustLevel.COMPLETE;
            }
            trustLevelPerCmHandleIdForCache.put(cmHandleId, initialTrustLevel);
            if (TrustLevel.NONE.equals(initialTrustLevel)) {
                cmAvcEventPublisher.publishAvcEvent(cmHandleId,
                        AVC_CHANGED_ATTRIBUTE_NAME,
                        AVC_NO_OLD_VALUE,
                        initialTrustLevel.name());
            }
        }
        trustLevelPerCmHandleId.putAllAsync(trustLevelPerCmHandleIdForCache);
    }

    /**
     * Updates trust level of dmi plugin in the cache and publish notification for trust level of cmHandles if it
     * has changed.
     *
     * @param dmiServiceName        dmi service name
     * @param affectedCmHandleIds   cm handle ids belonging to dmi service name
     * @param newDmiTrustLevel      new trust level of the dmi plugin
     */
    public void updateDmi(final String dmiServiceName,
                          final Collection<String> affectedCmHandleIds,
                          final TrustLevel newDmiTrustLevel) {
        final TrustLevel oldDmiTrustLevel  = trustLevelPerDmiPlugin.get(dmiServiceName);
        trustLevelPerDmiPlugin.putAsync(dmiServiceName, newDmiTrustLevel);
        for (final String affectedCmHandleId : affectedCmHandleIds) {
            final TrustLevel cmHandleTrustLevel = trustLevelPerCmHandleId.get(affectedCmHandleId);
            final TrustLevel oldEffectiveTrustLevel = cmHandleTrustLevel.getEffectiveTrustLevel(oldDmiTrustLevel);
            final TrustLevel newEffectiveTrustLevel = cmHandleTrustLevel.getEffectiveTrustLevel(newDmiTrustLevel);
            sendAvcNotificationIfRequired(affectedCmHandleId, oldEffectiveTrustLevel, newEffectiveTrustLevel);
        }
    }

    /**
     * Updates trust level of device in the cache and publish notification for trust level of device if it has
     * changed.
     *
     * @param cmHandleId            cm handle id
     * @param newCmHandleTrustLevel   new trust level of the device
     */
    public void updateCmHandleTrustLevel(final String cmHandleId,
                                         final TrustLevel newCmHandleTrustLevel) {
        final String dmiServiceName = getDmiServiceName(cmHandleId);

        final TrustLevel dmiTrustLevel = trustLevelPerDmiPlugin.get(dmiServiceName);
        final TrustLevel oldCmHandleTrustLevel = trustLevelPerCmHandleId.get(cmHandleId);

        final TrustLevel oldEffectiveTrustLevel = oldCmHandleTrustLevel.getEffectiveTrustLevel(dmiTrustLevel);
        final TrustLevel newEffectiveTrustLevel = newCmHandleTrustLevel.getEffectiveTrustLevel(dmiTrustLevel);

        trustLevelPerCmHandleId.putAsync(cmHandleId, newCmHandleTrustLevel);
        sendAvcNotificationIfRequired(cmHandleId, oldEffectiveTrustLevel, newEffectiveTrustLevel);
    }

    /**
     * Apply effective trust levels for a collection of cm handles.
     * Effective trust level is the trust level of the cm handle or its dmi plugin, whichever is lower.
     *
     * @param ncmpServiceCmHandles a collection of cm handles to apply trust levels to
     */
    public void applyEffectiveTrustLevels(final Collection<NcmpServiceCmHandle> ncmpServiceCmHandles) {
        final Set<String> cmHandleIds = getCmHandleIds(ncmpServiceCmHandles);
        final Map<String, TrustLevel> trustLevelPerCmHandleIdInBatch = trustLevelPerCmHandleId.getAll(cmHandleIds);
        final Map<String, TrustLevel> trustLevelPerDmiPluginInBatch = new HashMap<>(trustLevelPerDmiPlugin);
        for (final NcmpServiceCmHandle ncmpServiceCmHandle : ncmpServiceCmHandles) {
            final String cmHandleId = ncmpServiceCmHandle.getCmHandleId();
            final String dmiDataServiceName = DmiServiceNameResolver.resolveDmiServiceName(RequiredDmiService.DATA,
                    ncmpServiceCmHandle);
            final TrustLevel dmiTrustLevel = trustLevelPerDmiPluginInBatch.getOrDefault(dmiDataServiceName,
                    TrustLevel.NONE);
            final TrustLevel cmHandleTrustLevel = trustLevelPerCmHandleIdInBatch.getOrDefault(cmHandleId,
                    TrustLevel.NONE);
            final TrustLevel effectiveTrustLevel = dmiTrustLevel.getEffectiveTrustLevel(cmHandleTrustLevel);
            ncmpServiceCmHandle.setCurrentTrustLevel(effectiveTrustLevel);
        }
    }

    /**
     * Apply effective trust level to a cm handle.
     * Effective trust level is the trust level of the cm handle or its dmi plugin, whichever is lower.
     *
     * @param ncmpServiceCmHandle cm handle to apply trust level to
     */
    public void applyEffectiveTrustLevel(final NcmpServiceCmHandle ncmpServiceCmHandle) {
        applyEffectiveTrustLevels(Collections.singletonList(ncmpServiceCmHandle));
    }

    /**
     * Remove cm handle trust level from the cache.
     *
     * @param cmHandleIds       cm handle ids to be removed from the cache
     */
    public void removeCmHandles(final Collection<String> cmHandleIds) {
        for (final String cmHandleId : cmHandleIds) {
            trustLevelPerCmHandleId.removeAsync(cmHandleId);
        }
    }

    private Set<String> getCmHandleIds(final Collection<NcmpServiceCmHandle> ncmpServiceCmHandles) {
        return ncmpServiceCmHandles.stream()
                .map(NcmpServiceCmHandle::getCmHandleId)
                .collect(Collectors.toUnmodifiableSet());
    }

    private String getDmiServiceName(final String cmHandleId) {
        final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(cmHandleId);
        return DmiServiceNameResolver.resolveDmiServiceName(RequiredDmiService.DATA, yangModelCmHandle);
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
            cmAvcEventPublisher.publishAvcEvent(notificationCandidateCmHandleId,
                    AVC_CHANGED_ATTRIBUTE_NAME,
                    oldEffectiveTrustLevel.name(),
                    newEffectiveTrustLevel.name());
        }
    }

}
