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

package org.onap.cps.config

import org.onap.cps.api.CpsAnchorService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsDataspaceService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.impl.CpsServicesBundle
import spock.lang.Specification

class CpsServicesConfigSpec extends Specification {
    def 'cpsServices returns bundle wired with given services'() {
        given: 'service dependencies'
            def dataspaceService = Mock(CpsDataspaceService)
            def moduleService    = Mock(CpsModuleService)
            def anchorService    = Mock(CpsAnchorService)
            def dataService      = Mock(CpsDataService)
        and: 'the config under test'
            def config = new CpsServicesConfig()
        when: 'cpsServices bean method is invoked'
            CpsServicesBundle bundle = config.cpsServices(
                    dataspaceService,
                    moduleService,
                    anchorService,
                    dataService
            )
        then: 'a bundle is returned'
            bundle != null
        and: 'it is wired with the same instances that were passed in'
            bundle.dataspaceService.is(dataspaceService)
            bundle.moduleService.is(moduleService)
            bundle.anchorService.is(anchorService)
            bundle.dataService.is(dataService)
    }
}