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
import org.onap.cps.spi.entities.DataspaceEntity;
import org.onap.cps.spi.entities.FragmentEntity;
import org.onap.cps.spi.exceptions.DataNodeNotFoundException;
import org.onap.cps.spi.utils.EscapeUtils;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FragmentRepository extends JpaRepository<FragmentEntity, Long>, FragmentRepositoryCpsPathQuery,
        FragmentPrefetchRepository {

    Optional<FragmentEntity> findByAnchorAndXpath(AnchorEntity anchorEntity, String xpath);

    default FragmentEntity getByAnchorAndXpath(final AnchorEntity anchorEntity, final String xpath) {
        return findByAnchorAndXpath(anchorEntity, xpath).orElseThrow(() ->
            new DataNodeNotFoundException(anchorEntity.getDataspace().getName(), anchorEntity.getName(), xpath));
    }

    @Query(value = "SELECT * FROM fragment WHERE anchor_id = :anchorId AND xpath = ANY (:xpaths)",
            nativeQuery = true)
    List<FragmentEntity> findByAnchorIdAndXpathIn(@Param("anchorId") long anchorId,
                                                  @Param("xpaths") String[] xpaths);

    default List<FragmentEntity> findByAnchorAndXpathIn(final AnchorEntity anchorEntity,
                                                        final Collection<String> xpaths) {
        return findByAnchorIdAndXpathIn(anchorEntity.getId(), xpaths.toArray(new String[0]));
    }

    @Query(value = "SELECT fragment.* FROM fragment JOIN anchor ON anchor.id = fragment.anchor_id "
        + "WHERE dataspace_id = :dataspaceId AND xpath = ANY (:xpaths)", nativeQuery = true)
    List<FragmentEntity> findByDataspaceIdAndXpathIn(@Param("dataspaceId") int dataspaceId,
                                                     @Param("xpaths") String[] xpaths);

    default List<FragmentEntity> findByDataspaceAndXpathIn(final DataspaceEntity dataspaceEntity,
                                                           final Collection<String> xpaths) {
        return findByDataspaceIdAndXpathIn(dataspaceEntity.getId(), xpaths.toArray(new String[0]));
    }

    @Query(value = "SELECT * FROM fragment WHERE anchor_id IN (:anchorIds)"
            + " AND xpath = ANY (:xpaths)", nativeQuery = true)
    List<FragmentEntity> findByAnchorIdsAndXpathIn(@Param("anchorIds") Long[] anchorIds,
                                                   @Param("xpaths") String[] xpaths);

    @Query(value = "SELECT * FROM fragment WHERE anchor_id = :anchorId LIMIT 1", nativeQuery = true)
    Optional<FragmentEntity> findOneByAnchorId(@Param("anchorId") long anchorId);

    @Modifying
    @Query(value = "DELETE FROM fragment WHERE anchor_id = ANY (:anchorIds)", nativeQuery = true)
    void deleteByAnchorIdIn(@Param("anchorIds") long[] anchorIds);

    default void deleteByAnchorIn(final Collection<AnchorEntity> anchorEntities) {
        deleteByAnchorIdIn(anchorEntities.stream().map(AnchorEntity::getId).mapToLong(id -> id).toArray());
    }

    @Modifying
    @Query(value = "DELETE FROM fragment WHERE anchor_id = :anchorId AND xpath = ANY (:xpaths)", nativeQuery = true)
    void deleteByAnchorIdAndXpaths(@Param("anchorId") long anchorId, @Param("xpaths") String[] xpaths);

    default void deleteByAnchorIdAndXpaths(final long anchorId, final Collection<String> xpaths) {
        deleteByAnchorIdAndXpaths(anchorId, xpaths.toArray(new String[0]));
    }

    @Modifying
    @Query(value = "DELETE FROM fragment f WHERE anchor_id = :anchorId AND xpath LIKE ANY (:xpathPatterns)",
        nativeQuery = true)
    void deleteByAnchorIdAndXpathLikeAny(@Param("anchorId") long anchorId,
                                         @Param("xpathPatterns") String[] xpathPatterns);

    default void deleteListsByAnchorIdAndXpaths(long anchorId, Collection<String> xpaths) {
        deleteByAnchorIdAndXpathLikeAny(anchorId,
                xpaths.stream().map(xpath -> EscapeUtils.escapeForSqlLike(xpath) + "[@%").toArray(String[]::new));
    }

    @Query(value = "SELECT xpath FROM fragment WHERE anchor_id = :anchorId AND xpath = ANY (:xpaths)",
        nativeQuery = true)
    List<String> findAllXpathByAnchorIdAndXpathIn(@Param("anchorId") long anchorId,
                                                  @Param("xpaths") String[] xpaths);

    default List<String> findAllXpathByAnchorAndXpathIn(final AnchorEntity anchorEntity,
                                                        final Collection<String> xpaths) {
        return findAllXpathByAnchorIdAndXpathIn(anchorEntity.getId(), xpaths.toArray(new String[0]));
    }

    boolean existsByAnchorAndXpathStartsWith(AnchorEntity anchorEntity, String xpath);

    @Query("SELECT xpath FROM FragmentEntity WHERE anchor = :anchor AND parentId IS NULL")
    List<String> findAllXpathByAnchorAndParentIdIsNull(@Param("anchor") AnchorEntity anchorEntity);

}
