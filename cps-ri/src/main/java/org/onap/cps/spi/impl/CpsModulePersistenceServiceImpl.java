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
import org.onap.cps.spi.exceptions.SchemaSetAlreadyDefinedException;
import org.onap.cps.spi.repository.DataspaceRepository;
import org.onap.cps.spi.repository.SchemaSetRepository;
import org.onap.cps.spi.repository.YangResourceRepository;
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
    @Transactional
    public void storeSchemaSet(final String dataspaceName, final String schemaSetName,
        final Map<String, String> yangResourcesNameToContentMap) {

        final Dataspace dataspace = dataspaceRepository.getByName(dataspaceName);
        final Set<YangResource> yangResources = synchronizeYangResources(yangResourcesNameToContentMap);
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

    private Set<YangResource> synchronizeYangResources(final Map<String, String> yangResourcesNameToContentMap) {
        final Map<String, YangResource> checksumToEntityMap = yangResourcesNameToContentMap.entrySet().stream()
            .map(entry -> {
                final YangResource yangResource = new YangResource();
                yangResource.setName(entry.getKey());
                yangResource.setContent(entry.getValue());
                yangResource.setChecksum(DigestUtils.md5DigestAsHex(entry.getValue().getBytes()));
                return yangResource;
            })
            .collect(Collectors.toMap(
                YangResource::getChecksum,
                entity -> entity
            ));

        final List<YangResource> existingYangResources =
            yangResourceRepository.findAllByChecksumIn(checksumToEntityMap.keySet());
        existingYangResources.forEach(yangFile -> checksumToEntityMap.remove(yangFile.getChecksum()));

        final Collection<YangResource> newYangResources = checksumToEntityMap.values();
        if (!newYangResources.isEmpty()) {
            yangResourceRepository.saveAll(newYangResources);
        }

        return ImmutableSet.<YangResource>builder()
            .addAll(existingYangResources)
            .addAll(newYangResources)
            .build();
    }

    @Override
    public Map<String, String> getYangSchemaResources(final String dataspaceName, final String schemaSetName) {
        final Dataspace dataspace = dataspaceRepository.getByName(dataspaceName);
        final SchemaSet schemaSet = schemaSetRepository.getByDataspaceAndName(dataspace, schemaSetName);
        return schemaSet.getYangResources().stream().collect(
            Collectors.toMap(YangResource::getName, YangResource::getContent));
    }
}
