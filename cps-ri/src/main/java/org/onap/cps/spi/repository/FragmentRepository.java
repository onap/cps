/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2020-2021 Nordix Foundation.
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
import javax.validation.constraints.NotNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.onap.cps.spi.entities.AnchorEntity;
import org.onap.cps.spi.entities.DataspaceEntity;
import org.onap.cps.spi.entities.FragmentEntity;
import org.onap.cps.spi.entities.FragmentEntityArranger;
import org.onap.cps.spi.entities.FragmentExtract;
import org.onap.cps.spi.exceptions.DataNodeNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FragmentRepository extends JpaRepository<FragmentEntity, Long>, FragmentRepositoryCpsPathQuery {

    Optional<FragmentEntity> findByDataspaceAndAnchorAndXpath(@NonNull DataspaceEntity dataspaceEntity,
                                                              @NonNull AnchorEntity anchorEntity,
                                                              @NonNull String xpath);

    default FragmentEntity getByDataspaceAndAnchorAndXpath(@NonNull DataspaceEntity dataspaceEntity,
                                                           @NonNull AnchorEntity anchorEntity,
                                                           @NonNull String xpath) {
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
     * find root data node of fragment by anchor.
     *
     * @param anchorEntity anchor info of root fragment
     * @return FragmentEntity fragment of root node
     */
    default FragmentEntity findFirstRootByAnchor(@NonNull AnchorEntity anchorEntity) {
        final List<FragmentExtract> fragmentExtracts = findRootsByAnchorId(anchorEntity.getId());
        if (fragmentExtracts.isEmpty()) {
            throw new DataNodeNotFoundException(anchorEntity.getName());
        }
        return FragmentEntityArranger.toFragmentEntityTree(anchorEntity, fragmentExtracts);
    }

    List<FragmentEntity> findAllByAnchorAndXpathIn(@NonNull AnchorEntity anchorEntity,
                                                   @NonNull Collection<String> xpath);

    @Modifying
    @Query("DELETE FROM FragmentEntity fe WHERE fe.anchor IN (:anchors)")
    void deleteByAnchorIn(@NotNull @Param("anchors") Collection<AnchorEntity> anchorEntities);

    @Query(value = "SELECT id, anchor_id AS anchorId, xpath, parent_id AS parentId,"
        + " CAST(attributes AS TEXT) AS attributes"
        + " FROM FRAGMENT WHERE anchor_id = :anchorId"
        + " AND ( xpath = :parentXpath OR xpath LIKE CONCAT(:parentXpath,'/%') )",
           nativeQuery = true)
    List<FragmentExtract> findByAnchorIdAndParentXpath(@Param("anchorId") int anchorId,
                                                       @Param("parentXpath") String parentXpath);
}
