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

package org.onap.cps.spi;

/**
 * Defines methods to access and manipulate data using the chosen database solution.
 */
public interface DataPersistencyService {

    /**
     * Store the JSON structure in the database.
     *
     * @param jsonStructure the JSON structure.
     * @return jsonEntityID the ID of the JSON entity.
     */
    Integer storeJsonStructure(final String jsonStructure);

    /**
     * Get the JSON structure from the database using the entity identifier.
     *
     * @param jsonStructureId the json entity identifier.
     * @return a JSON Structure.
     */
    String getJsonById(int jsonStructureId);

    /**
     * Delete the JSON structure from the database using the entity identifier.
     *
     * @param jsonStructureId the json entity identifier.
     */
    void deleteJsonById(int jsonStructureId);
}