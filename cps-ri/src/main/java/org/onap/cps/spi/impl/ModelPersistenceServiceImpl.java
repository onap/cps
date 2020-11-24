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

import static org.onap.cps.exceptions.CpsExceptionProvider.dataspaceNotFoundException;
import static org.onap.cps.exceptions.CpsExceptionProvider.invalidDataspaceException;
import static org.onap.cps.exceptions.CpsExceptionProvider.invalidModuleSetException;
import static org.onap.cps.exceptions.CpsExceptionProvider.moduleSetNotFoundException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.onap.cps.api.model.ModuleDescriptor;
import org.onap.cps.api.model.ModuleSetDescriptor;
import org.onap.cps.exceptions.CpsValidationException;
import org.onap.cps.spi.ModelPersistenceService;
import org.onap.cps.spi.entities.Dataspace;
import org.onap.cps.spi.entities.Module;
import org.onap.cps.spi.entities.ModuleSet;
import org.onap.cps.spi.repository.DataspaceRepository;
import org.onap.cps.spi.repository.ModuleRepository;
import org.onap.cps.spi.repository.ModuleSetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class ModelPersistenceServiceImpl implements ModelPersistenceService {

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private ModuleSetRepository moduleSetRepository;

    @Autowired
    private DataspaceRepository dataspaceRepository;

    @Override
    public void storeModule(final String namespace, final String moduleContent, final String revision,
        final String dataspaceName) {
        final Dataspace dataspace = new Dataspace(dataspaceName);
        if (Boolean.FALSE.equals(dataspaceRepository.existsByName(dataspaceName))) {
            dataspaceRepository.save(dataspace);
        }
        dataspace.setId(dataspaceRepository.getByName(dataspaceName).getId());
        final Module module = new Module(namespace, moduleContent, revision, dataspace);
        try {
            moduleRepository.save(module);
        } catch (final DataIntegrityViolationException ex) {
            throw new CpsValidationException("Duplicate Entry",
                String.format("Module already exist in dataspace %s.", dataspaceName));
        }
    }

    @Override
    public void storeModuleSet(ModuleSetDescriptor modulesetDescriptor) {


    }

    @Override
    public void deleteModuleSet(String dataspaceName, String moduleSetName) {
        Dataspace dataspace = dataspaceRepository.findByName(dataspaceName)
            .orElseThrow(() -> invalidDataspaceException(dataspaceName));
        ModuleSet moduleSet = moduleSetRepository.findByDataspaceAndName(dataspace, moduleSetName)
            .orElseThrow(() -> invalidModuleSetException(dataspaceName, moduleSetName));
        moduleSetRepository.delete(moduleSet);
    }

    @Override
    public List<ModuleSetDescriptor> getAllModuleSets(String dataspaceName) {
        Dataspace dataspace = dataspaceRepository.findByName(dataspaceName)
            .orElseThrow(() -> dataspaceNotFoundException(dataspaceName));
        List<ModuleSet> moduleSets = moduleSetRepository.findAllByDataspace(dataspace);

        return moduleSets.stream().map(
            moduleSet -> ModuleSetDescriptor.builder()
                .dataspace(dataspaceName).name(moduleSet.getName()).build()
        ).collect(Collectors.toList());
    }

    @Override
    public ModuleSetDescriptor getModuleSet(String dataspaceName, String moduleSetName) {
        Dataspace dataspace = dataspaceRepository.findByName(dataspaceName)
            .orElseThrow(() -> dataspaceNotFoundException(dataspaceName));
        ModuleSet moduleSet = moduleSetRepository.findByDataspaceAndName(dataspace, moduleSetName)
            .orElseThrow(() -> moduleSetNotFoundException(dataspaceName, moduleSetName));

        Set<ModuleDescriptor> modules = moduleSet.getModules().stream()
            .map(
                module -> ModuleDescriptor.builder()
                    .dataspace(module.getDataspace().getName())
                    .moduleset(module.getModuleSet().getName())
                    .namespace(module.getNamespace())
                    .revision(module.getRevision())
                    .content(module.getModuleContent()).build()
            ).collect(Collectors.toSet());

        return ModuleSetDescriptor.builder()
            .dataspace(moduleSet.getDataspace().getName())
            .name(moduleSet.getName())
            .modules(modules).build();
    }
}
