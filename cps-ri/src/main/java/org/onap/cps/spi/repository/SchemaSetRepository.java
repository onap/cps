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

import java.util.Optional;
import javax.validation.constraints.NotNull;
import org.onap.cps.spi.entities.DataspaceEntity;
import org.onap.cps.spi.entities.SchemaSetEntity;
import org.onap.cps.spi.exceptions.SchemaSetNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchemaSetRepository extends JpaRepository<SchemaSetEntity, Integer> {

    Optional<SchemaSetEntity> findByDataspaceAndName(@NotNull DataspaceEntity dataspaceEntity,
        @NotNull String schemaSetName);

    Integer countByDataspace(@NotNull DataspaceEntity dataspaceEntity);

    /**
     * Gets a schema set by dataspace and schema set name.
     *
     * @param dataspaceEntity dataspace entity
     * @param schemaSetName   schema set name
     * @return schema set entity
     * @throws SchemaSetNotFoundException if SchemaSet not found
     */
    default SchemaSetEntity getByDataspaceAndName(@NotNull final DataspaceEntity dataspaceEntity,
        @NotNull final String schemaSetName) {
        return findByDataspaceAndName(dataspaceEntity, schemaSetName)
            .orElseThrow(() -> new SchemaSetNotFoundException(dataspaceEntity.getName(), schemaSetName));
    }
}
