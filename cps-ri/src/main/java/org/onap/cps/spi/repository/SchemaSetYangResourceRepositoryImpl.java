/*-
 * ============LICENSE_START=======================================================
 *  Modifications Copyright (c) 2022 Nordix Foundation
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

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Transactional
@Slf4j
public class SchemaSetYangResourceRepositoryImpl implements SchemaSetYangResourceRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void insertSchemaSetIdYangResourceId(final Integer schemaSetId, final List<Long> yangResourceIds) {
        Session hibernateSession = entityManager.unwrap(Session.class);
        final var sqlQuery = "INSERT INTO SCHEMA_SET_YANG_RESOURCES (SCHEMA_SET_ID, YANG_RESOURCE_ID) "
                + "VALUES ( ?, ?)";
        hibernateSession.doWork(connection -> {
            try (PreparedStatement preparedStatement = connection.prepareStatement(sqlQuery)) {
                int sqlQueryCount = 1;
                for (long resourceId : yangResourceIds) {
                    preparedStatement.setInt(1, schemaSetId);
                    preparedStatement.setLong(2, resourceId);
                    preparedStatement.addBatch();
                    //Batch size: 100
                    if (sqlQueryCount % 100 == 0 || sqlQueryCount == yangResourceIds.size()) {
                        preparedStatement.executeBatch();
                    }
                    sqlQueryCount++;
                }
            } catch (SQLException e) {
                log.error("An exception while performing bulk insertion into schema set yang resource." +
                        " error code : {} error message: {}", e.getErrorCode(), e.getMessage());
            }
        });
    }
}
