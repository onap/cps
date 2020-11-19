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
import org.onap.cps.spi.entities.Dataspace;
import org.onap.cps.spi.entities.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModuleRepository extends JpaRepository<Module, Integer> {

    Optional<Module> findByDataspaceAndNamespaceAndRevision(@NotNull Dataspace dataspace,
        @NotNull String namespace,
        @NotNull String revision);

    /**
     * This method gets a Module by dataspace, namespace and revision.
     *
     * @param dataspace the dataspace
     * @param namespace the namespace
     * @param revision the revision
     * @return the Module
     * @throws CpsNotFoundException if Module not found
     */
    default Module getByDataspaceAndNamespaceAndRevision(@NotNull Dataspace dataspace, @NotNull String namespace,
        @NotNull String revision) {
        return findByDataspaceAndNamespaceAndRevision(dataspace, namespace,
            revision)
            .orElseThrow(() -> new CpsNotFoundException("Validation Error", String.format(
                "Module with dataspace %s, revision %s does not exist in namespace %s.",
                dataspace.getName(), revision, namespace)));
    }
}