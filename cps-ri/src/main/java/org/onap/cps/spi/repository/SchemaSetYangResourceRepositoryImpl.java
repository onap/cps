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

package org.onap.cps.spi.repository;

import java.util.Collection;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.spi.model.ModuleReference;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional
public class SchemaSetYangResourceRepositoryImpl implements SchemaSetYangResourceRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void insertSchemaSetIdYangResourceId(final Integer schemaSetId, final List<Long> yangResourceId) {
        final var query = "INSERT INTO SCHEMA_SET_YANG_RESOURCES (SCHEMA_SET_ID, YANG_RESOURCE_ID) "
                + "VALUES ( :schemaSetId, :yangResourceId)";
        yangResourceId.forEach(id ->
                entityManager.createNativeQuery(query)
                        .setParameter("schemaSetId", schemaSetId)
                        .setParameter("yangResourceId", id)
                        .executeUpdate()
        );
    }

    /**
     * Create temporary table.
     *
     * @param knownModuleReferencesInCps the knownModuleReferencesInCps
     * @param inputYangResourceModuleReference the inputYangResourceModuleReference
     */
    public void createTemporaryTablesAndInsertData(
        final Collection<ModuleReference> knownModuleReferencesInCps,
        final Collection<ModuleReference> inputYangResourceModuleReference) {

        // create temp tables
        createTemporaryTable("moduleReference");
        createTemporaryTable("inputYangResourceModuleReference");

        // insert data into temp tables
        insertDataIntoTable("moduleReference", knownModuleReferencesInCps);
        insertDataIntoTable("inputYangResourceModuleReference", inputYangResourceModuleReference);
    }

    private void createTemporaryTable(final String tempTableName) {
        final StringBuilder sqlStringBuilder = new StringBuilder("CREATE TEMPORARY TABLE " + tempTableName + "(\n");
        sqlStringBuilder.append(" id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, \n");
        sqlStringBuilder.append(" module_name varchar NOT NULL, \n");
        sqlStringBuilder.append(" revision varchar NOT NULL\n");
        sqlStringBuilder.append(");");

        final Query query = entityManager.createNativeQuery(sqlStringBuilder.toString());
        query.executeUpdate();
    }

    private void insertDataIntoTable(final String tempTableName, final Collection<ModuleReference> moduleReferences) {
        // bulk updatesc
        moduleReferences.stream().forEach(moduleReference -> {
            final StringBuilder sqlStringBuilder = new StringBuilder("INSERT INTO  " + tempTableName);
            sqlStringBuilder.append(" (module_name, revision) ");
            sqlStringBuilder.append(" VALUES ('");
            sqlStringBuilder.append(moduleReference.getModuleName());
            sqlStringBuilder.append("', '");
            sqlStringBuilder.append(moduleReference.getRevision());
            sqlStringBuilder.append("');");

            final Query query = entityManager.createNativeQuery(sqlStringBuilder.toString());
            query.executeUpdate();
        });
    }
}
