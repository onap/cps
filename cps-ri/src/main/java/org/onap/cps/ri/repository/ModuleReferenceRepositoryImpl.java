/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation.
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.spi.api.model.ModuleReference;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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

    /**
     * Finds module references based on specified dataspace, anchor, and attribute filters.
     * This method constructs and executes a SQL query to retrieve module references. The query applies filters to
     * parent and child fragments using the provided attribute maps. The `parentAttributes` are used to filter
     * parent fragments, while `childAttributes` filter child fragments.
     *
     * @param dataspaceName    the name of the dataspace to filter on.
     * @param anchorName       the name of the anchor to filter on.
     * @param parentAttributes a map of attributes for filtering parent fragments.
     * @param childAttributes  a map of attributes for filtering child fragments.
     * @return a collection of {@link ModuleReference} objects that match the specified filters.
     */
    @Transactional
    @SuppressWarnings("unchecked")
    @Override
    public Collection<ModuleReference> findModuleReferences(final String dataspaceName, final String anchorName,
                                                            final Map<String, String> parentAttributes,
                                                            final Map<String, String> childAttributes) {

        final String parentFragmentWhereClause = buildWhereClause(childAttributes, "parentFragment");
        final String childFragmentWhereClause = buildWhereClause(parentAttributes, "childFragment");

        final String moduleReferencesSqlQuery = buildModuleReferencesSqlQuery(parentFragmentWhereClause,
                childFragmentWhereClause);

        final Query query = entityManager.createNativeQuery(moduleReferencesSqlQuery);
        setQueryParameters(query, parentAttributes, childAttributes, anchorName, dataspaceName);
        return processQueryResults(query.getResultList());
    }

    private String buildWhereClause(final Map<String, String> attributes, final String alias) {
        return attributes.keySet().stream()
                .map(attributeName -> String.format("%s.attributes->>'%s' = ?", alias, attributeName))
                .collect(Collectors.joining(" AND "));
    }

    private void setQueryParameters(final Query query, final Map<String, String> parentAttributes,
                                    final Map<String, String> childAttributes, final String anchorName,
                                    final String dataspaceName) {
        final String childAttributeValue = childAttributes.entrySet().iterator().next().getValue();
        query.setParameter(1, childAttributeValue);

        final String parentAttributeValue = parentAttributes.entrySet().iterator().next().getValue();
        query.setParameter(2, parentAttributeValue);

        query.setParameter(3, anchorName);
        query.setParameter(4, dataspaceName);
    }

    @SuppressFBWarnings(value = "VA_FORMAT_STRING_USES_NEWLINE", justification = "no \n in string just in file format")
    private String buildModuleReferencesSqlQuery(final String parentFragmentClause, final String childFragmentClause) {
        return """
                WITH Fragment AS (
                    SELECT childFragment.attributes->>'id' AS schema_set_name
                    FROM fragment parentFragment
                    JOIN fragment childFragment ON parentFragment.parent_id = childFragment.id
                    JOIN anchor anchorInfo ON parentFragment.anchor_id = anchorInfo.id
                    JOIN dataspace dataspaceInfo ON anchorInfo.dataspace_id = dataspaceInfo.id
                    WHERE %s
                    AND %s
                    AND anchorInfo.name = ?
                    AND dataspaceInfo.name = ?
                    LIMIT 1
                ),
                SchemaSet AS (
                    SELECT id
                    FROM schema_set
                    WHERE name = (SELECT schema_set_name FROM Fragment)
                )
                SELECT yangResource.module_name, yangResource.revision
                FROM yang_resource yangResource
                JOIN schema_set_yang_resources schemaSetYangResources
                ON yangResource.id = schemaSetYangResources.yang_resource_id
                WHERE schemaSetYangResources.schema_set_id = (SELECT id FROM SchemaSet);
                """.formatted(parentFragmentClause, childFragmentClause);
    }

    private Collection<ModuleReference> processQueryResults(final List<Object[]> queryResults) {
        if (queryResults.isEmpty()) {
            log.info("No module references found for the provided attributes.");
            return Collections.emptyList();
        }
        return queryResults.stream()
                .map(queryResult -> {
                    final String name = (String) queryResult[0];
                    final String revision = (String) queryResult[1];
                    return new ModuleReference(name, revision);
                })
                .collect(Collectors.toList());
    }

    private Collection<ModuleReference> identifyNewModuleReferencesForCmHandle(final String tempTableName) {
        final String sql = String.format(
                "SELECT %1$s.module_name, %1$s.revision"
                        + " FROM %1$s LEFT JOIN yang_resource"
                        + " ON yang_resource.module_name=%1$s.module_name"
                        + " AND yang_resource.revision=%1$s.revision"
                        + " WHERE yang_resource.module_name IS NULL;", tempTableName);

        @SuppressWarnings("unchecked")
        final List<Object[]> resultsAsObjects = entityManager.createNativeQuery(sql).getResultList();

        final List<ModuleReference> resultsAsModuleReferences = new ArrayList<>(resultsAsObjects.size());
        for (final Object[] row : resultsAsObjects) {
            resultsAsModuleReferences.add(new ModuleReference((String) row[0], (String) row[1]));
        }
        return resultsAsModuleReferences;
    }
}
