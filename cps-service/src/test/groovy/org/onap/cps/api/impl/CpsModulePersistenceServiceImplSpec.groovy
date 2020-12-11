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

import org.onap.cps.TestUtils
import org.onap.cps.spi.CpsModulePersistenceService
import org.onap.cps.spi.exceptions.CpsException
import org.opendaylight.yangtools.yang.common.Revision
import org.opendaylight.yangtools.yang.model.api.SchemaContext
import spock.lang.Specification

class CpsModulePersistenceServiceImplSpec extends Specification {
    def mockModuleStoreService = Mock(CpsModulePersistenceService)
    def objectUnderTest = new CpsModuleServiceImpl()

    def setup() {
        objectUnderTest.cpsModulePersistenceService = mockModuleStoreService
    }

    def assertModule(SchemaContext schemaContext) {
        def optionalModule = schemaContext.findModule('stores', Revision.of('2020-09-15'))
        return schemaContext.modules.size() == 1 && optionalModule.isPresent()
    }

    def 'Store a SchemaContext'() {
        expect: 'No exception to be thrown when a valid model (schema) is stored'
            objectUnderTest.storeSchemaContext(Stub(SchemaContext.class), "sampleDataspace")
    }

}
