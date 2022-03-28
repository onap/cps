/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2022 Nordix Foundation
 *  Modifications Copyright (C) 2020-2022 Bell Canada.
 *  Modifications Copyright (C) 2021 Pantheon.tech
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
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.spi.CpsAdminPersistenceService;
import org.onap.cps.spi.model.Anchor;
import org.onap.cps.spi.model.CmHandleQueryParameters;
import org.onap.cps.utils.CpsValidator;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component("CpsAdminServiceImpl")
@AllArgsConstructor(onConstructor = @__(@Lazy))
public class CpsAdminServiceImpl implements CpsAdminService {

    private final CpsAdminPersistenceService cpsAdminPersistenceService;
    @Lazy
    private final CpsDataService cpsDataService;

    @Override
    public void createDataspace(final String dataspaceName) {
        CpsValidator.validateNameCharacters(dataspaceName);
        cpsAdminPersistenceService.createDataspace(dataspaceName);
    }

    @Override
    public void deleteDataspace(final String dataspaceName) {
        CpsValidator.validateNameCharacters(dataspaceName);
        cpsAdminPersistenceService.deleteDataspace(dataspaceName);
    }

    @Override
    public void createAnchor(final String dataspaceName, final String schemaSetName, final String anchorName) {
        CpsValidator.validateNameCharacters(dataspaceName, schemaSetName, anchorName);
        cpsAdminPersistenceService.createAnchor(dataspaceName, schemaSetName, anchorName);
    }

    @Override
    public Collection<Anchor> getAnchors(final String dataspaceName) {
        CpsValidator.validateNameCharacters(dataspaceName);
        return cpsAdminPersistenceService.getAnchors(dataspaceName);
    }

    @Override
    public Collection<Anchor> getAnchors(final String dataspaceName, final String schemaSetName) {
        CpsValidator.validateNameCharacters(dataspaceName, schemaSetName);
        return cpsAdminPersistenceService.getAnchors(dataspaceName, schemaSetName);
    }

    @Override
    public Anchor getAnchor(final String dataspaceName, final String anchorName) {
        CpsValidator.validateNameCharacters(dataspaceName, anchorName);
        return cpsAdminPersistenceService.getAnchor(dataspaceName, anchorName);
    }

    @Override
    public void deleteAnchor(final String dataspaceName, final String anchorName) {
        CpsValidator.validateNameCharacters(dataspaceName, anchorName);
        cpsDataService.deleteDataNodes(dataspaceName, anchorName, OffsetDateTime.now());
        cpsAdminPersistenceService.deleteAnchor(dataspaceName, anchorName);
    }

    @Override
    public Collection<String> queryAnchorNames(final String dataspaceName, final Collection<String> moduleNames) {
        CpsValidator.validateNameCharacters(dataspaceName);
        final Collection<Anchor> anchors = cpsAdminPersistenceService.queryAnchors(dataspaceName, moduleNames);
        return anchors.stream().map(Anchor::getName).collect(Collectors.toList());
    }

    @Override
    public Set<String> queryCmHandles(final CmHandleQueryParameters cmHandleQueryParameters) {
        return  cpsAdminPersistenceService.queryCmHandles(cmHandleQueryParameters);
    }
}
