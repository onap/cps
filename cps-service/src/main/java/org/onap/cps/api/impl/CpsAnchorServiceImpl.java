/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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

import java.util.Collection;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.spi.CpsAdminPersistenceService;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.model.Anchor;
import org.onap.cps.spi.utils.CpsValidator;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CpsAnchorServiceImpl implements CpsAnchorService {

    private final CpsAdminPersistenceService cpsAdminPersistenceService;
    private final CpsDataPersistenceService cpsDataPersistenceService;
    private final CpsValidator cpsValidator;

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
        cpsDataPersistenceService.deleteDataNodes(dataspaceName, anchorName);
        cpsAdminPersistenceService.deleteAnchor(dataspaceName, anchorName);
    }

    @Override
    public void deleteAnchors(final String dataspaceName, final Collection<String> anchorNames) {
        cpsValidator.validateNameCharacters(dataspaceName);
        cpsValidator.validateNameCharacters(anchorNames);
        cpsDataPersistenceService.deleteDataNodes(dataspaceName, anchorNames);
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
