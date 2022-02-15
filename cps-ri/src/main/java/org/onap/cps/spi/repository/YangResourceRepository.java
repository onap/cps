/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Pantheon.tech
 *  Modifications Copyright (C) 2021-2022 Nordix Foundation
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
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;
import org.onap.cps.spi.entities.YangResourceEntity;
import org.onap.cps.spi.entities.YangResourceModuleReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface YangResourceRepository extends JpaRepository<YangResourceEntity, Long>,
        SchemaSetYangResourceRepository {

    List<YangResourceEntity> findAllByChecksumIn(@NotNull Set<String> checksum);

    @Query(value = "SELECT DISTINCT\n"
        + "yang_resource.module_name AS module_name,\n"
        + "yang_resource.revision AS revision\n"
        + "FROM\n"
        + "dataspace\n"
        + "JOIN schema_set ON schema_set.dataspace_id = dataspace.id\n"
        + "JOIN schema_set_yang_resources ON schema_set_yang_resources.schema_set_id = "
        + "schema_set.id\n"
        + "JOIN yang_resource ON yang_resource.id = schema_set_yang_resources.yang_resource_id\n"
        + "WHERE\n"
        + "dataspace.name = :dataspaceName", nativeQuery = true)
    Set<YangResourceModuleReference> findAllModuleReferences(@Param("dataspaceName") String dataspaceName);

    @Query(value = "SELECT DISTINCT\n"
        + "yang_resource.module_Name AS module_name,\n"
        + "yang_resource.revision AS revision\n"
        + "FROM\n"
        + "dataspace\n"
        + "JOIN anchor ON anchor.dataspace_id = dataspace.id\n"
        + "JOIN schema_set ON schema_set.id = anchor.schema_set_id\n"
        + "JOIN schema_set_yang_resources ON schema_set_yang_resources.schema_set_id = "
        + "schema_set.id\n"
        + "JOIN yang_resource ON yang_resource.id = schema_set_yang_resources.yang_resource_id\n"
        + "WHERE\n"
        + "dataspace.name = :dataspaceName AND\n"
        + "anchor.name =:anchorName", nativeQuery = true)
    Set<YangResourceModuleReference> findAllModuleReferences(
        @Param("dataspaceName") String dataspaceName, @Param("anchorName") String anchorName);

    @Query(value = "SELECT DISTINCT\n"
        + "yang_resource.*\n"
        + "FROM\n"
        + "dataspace\n"
        + "JOIN schema_set ON schema_set.dataspace_id = dataspace.id\n"
        + "JOIN schema_set_yang_resources ON schema_set_yang_resources.schema_set_id = "
        + "schema_set.id\n"
        + "JOIN yang_resource ON yang_resource.id = schema_set_yang_resources.yang_resource_id\n"
        + "WHERE\n"
        + "dataspace.name = :dataspaceName and yang_resource.module_Name IN (:moduleNames)", nativeQuery = true)
    Set<YangResourceModuleReference> findAllModuleReferences(@Param("dataspaceName") String dataspaceName,
        @Param("moduleNames") Collection<String> moduleNames);


    @Query(value = "SELECT id FROM yang_resource WHERE module_name=:name and revision=:revision", nativeQuery = true)
    Long getIdByModuleNameAndRevision(@Param("name") String moduleName, @Param("revision") String revision);

    @Modifying
    @Query(value = "DELETE FROM yang_resource yr WHERE NOT EXISTS "
        + "(SELECT 1 FROM schema_set_yang_resources ssyr WHERE ssyr.yang_resource_id = yr.id)", nativeQuery = true)
    void deleteOrphans();
}
