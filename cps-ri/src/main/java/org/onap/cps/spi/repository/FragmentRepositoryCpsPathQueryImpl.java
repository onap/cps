/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
 *  Modifications Copyright (C) 2023 Tech Mahindra
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.cpspath.parser.CpsPathBooleanOperatorsType;
import org.onap.cps.cpspath.parser.CpsPathQuery;
import org.onap.cps.spi.entities.FragmentEntity;

@RequiredArgsConstructor
@Slf4j
public class FragmentRepositoryCpsPathQueryImpl implements FragmentRepositoryCpsPathQuery {

    @PersistenceContext
    private EntityManager entityManager;

    private final FragmentQueryBuilder fragmentQueryBuilder;

    @Override
    @Transactional
    public List<FragmentEntity> findByAnchorAndCpsPath(final int anchorId, final CpsPathQuery cpsPathQuery) {
        final Map<String, Object> leaves = cpsPathQuery.getLeavesData();
        final List<String> booleanOperators = cpsPathQuery.getBooleanOperatorsTypes();
        final CpsPathBooleanOperatorsType booleanOperatorsTypes = CpsPathBooleanOperatorsType.OR;
        if (booleanOperators == null || !booleanOperators.contains(booleanOperatorsTypes.getValues())) {
            final Query query = fragmentQueryBuilder.getQueryForAnchorAndCpsPath(anchorId, cpsPathQuery, leaves);
            final List<FragmentEntity> fragmentEntities = query.getResultList();
            log.debug("Fetched {} fragment entities by anchor and cps path.", fragmentEntities.size());
            return fragmentEntities;
        } else {
            final List<FragmentEntity> fragmentEntityList = new ArrayList<>();
            for (final Map.Entry<String, Object> entry : leaves.entrySet()) {
                final Map<String, Object> leaveEntry = new HashMap<>();
                leaveEntry.put(entry.getKey(), entry.getValue());
                final Query query = fragmentQueryBuilder.getQueryForAnchorAndCpsPath(anchorId, cpsPathQuery,
                                                                                     leaveEntry);
                final List<FragmentEntity> fragmentEntities = query.getResultList();
                log.debug("Fetched {} fragment entities by anchor and cps path.", fragmentEntities.size());
                fragmentEntityList.addAll(fragmentEntities);
            }
            return fragmentEntityList;
        }
    }
}
