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

import java.sql.PreparedStatement;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.hibernate.Session;
import org.springframework.stereotype.Repository;

@Repository
public class FragmentNativeRepositoryImpl implements FragmentNativeRepository {

    private static final String DROP_FRAGMENT_CONSTRAINT
            = "ALTER TABLE fragment DROP CONSTRAINT fragment_parent_id_fkey;";
    private static final String ADD_FRAGMENT_CONSTRAINT_WITH_CASCADE
            = "ALTER TABLE fragment ADD CONSTRAINT fragment_parent_id_fkey FOREIGN KEY (parent_id) "
            + "REFERENCES fragment (id) ON DELETE CASCADE;";
    private static final String DELETE_FRAGMENT = "DELETE FROM fragment WHERE id =?;";
    private static final String ADD_ORIGINAL_FRAGMENT_CONSTRAINT
            = "ALTER TABLE fragment ADD CONSTRAINT fragment_parent_id_fkey FOREIGN KEY (parent_id) "
            + "REFERENCES fragment (id) ON DELETE NO ACTION;";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void deleteFragmentEntity(final long fragmentEntityId) {
        final Session session = entityManager.unwrap(Session.class);
        session.doWork(connection -> {
            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    DROP_FRAGMENT_CONSTRAINT
                            + ADD_FRAGMENT_CONSTRAINT_WITH_CASCADE
                            + DELETE_FRAGMENT
                            + DROP_FRAGMENT_CONSTRAINT
                            + ADD_ORIGINAL_FRAGMENT_CONSTRAINT)) {
                preparedStatement.setLong(1, fragmentEntityId);
                preparedStatement.executeUpdate();
            }
        });
    }
}

