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

import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.CpsDataspaceService;
import org.onap.cps.api.model.Dataspace;
import org.onap.cps.impl.utils.CpsValidator;
import org.onap.cps.spi.CpsAdminPersistenceService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CpsDataspaceServiceImpl implements CpsDataspaceService {

    private final CpsAdminPersistenceService cpsAdminPersistenceService;
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

}
