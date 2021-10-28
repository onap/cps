/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021 Nordix Foundation
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

import java.util.Collection;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import org.onap.cps.spi.entities.AnchorEntity;
import org.onap.cps.spi.entities.DataspaceEntity;
import org.onap.cps.spi.entities.SchemaSetEntity;
import org.onap.cps.spi.exceptions.AnchorNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnchorRepository extends JpaRepository<AnchorEntity, Integer> {

    Optional<AnchorEntity> findByDataspaceAndName(@NotNull DataspaceEntity dataspaceEntity, @NotNull String name);

    default AnchorEntity getByDataspaceAndName(@NotNull DataspaceEntity dataspace,
        @NotNull String anchorName) {
        return findByDataspaceAndName(dataspace, anchorName)
            .orElseThrow(() -> new AnchorNotFoundException(anchorName, dataspace.getName()));
    }

    Collection<AnchorEntity> findAllByDataspace(@NotNull DataspaceEntity dataspaceEntity);

    Collection<AnchorEntity> findAllBySchemaSet(@NotNull SchemaSetEntity schemaSetEntity);

    @Query(value = "SELECT DISTINCT\n"
        + "anchor.*\n"
        + "FROM\n"
        + "yang_resource\n"
        + "JOIN schema_set_yang_resources ON "
        + "schema_set_yang_resources.yang_resource_id = yang_resource.id\n"
        + "JOIN schema_set ON schema_set.id = schema_set_yang_resources.schema_set_id\n"
        + "JOIN anchor ON anchor.schema_set_id = schema_set.id\n"
        + "JOIN dataspace ON dataspace.id = anchor.dataspace_id AND dataspace.name = :dataspaceName\n"
        + "WHERE yang_resource.module_Name IN (:moduleNames)", nativeQuery = true)
    Collection<AnchorEntity> getAnchorsByDataspaceNameAndModuleNames(@Param("dataspaceName") String dataspaceName,
        @Param("moduleNames") Collection<String> moduleNames);
}