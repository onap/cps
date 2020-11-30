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
    public String createAnchor(final Anchor anchor) {
        return cpsAdminPersistenceService.createAnchor(anchor);
    }
}
