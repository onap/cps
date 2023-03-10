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
        FragmentRepositoryMultiPathQuery, FragmentNativeRepository {

    Optional<FragmentEntity> findByAnchorAndXpath(AnchorEntity anchorEntity, String xpath);

    default FragmentEntity getByAnchorAndXpath(final AnchorEntity anchorEntity, final String xpath) {
        return findByAnchorAndXpath(anchorEntity, xpath).orElseThrow(() ->
            new DataNodeNotFoundException(anchorEntity.getDataspace().getName(), anchorEntity.getName(), xpath));
    }

    boolean existsByAnchorId(int anchorId);

    @Query("SELECT f FROM FragmentEntity f WHERE anchor = :anchor")
    List<FragmentExtract> findAllExtractsByAnchor(@Param("anchor") AnchorEntity anchorEntity);

    @Modifying
    @Query("DELETE FROM FragmentEntity WHERE anchor IN (:anchors)")
    void deleteByAnchorIn(@Param("anchors") Collection<AnchorEntity> anchorEntities);

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

    @Query("SELECT xpath FROM FragmentEntity f WHERE anchor = :anchor AND xpath IN :xpaths")
    List<String> findAllXpathByAnchorAndXpathIn(@Param("anchor") AnchorEntity anchorEntity,
                                                @Param("xpaths") Collection<String> xpaths);

    boolean existsByAnchorAndXpathStartsWith(AnchorEntity anchorEntity, String xpath);
}
