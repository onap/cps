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
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.spi.CpsAdminPersistenceService;
import org.onap.cps.spi.model.Anchor;
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
    public void createDataspace(final String dataspaceId) {
        CpsValidator.validateFunctionIds(dataspaceId);
        cpsAdminPersistenceService.createDataspace(dataspaceId);
    }

    @Override
    public void deleteDataspace(final String dataspaceId) {
        CpsValidator.validateFunctionIds(dataspaceId);
        cpsAdminPersistenceService.deleteDataspace(dataspaceId);
    }

    @Override
    public void createAnchor(final String dataspaceId, final String schemaSetId, final String anchorId) {
        CpsValidator.validateFunctionIds(dataspaceId, schemaSetId, anchorId);
        cpsAdminPersistenceService.createAnchor(dataspaceId, schemaSetId, anchorId);
    }

    @Override
    public Collection<Anchor> getAnchors(final String dataspaceId) {
        CpsValidator.validateFunctionIds(dataspaceId);
        return cpsAdminPersistenceService.getAnchors(dataspaceId);
    }

    @Override
    public Collection<Anchor> getAnchors(final String dataspaceId, final String schemaSetId) {
        CpsValidator.validateFunctionIds(dataspaceId, schemaSetId);
        return cpsAdminPersistenceService.getAnchors(dataspaceId, schemaSetId);
    }

    @Override
    public Anchor getAnchor(final String dataspaceId, final String anchorId) {
        CpsValidator.validateFunctionIds(dataspaceId, anchorId);
        return cpsAdminPersistenceService.getAnchor(dataspaceId, anchorId);
    }

    @Override
    public void deleteAnchor(final String dataspaceId, final String anchorId) {
        CpsValidator.validateFunctionIds(dataspaceId, anchorId);
        cpsDataService.deleteDataNodes(dataspaceId, anchorId, OffsetDateTime.now());
        cpsAdminPersistenceService.deleteAnchor(dataspaceId, anchorId);
    }

    @Override
    public Collection<String> queryAnchorNames(final String dataspaceId, final Collection<String> moduleNames) {
        final Collection<Anchor> anchors = cpsAdminPersistenceService.queryAnchors(dataspaceId, moduleNames);
        return anchors.stream().map(Anchor::getName).collect(Collectors.toList());
    }
}
