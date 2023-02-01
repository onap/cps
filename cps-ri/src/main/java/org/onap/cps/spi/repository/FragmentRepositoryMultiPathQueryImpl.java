/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation.
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
import java.util.Collections;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.spi.entities.FragmentEntity;

@Slf4j
@RequiredArgsConstructor
public class FragmentRepositoryMultiPathQueryImpl implements FragmentRepositoryMultiPathQuery {

    @PersistenceContext
    private final EntityManager entityManager;

    private final TempTableCreator tempTableCreator;

    @Override
    @Transactional
    public List<FragmentEntity> findByAnchorAndMultipleCpsPaths(final Integer anchorId,
                                                                final Collection<String> cpsPathQueryList) {
        if (cpsPathQueryList.isEmpty()) {
            return Collections.emptyList();
        }
        final String tempTableName = tempTableCreator.createTemporaryTable(
            "xpathTemporaryTable", cpsPathQueryList, "xpath");
        final String sql = String.format(
            "SELECT * FROM FRAGMENT WHERE anchor_id = %d AND xpath IN (select xpath FROM %s);",
            anchorId, tempTableName);
        final List<FragmentEntity> fragmentEntities = entityManager.createNativeQuery(sql, FragmentEntity.class)
            .getResultList();
        log.debug("Fetched {} fragment entities by anchor and cps path.", fragmentEntities.size());
        return fragmentEntities;
    }
}
