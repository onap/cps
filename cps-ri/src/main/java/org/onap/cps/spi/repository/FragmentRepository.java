/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2023 Nordix Foundation.
 * Modifications Copyright (C) 2020-2021 Bell Canada.
 * Modifications Copyright (C) 2020-2021 Pantheon.tech.
 * Modifications Copyright (C) 2023 TechMahindra Ltd.
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
import org.onap.cps.spi.entities.FragmentEntity;
import org.onap.cps.spi.entities.FragmentExtract;
import org.onap.cps.spi.exceptions.DataNodeNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FragmentRepository extends JpaRepository<FragmentEntity, Long>, FragmentRepositoryCpsPathQuery {

    Optional<FragmentEntity> findByAnchorAndXpath(AnchorEntity anchorEntity, String xpath);

    default FragmentEntity getByAnchorAndXpath(final AnchorEntity anchorEntity, final String xpath) {
        return findByAnchorAndXpath(anchorEntity, xpath).orElseThrow(() ->
            new DataNodeNotFoundException(anchorEntity.getDataspace().getName(), anchorEntity.getName(), xpath));
    }

    boolean existsByAnchorId(int anchorId);

    @Query("SELECT f FROM FragmentEntity f WHERE anchor = :anchor")
    List<FragmentExtract> findAllExtractsByAnchor(@Param("anchor") AnchorEntity anchorEntity);

    @Query(value = "SELECT * FROM fragment WHERE xpath = ANY (:xpaths)", nativeQuery = true)
    List<FragmentEntity> findAllByXpathIn(@Param("xpaths") String[] xpath);

    default List<FragmentEntity> findAllByXpathIn(final Collection<String> xpaths) {
        return findAllByXpathIn(xpaths.toArray(new String[0]));
    }

    @Modifying
    @Query(value = "DELETE FROM fragment WHERE anchor_id = ANY (:anchorIds)", nativeQuery = true)
    void deleteByAnchorIdIn(@Param("anchorIds") int[] anchorIds);

    default void deleteByAnchorIn(final Collection<AnchorEntity> anchorEntities) {
        deleteByAnchorIdIn(anchorEntities.stream().map(AnchorEntity::getId).mapToInt(id -> id).toArray());
    }

    @Modifying
    @Query(value = "DELETE FROM fragment WHERE anchor_id = :anchorId AND xpath = ANY (:xpaths)", nativeQuery = true)
    void deleteByAnchorIdAndXpaths(@Param("anchorId") int anchorId, @Param("xpaths") String[] xpaths);

    default void deleteByAnchorIdAndXpaths(final int anchorId, final Collection<String> xpaths) {
        deleteByAnchorIdAndXpaths(anchorId, xpaths.toArray(new String[0]));
    }

    @Modifying
    @Query(value = "DELETE FROM fragment f WHERE anchor_id = :anchorId AND xpath LIKE ANY (:xpathPatterns)",
        nativeQuery = true)
    void deleteByAnchorIdAndXpathLikeAny(@Param("anchorId") int anchorId,
                                         @Param("xpathPatterns") String[] xpathPatterns);

    default void deleteListsByAnchorIdAndXpaths(int anchorId, Collection<String> xpaths) {
        final String[] listXpathPatterns = xpaths.stream().map(xpath -> xpath + "[%").toArray(String[]::new);
        deleteByAnchorIdAndXpathLikeAny(anchorId, listXpathPatterns);
    }

    @Query("SELECT f FROM FragmentEntity f WHERE anchor = :anchor"
        + " AND (xpath = :parentXpath OR xpath LIKE CONCAT(:parentXpath,'/%'))")
    List<FragmentExtract> findByAnchorAndParentXpath(@Param("anchor") AnchorEntity anchorEntity,
                                                     @Param("parentXpath") String parentXpath);

    @Query(value = "SELECT id, anchor_id AS anchorId, xpath, parent_id AS parentId,"
        + " CAST(attributes AS TEXT) AS attributes"
        + " FROM FRAGMENT WHERE anchor_id = :anchorId"
        + " AND xpath ~ :xpathRegex",
        nativeQuery = true)
    List<FragmentExtract> quickFindWithDescendants(@Param("anchorId") int anchorId,
                                                   @Param("xpathRegex") String xpathRegex);

    @Query(value = "SELECT xpath FROM fragment WHERE anchor_id = :anchorId AND xpath = ANY (:xpaths)",
        nativeQuery = true)
    List<String> findAllXpathByAnchorIdAndXpathIn(@Param("anchorId") int anchorId,
                                                  @Param("xpaths") String[] xpaths);

    default List<String> findAllXpathByAnchorAndXpathIn(final AnchorEntity anchorEntity,
                                                        final Collection<String> xpaths) {
        return findAllXpathByAnchorIdAndXpathIn(anchorEntity.getId(), xpaths.toArray(new String[0]));
    }

    boolean existsByAnchorAndXpathStartsWith(AnchorEntity anchorEntity, String xpath);

    @Query("SELECT xpath FROM FragmentEntity WHERE anchor = :anchor AND parentId IS NULL")
    List<String> findAllXpathByAnchorAndParentIdIsNull(@Param("anchor") AnchorEntity anchorEntity);

    @Query(value
        = "WITH RECURSIVE parent_search AS ("
        + "  SELECT id, 0 AS depth "
        + "    FROM fragment "
        + "   WHERE anchor_id = :anchorId AND xpath = ANY (:xpaths) "
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
                                                      @Param("xpaths") String[] xpaths,
                                                      @Param("maxDepth") int maxDepth);

    default List<FragmentExtract> findExtractsWithDescendants(final int anchorId, final Collection<String> xpaths,
                                                              final int maxDepth) {
        return findExtractsWithDescendants(anchorId, xpaths.toArray(new String[0]), maxDepth);
    }

    @Query(value = "SELECT id, anchor_id AS anchorId, xpath, parent_id AS parentId,"
            + " CAST(attributes AS TEXT) AS attributes"
            + " FROM FRAGMENT WHERE xpath ~ :xpathRegex",
            nativeQuery = true)
    List<FragmentExtract> quickFindWithDescendantsAcrossAnchor(@Param("xpathRegex") String xpathRegex);
}
