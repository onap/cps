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

import com.google.common.collect.Sets;
import com.hazelcast.map.IMap;
import java.util.Collection;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public class TrustLevelComparator implements Comparable<TrustLevel> {

    private final TrustLevel targetTrustLevel;
    private final IMap<String, TrustLevel> trustLevelPerCmHandle;

    @Override
    public int compareTo(@NonNull final TrustLevel other) {
        return Integer.compare(this.targetTrustLevel.getValue(), other.getValue());
    }

    /**
     * This method return cm handles that matches with given trust level.
     *
     * @return cm handle ids.
     */
    public Collection<String> getAllCmHandles() {
        final Collection<String> allCmHandles = Sets.newHashSet();
        trustLevelPerCmHandle.entrySet().forEach(cmHandleTrustLevelEntrySet -> {
            final int respComp = compareTo(cmHandleTrustLevelEntrySet.getValue());
            if (respComp == 0) {
                allCmHandles.add(cmHandleTrustLevelEntrySet.getKey());
            }
        });
        return allCmHandles;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final TrustLevelComparator that = (TrustLevelComparator) o;
        return targetTrustLevel == that.targetTrustLevel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetTrustLevel);
    }
}
