/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.spi.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.entities.AnchorEntity;
import org.onap.cps.spi.entities.FragmentEntity;
import org.onap.cps.spi.entities.FragmentEntityArranger;
import org.onap.cps.spi.entities.FragmentExtract;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FragmentPrefetchRepository {

    private final FragmentRepository fragmentRepository;
    private final AnchorRepository anchorRepository;

    private static final AnchorEntity ALL_ANCHORS = null;

    /**
     * Prefetch descendants for fragment entities.
     * @param fetchDescendantsOption Fetch descendants option
     * @param anchorEntity Anchor entity
     * @param proxiedFragmentEntities Collection of FragmentEntity
     * @return Collection of FragmentEntity
     */
    public Collection<FragmentEntity> prefetchDescendantsForFragmentEntities(
            final FetchDescendantsOption fetchDescendantsOption,
            final AnchorEntity anchorEntity,
            final Collection<FragmentEntity> proxiedFragmentEntities) {
        if (FetchDescendantsOption.OMIT_DESCENDANTS.equals(fetchDescendantsOption)) {
            return proxiedFragmentEntities;
        }
        final List<Long> fragmentEntityIds = getFragmentIds(proxiedFragmentEntities);
        if (anchorEntity == ALL_ANCHORS) {
            return findAllByIdWithDescendants(fragmentEntityIds, fetchDescendantsOption);
        }
        return findAllByAnchorAndIdWithDescendants(anchorEntity, fragmentEntityIds, fetchDescendantsOption);
    }

    private Collection<FragmentEntity> findAllByAnchorAndIdWithDescendants(
            final AnchorEntity anchorEntity,
            final Collection<Long> ids,
            final FetchDescendantsOption fetchDescendantsOption) {
        final List<FragmentExtract> fragmentExtracts =
            fragmentRepository.findExtractsWithDescendantsByIds(ids, fetchDescendantsOption.getDepth());
        return FragmentEntityArranger.toFragmentEntityTrees(anchorEntity, fragmentExtracts);
    }

    private Collection<FragmentEntity> findAllByIdWithDescendants(
            final Collection<Long> ids,
            final FetchDescendantsOption fetchDescendantsOption) {
        final List<FragmentExtract> fragmentExtracts =
            fragmentRepository.findExtractsWithDescendantsByIds(ids, fetchDescendantsOption.getDepth());

        final Collection<Long> anchorIds = fragmentExtracts.stream()
                .map(FragmentExtract::getAnchorId).collect(Collectors.toSet());
        final List<AnchorEntity> anchorEntities = anchorRepository.findAllById(anchorIds);
        final Map<Long, AnchorEntity> anchorEntityPerId = anchorEntities.stream()
                .collect(Collectors.toMap(AnchorEntity::getId, Function.identity()));

        return FragmentEntityArranger.toFragmentEntityTreesAcrossAnchors(anchorEntityPerId, fragmentExtracts);
    }

    private static List<Long> getFragmentIds(final Collection<FragmentEntity> fragmentEntities) {
        return fragmentEntities.stream().map(FragmentEntity::getId).collect(Collectors.toList());
    }
}
