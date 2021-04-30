/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020-201 Nordix Foundation. All rights reserved.
 *  Modifications Copyright (C) 2020-2021 Bell Canada. All rights reserved.
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
public interface FragmentRepository extends JpaRepository<FragmentEntity, Long> {

    Optional<FragmentEntity> findByDataspaceAndAnchorAndXpath(@NonNull DataspaceEntity dataspaceEntity,
        @NonNull AnchorEntity anchorEntity, @NonNull String xpath);

    default FragmentEntity getByDataspaceAndAnchorAndXpath(@NonNull DataspaceEntity dataspaceEntity,
        @NonNull AnchorEntity anchorEntity, @NonNull String xpath) {
        return findByDataspaceAndAnchorAndXpath(dataspaceEntity, anchorEntity, xpath)
            .orElseThrow(() -> new DataNodeNotFoundException(dataspaceEntity.getName(), anchorEntity.getName(), xpath));
    }

    Optional<FragmentEntity> findFirstByDataspaceAndAnchor(@NonNull DataspaceEntity dataspaceEntity,
        @NonNull AnchorEntity anchorEntity);

    default FragmentEntity getFirstByDataspaceAndAnchor(@NonNull DataspaceEntity dataspaceEntity,
        @NonNull AnchorEntity anchorEntity) {
        return findFirstByDataspaceAndAnchor(dataspaceEntity, anchorEntity)
            .orElseThrow(() -> new DataNodeNotFoundException(dataspaceEntity.getName(), anchorEntity.getName()));
    }

    List<FragmentEntity> findAllByAnchorAndXpathIn(@NonNull AnchorEntity anchorEntity,
        @NonNull Collection<String> xpath);

    @Modifying
    @Query("DELETE FROM FragmentEntity fe WHERE fe.anchor IN (:anchors)")
    void deleteByAnchorIn(@NotNull @Param("anchors") Collection<AnchorEntity> anchorEntities);

    @Query(value =
        "SELECT * FROM FRAGMENT WHERE (anchor_id = :anchor) AND (xpath = (:xpath) OR xpath LIKE "
            + "CONCAT(:xpath,'\\[@%]')) AND attributes @> jsonb_build_object(:leafName , :leafValue)",
        nativeQuery = true)
    // Above query will match an xpath with or without the index for a list [@key=value] and match anchor id,
    // leaf name and leaf value
    List<FragmentEntity> getByAnchorAndXpathAndLeafAttributes(@Param("anchor") int anchorId, @Param("xpath")
        String xpathPrefix, @Param("leafName") String leafName, @Param("leafValue") Object leafValue);

    @Query(value = "SELECT * FROM FRAGMENT WHERE anchor_id = :anchor AND xpath LIKE CONCAT('%/',:descendantName)",
        nativeQuery = true)
    // Above query will match the anchor id and last descendant name
    List<FragmentEntity> getByAnchorAndXpathEndsInDescendantName(@Param("anchor") int anchorId,
                                                                 @Param("descendantName") String descendantName);

    @Query(value = "SELECT * FROM FRAGMENT WHERE anchor_id = :anchor AND (xpath LIKE CONCAT('%/',:descendantName) OR "
        + "xpath LIKE CONCAT('%/', :descendantName,'\\[@%]')) AND attributes @> :leafDataAsJson\\:\\:jsonb",
        nativeQuery = true)
    // Above query will match the anchor id, last descendant name and all parameters passed into leafDataASJson with the
    // attribute values of the requested data node eg: {"leaf_name":"value", "another_leaf_name":"another value"}​​​​​​
    List<FragmentEntity> getByAnchorAndDescendentNameAndLeafValues(@Param("anchor") int anchorId,
        @Param("descendantName") String descendantName, @Param("leafDataAsJson") String leafDataAsJson);
}
