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

import com.google.common.collect.ImmutableSet;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import org.apache.commons.codec.digest.DigestUtils;
import org.onap.cps.spi.CascadeDeleteAllowed;
import org.onap.cps.spi.CpsAdminPersistenceService;
import org.onap.cps.spi.CpsModulePersistenceService;
import org.onap.cps.spi.entities.AnchorEntity;
import org.onap.cps.spi.entities.DataspaceEntity;
import org.onap.cps.spi.entities.SchemaSetEntity;
import org.onap.cps.spi.entities.YangResourceEntity;
import org.onap.cps.spi.exceptions.AlreadyDefinedException;
import org.onap.cps.spi.exceptions.SchemaSetInUseException;
import org.onap.cps.spi.model.Anchor;
import org.onap.cps.spi.repository.AnchorRepository;
import org.onap.cps.spi.repository.DataspaceRepository;
import org.onap.cps.spi.repository.FragmentRepository;
import org.onap.cps.spi.repository.SchemaSetRepository;
import org.onap.cps.spi.repository.YangResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;


@Component
public class CpsModulePersistenceServiceImpl implements CpsModulePersistenceService {

    @Autowired
    private YangResourceRepository yangResourceRepository;

    @Autowired
    private SchemaSetRepository schemaSetRepository;

    @Autowired
    private DataspaceRepository dataspaceRepository;

    @Autowired
    private AnchorRepository anchorRepository;

    @Autowired
    private FragmentRepository fragmentRepository;

    @Autowired
    private CpsAdminPersistenceService cpsAdminPersistenceService;

    @Override
    @Transactional
    public void storeSchemaSet(final String dataspaceName, final String schemaSetName,
        final Map<String, String> yangResourcesNameToContentMap) {

        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final Set<YangResourceEntity> yangResourceEntities = synchronizeYangResources(yangResourcesNameToContentMap);
        final SchemaSetEntity schemaSetEntity = new SchemaSetEntity();
        schemaSetEntity.setName(schemaSetName);
        schemaSetEntity.setDataspace(dataspaceEntity);
        schemaSetEntity.setYangResources(yangResourceEntities);
        try {
            schemaSetRepository.save(schemaSetEntity);
        } catch (final DataIntegrityViolationException e) {
            throw new AlreadyDefinedException("Schema Set", schemaSetName, dataspaceName, e);
        }
    }

    private Set<YangResourceEntity> synchronizeYangResources(final Map<String, String> yangResourcesNameToContentMap) {
        final Map<String, YangResourceEntity> checksumToEntityMap = yangResourcesNameToContentMap.entrySet().stream()
            .map(entry -> {
                final String checksum = DigestUtils.sha256Hex(entry.getValue().getBytes(StandardCharsets.UTF_8));
                final YangResourceEntity yangResourceEntity = new YangResourceEntity();
                yangResourceEntity.setName(entry.getKey());
                yangResourceEntity.setContent(entry.getValue());
                yangResourceEntity.setChecksum(checksum);
                return yangResourceEntity;
            })
            .collect(Collectors.toMap(
                YangResourceEntity::getChecksum,
                entity -> entity
            ));

        final List<YangResourceEntity> existingYangResourceEntities =
            yangResourceRepository.findAllByChecksumIn(checksumToEntityMap.keySet());
        existingYangResourceEntities.forEach(yangFile -> checksumToEntityMap.remove(yangFile.getChecksum()));

        final Collection<YangResourceEntity> newYangResourceEntities = checksumToEntityMap.values();
        if (!newYangResourceEntities.isEmpty()) {
            yangResourceRepository.saveAll(newYangResourceEntities);
        }

        return ImmutableSet.<YangResourceEntity>builder()
            .addAll(existingYangResourceEntities)
            .addAll(newYangResourceEntities)
            .build();
    }

    @Override
    public Map<String, String> getYangSchemaResources(final String dataspaceName, final String schemaSetName) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final SchemaSetEntity schemaSetEntity =
            schemaSetRepository.getByDataspaceAndName(dataspaceEntity, schemaSetName);
        return schemaSetEntity.getYangResources().stream().collect(
            Collectors.toMap(YangResourceEntity::getName, YangResourceEntity::getContent));
    }

    @Override
    public Map<String, String> getYangSchemaSetResources(final String dataspaceName, final String anchorName) {
        final Anchor anchor = cpsAdminPersistenceService.getAnchor(dataspaceName, anchorName);
        return getYangSchemaResources(dataspaceName, anchor.getSchemaSetName());
    }

    @Override
    @Transactional
    public void deleteSchemaSet(final String dataspaceName, final String schemaSetName,
        final CascadeDeleteAllowed cascadeDeleteAllowed) {
        final DataspaceEntity dataspaceEntity = dataspaceRepository.getByName(dataspaceName);
        final SchemaSetEntity schemaSetEntity =
            schemaSetRepository.getByDataspaceAndName(dataspaceEntity, schemaSetName);

        final Collection<AnchorEntity> anchorEntities = anchorRepository.findAllBySchemaSet(schemaSetEntity);
        if (!anchorEntities.isEmpty()) {
            if (cascadeDeleteAllowed != CascadeDeleteAllowed.CASCADE_DELETE_ALLOWED) {
                throw new SchemaSetInUseException(dataspaceName, schemaSetName);
            }
            fragmentRepository.deleteByAnchorIn(anchorEntities);
            anchorRepository.deleteAll(anchorEntities);
        }
        schemaSetRepository.delete(schemaSetEntity);
        yangResourceRepository.deleteOrphans();
    }
}
