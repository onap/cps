/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Bell Canada. All rights reserved.
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
import org.onap.cps.spi.entities.Dataspace;
import org.onap.cps.spi.exceptions.DataspaceNotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataspaceRepository extends JpaRepository<Dataspace, Integer> {
    Boolean existsByName(String name); //Checks if there are any records by name()

    Optional<Dataspace> findByName(@NotNull String name);

    /**
     * Get a dataspace by name.
     * throws a DataspaceNotFoundException if it does not exist
     *
     * @param name the name of the dataspace
     * @return the Dataspace found
     */
    default Dataspace getByName(@NotNull final String name) {
        return findByName(name).orElseThrow(() -> new DataspaceNotFoundException(name));
    }
}
