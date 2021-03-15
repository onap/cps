/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation. All rights reserved.
 *  Modifications Copyright (C) 2020 Bell Canada. All rights reserved.
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
import java.util.stream.Collectors;
import org.onap.cps.spi.CpsAdminPersistenceService;
import org.onap.cps.spi.entities.AnchorEntity;
import org.onap.cps.spi.entities.DataspaceEntity;
import org.onap.cps.spi.entities.SchemaSetEntity;
import org.onap.cps.spi.exceptions.AlreadyDefinedException;
import org.onap.cps.spi.model.Anchor;
import org.onap.cps.spi.repository.AnchorRepository;
import org.onap.cps.spi.repository.DataspaceRepository;
import org.onap.cps.spi.repository.SchemaSetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class CpsAdminPersistenceServiceImpl implements CpsAdminPersistenceService {

    @Autowired
    private DataspaceRepository dataspaceRepository;

    @Autowired
    private AnchorRepository anchorRepository;

    @Autowired
    private SchemaSetRepository schemaSetRepository;

    @Override
    public void createDataspace(final String dataspaceName) {
        try {
            dataspaceRepository.save(new DataspaceEntity(dataspaceName));
        } catch (final DataIntegrityViolationException e) {
            throw new AlreadyDefinedException("Dataspace", dataspaceName, e);
        }
    }

    @Override
    public void createAnchor(final String dataspaceName, final String schemaSetName, final String anchorName) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final SchemaSetEntity schemaSetEntity =
            schemaSetRepository.getByDataspaceAndName(dataspaceEntity, schemaSetName);
        final AnchorEntity anchorEntity = AnchorEntity.builder()
            .name(anchorName)
            .dataspace(dataspaceEntity)
            .schemaSet(schemaSetEntity)
            .build();
        try {
            anchorRepository.save(anchorEntity);
        } catch (final DataIntegrityViolationException e) {
            throw new AlreadyDefinedException("Anchor", anchorName, dataspaceName, e);
        }
    }

    @Override
    public Collection<Anchor> getAnchors(final String dataspaceName) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final Collection<AnchorEntity> anchorEntities = anchorRepository.findAllByDataspace(dataspaceEntity);
        return anchorEntities.stream().map(CpsAdminPersistenceServiceImpl::toAnchor).collect(Collectors.toList());
    }

    @Override
    public Anchor getAnchor(final String dataspaceName, final String anchorName) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final AnchorEntity anchorEntity =
            anchorRepository.getByDataspaceAndName(dataspaceEntity, anchorName);
        return toAnchor(anchorEntity);
    }

    private static Anchor toAnchor(final AnchorEntity anchorEntity) {
        return Anchor.builder()
            .name(anchorEntity.getName())
            .dataspaceName(anchorEntity.getDataspace().getName())
            .schemaSetName(anchorEntity.getSchemaSet().getName())
            .build();
    }
}