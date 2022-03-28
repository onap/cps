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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.CmHandleQueryParameters;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.ModuleReference;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
     * Query and return cm handles that match the given query parameters.
     *
     * @param cmHandleQueryParameters the cm handle query parameters
     * @return collection of cm handle ids
     */
    @Override
    public Set<String> queryCmHandles(final CmHandleQueryParameters cmHandleQueryParameters) {

        if (cmHandleQueryParameters.getPublicProperties().entrySet().isEmpty()) {
            return getAllCmHandles();
        }

        final Collection<DataNode> queryResult = new ArrayList<>();
        for (final Map.Entry<String, String> entry : cmHandleQueryParameters.getPublicProperties().entrySet()) {
            final StringBuilder cmHandlePath = new StringBuilder();
            cmHandlePath.append("//public-properties[@name='").append(entry.getKey()).append("' ");
            cmHandlePath.append("and @value='").append(entry.getValue()).append("']");
            cmHandlePath.append("/ancestor::cm-handles");

            if (queryResult.isEmpty() || queryResult == null) {
                queryResult.addAll(cpsDataPersistenceService.queryDataNodes("NCMP-Admin",
                    "ncmp-dmi-registry", String.valueOf(cmHandlePath), FetchDescendantsOption.OMIT_DESCENDANTS));
            } else {
                queryResult.retainAll(cpsDataPersistenceService.queryDataNodes("NCMP-Admin",
                    "ncmp-dmi-registry", String.valueOf(cmHandlePath), FetchDescendantsOption.OMIT_DESCENDANTS));
            }

            if (queryResult.isEmpty()) {
                break;
            }
        }

        return extractCmHandleIds(queryResult);
    }

    private Set<String> getAllCmHandles() {
        final Collection<DataNode> cmHandles = cpsDataPersistenceService.queryDataNodes("NCMP-Admin",
            "ncmp-dmi-registry", "//public-properties/ancestor::cm-handles",
            FetchDescendantsOption.OMIT_DESCENDANTS);
        return extractCmHandleIds(cmHandles);
    }

    private Set<String> extractCmHandleIds(final Collection<DataNode> cmHandles) {
        return cmHandles.stream().map(cmHandle -> cmHandle.getLeaves().get("id").toString())
            .collect(Collectors.toSet());
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
