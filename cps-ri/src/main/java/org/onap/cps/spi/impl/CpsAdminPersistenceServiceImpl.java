/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2020-2022 Nordix Foundation.
 * Modifications Copyright (C) 2020-2022 Bell Canada.
 * Modifications Copyright (C) 2021 Pantheon.tech
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.spi.impl;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.onap.cps.spi.CpsAdminPersistenceService;
import org.onap.cps.spi.entities.AnchorEntity;
import org.onap.cps.spi.entities.DataspaceEntity;
import org.onap.cps.spi.entities.SchemaSetEntity;
import org.onap.cps.spi.entities.YangResourceModuleReference;
import org.onap.cps.spi.exceptions.AlreadyDefinedException;
import org.onap.cps.spi.exceptions.DataspaceInUseException;
import org.onap.cps.spi.exceptions.ModuleNamesNotFoundException;
import org.onap.cps.spi.model.Anchor;
import org.onap.cps.spi.model.CmHandleQueryParameters;
import org.onap.cps.spi.repository.AnchorRepository;
import org.onap.cps.spi.repository.DataspaceRepository;
import org.onap.cps.spi.repository.ModuleReferenceRepository;
import org.onap.cps.spi.repository.SchemaSetRepository;
import org.onap.cps.spi.repository.YangResourceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CpsAdminPersistenceServiceImpl implements CpsAdminPersistenceService {

    private final DataspaceRepository dataspaceRepository;
    private final AnchorRepository anchorRepository;
    private final SchemaSetRepository schemaSetRepository;
    private final YangResourceRepository yangResourceRepository;
    private final ModuleReferenceRepository moduleReferenceRepository;

    @Override
    public void createDataspace(final String dataspaceName) {
        try {
            dataspaceRepository.save(new DataspaceEntity(dataspaceName));
        } catch (final DataIntegrityViolationException e) {
            throw AlreadyDefinedException.forDataspace(dataspaceName, e);
        }
    }

    @Override
    public void deleteDataspace(final String dataspaceName) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final int numberOfAssociatedAnchors = anchorRepository.countByDataspace(dataspaceEntity);
        if (numberOfAssociatedAnchors != 0) {
            throw new DataspaceInUseException(dataspaceName,
                String.format("Dataspace contains %d anchor(s)", numberOfAssociatedAnchors));
        }
        final int numberOfAssociatedSchemaSets = schemaSetRepository.countByDataspace(dataspaceEntity);
        if (numberOfAssociatedSchemaSets != 0) {
            throw new DataspaceInUseException(dataspaceName,
                String.format("Dataspace contains %d schemaset(s)", numberOfAssociatedSchemaSets));
        }
        dataspaceRepository.delete(dataspaceEntity);
    }

    @Override
    public void createAnchor(final String dataspaceName, final String schemaSetName, final String anchorName) {
        final var dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final var schemaSetEntity =
            schemaSetRepository.getByDataspaceAndName(dataspaceEntity, schemaSetName);
        final var anchorEntity = AnchorEntity.builder()
            .name(anchorName)
            .dataspace(dataspaceEntity)
            .schemaSet(schemaSetEntity)
            .build();
        try {
            anchorRepository.save(anchorEntity);
        } catch (final DataIntegrityViolationException e) {
            throw AlreadyDefinedException.forAnchor(anchorName, dataspaceName, e);
        }
    }

    @Override
    public Collection<Anchor> getAnchors(final String dataspaceName) {
        final var dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final Collection<AnchorEntity> anchorEntities = anchorRepository.findAllByDataspace(dataspaceEntity);
        return anchorEntities.stream().map(CpsAdminPersistenceServiceImpl::toAnchor).collect(Collectors.toSet());
    }

    @Override
    public Collection<Anchor> getAnchors(final String dataspaceName, final String schemaSetName) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final SchemaSetEntity schemaSetEntity = schemaSetRepository.getByDataspaceAndName(
            dataspaceEntity, schemaSetName);
        return anchorRepository.findAllBySchemaSet(schemaSetEntity)
            .stream().map(CpsAdminPersistenceServiceImpl::toAnchor)
            .collect(Collectors.toSet());
    }

    @Override
    public Collection<Anchor> queryAnchors(final String dataspaceName, final Collection<String> inputModuleNames) {
        validateDataspaceAndModuleNames(dataspaceName, inputModuleNames);
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final Collection<AnchorEntity> anchorEntities = anchorRepository
            .getAnchorsByDataspaceIdAndModuleNames(dataspaceEntity.getId(), inputModuleNames, inputModuleNames.size());
        return anchorEntities.stream().map(CpsAdminPersistenceServiceImpl::toAnchor).collect(Collectors.toSet());
    }

    @Override
    public Anchor getAnchor(final String dataspaceName, final String anchorName) {
        return toAnchor(getAnchorEntity(dataspaceName, anchorName));
    }

    @Transactional
    @Override
    public void deleteAnchor(final String dataspaceName, final String anchorName) {
        final var anchorEntity = getAnchorEntity(dataspaceName, anchorName);
        anchorRepository.delete(anchorEntity);
    }

    @Override
    public Set<String> queryCmHandles(final CmHandleQueryParameters cmHandleQueryParameters) {
        return moduleReferenceRepository.queryCmHandles(cmHandleQueryParameters);
    }

    private AnchorEntity getAnchorEntity(final String dataspaceName, final String anchorName) {
        final var dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        return anchorRepository.getByDataspaceAndName(dataspaceEntity, anchorName);
    }

    private static Anchor toAnchor(final AnchorEntity anchorEntity) {
        return Anchor.builder()
            .name(anchorEntity.getName())
            .dataspaceName(anchorEntity.getDataspace().getName())
            .schemaSetName(anchorEntity.getSchemaSet().getName())
            .build();
    }

    private void validateDataspaceAndModuleNames(final String dataspaceName,
        final Collection<String> inputModuleNames) {
        final Collection<String> retrievedModuleReferences =
            yangResourceRepository.findAllModuleReferencesByDataspaceAndModuleNames(dataspaceName, inputModuleNames)
                .stream().map(YangResourceModuleReference::getModuleName)
                .collect(Collectors.toList());
        if (retrievedModuleReferences.isEmpty()) {
            verifyDataspaceName(dataspaceName);
        }
        if (inputModuleNames.size() > retrievedModuleReferences.size()) {
            final List<String> moduleNamesNotFound = inputModuleNames.stream()
                .filter(moduleName -> !retrievedModuleReferences.contains(moduleName))
                .collect(Collectors.toList());
            if (!moduleNamesNotFound.isEmpty()) {
                throw new ModuleNamesNotFoundException(dataspaceName, moduleNamesNotFound);
            }
        }
    }

    private void verifyDataspaceName(final String dataspaceName) {
        dataspaceRepository.getByName(dataspaceName);
    }
}
