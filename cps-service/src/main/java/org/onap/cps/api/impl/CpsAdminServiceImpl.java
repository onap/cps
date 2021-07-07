/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
 *  Modifications Copyright (C) 2020 Bell Canada.
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

import java.util.Collection;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.spi.CpsAdminPersistenceService;
import org.onap.cps.spi.model.Anchor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("CpsAdminServiceImpl")
public class CpsAdminServiceImpl implements CpsAdminService {

    @Autowired
    private CpsAdminPersistenceService cpsAdminPersistenceService;

    @Override
    public void createDataspace(final String dataspaceName) {
        cpsAdminPersistenceService.createDataspace(dataspaceName);
    }

    @Override
    public void createAnchor(final String dataspaceName, final String schemaSetName, final String anchorName) {
        cpsAdminPersistenceService.createAnchor(dataspaceName, schemaSetName, anchorName);
    }

    @Override
    public Collection<Anchor> getAnchors(final String dataspaceName) {
        return cpsAdminPersistenceService.getAnchors(dataspaceName);
    }

    @Override
    public Anchor getAnchor(final String dataspaceName, final String anchorName) {
        return cpsAdminPersistenceService.getAnchor(dataspaceName, anchorName);
    }

    @Override
    public void deleteAnchor(final String dataspaceName, final String anchorName) {
        cpsAdminPersistenceService.deleteAnchor(dataspaceName, anchorName);
    }
}
