/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2020-2024 Nordix Foundation.
 * Modifications Copyright (C) 2020-2022 Bell Canada.
 * Modifications Copyright (C) 2021 Pantheon.tech
 * Modifications Copyright (C) 2022 TechMahindra Ltd.
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

package org.onap.cps.ri;

import jakarta.transaction.Transactional;
import java.util.Collection;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ri.models.AnchorEntity;
import org.onap.cps.ri.models.DataspaceEntity;
import org.onap.cps.ri.models.SchemaSetEntity;
import org.onap.cps.ri.repository.AnchorRepository;
import org.onap.cps.ri.repository.DataspaceRepository;
import org.onap.cps.ri.repository.SchemaSetRepository;
import org.onap.cps.spi.CpsAdminPersistenceService;
import org.onap.cps.spi.api.exceptions.AlreadyDefinedException;
import org.onap.cps.spi.api.exceptions.DataspaceInUseException;
import org.onap.cps.spi.api.model.Anchor;
import org.onap.cps.spi.api.model.Dataspace;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CpsAdminPersistenceServiceImpl implements CpsAdminPersistenceService {

    private final DataspaceRepository dataspaceRepository;
    private final AnchorRepository anchorRepository;
    private final SchemaSetRepository schemaSetRepository;

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
    public Dataspace getDataspace(final String dataspaceName) {
        final DataspaceEntity dataspaceEntity =  dataspaceRepository.getByName(dataspaceName);
        return toDataspace(dataspaceEntity);
    }

    @Override
    public Collection<Dataspace> getAllDataspaces() {
        final Collection<DataspaceEntity> dataspaceEntities = dataspaceRepository.findAll();
        return dataspaceEntities.stream().map(CpsAdminPersistenceServiceImpl::toDataspace)
                .collect(Collectors.toSet());
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
    public Anchor getAnchor(final String dataspaceName, final String anchorName) {
        final AnchorEntity anchorEntity = getAnchorEntity(dataspaceName, anchorName);
        return toAnchor(anchorEntity);
    }

    @Override
    public Collection<Anchor> getAnchors(final String dataspaceName) {
        final var dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final Collection<AnchorEntity> anchorEntities = anchorRepository.findAllByDataspace(dataspaceEntity);
        return anchorEntities.stream().map(CpsAdminPersistenceServiceImpl::toAnchor).collect(Collectors.toSet());
    }

    @Override
    public Collection<Anchor> getAnchors(final String dataspaceName, final Collection<String> anchorNames) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        return anchorRepository.findAllByDataspaceAndNameIn(dataspaceEntity, anchorNames)
                .stream().map(CpsAdminPersistenceServiceImpl::toAnchor).collect(Collectors.toSet());
    }

    @Override
    public Collection<Anchor> getAnchorsBySchemaSetName(final String dataspaceName, final String schemaSetName) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final SchemaSetEntity schemaSetEntity = schemaSetRepository.getByDataspaceAndName(
            dataspaceEntity, schemaSetName);
        return anchorRepository.findAllBySchemaSet(schemaSetEntity)
            .stream().map(CpsAdminPersistenceServiceImpl::toAnchor)
            .collect(Collectors.toSet());
    }

    @Override
    public Collection<Anchor> getAnchorsBySchemaSetNames(final String dataspaceName,
                                                         final Collection<String> schemaSetNames) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        return anchorRepository.findAllByDataspaceAndSchemaSetNameIn(dataspaceEntity, schemaSetNames)
            .stream().map(CpsAdminPersistenceServiceImpl::toAnchor).collect(Collectors.toSet());
    }

    @Override
    public Collection<String> queryAnchorNames(final String dataspaceName, final Collection<String> inputModuleNames) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        return anchorRepository.getAnchorNamesByDataspaceIdAndModuleNames(dataspaceEntity.getId(), inputModuleNames,
                inputModuleNames.size());
    }

    @Transactional
    @Override
    public void deleteAnchor(final String dataspaceName, final String anchorName) {
        final var anchorEntity = getAnchorEntity(dataspaceName, anchorName);
        anchorRepository.delete(anchorEntity);
    }

    @Transactional
    @Override
    public void deleteAnchors(final String dataspaceName, final Collection<String> anchorNames) {
        final var dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        anchorRepository.deleteAllByDataspaceAndNameIn(dataspaceEntity, anchorNames);
    }

    @Transactional
    @Override
    public void updateAnchorSchemaSet(final String dataspaceName,
                                         final String anchorName,
                                         final String schemaSetName) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final AnchorEntity anchorEntity = anchorRepository.getByDataspaceAndName(dataspaceEntity, anchorName);
        final SchemaSetEntity schemaSetEntity = schemaSetRepository
                .getByDataspaceAndName(dataspaceEntity, schemaSetName);
        anchorRepository.updateAnchorSchemaSetId(schemaSetEntity.getId(), anchorEntity.getId());
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

    private static Dataspace toDataspace(final DataspaceEntity dataspaceEntity) {
        return Dataspace.builder().name(dataspaceEntity.getName()).build();
    }
}
