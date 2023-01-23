/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2020-2022 Bell Canada.
 *  Modifications Copyright (C) 2022 TechMahindra Ltd.
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
import org.onap.cps.spi.entities.FragmentEntity;
import org.springframework.stereotype.Repository;

@Repository
public class FragmentNativeRepositoryImpl implements FragmentNativeRepository {

    final String DROP_FRAGMENT_CONSTRAINT = "ALTER TABLE fragment DROP CONSTRAINT fragment_parent_id_fkey;";
    final String ADD_FRAGMENT_CONSTRAINT_WITH_CASCADE = "ALTER TABLE fragment ADD CONSTRAINT fragment_parent_id_fkey "
            + "FOREIGN KEY (parent_id) REFERENCES fragment (id) ON DELETE CASCADE;";
    final String DELETE_FRAGMENT_WITH_CASCADE = "DELETE FROM fragment WHERE id =?;";
    final String ADD_ORIGINAL_FRAGMENT_CONSTRAINT = "ALTER TABLE fragment ADD CONSTRAINT fragment_parent_id_fkey "
            + "FOREIGN KEY (parent_id) REFERENCES fragment (id) ON DELETE NO ACTION;";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void deleteFragmentEntity(final FragmentEntity parentFragmentEntity) {
        final Session session = entityManager.unwrap(Session.class);
        session.doWork(connection -> {
            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    DROP_FRAGMENT_CONSTRAINT + ADD_FRAGMENT_CONSTRAINT_WITH_CASCADE + DELETE_FRAGMENT_WITH_CASCADE
                            + DROP_FRAGMENT_CONSTRAINT + ADD_ORIGINAL_FRAGMENT_CONSTRAINT)) {
                preparedStatement.setLong(1, parentFragmentEntity.getId().longValue());
                preparedStatement.executeUpdate();
            }
        });
    }
}

