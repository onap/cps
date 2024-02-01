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

    public boolean addMapping(final String cmHandleId, final String alternateId) {
        initializeCache();
        return addMappingWithValidation(cmHandleId, alternateId);
    }


    private boolean addMappingWithValidation(final String cmHandleId, final String alternateId) {
        if (validateDuplication(cmHandleId, alternateId)) {
            return false;
        }
        if (StringUtils.isBlank(alternateId)) {
            return false;
        }
        alternateIdPerCmHandleId.put(cmHandleId, alternateId);
        cmHandleIdPerAlternateId.put(alternateId, cmHandleId);
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

    public boolean isDuplicateId(final String cmHandleId, final String alternateId) {
        return validateDuplication(cmHandleId, alternateId);
    }

    private boolean validateDuplication(final String cmHandleId, final String alternateId) {
        if (!StringUtils.isBlank(alternateId) && cmHandleIdPerAlternateId.get(alternateId) != null) {
            log.warn("The given alternate id was added to the cache already: {}", alternateId);
            return true;
        }
        if (alternateIdPerCmHandleId.get(cmHandleId) != null) {
            log.warn("The given cmhandle id was added to the cache already: {}", cmHandleId);
            return true;
        }
        return false;
    }

    private void initializeCache() {
        if (!cacheIsInitialized) {
            networkCmProxyCmHandleQueryService.getAllCmHandles().forEach(cmHandle ->
                    addMappingWithValidation(cmHandle.getCmHandleId(), cmHandle.getAlternateId())
            );
            log.info("Alternate ID cache initialized from DB with {} cm handle/alternate id pairs ",
                    alternateIdPerCmHandleId.size());
            cacheIsInitialized = true;
        }
    }
}
