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

package org.onap.cps.api.impl;

import org.onap.cps.api.CpService;
import org.onap.cps.spi.DataPersistenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CpServiceImpl implements CpService {

    @Autowired
    private DataPersistenceService dataPersistenceService;


    @Override
    public final Integer storeJsonStructure(final String jsonStructure) {
        return dataPersistenceService.storeJsonStructure(jsonStructure);
    }

    @Override
    public final String getJsonById(final int jsonObjectId) {
        return dataPersistenceService.getJsonById(jsonObjectId);
    }

    @Override
    public void deleteJsonById(final int jsonObjectId) {
        dataPersistenceService.deleteJsonById(jsonObjectId);
    }
}