/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
 *  Modifications Copyright (C) 2020 Bell Canada. All rights reserved.
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

import org.onap.cps.exceptions.CpsValidationException;
import org.onap.cps.spi.ModelPersistenceService;
import org.onap.cps.spi.entities.DataspaceEntity;
import org.onap.cps.spi.entities.ModuleEntity;
import org.onap.cps.spi.repository.DataspaceRepository;
import org.onap.cps.spi.repository.ModuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class ModelPersistenceServiceImpl implements ModelPersistenceService {

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private DataspaceRepository dataspaceRepository;

    @Override
    public void storeModule(final String namespace, final String moduleContent, final String revision,
        final String dataspaceName) {
        final DataspaceEntity dataspaceEntity = new DataspaceEntity(dataspaceName);
        if (Boolean.FALSE.equals(dataspaceRepository.existsByName(dataspaceName))) {
            dataspaceRepository.save(dataspaceEntity);
        }
        dataspaceEntity.setId(dataspaceRepository.getByName(dataspaceName).getId());
        final ModuleEntity module = new ModuleEntity(namespace, moduleContent, revision, dataspaceEntity);
        try {
            moduleRepository.save(module);
        } catch (final DataIntegrityViolationException ex) {
            throw new CpsValidationException("Duplicate Entry",
                String.format("Module already exist in dataspace %s.", dataspaceName));
        }
    }
}
