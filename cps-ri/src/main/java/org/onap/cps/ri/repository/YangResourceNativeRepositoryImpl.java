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

package org.onap.cps.ri.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.type.StandardBasicTypes;
import org.onap.cps.api.model.ModuleReference;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Repository
public class YangResourceNativeRepositoryImpl implements YangResourceNativeRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public List<Integer> getResourceIdsByModuleReferences(final Collection<ModuleReference> moduleReferences) {
        if (moduleReferences.isEmpty()) {
            return Collections.emptyList();
        }
        final Query query = entityManager.createNativeQuery(getCombinedSelectSqlQuery(moduleReferences))
            .unwrap(org.hibernate.query.NativeQuery.class)
            .addScalar("id", StandardBasicTypes.INTEGER);
        final List<Integer> yangResourceIds = query.getResultList();
        if (yangResourceIds.size() != moduleReferences.size()) {
            log.warn("ModuleReferences size : {} and QueryResult size : {}", moduleReferences.size(),
                    yangResourceIds.size());
        }
        return yangResourceIds;
    }

    private String getCombinedSelectSqlQuery(final Collection<ModuleReference> moduleReferences) {
        final StringJoiner sqlQueryJoiner = new StringJoiner(" UNION ALL ");
        moduleReferences.forEach(moduleReference ->
            sqlQueryJoiner.add(String.format("SELECT id FROM yang_resource WHERE module_name='%s' and revision='%s'",
                moduleReference.getModuleName(),
                moduleReference.getRevision()))
        );
        return sqlQueryJoiner.toString();
    }
}
