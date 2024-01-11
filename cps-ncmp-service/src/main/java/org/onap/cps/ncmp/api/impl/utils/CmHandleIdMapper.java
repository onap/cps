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

import java.util.Collection;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.NetworkCmProxyCmHandleQueryService;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CmHandleIdMapper {

    private final Map<String, String> alternateIdPerCmHandleId;
    private final Map<String, String> cmHandleIdPerAlternateId;
    private final NetworkCmProxyCmHandleQueryService networkCmProxyCmHandleQueryService;

    private boolean cacheIsInitialized = false;

    public String cmHandleIdToAlternateId(final String cmHandleId) {
        initializeCache();
        return alternateIdPerCmHandleId.get(cmHandleId);
    }

    public String alternateIdToCmHandleId(final String alternateId) {
        initializeCache();
        return cmHandleIdPerAlternateId.get(alternateId);
    }

    public void addMapping(final String cmHandleId, final String alternateId) {
        initializeCache();
        addMappingWithValidation(cmHandleId, alternateId);
    }

    private void addMappingWithValidation(final String cmHandleId, final String alternateId) {
        if (alternateIdPerCmHandleId.containsKey(cmHandleId)) {
            if (!alternateIdPerCmHandleId.get(cmHandleId).equals(alternateId)) {
                log.warn("Update ignored: cannot change alternate id: {} already has an alternate id {}", cmHandleId,
                        alternateId);
            }
        } else {
            alternateIdPerCmHandleId.put(cmHandleId, alternateId);
            cmHandleIdPerAlternateId.put(alternateId, cmHandleId);
        }
    }

    public void removeMapping(final String cmHandleId) {
        final String alternateId = alternateIdPerCmHandleId.remove(cmHandleId);
        cmHandleIdPerAlternateId.remove(alternateId);
    }

    private void initializeCache() {
        if (!cacheIsInitialized) {
            final Collection<NcmpServiceCmHandle> allCmHandles = networkCmProxyCmHandleQueryService.getAllCmHandles();
            allCmHandles.forEach(cmHandle -> {
                if (cmHandle.getAlternateId() != null) {
                    alternateIdPerCmHandleId.putIfAbsent(cmHandle.getCmHandleId(), cmHandle.getAlternateId());
                    cmHandleIdPerAlternateId.putIfAbsent(cmHandle.getAlternateId(), cmHandle.getCmHandleId());
                }
            });
            log.info("Alternate ID cache initialized from DB with {} cm handle/alternate id pairs ",
                    alternateIdPerCmHandleId.size());
            cacheIsInitialized = true;
        }
    }
}
