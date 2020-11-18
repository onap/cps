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
import org.onap.cps.spi.ModelPersistencyService;
import org.onap.cps.spi.entities.Dataspace;
import org.onap.cps.spi.entities.ModuleEntity;
import org.onap.cps.spi.repository.DataspaceRepository;
import org.onap.cps.spi.repository.ModuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class ModelPersistencyServiceImpl implements ModelPersistencyService {


    private final ModuleRepository moduleRepository;

    private final DataspaceRepository dataspaceRepository;

    @Autowired
    public ModelPersistencyServiceImpl(final ModuleRepository moduleRepository,
        final DataspaceRepository dataspaceRepository) {
        this.moduleRepository = moduleRepository;
        this.dataspaceRepository = dataspaceRepository;
    }

    @Override
    public void storeModule(final String namespace, final String moduleContent, final String revision,
        final String dataspaceName) {
        try {
            final Dataspace dataspace = new Dataspace(dataspaceName);
            if (Boolean.FALSE.equals(dataspaceRepository.existsByName(dataspaceName))) {
                dataspaceRepository.save(dataspace);
            }
            dataspace.setId(dataspaceRepository.findByName(dataspaceName).getId());
            final ModuleEntity moduleEntity = new ModuleEntity(namespace, moduleContent, revision, dataspace);
            moduleRepository.save(moduleEntity);
        } catch (final DataIntegrityViolationException ex) {
            throw new CpsValidationException("Duplication Error",
                String.format("Module already exist in dataspace %s.", dataspaceName));
        }
    }
}
