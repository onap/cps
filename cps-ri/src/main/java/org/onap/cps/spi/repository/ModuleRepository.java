/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
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


import java.util.Optional;
import javax.validation.constraints.NotNull;
import org.onap.cps.exceptions.CpsNotFoundException;
import org.onap.cps.spi.entities.DataspaceEntity;
import org.onap.cps.spi.entities.ModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModuleRepository extends JpaRepository<ModuleEntity, Integer> {

    Optional<ModuleEntity> findByDataspaceAndNamespaceAndRevision(@NotNull DataspaceEntity dataspaceEntity,
        @NotNull String namespace,
        @NotNull String revision);

    /**
     * This method gets a Module by dataspace, namespace and revision.
     *
     * @param dataspaceEntity the dataspace
     * @param namespace       the namespace
     * @param revision        the revision
     * @return the Module
     * @throws CpsNotFoundException if Module not found
     */
    default ModuleEntity getByDataspaceAndNamespaceAndRevision(@NotNull DataspaceEntity dataspaceEntity,
        @NotNull String namespace,
        @NotNull String revision) {
        return findByDataspaceAndNamespaceAndRevision(dataspaceEntity, namespace,
            revision)
            .orElseThrow(() -> new CpsNotFoundException("Validation Error", String.format(
                "Module with dataspace %s, revision %s does not exist in namespace %s.",
                dataspaceEntity.getName(), revision, namespace)));
    }
}