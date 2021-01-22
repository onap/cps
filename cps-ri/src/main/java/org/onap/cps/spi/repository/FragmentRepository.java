/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation. All rights reserved.
 *  Modifications Copyright (C) 2020 Bell Canada. All rights reserved.
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
import java.util.Optional;
import javax.validation.constraints.NotNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.onap.cps.spi.entities.AnchorEntity;
import org.onap.cps.spi.entities.DataspaceEntity;
import org.onap.cps.spi.entities.FragmentEntity;
import org.onap.cps.spi.exceptions.FragmentNotFoundException;
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
            .orElseThrow(() -> new FragmentNotFoundException(dataspaceEntity.getName(), xpath));
    }

    @Modifying
    @Query("DELETE FROM FragmentEntity fe WHERE fe.anchor IN (:anchors)")
    void deleteByAnchorIn(@NotNull @Param("anchors") Collection<AnchorEntity> anchorEntities);

}