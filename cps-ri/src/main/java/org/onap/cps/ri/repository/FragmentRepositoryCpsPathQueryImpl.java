/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation.
 *  Modifications Copyright (C) 2023 TechMahindra Ltd.
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

package org.onap.cps.ri.repository;

import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.cpspath.parser.CpsPathQuery;
import org.onap.cps.ri.models.AnchorEntity;
import org.onap.cps.ri.models.DataspaceEntity;
import org.onap.cps.ri.models.FragmentEntity;
import org.onap.cps.spi.api.PaginationOption;

@RequiredArgsConstructor
@Slf4j
public class FragmentRepositoryCpsPathQueryImpl implements FragmentRepositoryCpsPathQuery {

    private final FragmentQueryBuilder fragmentQueryBuilder;

    @Override
    @Transactional
    public List<FragmentEntity> findByAnchorAndCpsPath(final AnchorEntity anchorEntity,
                                                       final CpsPathQuery cpsPathQuery) {
        final Query query = fragmentQueryBuilder.getQueryForAnchorAndCpsPath(anchorEntity, cpsPathQuery);
        final List<FragmentEntity> fragmentEntities = query.getResultList();
        log.debug("Fetched {} fragment entities by anchor and cps path.", fragmentEntities.size());
        return fragmentEntities;
    }

    @Override
    @Transactional
    public List<FragmentEntity> findByDataspaceAndCpsPath(final DataspaceEntity dataspaceEntity,
                                                          final CpsPathQuery cpsPathQuery, final List<Long> anchorIds) {
        final Query query = fragmentQueryBuilder.getQueryForDataspaceAndCpsPath(
                dataspaceEntity, cpsPathQuery, anchorIds);
        final List<FragmentEntity> fragmentEntities = query.getResultList();
        log.debug("Fetched {} fragment entities by cps path across all anchors.", fragmentEntities.size());
        return fragmentEntities;
    }

    @Override
    @Transactional
    public List<Long> findAnchorIdsForPagination(final DataspaceEntity dataspaceEntity, final CpsPathQuery cpsPathQuery,
                                                 final PaginationOption paginationOption) {
        final Query query = fragmentQueryBuilder.getQueryForAnchorIdsForPagination(
                dataspaceEntity, cpsPathQuery, paginationOption);
        return query.getResultList();
    }

}
