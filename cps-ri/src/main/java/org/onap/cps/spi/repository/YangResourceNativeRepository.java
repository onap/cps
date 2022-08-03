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

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.onap.cps.spi.model.ModuleReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;

@Service
@Slf4j
public class YangResourceNativeRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public List<Long> getResourceIdsByModuleNameAndRevision(final Collection<ModuleReference> moduleReferences,
                                                            final List<Long> resourceIds) {
        Session hibernateSession = entityManager.unwrap(Session.class);
        final var sqlQuery = getCombinedSelectSqlQuery(moduleReferences);
        hibernateSession.doWork(connection -> {
            try (PreparedStatement preparedStatement = connection.prepareStatement(sqlQuery)) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        resourceIds.add(resultSet.getLong("id"));
                    }
                    log.info("getIdByModuleNameAndRevision Found: {}", resourceIds);
                } catch (SQLException e) {
                    log.error("An exception while fetching  resourceIds." +
                            " error code : {} error message: {}", e.getErrorCode(), e.getMessage());
                }
            }
        });
        return resourceIds;
    }

    private String getCombinedSelectSqlQuery(final Collection<ModuleReference> moduleReferences) {
        StringJoiner sqlQueryJoiner = new StringJoiner(" UNION ALL ");
        moduleReferences.stream().forEach(moduleReference -> {
            sqlQueryJoiner.add(String.format("SELECT id FROM yang_resource WHERE module_name=%s and revision=%s",
                    "'" + moduleReference.getModuleName() + "'",
                    "'" + moduleReference.getRevision() + "'"));
        });
        return sqlQueryJoiner.toString();
    }

}
