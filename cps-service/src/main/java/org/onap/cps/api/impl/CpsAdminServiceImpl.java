/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2023 Nordix Foundation
 *  Modifications Copyright (C) 2020-2022 Bell Canada.
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2022 TechMahindra Ltd.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.api.impl;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.spi.CpsAdminPersistenceService;
import org.onap.cps.spi.model.Anchor;
import org.onap.cps.spi.model.Dataspace;
import org.onap.cps.spi.utils.CpsValidator;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component("CpsAdminServiceImpl")
@RequiredArgsConstructor(onConstructor = @__(@Lazy))
public class CpsAdminServiceImpl implements CpsAdminService {

    private final CpsAdminPersistenceService cpsAdminPersistenceService;
    @Lazy
    private final CpsDataService cpsDataService;
    private final CpsValidator cpsValidator;

    @Override
    public void createDataspace(final String dataspaceName) {
        cpsValidator.validateNameCharacters(dataspaceName);
        cpsAdminPersistenceService.createDataspace(dataspaceName);
    }

    @Override
    public void deleteDataspace(final String dataspaceName) {
        cpsValidator.validateNameCharacters(dataspaceName);
        cpsAdminPersistenceService.deleteDataspace(dataspaceName);
    }

    @Override
    public Dataspace getDataspace(final String dataspaceName) {
        cpsValidator.validateNameCharacters(dataspaceName);
        return cpsAdminPersistenceService.getDataspace(dataspaceName);
    }

    @Override
    public Collection<Dataspace> getAllDataspaces() {
        return cpsAdminPersistenceService.getAllDataspaces();
    }

    @Override
    public void createAnchor(final String dataspaceName, final String schemaSetName, final String anchorName) {
        cpsValidator.validateNameCharacters(dataspaceName, schemaSetName, anchorName);
        cpsAdminPersistenceService.createAnchor(dataspaceName, schemaSetName, anchorName);
    }

    @Override
    public Collection<Anchor> getAnchors(final String dataspaceName) {
        cpsValidator.validateNameCharacters(dataspaceName);
        return cpsAdminPersistenceService.getAnchors(dataspaceName);
    }

    @Override
    public Collection<Anchor> getAnchors(final String dataspaceName, final String schemaSetName) {
        cpsValidator.validateNameCharacters(dataspaceName, schemaSetName);
        return cpsAdminPersistenceService.getAnchors(dataspaceName, schemaSetName);
    }

    @Override
    public Collection<Anchor> getAnchors(final String dataspaceName, final Collection<String> schemaSetNames) {
        cpsValidator.validateNameCharacters(dataspaceName);
        cpsValidator.validateNameCharacters(schemaSetNames);
        return cpsAdminPersistenceService.getAnchors(dataspaceName, schemaSetNames);
    }

    @Override
    public Anchor getAnchor(final String dataspaceName, final String anchorName) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        return cpsAdminPersistenceService.getAnchor(dataspaceName, anchorName);
    }

    @Override
    public void deleteAnchor(final String dataspaceName, final String anchorName) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        cpsDataService.deleteDataNodes(dataspaceName, anchorName, OffsetDateTime.now());
        cpsAdminPersistenceService.deleteAnchor(dataspaceName, anchorName);
    }

    @Override
    public void deleteAnchors(final String dataspaceName, final Collection<String> anchorNames) {
        cpsValidator.validateNameCharacters(dataspaceName);
        cpsValidator.validateNameCharacters(anchorNames);
        cpsDataService.deleteDataNodes(dataspaceName, anchorNames, OffsetDateTime.now());
        cpsAdminPersistenceService.deleteAnchors(dataspaceName, anchorNames);
    }

    @Override
    public Collection<String> queryAnchorNames(final String dataspaceName, final Collection<String> moduleNames) {
        cpsValidator.validateNameCharacters(dataspaceName);
        final Collection<Anchor> anchors = cpsAdminPersistenceService.queryAnchors(dataspaceName, moduleNames);
        return anchors.stream().map(Anchor::getName).collect(Collectors.toList());
    }

    @Override
    public void updateAnchorSchemaSet(final String dataspaceName,
                                         final String anchorName,
                                         final String schemaSetName) {
        cpsAdminPersistenceService.updateAnchorSchemaSet(dataspaceName, anchorName, schemaSetName);
    }
}
