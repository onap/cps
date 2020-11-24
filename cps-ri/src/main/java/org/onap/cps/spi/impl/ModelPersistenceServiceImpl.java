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
import static org.onap.cps.exceptions.CpsExceptionBuilder.invalidDataspaceException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import org.onap.cps.spi.ModelPersistenceService;
import org.onap.cps.spi.entities.Dataspace;
import org.onap.cps.spi.entities.SchemaSet;
import org.onap.cps.spi.entities.YangResource;
import org.onap.cps.spi.repository.DataspaceRepository;
import org.onap.cps.spi.repository.SchemaSetRepository;
import org.onap.cps.spi.repository.YangResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

@Component
public class ModelPersistenceServiceImpl implements ModelPersistenceService {

    @Autowired
    private YangResourceRepository yangResourceRepository;

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
    public void storeSchemaSet(final String dataspaceName, final String schemaSetName,
                               final Set<String> yangResources) {

        final Dataspace dataspace = dataspaceRepository.findByName(dataspaceName)
            .orElseThrow(() -> invalidDataspaceException(dataspaceName));

        final Set<YangResource> yangResourceEntities = processYangResources(yangResources);
        final SchemaSet schemaSet = new SchemaSet();
        schemaSet.setName(schemaSetName);
        schemaSet.setDataspace(dataspace);
        schemaSet.setYangResources(yangResourceEntities);
        try {
            schemaSetRepository.save(schemaSet);
        } catch (DataIntegrityViolationException e) {
            throw duplicateSchemaSetException(dataspaceName, schemaSetName);
        }
    }

    private Set<YangResource> processYangResources(final Set<String> yangResources) {
        final Map<String, String> checksumToContentMap = yangResources.stream()
            .collect(Collectors.toMap(
                content -> DigestUtils.md5DigestAsHex(content.getBytes()),
                content -> content)
            );

        final List<YangResource> existingYangFiles =
            yangResourceRepository.findAllByChecksumIn(checksumToContentMap.keySet());
        existingYangFiles.forEach(yangFile -> checksumToContentMap.remove(yangFile.getChecksum()));

        final List<YangResource> newYangFiles = checksumToContentMap.entrySet().stream()
            .map(entry -> {
                final YangResource entity = new YangResource();
                entity.setChecksum(entry.getKey());
                entity.setContent(entry.getValue());
                return entity;
            }).collect(Collectors.toList());
        if (!newYangFiles.isEmpty()) {
            yangResourceRepository.saveAll(newYangFiles);
        }

        final Set<YangResource> result = new HashSet<>(yangResources.size());
        result.addAll(existingYangFiles);
        result.addAll(newYangFiles);
        return result;
    }

}
