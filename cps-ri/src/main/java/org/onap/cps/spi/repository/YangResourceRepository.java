/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Pantheon.tech
 *  Modifications Copyright (C) Nordix Foundation
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

    @Query(value = "SELECT\n"
        + "yr.module_name AS module_name,\n"
        + "yr.revision AS revision\n"
        + "FROM\n"
        + "dataspace d\n"
        + "JOIN schema_set ss ON ss.dataspace_id = d.id\n"
        + "JOIN schema_set_yang_resources ssyr ON ssyr.schema_set_id = ss.id\n"
        + "JOIN yang_resource yr ON yr.id = ssyr.yang_resource_id\n"
        + "WHERE\n"
        + "d.name = :dataspaceName", nativeQuery = true)
    List<YangResourceModuleReference> findAllModuleNameAndRevisionReferencesDataspaceName(
        @Param("dataspaceName") String dataspaceName);

    @Query(value = "SELECT\n"
        + "yr.module_Name AS module_name,\n"
        + "yr.revision AS revision\n"
        + "FROM\n"
        + "dataspace d\n"
        + "JOIN anchor a ON a.dataspace_id = d.id\n"
        + "JOIN schema_set ss ON ss.dataspace_id = a.dataspace_id\n"
        + "JOIN schema_set_yang_resources ssyr ON ssyr.schema_set_id = ss.id\n"
        + "JOIN yang_resource yr ON yr.id = ssyr.yang_resource_id\n"
        + "WHERE\n"
        + "d.name = :dataspaceName AND\n"
        + "a.name =:anchorName", nativeQuery = true)
    List<YangResourceModuleReference> findAllModuleNameAndRevisionReferencesDataspaceNameAndAnchorName(
        @Param("dataspaceName") String dataspaceName, @Param("anchorName") String anchorName);

    @Query(value = "SELECT id FROM yang_resource WHERE module_name=:name and revision=:revision", nativeQuery = true)
    Long getIdByModuleNameAndRevision(@Param("name") String moduleName, @Param("revision") String revision);

    @Modifying
    @Query(value = "DELETE FROM yang_resource yr WHERE NOT EXISTS "
        + "(SELECT 1 FROM schema_set_yang_resources ssyr WHERE ssyr.yang_resource_id = yr.id)", nativeQuery = true)
    void deleteOrphans();
}
