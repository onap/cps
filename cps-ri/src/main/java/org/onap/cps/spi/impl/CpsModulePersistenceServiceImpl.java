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
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import org.onap.cps.spi.CpsModulePersistenceService;
import org.onap.cps.spi.entities.Dataspace;
import org.onap.cps.spi.entities.SchemaSet;
import org.onap.cps.spi.entities.YangResource;
import org.onap.cps.spi.exceptions.CpsException;
import org.onap.cps.spi.exceptions.ModelValidationException;
import org.onap.cps.spi.exceptions.SchemaSetAlreadyDefinedException;
import org.onap.cps.spi.model.ModuleRef;
import org.onap.cps.spi.repository.DataspaceRepository;
import org.onap.cps.spi.repository.SchemaSetRepository;
import org.onap.cps.spi.repository.YangResourceRepository;
import org.onap.cps.utils.YangUtils;
import org.onap.cps.yang.YangTextSchemaSourceSet;
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder;
import org.opendaylight.yangtools.yang.model.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

@Component
public class CpsModulePersistenceServiceImpl implements CpsModulePersistenceService {

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
        // Modules to be processed within schema sets only.
    }

    @Override
    @Transactional
    public void storeSchemaSet(final String dataspaceName, final String schemaSetName,
                               final Set<String> yangResourcesAsStrings) {

        final Dataspace dataspace = dataspaceRepository.getByName(dataspaceName);
        final Set<YangResource> yangResources = synchronizeYangResources(yangResourcesAsStrings);
        final SchemaSet schemaSet = new SchemaSet();
        schemaSet.setName(schemaSetName);
        schemaSet.setDataspace(dataspace);
        schemaSet.setYangResources(yangResources);
        try {
            schemaSetRepository.save(schemaSet);
        } catch (final DataIntegrityViolationException e) {
            throw new SchemaSetAlreadyDefinedException(dataspaceName, schemaSetName, e);
        }
    }

    private Set<YangResource> synchronizeYangResources(final Set<String> yangResourcesAsStrings) {
        final Map<String, String> checksumToContentMap = yangResourcesAsStrings.stream()
            .collect(Collectors.toMap(
                content -> DigestUtils.md5DigestAsHex(content.getBytes()),
                content -> content)
            );

        final List<YangResource> existingYangResources =
            yangResourceRepository.findAllByChecksumIn(checksumToContentMap.keySet());
        existingYangResources.forEach(yangFile -> checksumToContentMap.remove(yangFile.getChecksum()));

        final List<YangResource> newYangResources = checksumToContentMap.entrySet().stream()
            .map(entry -> {
                final YangResource yangResource = new YangResource();
                yangResource.setChecksum(entry.getKey());
                yangResource.setContent(entry.getValue());
                return yangResource;
            }).collect(Collectors.toList());
        if (!newYangResources.isEmpty()) {
            yangResourceRepository.saveAll(newYangResources);
        }

        return ImmutableSet.<YangResource>builder()
            .addAll(existingYangResources)
            .addAll(newYangResources)
            .build();
    }

    @Override
    public Collection<ModuleRef> getModuleReferences(final String namespace, final String schemaSetName) {
        final Dataspace dataspace = dataspaceRepository.getByName(namespace);
        final SchemaSet schemas = schemaSetRepository.getByDataspaceAndName(dataspace, schemaSetName);
        final Map<String, String> yangResources = schemas.getYangResources().stream().collect(
                Collectors.toMap(YangResource::getName, YangResource::getContent));
        try {
            final YangTextSchemaSourceSet schemaSourceSet = YangTextSchemaSourceSetBuilder.of(yangResources);
            return schemaSourceSet.getModules();
        } catch (final ReactorException | YangSyntaxErrorException e) {
            throw new ModelValidationException("Yang file validation failed", e.getMessage(), e);
        } catch (final IOException e) {
            throw new CpsException(e);
        }
    }
}
