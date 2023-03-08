/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2023 Nordix Foundation.
 * Modifications Copyright (C) 2020-2021 Bell Canada.
 * Modifications Copyright (C) 2020-2021 Pantheon.tech.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.spi.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.onap.cps.spi.entities.AnchorEntity;
import org.onap.cps.spi.entities.DataspaceEntity;
import org.onap.cps.spi.entities.FragmentEntity;
import org.onap.cps.spi.entities.FragmentExtract;
import org.onap.cps.spi.exceptions.DataNodeNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FragmentRepository extends JpaRepository<FragmentEntity, Long>, FragmentRepositoryCpsPathQuery,
        FragmentNativeRepository {

    Optional<FragmentEntity> findByDataspaceAndAnchorAndXpath(DataspaceEntity dataspaceEntity,
                                                              AnchorEntity anchorEntity,
                                                              String xpath);

    default FragmentEntity getByDataspaceAndAnchorAndXpath(DataspaceEntity dataspaceEntity,
                                                           AnchorEntity anchorEntity,
                                                           String xpath) {
        return findByDataspaceAndAnchorAndXpath(dataspaceEntity, anchorEntity, xpath)
            .orElseThrow(() -> new DataNodeNotFoundException(dataspaceEntity.getName(), anchorEntity.getName(), xpath));
    }

    @Query(
        value = "SELECT * FROM FRAGMENT WHERE anchor_id = :anchor AND dataspace_id = :dataspace AND parent_id is NULL",
        nativeQuery = true)
    List<FragmentEntity> findRootsByDataspaceAndAnchor(@Param("dataspace") int dataspaceId,
                                                       @Param("anchor") int anchorId);

    @Query(value = "SELECT id, anchor_id AS anchorId, xpath, parent_id AS parentId,"
            + " CAST(attributes AS TEXT) AS attributes"
            + " FROM FRAGMENT WHERE anchor_id = :anchorId",
            nativeQuery = true)
    List<FragmentExtract> findRootsByAnchorId(@Param("anchorId") int anchorId);

    /**
     * find top level fragment by anchor.
     *
     * @param dataspaceEntity dataspace entity
     * @param anchorEntity anchor entity
     * @return FragmentEntity fragment entity
     */
    default List<FragmentExtract> getTopLevelFragments(DataspaceEntity dataspaceEntity,
                                                       AnchorEntity anchorEntity) {
        final List<FragmentExtract> fragmentExtracts = findRootsByAnchorId(anchorEntity.getId());
        if (fragmentExtracts.isEmpty()) {
            throw new DataNodeNotFoundException(dataspaceEntity.getName(), anchorEntity.getName());
        }
        return fragmentExtracts;
    }

    @Modifying
    @Query("DELETE FROM FragmentEntity WHERE anchor IN (:anchors)")
    void deleteByAnchorIn(@Param("anchors") Collection<AnchorEntity> anchorEntities);

    @Query(value = "SELECT id, anchor_id AS anchorId, xpath, parent_id AS parentId,"
        + " CAST(attributes AS TEXT) AS attributes"
        + " FROM FRAGMENT WHERE anchor_id = :anchorId"
        + " AND ( xpath = :parentXpath OR xpath LIKE CONCAT(:parentXpath,'/%') )",
           nativeQuery = true)
    List<FragmentExtract> findByAnchorIdAndParentXpath(@Param("anchorId") int anchorId,
                                                       @Param("parentXpath") String parentXpath);

    @Query(value = "SELECT id, anchor_id AS anchorId, xpath, parent_id AS parentId,"
        + " CAST(attributes AS TEXT) AS attributes"
        + " FROM FRAGMENT WHERE anchor_id = :anchorId"
        + " AND xpath ~ :xpathRegex",
        nativeQuery = true)
    List<FragmentExtract> quickFindWithDescendants(@Param("anchorId") int anchorId,
                                                   @Param("xpathRegex") String xpathRegex);

    @Query("SELECT xpath FROM FragmentEntity WHERE anchor = :anchor AND xpath IN :xpaths")
    List<String> findAllXpathByAnchorAndXpathIn(@Param("anchor") AnchorEntity anchorEntity,
                                                @Param("xpaths") Collection<String> xpaths);

    boolean existsByAnchorAndXpathStartsWith(AnchorEntity anchorEntity, String xpath);

    @Query("SELECT xpath FROM FragmentEntity WHERE anchor = :anchor AND parentId IS NULL")
    List<String> findAllXpathByAnchorAndParentIdIsNull(@Param("anchor") AnchorEntity anchorEntity);

    @Query(value
        = "WITH RECURSIVE parent_search AS ("
        + "  SELECT id, 0 AS depth "
        + "    FROM fragment "
        + "   WHERE anchor_id = :anchorId AND xpath IN :xpaths "
        + "   UNION "
        + "  SELECT c.id, depth + 1 "
        + "    FROM fragment c INNER JOIN parent_search p ON c.parent_id = p.id"
        + "   WHERE depth <= (SELECT CASE WHEN :maxDepth = -1 THEN " + Integer.MAX_VALUE + " ELSE :maxDepth END) "
        + ") "
        + "SELECT f.id, anchor_id AS anchorId, xpath, f.parent_id AS parentId, CAST(attributes AS TEXT) AS attributes "
        + "FROM fragment f INNER JOIN parent_search p ON f.id = p.id",
        nativeQuery = true
    )
    List<FragmentExtract> findExtractsWithDescendants(@Param("anchorId") int anchorId,
                                                      @Param("xpaths") Collection<String> xpaths,
                                                      @Param("maxDepth") int maxDepth);
}
