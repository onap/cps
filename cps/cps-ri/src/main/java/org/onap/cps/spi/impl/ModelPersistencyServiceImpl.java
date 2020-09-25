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

import org.onap.cps.spi.ModelPersistencyService;
import org.onap.cps.spi.entities.ModuleEntity;
import org.onap.cps.spi.repository.ModuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ModelPersistencyServiceImpl implements ModelPersistencyService {


    private final ModuleRepository moduleRepository;

    @Autowired
    public ModelPersistencyServiceImpl(final ModuleRepository moduleRepository) {
        this.moduleRepository = moduleRepository;
    }

    @Override
    public void storeModule(final String name, final String moduleContent, final String revision) {
        final ModuleEntity moduleEntity = new ModuleEntity(name, moduleContent, revision);
        moduleRepository.save(moduleEntity);

    }
}
