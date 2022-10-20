/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.spi.entities;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FragmentEntityArranger {

    /**
     * Convert a collection of (related) FragmentExtracts into a FragmentEntity (tree) with descendants.
     * Multiple top level nodes not yet support. If found only the first top level element is returned
     *
     * @param fragmentExtracts FragmentExtracts  to convert
     * @return a FragmentEntity (tree) with descendants, null if none found.
     */
    public static FragmentEntity toFragmentEntityTree(final Collection<FragmentExtract> fragmentExtracts) {
        final Map<Long, FragmentEntity> fragmentEntityPerId = new HashMap<>();
        for (final FragmentExtract fragmentExtract : fragmentExtracts) {
            final FragmentEntity fragmentEntity = new FragmentEntity();
            fragmentEntity.setId(fragmentExtract.getId());
            fragmentEntity.setXpath(fragmentExtract.getXpath());
            fragmentEntity.setAttributes(fragmentExtract.getAttributes());
            fragmentEntity.setParentId(fragmentExtract.getParentId());
            fragmentEntity.setChildFragments(new HashSet<>());
            fragmentEntityPerId.put(fragmentEntity.getId(), fragmentEntity);
        }
        final Collection<FragmentEntity> fragmentEntitiesWithoutParentInResultSet = new HashSet<>();
        for (final FragmentEntity fragmentEntity : fragmentEntityPerId.values()) {
            final FragmentEntity parentFragmentEntity = fragmentEntityPerId.get(fragmentEntity.getParentId());
            if (parentFragmentEntity == null) {
                fragmentEntitiesWithoutParentInResultSet.add(fragmentEntity);
            } else {
                parentFragmentEntity.getChildFragments().add(fragmentEntity);
            }
        }
        if (fragmentEntitiesWithoutParentInResultSet.iterator().hasNext()) {
            return fragmentEntitiesWithoutParentInResultSet.iterator().next();
        }
        return null;
    }
}
