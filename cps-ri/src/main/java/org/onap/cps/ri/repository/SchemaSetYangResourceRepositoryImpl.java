/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 Nordix Foundation.
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
import java.sql.PreparedStatement;
import java.util.List;
import org.hibernate.Session;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class SchemaSetYangResourceRepositoryImpl implements SchemaSetYangResourceRepository {

    private static final int MAX_INSERT_BATCH_SIZE = 100;

    private static final String DELETE_ORPHANS_SQL =
        "DELETE FROM schema_set WHERE NOT EXISTS (SELECT 1 FROM anchor WHERE anchor.schema_set_id = schema_set.id)";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void insertSchemaSetIdYangResourceId(final Integer schemaSetId, final List<Integer> yangResourceIds) {
        final Session session = entityManager.unwrap(Session.class);
        session.doWork(connection -> {
            try (PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO SCHEMA_SET_YANG_RESOURCES (SCHEMA_SET_ID, YANG_RESOURCE_ID) VALUES ( ?, ?)")) {
                int sqlQueryCount = 1;
                for (final int yangResourceId : yangResourceIds) {
                    preparedStatement.setInt(1, schemaSetId);
                    preparedStatement.setInt(2, yangResourceId);
                    preparedStatement.addBatch();
                    if (sqlQueryCount % MAX_INSERT_BATCH_SIZE == 0 || sqlQueryCount == yangResourceIds.size()) {
                        preparedStatement.executeBatch();
                    }
                    sqlQueryCount++;
                }
            }
        });
    }

    public void deleteOrphanedYangResourceReferences() {
        entityManager.createNativeQuery(DELETE_ORPHANS_SQL).executeUpdate();
    }

}

