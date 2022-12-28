/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
 *  Modifications Copyright (C) 2022 TechMahindra Ltd.
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

import java.util.*;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FragmentEntityArranger {

    /**
     * Convert a collection of (related) FragmentExtracts into a FragmentEntity (tree) with descendants.
     *
     *
     * @param anchorEntity the anchor(entity) all the fragments belong to
     * @param fragmentExtracts FragmentExtracts to convert
     * @return a collection of FragmentEntity (tree) with descendants, null if none found.
     */
    public static Collection<FragmentEntity>toFragmentEntityTree(final AnchorEntity anchorEntity,
                                                                  final Collection<FragmentExtract> fragmentExtracts) {
        if (!fragmentExtracts.isEmpty()){
            final Map<Long, FragmentEntity> fragmentEntityPerId = new HashMap<>();
            for (final FragmentExtract fragmentExtract : fragmentExtracts) {
                final FragmentEntity fragmentEntity = toFragmentEntity(anchorEntity, fragmentExtract);
                fragmentEntityPerId.put(fragmentEntity.getId(), fragmentEntity);
            }
            return reuniteChildrenWithTheirParents(fragmentEntityPerId);
        }
        return Collections.emptySet();
    }

    private static FragmentEntity toFragmentEntity(final AnchorEntity anchorEntity,
                                                   final FragmentExtract fragmentExtract) {
        final FragmentEntity fragmentEntity = new FragmentEntity();
        fragmentEntity.setAnchor(anchorEntity);
        fragmentEntity.setId(fragmentExtract.getId());
        fragmentEntity.setXpath(fragmentExtract.getXpath());
        fragmentEntity.setAttributes(fragmentExtract.getAttributes());
        fragmentEntity.setParentId(fragmentExtract.getParentId());
        fragmentEntity.setChildFragments(new HashSet<>());
        fragmentEntity.setDataspace(anchorEntity.getDataspace());
        return fragmentEntity;
    }

    private static Collection<FragmentEntity> reuniteChildrenWithTheirParents(final Map<Long, FragmentEntity> fragmentEntityPerId) {
        final Collection<FragmentEntity> fragmentEntitiesWithoutParentInResultSet = new HashSet<>();
        for (final FragmentEntity fragmentEntity : fragmentEntityPerId.values()) {
            final FragmentEntity parentFragmentEntity = fragmentEntityPerId.get(fragmentEntity.getParentId());
            if (parentFragmentEntity == null) {
                fragmentEntitiesWithoutParentInResultSet.add(fragmentEntity);
            } else {
                parentFragmentEntity.getChildFragments().add(fragmentEntity);
            }
        }
        return fragmentEntitiesWithoutParentInResultSet;
    }

}
