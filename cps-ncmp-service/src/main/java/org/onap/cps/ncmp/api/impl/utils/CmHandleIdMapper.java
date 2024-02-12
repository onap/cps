/*
 * ============LICENSE_START========================================================
 * Copyright (c) 2024 Nordix Foundation.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.utils;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.onap.cps.ncmp.api.NetworkCmProxyCmHandleQueryService;
import org.onap.cps.ncmp.api.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.spi.exceptions.DataNodeNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CmHandleIdMapper {

    private final Map<String, String> alternateIdPerCmHandleId;
    private final Map<String, String> cmHandleIdPerAlternateId;
    private final NetworkCmProxyCmHandleQueryService networkCmProxyCmHandleQueryService;
    private final InventoryPersistence inventoryPersistence;

    private boolean cacheIsInitialized = false;

    public String cmHandleIdToAlternateId(final String cmHandleId) {
        initializeCache();
        return alternateIdPerCmHandleId.get(cmHandleId);
    }

    public String alternateIdToCmHandleId(final String alternateId) {
        initializeCache();
        return cmHandleIdPerAlternateId.get(alternateId);
    }

    /**
     *  A method that decides to add mapping for alternate id by searching for the current alternate id in the database.
     *
     * @param cmHandleId cm handle id
     * @param newAlternateId new alternate id
     * @return true if the new alternate id not in use or equal to current alternate id, false otherwise
     */
    public boolean addMapping(final String cmHandleId, final String newAlternateId) {
        String currentAlternateId = "";
        try {
            final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(cmHandleId);
            currentAlternateId = yangModelCmHandle.getAlternateId();
        } catch (final DataNodeNotFoundException dataNodeNotFoundException) {
            // work with blank current alternate id
        }
        return addMapping(cmHandleId, currentAlternateId, newAlternateId);
    }

    /**
     *  A method that decides to add mapping for alternate id.
     *
     * @param cmHandleId   cm handle id
     * @param currentAlternateId current alternate id
     * @param newAlternateId new alternate id
     * @return true if the new alternate id not in use or equal to current alternate id, false otherwise
     */
    public boolean addMapping(final String cmHandleId, final String currentAlternateId, final String newAlternateId) {
        if (StringUtils.isBlank(currentAlternateId)) {
            if (alternateIdInUse(newAlternateId)) {
                log.warn("Alternate id update ignored, cannot update cm handle {}, alternate id is already "
                    + "assigned to a different cm handle", cmHandleId);
                return false;
            }
            return true;
        }
        if (currentAlternateId.equals(newAlternateId)) {
            return true;
        }
        log.warn("Alternate id update ignored, cannot update cm handle {}, already has an alternate id of {}",
            cmHandleId, currentAlternateId);
        return false;
    }

    private boolean alternateIdInUse(final String alternateId) {
        try {
            inventoryPersistence.getCmHandleDataNodeByAlternateId(alternateId);
        } catch (final DataNodeNotFoundException dataNodeNotFoundException) {
            return false;
        }
        return true;
    }

    public void removeMapping(final String cmHandleId) {
        final String alternateId = alternateIdPerCmHandleId.remove(cmHandleId);
        removeAlternateIdWithValidation(alternateId);
    }

    private void removeAlternateIdWithValidation(final String alternateId) {
        if (alternateId != null) {
            cmHandleIdPerAlternateId.remove(alternateId);
        }
    }

    private void initializeCache() {
        if (!cacheIsInitialized) {
            networkCmProxyCmHandleQueryService.getAllCmHandles().forEach(cmHandle ->
                addMapping(cmHandle.getCmHandleId(), cmHandle.getAlternateId())
            );
            log.info("Alternate ID cache initialized from DB with {} cm handle/alternate id pairs ",
                    alternateIdPerCmHandleId.size());
            cacheIsInitialized = true;
        }
    }
}
