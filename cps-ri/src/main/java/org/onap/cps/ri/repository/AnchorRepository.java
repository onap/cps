/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021-2024 Nordix Foundation
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
import java.util.Optional;
import org.onap.cps.ri.models.AnchorEntity;
import org.onap.cps.ri.models.DataspaceEntity;
import org.onap.cps.ri.models.SchemaSetEntity;
import org.onap.cps.spi.api.exceptions.AnchorNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AnchorRepository extends JpaRepository<AnchorEntity, Long> {

    Optional<AnchorEntity> findByDataspaceAndName(DataspaceEntity dataspaceEntity, String name);

    default AnchorEntity getByDataspaceAndName(DataspaceEntity dataspace, String anchorName) {
        return findByDataspaceAndName(dataspace, anchorName)
            .orElseThrow(() -> new AnchorNotFoundException(anchorName, dataspace.getName()));
    }

    Collection<AnchorEntity> findAllByDataspace(DataspaceEntity dataspaceEntity);

    Collection<AnchorEntity> findAllBySchemaSet(SchemaSetEntity schemaSetEntity);

    @Query(value = "SELECT * FROM anchor WHERE dataspace_id = :dataspaceId AND name IN (:anchorNames)",
        nativeQuery = true)
    Collection<AnchorEntity> findAllByDataspaceIdAndNameIn(@Param("dataspaceId") int dataspaceId,
                                                           @Param("anchorNames") Collection<String> anchorNames);

    default Collection<AnchorEntity> findAllByDataspaceAndNameIn(final DataspaceEntity dataspaceEntity,
                                                                 final Collection<String> anchorNames) {
        return findAllByDataspaceIdAndNameIn(dataspaceEntity.getId(), anchorNames);
    }

    @Query(value = "SELECT a.* FROM anchor a"
        + " LEFT OUTER JOIN schema_set s ON a.schema_set_id = s.id"
        + " WHERE a.dataspace_id = :dataspaceId AND s.name IN (:schemaSetNames)",
        nativeQuery = true)
    Collection<AnchorEntity> findAllByDataspaceIdAndSchemaSetNameIn(
            @Param("dataspaceId") int dataspaceId, @Param("schemaSetNames") Collection<String> schemaSetNames);

    default Collection<AnchorEntity> findAllByDataspaceAndSchemaSetNameIn(final DataspaceEntity dataspaceEntity,
                                                                          final Collection<String> schemaSetNames) {
        return findAllByDataspaceIdAndSchemaSetNameIn(dataspaceEntity.getId(), schemaSetNames);
    }

    Integer countByDataspace(DataspaceEntity dataspaceEntity);

    @Query(value = """
            SELECT
                anchor.name
            FROM
                     yang_resource
                JOIN schema_set_yang_resources ON schema_set_yang_resources.yang_resource_id = yang_resource.id
                JOIN schema_set ON schema_set.id = schema_set_yang_resources.schema_set_id
                JOIN anchor ON anchor.schema_set_id = schema_set.id
            WHERE
                    schema_set.dataspace_id = :dataspaceId
                AND module_name IN (:moduleNames)
            GROUP BY
                anchor.id,
                anchor.name,
                anchor.dataspace_id,
                anchor.schema_set_id
            HAVING
                COUNT(DISTINCT module_name) = :sizeOfModuleNames
            """, nativeQuery = true)
    Collection<String> getAnchorNamesByDataspaceIdAndModuleNames(@Param("dataspaceId") int dataspaceId,
                                                                 @Param("moduleNames") Collection<String> moduleNames,
                                                                 @Param("sizeOfModuleNames") int sizeOfModuleNames);

    @Modifying
    @Query(value = "DELETE FROM anchor WHERE dataspace_id = :dataspaceId AND name IN (:anchorNames)",
        nativeQuery = true)
    void deleteAllByDataspaceIdAndNameIn(@Param("dataspaceId") int dataspaceId,
                                         @Param("anchorNames") Collection<String> anchorNames);

    default void deleteAllByDataspaceAndNameIn(final DataspaceEntity dataspaceEntity,
                                               final Collection<String> anchorNames) {
        deleteAllByDataspaceIdAndNameIn(dataspaceEntity.getId(), anchorNames);
    }

    @Modifying
    @Query(value = "UPDATE anchor SET schema_set_id =:schemaSetId WHERE id = :anchorId ", nativeQuery = true)
    void updateAnchorSchemaSetId(@Param("schemaSetId") int schemaSetId, @Param("anchorId") long anchorId);

}
