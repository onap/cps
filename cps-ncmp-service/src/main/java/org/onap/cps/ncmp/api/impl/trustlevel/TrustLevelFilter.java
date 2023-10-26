/*
 *  ============LICENSE_START=======================================================
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

package org.onap.cps.ncmp.api.impl.trustlevel;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Component
public class TrustLevelFilter {
    private final NetworkCmProxyDataService networkCmProxyDataService;
    private final Map<String, TrustLevel> trustLevelPerDmiPlugin;
    private final Map<String, TrustLevel> trustLevelPerCmHandle;


    /**
     * This method return cm handles that matches with given trust level.
     *
     * @return cm handle ids
     */
    public Collection<String> getCmHandleIdsByTrustLevel(final TrustLevel targetTrustLevel) {
        final Collection<String> selectedCmHandleIds = new HashSet<>();

        for (final Map.Entry<String, TrustLevel> mapEntry : trustLevelPerDmiPlugin.entrySet()) {
            final String dmiPluginIdentifier = mapEntry.getKey();
            final TrustLevel dmiTrustLevel = mapEntry.getValue();
            final Collection<String> candidateCmHandleIds =
                networkCmProxyDataService.getAllCmHandleIdsByDmiPluginIdentifier(dmiPluginIdentifier);
            for (final String candidateCmHandleId : candidateCmHandleIds) {
                final TrustLevel candidateCmHandleTrustLevel = trustLevelPerCmHandle.get(candidateCmHandleId);
                final TrustLevel effectiveTrustlevel =
                    candidateCmHandleTrustLevel.getEffectiveTrustLevel(dmiTrustLevel);
                if (targetTrustLevel.equals(effectiveTrustlevel)) {
                    selectedCmHandleIds.add(candidateCmHandleId);
                }
            }
        }

        return selectedCmHandleIds;
    }

}
