/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Pantheon.tech
 *  Modifications Copyright (C) 2021-2025 Nordix Foundation
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
import java.util.Set;
import org.onap.cps.ri.models.YangResourceEntity;
import org.onap.cps.ri.models.YangResourceModuleReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface YangResourceRepository extends JpaRepository<YangResourceEntity, Integer>,
    YangResourceNativeRepository, SchemaSetYangResourceRepository {

    List<YangResourceEntity> findAllByChecksumIn(Collection<String> checksums);

    @Query(value = """
            SELECT DISTINCT
                yang_resource.module_name AS module_name,
                yang_resource.revision    AS revision
            FROM
                     dataspace
                JOIN schema_set ON schema_set.dataspace_id = dataspace.id
                JOIN schema_set_yang_resources ON schema_set_yang_resources.schema_set_id = schema_set.id
                JOIN yang_resource ON yang_resource.id = schema_set_yang_resources.yang_resource_id
            WHERE
                dataspace.name = :dataspaceName
            """, nativeQuery = true)
    Set<YangResourceModuleReference> findAllModuleReferencesByDataspace(@Param("dataspaceName") String dataspaceName);

    @Query(value = """
            SELECT DISTINCT
                yang_resource.module_name AS module_name,
                yang_resource.revision    AS revision
            FROM
                     dataspace
                JOIN anchor ON anchor.dataspace_id = dataspace.id
                JOIN schema_set ON schema_set.id = anchor.schema_set_id
                JOIN schema_set_yang_resources ON schema_set_yang_resources.schema_set_id = schema_set.id
                JOIN yang_resource ON yang_resource.id = schema_set_yang_resources.yang_resource_id
            WHERE
                    dataspace.name = :dataspaceName
                AND anchor.name = :anchorName
            """, nativeQuery = true)
    Set<YangResourceModuleReference> findAllModuleReferencesByDataspaceAndAnchor(
        @Param("dataspaceName") String dataspaceName, @Param("anchorName") String anchorName);

    @Query(value = """
            SELECT DISTINCT
                yang_resource.*
            FROM
                     dataspace
                JOIN anchor ON anchor.dataspace_id = dataspace.id
                JOIN schema_set ON schema_set.id = anchor.schema_set_id
                JOIN schema_set_yang_resources ON schema_set_yang_resources.schema_set_id = schema_set.id
                JOIN yang_resource ON yang_resource.id = schema_set_yang_resources.yang_resource_id
            WHERE
                    dataspace.name = :dataspaceName
                AND anchor.name = :anchorName
                AND (:moduleName IS NULL OR yang_resource.module_name = :moduleName)
                AND (:revision IS NULL OR yang_resource.revision = :revision)
            """, nativeQuery = true)
    Set<YangResourceEntity> findAllModuleDefinitionsByDataspaceAndAnchorAndModule(
            @Param("dataspaceName") String dataspaceName, @Param("anchorName") String anchorName,
            @Param("moduleName") String moduleName, @Param("revision") String revision);

    @Modifying
    @Query(value = "DELETE FROM schema_set_yang_resources WHERE schema_set_id = :schemaSetId", nativeQuery = true)
    void deleteSchemaSetYangResourceForSchemaSetId(@Param("schemaSetId") int schemaSetId);

    @Modifying
    @Query(value = """
            DELETE FROM yang_resource WHERE NOT EXISTS (SELECT 1 FROM schema_set_yang_resources
            WHERE schema_set_yang_resources.yang_resource_id = yang_resource.id)", nativeQuery = true)
          """)
    void deleteOrphanedYangResources();
}
