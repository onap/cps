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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.spi.CpsAdminPersistenceService;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.onap.cps.spi.model.Anchor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component("CpsAdminServiceImpl")
@AllArgsConstructor(onConstructor = @__(@Lazy))
public class CpsAdminServiceImpl implements CpsAdminService {

    private final CpsAdminPersistenceService cpsAdminPersistenceService;
    @Lazy
    private final CpsDataService cpsDataService;

    private static final Pattern REG_EX_VALIDATION_PATTERN_FOR_NETWORK_FUNCTIONS = Pattern.compile("^[^,-]+$");

    @Override
    public void createDataspace(final String dataspaceName) {
        final Matcher matcher = REG_EX_VALIDATION_PATTERN_FOR_NETWORK_FUNCTIONS.matcher(dataspaceName);
        if (matcher.matches()) {
            cpsAdminPersistenceService.createDataspace(dataspaceName);
        } else {
            throw new DataValidationException("Invalid data.",
                "Dataspace Name Cannot have commas' or dashes as part of request");
        }
    }

    @Override
    public void deleteDataspace(final String dataspaceName) {
        cpsAdminPersistenceService.deleteDataspace(dataspaceName);
    }

    @Override
    public void createAnchor(final String dataspaceName, final String schemaSetName, final String anchorName) {
        final Matcher matcher = REG_EX_VALIDATION_PATTERN_FOR_NETWORK_FUNCTIONS.matcher(anchorName);
        if (matcher.matches()) {
            cpsAdminPersistenceService.createAnchor(dataspaceName, schemaSetName, anchorName);
        } else {
            throw new DataValidationException("Invalid data.",
                "Anchor Name Cannot have commas' or dashes as part of request");
        }
    }

    @Override
    public Collection<Anchor> getAnchors(final String dataspaceName) {
        return cpsAdminPersistenceService.getAnchors(dataspaceName);
    }

    @Override
    public Collection<Anchor> getAnchors(final String dataspaceName, final String schemaSetName) {
        return cpsAdminPersistenceService.getAnchors(dataspaceName, schemaSetName);
    }

    @Override
    public Anchor getAnchor(final String dataspaceName, final String anchorName) {
        return cpsAdminPersistenceService.getAnchor(dataspaceName, anchorName);
    }

    @Override
    public void deleteAnchor(final String dataspaceName, final String anchorName) {
        cpsDataService.deleteDataNodes(dataspaceName, anchorName, OffsetDateTime.now());
        cpsAdminPersistenceService.deleteAnchor(dataspaceName, anchorName);
    }

    @Override
    public Collection<String> queryAnchorNames(final String dataspaceName, final Collection<String> moduleNames) {
        final Collection<Anchor> anchors = cpsAdminPersistenceService.queryAnchors(dataspaceName, moduleNames);
        return anchors.stream().map(Anchor::getName).collect(Collectors.toList());
    }
}
