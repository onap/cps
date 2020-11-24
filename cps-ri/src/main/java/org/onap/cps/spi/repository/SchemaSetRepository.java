/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Pantheon.tech
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
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

import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import org.onap.cps.spi.entities.Dataspace;
import org.onap.cps.spi.entities.SchemaSet;
import org.onap.cps.spi.exceptions.SchemaSetNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchemaSetRepository extends JpaRepository<SchemaSet, Integer> {

    List<SchemaSet> findAllByDataspace(@NotNull Dataspace dataspace);

    Optional<SchemaSet> findByDataspaceAndName(@NotNull Dataspace dataspace, @NotNull String name);

    /**
     * Gets a schema set.
     * Throws SchemaSetNotFoundException if no entity found.
     *
     * @param dataspace dataspace entity
     * @param name      schema set name
     * @return schema set entity
     *
     */
    default SchemaSet getByDataspaceAndName(@NotNull final Dataspace dataspace, @NotNull final String name) {
        return findByDataspaceAndName(dataspace, name)
            .orElseThrow(() -> new SchemaSetNotFoundException(dataspace.getName(), name));
    }
}
