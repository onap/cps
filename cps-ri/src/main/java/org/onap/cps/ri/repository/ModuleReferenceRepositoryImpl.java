/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2025 Nordix Foundation.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.onap.cps.api.model.ModuleReference;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@RequiredArgsConstructor
public class ModuleReferenceRepositoryImpl implements ModuleReferenceQuery {

    @PersistenceContext
    private EntityManager entityManager;

    private final TempTableCreator tempTableCreator;

    @Override
    @SneakyThrows
    public Collection<ModuleReference> identifyNewModuleReferences(
            final Collection<ModuleReference> moduleReferencesToCheck) {

        if (moduleReferencesToCheck == null || moduleReferencesToCheck.isEmpty()) {
            return Collections.emptyList();
        }

        final Collection<List<String>> sqlData = new HashSet<>(moduleReferencesToCheck.size());
        for (final ModuleReference moduleReference : moduleReferencesToCheck) {
            final List<String> row = new ArrayList<>(2);
            row.add(moduleReference.getModuleName());
            row.add(moduleReference.getRevision());
            sqlData.add(row);
        }

        final String tempTableName = tempTableCreator.createTemporaryTable(
            "moduleReferencesToCheckTemp", sqlData, List.of("module_name", "revision"));

        return identifyNewModuleReferencesForCmHandle(tempTableName);
    }

    private Collection<ModuleReference> identifyNewModuleReferencesForCmHandle(final String tempTableName) {
        final String sql = """
                SELECT %1$s.module_name, %1$s.revision
                FROM %1$s
                LEFT JOIN yang_resource
                ON yang_resource.module_name=%1$s.module_name AND yang_resource.revision=%1$s.revision
                WHERE yang_resource.module_name IS NULL;
                """.formatted(tempTableName);

        @SuppressWarnings("unchecked")
        final List<Object[]> resultsAsObjects = entityManager.createNativeQuery(sql).getResultList();

        final List<ModuleReference> resultsAsModuleReferences = new ArrayList<>(resultsAsObjects.size());
        for (final Object[] row : resultsAsObjects) {
            resultsAsModuleReferences.add(new ModuleReference((String) row[0], (String) row[1]));
        }
        return resultsAsModuleReferences;
    }
}
