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
import org.onap.cps.ncmp.api.NetworkCmProxyCmHandleQueryService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CmHandleIdMapper {

    private final Map<String, String> alternateIdPerCmHandle;
    private final Map<String, String> cmHandlePerAlternateId;
    private final NetworkCmProxyCmHandleQueryService networkCmProxyCmHandleQueryService;

    private static boolean CACHE_IS_INITIALIZED = false;

    public String cmHandleIdToAlternateId(final String cmHandleId) {
        initializeCache();
        return alternateIdPerCmHandle.get(cmHandleId);
    }

    public String alternateIdToCmHandleId(final String alternateId) {
        initializeCache();
        return cmHandlePerAlternateId.get(alternateId);
    }

    /**
     * adds a cm handle id - alternate id pair to the distributed caches.
     *
     * @param cmHandleId as a string.
     * @param alternateId as a string.
     */
    public void addMapping(final String cmHandleId, final String alternateId) {
        initializeCache();
        cmHandlePerAlternateId.putIfAbsent(cmHandleId, alternateId);
        alternateIdPerCmHandle.putIfAbsent(alternateId, cmHandleId);
    }

    /**
     * removes a cm handle id - alternate id pair from the distributed caches.
     *
     * @param cmHandleId as a string.
     * @param alternateId as a string.
     */
    public void removeMapping(final String cmHandleId, final String alternateId) {
        cmHandlePerAlternateId.remove(cmHandleId);
        alternateIdPerCmHandle.remove(alternateId);
    }

    public static boolean isCacheIsInitialized() {
        return CACHE_IS_INITIALIZED;
    }

    public static void setCacheIsInitialized(final boolean cacheIsInitialized) {
        CACHE_IS_INITIALIZED = cacheIsInitialized;
    }

    private void initializeCache() {
        if (!isCacheIsInitialized()) {
            setCacheIsInitialized(true);
            if (alternateIdPerCmHandle.isEmpty()) {
                networkCmProxyCmHandleQueryService.getAllCmHandles().forEach(cmHandle -> {
                    if (cmHandle.getAlternateId() != null) {
                        cmHandlePerAlternateId.putIfAbsent(cmHandle.getCmHandleId(), cmHandle.getAlternateId());
                        alternateIdPerCmHandle.putIfAbsent(cmHandle.getAlternateId(), cmHandle.getCmHandleId());
                    }
                });
            }
        }
    }
}
