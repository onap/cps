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

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.spi.model.ModuleReference;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional
public class ModuleReferenceRepositoryImpl implements ModuleReferenceQuery {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SneakyThrows
    public Collection<ModuleReference> identifyNewModuleReferences(
        final Collection<ModuleReference> moduleReferencesToCheck) {
        final Random random = SecureRandom.getInstanceStrong();

        // Negation of random.nextInt() is to satisfy spotbugs RV_ABSOLUTE_VALUE_OF_RANDOM_INT
        final String tempTableName = "moduleReferencesToCheckTemp" + Math.abs(-random.nextInt());

        createTemporaryTable(tempTableName);
        insertDataIntoTable(tempTableName, moduleReferencesToCheck);

        return identifyNewModuleReferencesForCmHandle(tempTableName);
    }

    private void createTemporaryTable(final String tempTableName) {
        final StringBuilder sqlStringBuilder = new StringBuilder("CREATE TEMPORARY TABLE " + tempTableName + "(\n");
        sqlStringBuilder.append(" id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, \n");
        sqlStringBuilder.append(" module_name varchar NOT NULL, \n");
        sqlStringBuilder.append(" revision varchar NOT NULL\n");
        sqlStringBuilder.append(");");

        entityManager.createNativeQuery(sqlStringBuilder.toString()).executeUpdate();;
    }

    private void insertDataIntoTable(final String tempTableName, final Collection<ModuleReference> moduleReferences) {
        final StringBuilder sqlStringBuilder = new StringBuilder("INSERT INTO  " + tempTableName);
        sqlStringBuilder.append(" (module_name, revision) ");
        sqlStringBuilder.append(" VALUES ");

        for (final ModuleReference moduleReference : moduleReferences) {
            sqlStringBuilder.append("('");
            sqlStringBuilder.append(moduleReference.getModuleName());
            sqlStringBuilder.append("', '");
            sqlStringBuilder.append(moduleReference.getRevision());
            sqlStringBuilder.append("'),");
        }

        // remove last ','
        sqlStringBuilder.replace(sqlStringBuilder.length() - 1, sqlStringBuilder.length(), "");
        sqlStringBuilder.append(";");

        entityManager.createNativeQuery(sqlStringBuilder.toString()).executeUpdate();
    }

    private Collection<ModuleReference> identifyNewModuleReferencesForCmHandle(final String tempTableName) {
        final StringBuilder sqlStringBuilder = new StringBuilder();
        sqlStringBuilder.append(String.format("SELECT %1$s.module_name,  %1$s.revision", tempTableName));
        sqlStringBuilder.append(String.format(" FROM %1$s LEFT JOIN yang_resource", tempTableName));
        sqlStringBuilder.append(String.format(" ON yang_resource.module_name=%1$s.module_name", tempTableName));
        sqlStringBuilder.append(String.format(" AND yang_resource.revision=%1$s.revision", tempTableName));
        sqlStringBuilder.append(" WHERE yang_resource.module_name IS NULL;");

        final List<Object[]> resultsAsObjects =
            entityManager.createNativeQuery(sqlStringBuilder.toString()).getResultList();

        final List<ModuleReference> resultsAsModuleReferences = new ArrayList<>(resultsAsObjects.size());
        for (final Object[] record : resultsAsObjects) {
            resultsAsModuleReferences.add(new ModuleReference((String) record[0], (String) record[1]));
        }

        return resultsAsModuleReferences;
    }
}
