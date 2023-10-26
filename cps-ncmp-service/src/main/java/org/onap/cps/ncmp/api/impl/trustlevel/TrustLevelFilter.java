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
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;

@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TrustLevelFilter implements Comparator<TrustLevel> {

    @EqualsAndHashCode.Include
    private final TrustLevel targetTrustLevel;
    private final NetworkCmProxyDataService networkCmProxyDataService;
    private final Map<String, TrustLevel> trustLevelPerDmiPlugin;
    private final Map<String, TrustLevel> trustLevelPerCmHandle;

    /**
     * if left < right then return -1
     * if left > right then return 1
     * if left == right then return 0
     *
     * @param left the first object to be compared.
     * @param right the second object to be compared.
     *
     * @return integer value
     */
    @Override
    public int compare(@NonNull final TrustLevel left, @NonNull final TrustLevel right) {
        return left.compareTo(right);
    }

    /**
     * This method return cm handles that matches with given trust level.
     *
     * @return cm handle ids
     */
    public Collection<String> getAllCmHandleIdsByTargetTrustLevel() {
        final Collection<String> resultCmHandleIds = new HashSet<>();

        trustLevelPerDmiPlugin.keySet().forEach(dmiKey -> {
            final Collection<String> cmHandleKeySet =
                networkCmProxyDataService.getAllCmHandleIdsByDmiPluginIdentifier(dmiKey);

            cmHandleKeySet.forEach(cmHandleKey -> {
                final TrustLevel dmiTrustLevel = trustLevelPerDmiPlugin.get(dmiKey);
                final TrustLevel chTrustLevel = trustLevelPerCmHandle.get(cmHandleKey);
                int result = compare(dmiTrustLevel, chTrustLevel);
                if (result < 0) { // dmi has the lower value
                    if (targetTrustLevel.equals(dmiTrustLevel)) {
                        resultCmHandleIds.add(cmHandleKey);
                    }
                } else { // ch has the lower value
                    if (targetTrustLevel.equals(chTrustLevel)) {
                        resultCmHandleIds.add(cmHandleKey);
                    }
                }
            });

        });

        return resultCmHandleIds;
    }

}
