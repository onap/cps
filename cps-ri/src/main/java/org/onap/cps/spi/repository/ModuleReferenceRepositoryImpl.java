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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.ModuleReference;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@AllArgsConstructor
public class ModuleReferenceRepositoryImpl implements ModuleReferenceQuery {

    @PersistenceContext
    private EntityManager entityManager;

    private final CpsDataPersistenceService cpsDataPersistenceService;

    @Override
    @SneakyThrows
    public Collection<ModuleReference> identifyNewModuleReferences(
        final Collection<ModuleReference> moduleReferencesToCheck) {

        if (moduleReferencesToCheck == null || moduleReferencesToCheck.isEmpty()) {
            return Collections.emptyList();
        }

        final String tempTableName = "moduleReferencesToCheckTemp"
            + UUID.randomUUID().toString().replace("-", "");

        createTemporaryTable(tempTableName);
        insertDataIntoTable(tempTableName, moduleReferencesToCheck);

        return identifyNewModuleReferencesForCmHandle(tempTableName);
    }

    /**
     * Retrieve public properties for given cm handle.
     *
     * @param publicProperties the public properties to match
     * @return lit of cm handles that match
     */
    @Override
    public Set<String> getCmHandlesForMatchingPublicProperties(final Map<String, String> publicProperties) {
        final Collection<DataNode> cmHandles;
        final StringBuilder cmHandlePath = new StringBuilder();
        cmHandlePath.append("//public-properties[@name='").append(publicProperties.get("name")).append("' ");
        cmHandlePath.append("and @value='").append(publicProperties.get("value")).append("']");
        cmHandlePath.append("/ancestor::cm-handles");
        if (publicProperties.get("name").isEmpty() && publicProperties.get("value").isEmpty()) {
            cmHandles = cpsDataPersistenceService.queryDataNodes("NCMP-Admin",
                "ncmp-dmi-registry", "//public-properties/ancestor::cm-handles",
                FetchDescendantsOption.OMIT_DESCENDANTS);
        } else {
            cmHandles = cpsDataPersistenceService.queryDataNodes("NCMP-Admin",
                "ncmp-dmi-registry", String.valueOf(cmHandlePath),
                FetchDescendantsOption.OMIT_DESCENDANTS);
        }

        final Set<String> returnedCmHandles = new HashSet<>();

        for (final DataNode cmHandle: cmHandles) {
            final Map<String, Object> leaves = cmHandle.getLeaves();
            final String cmHandleId = leaves.get("id").toString();
            returnedCmHandles.add(cmHandleId);
        }
        return returnedCmHandles;
    }

    private void createTemporaryTable(final String tempTableName) {
        final StringBuilder sqlStringBuilder = new StringBuilder("CREATE TEMPORARY TABLE " + tempTableName + "(");
        sqlStringBuilder.append(" id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,");
        sqlStringBuilder.append(" module_name varchar NOT NULL,");
        sqlStringBuilder.append(" revision varchar NOT NULL");
        sqlStringBuilder.append(");");

        entityManager.createNativeQuery(sqlStringBuilder.toString()).executeUpdate();
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

        // replace last ',' with ';'
        sqlStringBuilder.replace(sqlStringBuilder.length() - 1, sqlStringBuilder.length(), ";");

        entityManager.createNativeQuery(sqlStringBuilder.toString()).executeUpdate();
    }

    private Collection<ModuleReference> identifyNewModuleReferencesForCmHandle(final String tempTableName) {
        final String sql = String.format(
            "SELECT %1$s.module_name, %1$s.revision"
                + " FROM %1$s LEFT JOIN yang_resource"
                + " ON yang_resource.module_name=%1$s.module_name"
                + " AND yang_resource.revision=%1$s.revision"
                + " WHERE yang_resource.module_name IS NULL;", tempTableName);

        final List<Object[]> resultsAsObjects =
            (List<Object[]>) entityManager.createNativeQuery(sql).getResultList();

        final List<ModuleReference> resultsAsModuleReferences = new ArrayList<>(resultsAsObjects.size());
        for (final Object[] row : resultsAsObjects) {
            resultsAsModuleReferences.add(new ModuleReference((String) row[0], (String) row[1]));
        }

        return resultsAsModuleReferences;
    }
}
