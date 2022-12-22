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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
                                       final String... columnNames) {
        final String tempTableName = prefix + UUID.randomUUID().toString().replace("-", "");
        final StringBuilder sqlStringBuilder = new StringBuilder("CREATE TEMPORARY TABLE ");
        sqlStringBuilder.append(tempTableName);
        defineColumns(sqlStringBuilder, columnNames);
        insertData(sqlStringBuilder, tempTableName, columnNames, sqlData);
        entityManager.createNativeQuery(sqlStringBuilder.toString()).executeUpdate();
        return tempTableName;
    }

    private static void defineColumns(final StringBuilder sqlStringBuilder, final String[] columnNames) {
        sqlStringBuilder.append('(');
        final Iterator<String> it = Arrays.stream(columnNames).iterator();
        while (it.hasNext()) {
            final String columnName = it.next();
            sqlStringBuilder.append(" ");
            sqlStringBuilder.append(columnName);
            sqlStringBuilder.append(" varchar NOT NULL");
            if (it.hasNext()) {
                sqlStringBuilder.append(",");
            }
        }
        sqlStringBuilder.append(");");
    }

    private static void insertData(final StringBuilder sqlStringBuilder,
                                   final String tempTableName,
                                   final String[] columnNames,
                                   final Collection<List<String>> sqlData) {
        final Collection<String> sqlInserts = new HashSet<>(sqlData.size());
        for (final Collection<String> row : sqlData) {
            sqlInserts.add("('" + String.join("','", row) + "')");
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
