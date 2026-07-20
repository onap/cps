/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.config

import org.onap.cps.api.CpsAnchorService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsDataspaceService
import org.onap.cps.api.CpsModuleService
import spock.lang.Specification

class CpsServicesConfigSpec extends Specification {

    def dataspaceService = Mock(CpsDataspaceService)
    def moduleService    = Mock(CpsModuleService)
    def anchorService    = Mock(CpsAnchorService)
    def dataService      = Mock(CpsDataService)

    def objectUnderTest = new CpsServicesConfig()

    def 'Create object mapper bean.'() {
        expect: 'can create an object mapper bean'
            objectUnderTest.objectMapper() != null
    }

    def 'Create cps services (bundle) bean.'() {
        when: 'create cps services (bundle) bean'
            def cpsServicesBundle = objectUnderTest.cpsServices(dataspaceService, moduleService, anchorService, dataService)
        then: 'it is wired with the same instances that were passed in'
            assert cpsServicesBundle.dataspaceService == dataspaceService
            assert cpsServicesBundle.moduleService == moduleService
            assert cpsServicesBundle.anchorService == anchorService
            assert cpsServicesBundle.dataService == dataService
    }
}
