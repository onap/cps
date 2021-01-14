/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
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

package org.onap.cps.spi.repository;

import java.util.Collection;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import org.onap.cps.spi.entities.AnchorEntity;
import org.onap.cps.spi.entities.DataspaceEntity;
import org.onap.cps.spi.exceptions.AnchorNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnchorRepository extends JpaRepository<AnchorEntity, Integer> {

    Optional<AnchorEntity> findByDataspaceAndName(@NotNull DataspaceEntity dataspaceEntity, @NotNull String name);

    default AnchorEntity getByDataspaceAndName(@NotNull DataspaceEntity dataspace,
        @NotNull String anchorName) {
        return findByDataspaceAndName(dataspace, anchorName)
            .orElseThrow(() -> new AnchorNotFoundException(anchorName, dataspace.getName()));
    }

    Collection<AnchorEntity> findAllByDataspace(@NotNull DataspaceEntity dataspaceEntity);
}