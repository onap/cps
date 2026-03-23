/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2024 Nordix Foundation.
 * Modifications Copyright (C) 2020-2021 Bell Canada.
 * Modifications Copyright (C) 2020-2021 Pantheon.tech.
 * Modifications Copyright (C) 2023 Deutsche Telekom AG
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

package org.onap.cps.ri.repository;

import java.util.Collection;
import java.util.List;
import org.onap.cps.ri.models.AnchorEntity;
import org.onap.cps.ri.models.FragmentEntity;
import org.onap.cps.ri.utils.EscapeUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FragmentRepository extends JpaRepository<FragmentEntity, Long>, FragmentRepositoryCpsPathQuery,
        FragmentPrefetchRepository {

    @Query(value = "SELECT * FROM fragment WHERE anchor_id = :anchorId AND xpath = :xpath", nativeQuery = true)
    FragmentEntity findByAnchorIdAndXpath(@Param("anchorId") long anchorId, @Param("xpath") String xpath);

    @Query(value = "SELECT * FROM fragment WHERE anchor_id = :anchorId AND xpath IN (:xpaths)",
            nativeQuery = true)
    List<FragmentEntity> findByAnchorIdAndXpathIn(@Param("anchorId") long anchorId,
                                                  @Param("xpaths") Collection<String> xpaths);

    default List<FragmentEntity> findByAnchorAndXpathIn(final AnchorEntity anchorEntity,
                                                        final Collection<String> xpaths) {
        return findByAnchorIdAndXpathIn(anchorEntity.getId(), xpaths);
    }

    @Query(value = "SELECT * FROM fragment WHERE anchor_id = :anchorId \n"
            + "AND xpath LIKE :escapedXpath||'[@%]' AND xpath NOT LIKE :escapedXpath||'[@%]/%[@%]'",
            nativeQuery = true)
    List<FragmentEntity> findListByAnchorIdAndEscapedXpath(@Param("anchorId") long anchorId,
                                                           @Param("escapedXpath") String escapedXpath);

    default List<FragmentEntity> findListByAnchorAndXpath(final AnchorEntity anchorEntity, final String xpath) {
        final String escapedXpath = EscapeUtils.escapeForSqlLike(xpath);
        return findListByAnchorIdAndEscapedXpath(anchorEntity.getId(), escapedXpath);
    }

    @Modifying
    @Query(value = "DELETE FROM fragment WHERE anchor_id IN (:anchorIds)", nativeQuery = true)
    void deleteByAnchorIdIn(@Param("anchorIds") Collection<Long> anchorIds);

    default void deleteByAnchorIn(final Collection<AnchorEntity> anchorEntities) {
        deleteByAnchorIdIn(anchorEntities.stream().map(AnchorEntity::getId).toList());
    }

    @Modifying
    @Query(value = "DELETE FROM fragment WHERE anchor_id = :anchorId AND xpath IN (:xpaths)", nativeQuery = true)
    void deleteByAnchorIdAndXpaths(@Param("anchorId") long anchorId, @Param("xpaths") Collection<String> xpaths);

    @Modifying
    @Query(value = "DELETE FROM fragment f WHERE anchor_id = :anchorId AND xpath LIKE :xpathPattern",
        nativeQuery = true)
    void deleteByAnchorIdAndXpathLike(@Param("anchorId") long anchorId, @Param("xpathPattern") String xpathPattern);

    default void deleteListByAnchorIdAndXpath(final long anchorId, final String xpath) {
        deleteByAnchorIdAndXpathLike(anchorId, EscapeUtils.escapeForSqlLike(xpath) + "[@%");
    }

    @Query(value = "SELECT xpath FROM fragment WHERE anchor_id = :anchorId AND xpath IN (:xpaths)",
        nativeQuery = true)
    List<String> findAllXpathByAnchorIdAndXpathIn(@Param("anchorId") long anchorId,
                                                  @Param("xpaths") Collection<String> xpaths);

    default List<String> findAllXpathByAnchorAndXpathIn(final AnchorEntity anchorEntity,
                                                        final Collection<String> xpaths) {
        return findAllXpathByAnchorIdAndXpathIn(anchorEntity.getId(), xpaths);
    }

    @Query(value = "SELECT EXISTS(SELECT 1 FROM fragment WHERE anchor_id = :anchorId"
            + " AND xpath LIKE :xpathPattern LIMIT 1)", nativeQuery = true)
    boolean existsByAnchorIdAndParentXpathAndXpathLike(@Param("anchorId") long anchorId,
                                                       @Param("xpathPattern") String xpathPattern);

    default boolean existsByAnchorAndXpathStartsWith(final AnchorEntity anchorEntity, final String xpath) {
        return existsByAnchorIdAndParentXpathAndXpathLike(anchorEntity.getId(),
                EscapeUtils.escapeForSqlLike(xpath) + "%");
    }

    @Query(value = "SELECT * FROM fragment WHERE anchor_id = :anchorId AND parent_id IS NULL", nativeQuery = true)
    List<FragmentEntity> findRootsByAnchorId(@Param("anchorId") long anchorId);

    @Query(value = "SELECT COALESCE(s.xpath, t.xpath) AS xpath, "
        + "s.id AS sourceId, t.id AS targetId, "
        + "s.attributes AS sourceAttributes, t.attributes AS targetAttributes "
        + "FROM (SELECT id, xpath, attributes FROM fragment WHERE anchor_id = :sourceAnchorId) s "
        + "FULL OUTER JOIN (SELECT id, xpath, attributes FROM fragment WHERE anchor_id = :targetAnchorId) t "
        + "ON s.xpath = t.xpath "
        + "WHERE (s.id IS NULL OR t.id IS NULL OR s.attributes IS DISTINCT FROM t.attributes) "
        + "AND (:escapedXpath = '/' OR COALESCE(s.xpath, t.xpath) LIKE :escapedXpath || '%')",
        nativeQuery = true)
    List<DeltaProjection> findCompositeDeltaFragments(@Param("sourceAnchorId") long sourceAnchorId,
                                                      @Param("targetAnchorId") long targetAnchorId,
                                                      @Param("escapedXpath") String escapedXpath);

    @Query(value = "SELECT COALESCE(s.xpath, t.xpath) AS xpath, "
        + "s.id AS sourceId, t.id AS targetId, "
        + "s.attributes AS sourceAttributes, t.attributes AS targetAttributes "
        + "FROM (SELECT id, xpath, attributes, parent_id FROM fragment WHERE anchor_id = :sourceAnchorId) s "
        + "FULL OUTER JOIN (SELECT id, xpath, attributes, parent_id FROM fragment WHERE anchor_id = :targetAnchorId) t "
        + "ON s.xpath = t.xpath "
        + "WHERE (s.id IS NULL OR t.id IS NULL OR s.attributes IS DISTINCT FROM t.attributes) "
        + "AND ((:xpath = '/' AND COALESCE(s.parent_id, t.parent_id) IS NULL) "
        + "  OR (:xpath <> '/' AND COALESCE(s.xpath, t.xpath) = :xpath))",
        nativeQuery = true)
    List<DeltaProjection> findCompositeDeltaFragmentsExactXpath(@Param("sourceAnchorId") long sourceAnchorId,
                                                                @Param("targetAnchorId") long targetAnchorId,
                                                                @Param("xpath") String xpath);

    @Query(value = "SELECT COALESCE(s.xpath, t.xpath) AS xpath, "
        + "s.id AS sourceId, t.id AS targetId, "
        + "s.attributes AS sourceAttributes, t.attributes AS targetAttributes "
        + "FROM (SELECT id, xpath, attributes, parent_id FROM fragment WHERE anchor_id = :sourceAnchorId) s "
        + "FULL OUTER JOIN (SELECT id, xpath, attributes, parent_id FROM fragment WHERE anchor_id = :targetAnchorId) t "
        + "ON s.xpath = t.xpath "
        + "WHERE (s.id IS NULL OR t.id IS NULL OR s.attributes IS DISTINCT FROM t.attributes) "
        + "AND ("
        + "  (:xpath = '/' AND COALESCE(s.parent_id, t.parent_id) IS NULL)"
        + "  OR (:xpath <> '/' AND (COALESCE(s.xpath, t.xpath) = :xpath "
        + "    OR COALESCE(s.xpath, t.xpath) LIKE :escapedXpath || '[@%]'"
        + "    AND COALESCE(s.xpath, t.xpath) NOT LIKE :escapedXpath || '[@%]/%[@%]'))"
        + "  OR COALESCE(s.parent_id, t.parent_id) IN ("
        + "    SELECT f.id FROM fragment f WHERE f.anchor_id IN (:sourceAnchorId, :targetAnchorId) "
        + "    AND ((:xpath = '/' AND f.parent_id IS NULL)"
        + "      OR (:xpath <> '/' AND (f.xpath = :xpath OR f.xpath LIKE :escapedXpath || '[@%]'"
        + "        AND f.xpath NOT LIKE :escapedXpath || '[@%]/%[@%]')))"
        + "  )"
        + ")",
        nativeQuery = true)
    List<DeltaProjection> findCompositeDeltaFragmentsWithDirectChildren(
        @Param("sourceAnchorId") long sourceAnchorId,
        @Param("targetAnchorId") long targetAnchorId,
        @Param("xpath") String xpath,
        @Param("escapedXpath") String escapedXpath);



    /**
     * Finds all delta fragments between two anchors for a given xpath (subtree match).
     *
     * @param sourceAnchorId the source anchor id
     * @param targetAnchorId the target anchor id
     * @param xpath          the xpath to filter fragments
     * @return list of delta projection DTOs
     */
    default List<DeltaProjectionDto> findAllDeltaFragments(final long sourceAnchorId, final long targetAnchorId,
                                                           final String xpath) {
        return findCompositeDeltaFragments(sourceAnchorId, targetAnchorId, EscapeUtils.escapeForSqlLike(xpath))
            .stream().map(DeltaProjectionDto::from).toList();
    }

    /**
     * Finds all delta fragments between two anchors for an exact xpath match.
     *
     * @param sourceAnchorId the source anchor id
     * @param targetAnchorId the target anchor id
     * @param xpath          the exact xpath to match fragments
     * @return list of delta projection DTOs
     */
    default List<DeltaProjectionDto> findAllDeltaFragmentsExactXpath(final long sourceAnchorId,
                                                                     final long targetAnchorId,
                                                                     final String xpath) {
        return findCompositeDeltaFragmentsExactXpath(sourceAnchorId, targetAnchorId, xpath)
            .stream().map(DeltaProjectionDto::from).toList();
    }

    /**
     * Finds all delta fragments between two anchors for a given xpath, including direct children.
     *
     * @param sourceAnchorId the source anchor id
     * @param targetAnchorId the target anchor id
     * @param xpath          the xpath to filter fragments and their direct children
     * @return list of delta projection DTOs
     */
    default List<DeltaProjectionDto> findAllDeltaFragmentsWithDirectChildren(final long sourceAnchorId,
                                                                             final long targetAnchorId,
                                                                             final String xpath) {
        return findCompositeDeltaFragmentsWithDirectChildren(sourceAnchorId, targetAnchorId,
            xpath, EscapeUtils.escapeForSqlLike(xpath))
            .stream().map(DeltaProjectionDto::from).toList();
    }

}
