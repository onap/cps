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

import static org.onap.cps.exceptions.CpsExceptionBuilder.duplicateSchemaSetException;
import static org.onap.cps.exceptions.CpsExceptionBuilder.emptySchemaSetException;
import static org.onap.cps.exceptions.CpsExceptionBuilder.invalidDataspaceException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import org.onap.cps.spi.model.SchemaSet;
import org.onap.cps.spi.model.YangFile;
import org.onap.cps.spi.ModelPersistenceService;
import org.onap.cps.spi.entities.Dataspace;
import org.onap.cps.spi.entities.SchemaSetEntity;
import org.onap.cps.spi.entities.YangFileEntity;
import org.onap.cps.spi.repository.DataspaceRepository;
import org.onap.cps.spi.repository.SchemaSetRepository;
import org.onap.cps.spi.repository.YangFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

@Component
public class ModelPersistenceServiceImpl implements ModelPersistenceService {

    @Autowired
    private YangFileRepository yangFileRepository;

    @Autowired
    private SchemaSetRepository schemaSetRepository;

    @Autowired
    private DataspaceRepository dataspaceRepository;

    @Override
    public void storeModule(final String namespace, final String moduleContent, final String revision,
                            final String dataspaceName) {
        // TODO this method should be removed as obsolete.
        // Modules to be processed within modulesets only.
    }

    @Override
    @Transactional
    public void storeSchemaSet(final SchemaSet schemaSet) {
        final Dataspace dataspace = dataspaceRepository.findByName(schemaSet.getDataspace())
            .orElseThrow(() -> invalidDataspaceException(schemaSet.getDataspace()));
        if (schemaSet.getYangFiles() == null || schemaSet.getYangFiles().isEmpty()) {
            throw emptySchemaSetException();
        }
        final Set<YangFileEntity> yangFiles = persistYangFiles(schemaSet.getYangFiles());
        final SchemaSetEntity schemaSetEntity = new SchemaSetEntity();
        schemaSetEntity.setName(schemaSet.getName());
        schemaSetEntity.setDataspace(dataspace);
        schemaSetEntity.setYangFiles(yangFiles);
        try {
            schemaSetRepository.save(schemaSetEntity);
        } catch (DataIntegrityViolationException e) {
            throw duplicateSchemaSetException(schemaSet.getDataspace(), schemaSet.getName());
        }
    }

    @Override
    public void deleteSchemaSet(final String dataspaceName, final String moduleSetName) {
        // not implemented yet
    }

    @Override
    public List<SchemaSet> getAllSchemaSets(final String dataspaceName) {
        // not implemented yet
        return null;
    }

    @Override
    public SchemaSet getSchemaSet(final String dataspaceName, final String moduleSetName) {
        // not implemented yet
        return null;
    }

    private Set<YangFileEntity> persistYangFiles(final Set<YangFile> yangFiles) {
        final Map<String, YangFile> checksumMap = yangFiles.stream().collect(Collectors.toMap(
            yangFile -> DigestUtils.md5DigestAsHex(yangFile.getContent().getBytes()),
            yangFile -> yangFile));

        final List<YangFileEntity> existingYangFiles = yangFileRepository.findAllByChecksumIn(checksumMap.keySet());
        existingYangFiles.forEach(yangFile -> checksumMap.remove(yangFile.getChecksum()));

        final List<YangFileEntity> newYangFiles = checksumMap.entrySet().stream().map(entry -> {
            final YangFileEntity entity = new YangFileEntity();
            entity.setName(entry.getValue().getName());
            entity.setContent(entry.getValue().getContent());
            entity.setChecksum(entry.getKey());
            return entity;
        }).collect(Collectors.toList());
        yangFileRepository.saveAll(newYangFiles);

        final Set<YangFileEntity> result = new HashSet<>();
        result.addAll(existingYangFiles);
        result.addAll(newYangFiles);
        return result;
    }

}
