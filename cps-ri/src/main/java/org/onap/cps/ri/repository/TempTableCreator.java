/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2024 Nordix Foundation.
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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ri.utils.EscapeUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional
@AllArgsConstructor
@Component
public class TempTableCreator {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Create a uniquely named temporary table.
     *
     * @param prefix      prefix for the table name (so you can recognize it)
     * @param sqlData     data to insert (strings only) the inner List present a row of data
     * @param columnNames column names (in same order as data in rows in sqlData)
     * @return a unique temporary table name with given prefix
     */
    public String createTemporaryTable(final String prefix,
                                       final Collection<List<String>> sqlData,
                                       final Collection<String> columnNames) {
        final String tempTableName = prefix + UUID.randomUUID().toString().replace("-", "");
        final StringBuilder sqlStringBuilder = new StringBuilder("CREATE TEMPORARY TABLE ");
        sqlStringBuilder.append(tempTableName);
        defineColumns(sqlStringBuilder, columnNames);
        sqlStringBuilder.append(" ON COMMIT DROP;");
        insertData(sqlStringBuilder, tempTableName, columnNames, sqlData);
        entityManager.createNativeQuery(sqlStringBuilder.toString()).executeUpdate();
        return tempTableName;
    }

    private static void defineColumns(final StringBuilder sqlStringBuilder, final Collection<String> columnNames) {
        final String columns = columnNames.stream()
                .map(columnName -> " " + columnName + " varchar NOT NULL")
                .collect(Collectors.joining(","));
        sqlStringBuilder.append('(').append(columns).append(')');
    }

    private static void insertData(final StringBuilder sqlStringBuilder,
                                   final String tempTableName,
                                   final Collection<String> columnNames,
                                   final Collection<List<String>> sqlData) {
        final Collection<String> sqlInserts = new HashSet<>(sqlData.size());
        for (final Collection<String> rowValues : sqlData) {
            final Collection<String> escapedValues =
                rowValues.stream().map(EscapeUtils::escapeForSqlStringLiteral).toList();
            sqlInserts.add("('" + String.join("','", escapedValues) + "')");
        }
        sqlStringBuilder.append("INSERT INTO ");
        sqlStringBuilder.append(tempTableName);
        sqlStringBuilder.append(" (");
        sqlStringBuilder.append(String.join(",", columnNames));
        sqlStringBuilder.append(") VALUES ");
        sqlStringBuilder.append(String.join(",", sqlInserts));
        sqlStringBuilder.append(";");
    }

}
