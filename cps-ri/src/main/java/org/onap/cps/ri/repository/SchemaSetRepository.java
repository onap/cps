/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Pantheon.tech
 *  Modifications Copyright (C) 2022 TechMahindra Ltd.
 *  Modifications Copyright (C) 2023-2024 Nordix Foundation
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

package org.onap.cps.ri.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.onap.cps.ri.models.DataspaceEntity;
import org.onap.cps.ri.models.SchemaSetEntity;
import org.onap.cps.spi.api.exceptions.SchemaSetNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SchemaSetRepository extends JpaRepository<SchemaSetEntity, Integer> {

    Optional<SchemaSetEntity> findByDataspaceAndName(DataspaceEntity dataspaceEntity, String schemaSetName);

    /**
     * Gets schema sets by dataspace.
     * @param dataspaceEntity dataspace entity
     * @return list of schema set entity
     */
    List<SchemaSetEntity> findByDataspace(DataspaceEntity dataspaceEntity);

    Integer countByDataspace(DataspaceEntity dataspaceEntity);

    /**
     * Gets a schema set by dataspace and schema set name.
     *
     * @param dataspaceEntity dataspace entity
     * @param schemaSetName   schema set name
     * @return schema set entity
     * @throws SchemaSetNotFoundException if SchemaSet not found
     */
    default SchemaSetEntity getByDataspaceAndName(final DataspaceEntity dataspaceEntity, final String schemaSetName) {
        return findByDataspaceAndName(dataspaceEntity, schemaSetName)
            .orElseThrow(() -> new SchemaSetNotFoundException(dataspaceEntity.getName(), schemaSetName));
    }

    @Modifying
    @Query(value = "DELETE FROM schema_set WHERE dataspace_id = :dataspaceId AND name IN (:schemaSetNames)",
        nativeQuery = true)
    void deleteByDataspaceIdAndNameIn(@Param("dataspaceId") final int dataspaceId,
                                      @Param("schemaSetNames") final Collection<String> schemaSetNames);

    /**
     * Delete multiple schema sets in a given dataspace.
     * @param dataspaceEntity dataspace entity
     * @param schemaSetNames  schema set names
     */
    default void deleteByDataspaceAndNameIn(final DataspaceEntity dataspaceEntity,
                                            final Collection<String> schemaSetNames) {
        deleteByDataspaceIdAndNameIn(dataspaceEntity.getId(), schemaSetNames);
    }

}
