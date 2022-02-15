/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2022 Nordix Foundation.
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
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.onap.cps.spi.entities.YangResourceEntity;
import org.onap.cps.spi.entities.YangResourceModuleReference;
import org.onap.cps.spi.impl.CpsModulePersistenceServiceImpl;
import org.onap.cps.spi.model.ModuleReference;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ModuleReferenceRepositoryImpl implements ModuleReferenceQuery{

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Collection<ModuleReference> identifyNewYangResourceModuleReferences(
        final Collection<ModuleReference> inputYangResourceModuleReference) {

        Random random = new Random ();
        final String tempTableName = "inputYangResourceModuleReference" + Math.abs(random.nextInt());
        System.out.println("********** temp " + tempTableName);
        createTemporaryTable(tempTableName);
        insertDataIntoTable(tempTableName, inputYangResourceModuleReference);
        return executeQuery(tempTableName);
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
        // TODO: @Joe - bulk updates investigation ongoing
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

    private Collection<ModuleReference> executeQuery(final String tempTableName){
        final StringBuilder sqlStringBuilder = new StringBuilder();
        sqlStringBuilder.append("SELECT ").append(tempTableName).append(".module_name");
        sqlStringBuilder.append(", ").append(tempTableName).append(".revision");
        sqlStringBuilder.append(" FROM ").append(tempTableName).append(" LEFT JOIN yang_resource");
        sqlStringBuilder.append(" ON yang_resource.module_name=").append(tempTableName).append(".module_name");
        sqlStringBuilder.append(" AND yang_resource.revision=").append(tempTableName).append(".revision");
        sqlStringBuilder.append(" WHERE yang_resource.module_name IS NULL");
        sqlStringBuilder.append(" AND ").append(tempTableName).append(".module_name != 'dummy_module_name';");

        final Query query = entityManager.createNativeQuery(sqlStringBuilder.toString());
        return (Collection<ModuleReference>) query.getResultList();
    }

    private ModuleReference toModuleReference(
        final YangResourceModuleReference yangResourceModuleReference) {
        return ModuleReference.builder()
            .moduleName(yangResourceModuleReference.getModuleName())
            .revision(yangResourceModuleReference.getRevision())
            .build();
    }

}
