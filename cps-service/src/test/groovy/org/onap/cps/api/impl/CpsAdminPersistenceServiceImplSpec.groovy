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

package org.onap.cps.api.impl

import org.onap.cps.exceptions.CpsValidationException
import org.onap.cps.spi.CpsAdminPersistenceService
import org.onap.cps.spi.model.Anchor
import spock.lang.Specification

class CpsAdminPersistenceServiceImplSpec extends Specification {
    def mockCpsAdminService = Mock(CpsAdminPersistenceService)
    def objectUnderTest = new CpsAdminServiceImpl()

    def setup() {
        objectUnderTest.cpsAdminPersistenceService = mockCpsAdminService
    }

    def 'Create anchor method invokes persistence service'() {
        when: 'Create anchor method is invoked'
            objectUnderTest.createAnchor('dummyDataspace', 'dummySchemaSet', 'dummyAnchorName')
        then: 'The persistence service method is invoked with same parameters'
            1 * mockCpsAdminService.createAnchor('dummyDataspace', 'dummySchemaSet', 'dummyAnchorName')
    }

}
