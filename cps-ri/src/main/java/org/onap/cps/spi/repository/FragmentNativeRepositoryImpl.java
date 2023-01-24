/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.spi.repository;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import lombok.AllArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;

@AllArgsConstructor
public class FragmentNativeRepositoryImpl implements FragmentNativeRepository {

    private static final String DROP_FRAGMENT_CONSTRAINT
            = "ALTER TABLE fragment DROP CONSTRAINT fragment_parent_id_fkey;";
    private static final String ADD_FRAGMENT_CONSTRAINT_WITH_CASCADE
            = "ALTER TABLE fragment ADD CONSTRAINT fragment_parent_id_fkey FOREIGN KEY (parent_id) "
            + "REFERENCES fragment (id) ON DELETE CASCADE;";
    private static final String ADD_ORIGINAL_FRAGMENT_CONSTRAINT
            = "ALTER TABLE fragment ADD CONSTRAINT fragment_parent_id_fkey FOREIGN KEY (parent_id) "
            + "REFERENCES fragment (id) ON DELETE NO ACTION;";

    @PersistenceContext
    private EntityManager entityManager;

    private TempTableCreator tempTableCreator;

    @Override
    public void deleteFragmentEntity(final long fragmentEntityId) {
        entityManager.createNativeQuery(
                DROP_FRAGMENT_CONSTRAINT
                    + ADD_FRAGMENT_CONSTRAINT_WITH_CASCADE
                    + "DELETE FROM fragment WHERE id = ?;"
                    + DROP_FRAGMENT_CONSTRAINT
                    + ADD_ORIGINAL_FRAGMENT_CONSTRAINT)
            .setParameter(1, fragmentEntityId)
            .executeUpdate();
    }

    @Override
    public void deleteByAnchorIdAndXpaths(final int anchorId, final @NonNull Collection<String> xpaths) {
        if (!xpaths.isEmpty()) {
            final String tempTableName = createTempTable(xpaths);
            // Note: setParameter cannot be used to set tempTableName. Thus String.format is used.
            final String sql = String.format(
                DROP_FRAGMENT_CONSTRAINT
                + ADD_FRAGMENT_CONSTRAINT_WITH_CASCADE
                + "DELETE FROM fragment f USING %s t WHERE f.anchor_id = %d AND f.xpath = t.xpath;"
                + DROP_FRAGMENT_CONSTRAINT
                + ADD_ORIGINAL_FRAGMENT_CONSTRAINT,
                tempTableName, anchorId);
            entityManager.createNativeQuery(sql).executeUpdate();
        }
    }

    @Override
    public void deleteListsByAnchorIdAndXpaths(final int anchorId, final @NonNull Collection<String> xpaths) {
        if (!xpaths.isEmpty()) {
            final String tempTableName = createTempTable(xpaths);
            // Note: setParameter cannot be used to set tempTableName. Thus String.format is used.
            final String sql = String.format(
                DROP_FRAGMENT_CONSTRAINT
                + ADD_FRAGMENT_CONSTRAINT_WITH_CASCADE
                + "DELETE FROM fragment f USING %s t WHERE f.anchor_id = %d AND f.xpath LIKE CONCAT(t.xpath, '[%%');"
                + DROP_FRAGMENT_CONSTRAINT
                + ADD_ORIGINAL_FRAGMENT_CONSTRAINT,
                tempTableName, anchorId);
            entityManager.createNativeQuery(sql).executeUpdate();
        }
    }

    private String createTempTable(final Collection<String> xpaths) {
        final Collection<List<String>> table = new HashSet<>(xpaths.size());
        for (final String xpath : xpaths) {
            final List<String> row = Collections.singletonList(xpath);
            table.add(row);
        }
        return tempTableCreator.createTemporaryTable("xpathTemporaryTable", table, "xpath");
    }
}

