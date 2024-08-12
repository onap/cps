/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
 *  Modifications Copyright (C) 2023 TechMahindra Ltd.
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

package org.onap.cps.spi.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.spi.model.ModuleReference;
import org.springframework.stereotype.Component;

/**
 * Service class responsible for managing and querying Fragment entities based on JSONB attributes in the `fragment`
 * table. This service ensures that indexes are created to optimize query performance and provides methods to retrieve
 * module references based on specific criteria.
 * The `findModuleReferences` method queries the database directly each time it is invoked, ensuring that the most
 * up-to-date data is always retrieved. The method uses SQL with Common Table Expressions (CTEs) for better query
 * structure and maintainability.
 */
@RequiredArgsConstructor
@Slf4j
@Component
public class FragmentQueryService {

    @PersistenceContext
    private EntityManager entityManager;

    private static final String FIND_MODULE_REFERENCES_SQL =
            "WITH FragmentCTE AS ("
                    + "    SELECT f2.attributes->>'id' AS schema_set_name "
                    + "    FROM fragment f1 "
                    + "    JOIN fragment f2 ON f1.parent_id = f2.id "
                    + "    JOIN anchor a1 ON f1.anchor_id = a1.id "
                    + "    JOIN dataspace d1 ON a1.dataspace_id = d1.id "
                    + "    WHERE f1.attributes->>'cm-handle-state' = 'READY' "
                    + "    AND f2.attributes->>'module-set-tag' = :moduleSetTag "
                    + "    AND a1.name = :anchorName "
                    + "    AND d1.name = :dataspaceName "
                    + "    LIMIT 1 "
                    + "), "
                    + "SchemaSetCTE AS ("
                    + "    SELECT id "
                    + "    FROM schema_set "
                    + "    WHERE name = (SELECT schema_set_name FROM FragmentCTE) "
                    + ") "
                    + "SELECT yr.module_name, yr.revision "
                    + "FROM yang_resource yr "
                    + "JOIN schema_set_yang_resources ssyr ON yr.id = ssyr.yang_resource_id "
                    + "WHERE ssyr.schema_set_id = (SELECT id FROM SchemaSetCTE)";

    /**
     * Retrieves a list of {@link ModuleReference} objects associated with the schema set identified by fragments
     * having a `cm-handle-state` of 'READY' and matching the specified `moduleSetTag`.
     * <p>
     * The query uses Common Table Expressions (CTEs) for improved readability and maintainability. The method
     * queries the database directly each time it is invoked, ensuring that the latest data is always retrieved.
     * </p>
     *
     * @param dataspaceName The name of the dataspace to filter fragments by.
     * @param anchorName    The name of the anchor to filter fragments by.
     * @param moduleSetTag  The module set tag to filter fragments by.
     * @return A list of {@link ModuleReference} objects containing module names and revisions,
     *     or an empty list if no matches are found.
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public List<ModuleReference> findModuleReferences(final String dataspaceName, final String anchorName,
                                                      final String moduleSetTag) {
        final long startTime = System.nanoTime();
        final Query query = entityManager.createNativeQuery(FIND_MODULE_REFERENCES_SQL);
        query.setParameter("dataspaceName", dataspaceName);
        query.setParameter("anchorName", anchorName);
        query.setParameter("moduleSetTag", moduleSetTag);

        final List<Object[]> queryResults = query.getResultList();

        if (queryResults.isEmpty()) {
            log.info("No module references found for moduleSetTag: {}", moduleSetTag);
            return Collections.emptyList();
        }

        final List<ModuleReference> moduleReferences = queryResults.stream()
                .map(queryResult -> {
                    final String name = (String) queryResult[0];
                    final String revision = (String) queryResult[1];
                    return new ModuleReference(name, revision);
                })
                .collect(Collectors.toList());

        final long endTime = System.nanoTime();
        final long duration = endTime - startTime;
        final long durationMillis = duration / 1_000_000;
        log.info("Time taken to execute queryModuleReferencesByModuleSetTag: {} ms", durationMillis);

        return moduleReferences;
    }
}

