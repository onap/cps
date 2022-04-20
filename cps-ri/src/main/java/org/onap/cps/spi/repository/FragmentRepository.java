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
                                                           String xpath) {
        return findByDataspaceAndAnchorAndXpath(dataspaceEntity, anchorEntity, xpath)
            .orElseThrow(() -> new DataNodeNotFoundException(dataspaceEntity.getName(), anchorEntity.getName(), xpath));
    }

    @Query(
        value = "SELECT * FROM FRAGMENT WHERE CAST (ATTRIBUTES AS TEXT)"
            + " LIKE '%\"state\": \"ADVISED\"%' FETCH FIRST 1 ROWS ONLY",
        nativeQuery = true
    )
    FragmentEntity getCmHandleByAdvisedState();

    @Query(
        value = "SELECT * FROM FRAGMENT WHERE anchor_id = :anchor AND dataspace_id = :dataspace AND parent_id is NULL",
        nativeQuery = true)
    List<FragmentEntity> findRootsByDataspaceAndAnchor(@Param("dataspace") int dataspaceId,
                                                       @Param("anchor") int anchorId);

    default FragmentEntity findFirstRootByDataspaceAndAnchor(@NonNull DataspaceEntity dataspaceEntity,
                                                             @NonNull AnchorEntity anchorEntity) {
        return findRootsByDataspaceAndAnchor(dataspaceEntity.getId(), anchorEntity.getId()).stream().findFirst()
            .orElseThrow(() -> new DataNodeNotFoundException(dataspaceEntity.getName(), anchorEntity.getName()));
    }

    List<FragmentEntity> findAllByAnchorAndXpathIn(@NonNull AnchorEntity anchorEntity,
                                                   @NonNull Collection<String> xpath);

    @Modifying
    @Query("DELETE FROM FragmentEntity fe WHERE fe.anchor IN (:anchors)")
    void deleteByAnchorIn(@NotNull @Param("anchors") Collection<AnchorEntity> anchorEntities);
}
