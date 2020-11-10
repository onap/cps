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

package org.onap.cps.spi.impl;

import org.onap.cps.spi.DataPersistenceService;
import org.onap.cps.spi.entities.JsonDataEntity;
import org.onap.cps.spi.repository.DataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DataPersistenceServiceImpl implements DataPersistenceService {

    @Autowired
    private DataRepository dataRepository;

    /**
     * Method to store a JSON data structure in the database.
     *
     * @param jsonStructure the JSON data structure.
     * @return the entity identifier.
     */
    @Override
    public final Integer storeJsonStructure(final String jsonStructure) {
        final JsonDataEntity jsonDataEntity = new JsonDataEntity(jsonStructure);
        dataRepository.save(jsonDataEntity);
        return jsonDataEntity.getId();
    }

    /*
     * Return the JSON structure from the database using the object identifier.
     *
     * @param jsonStructureId the JSON object identifier.
     *
     * @return the JSON structure from the database as a string.
     */
    @Override
    public final String getJsonById(final int jsonStructureId) {
        return dataRepository.getOne(jsonStructureId).getJsonStructure();
    }

    /**
     * Delete the JSON structure from the database using the object identifier.
     *
     * @param jsonStructureId the JSON object identifier.
     */
    @Override
    public void deleteJsonById(final int jsonStructureId) {
        dataRepository.deleteById(jsonStructureId);
    }

}
