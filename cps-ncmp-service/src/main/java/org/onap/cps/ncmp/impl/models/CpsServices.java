/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.models;

import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsDataspaceService;
import org.onap.cps.api.CpsModuleService;

public class CpsServices {

    private final CpsDataspaceService dataspaceService;
    private final CpsModuleService moduleService;
    private final CpsAnchorService anchorService;
    private final CpsDataService dataService;


    /**
     * Bundle CPS Services.
     *
     * @param dataspaceService      Dataspace service
     * @param moduleService         Module service
     * @param anchorService         Anchor service
     * @param dataService           Data service
     */
    public CpsServices(final CpsDataspaceService dataspaceService,
                       final CpsModuleService moduleService,
                       final CpsAnchorService anchorService,
                       final CpsDataService dataService) {
        this.dataspaceService = dataspaceService;
        this.moduleService = moduleService;
        this.anchorService = anchorService;
        this.dataService = dataService;
    }

    public CpsDataspaceService dataspaceService() {
        return dataspaceService;
    }

    public CpsModuleService moduleService() {
        return moduleService;
    }

    public CpsAnchorService anchorService() {
        return anchorService;
    }

    public CpsDataService dataService() {
        return dataService;
    }
}
