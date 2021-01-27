/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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
package org.onap.cps.spi.impl

import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.exceptions.DataspaceNotFoundException
import org.onap.cps.spi.model.DataNode
import org.onap.cps.spi.repository.FragmentRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql

class CpsDataPersistenceServiceSpec extends CpsPersistenceSpecBase {

    @Autowired
    CpsDataPersistenceService objectUnderTest

    @Autowired
    FragmentRepository fragmentRepository

    static final String SET_DATA = '/data/fragment.sql'

    @Sql([CLEAR_DATA, SET_DATA])
    def  'Create DataNode in a non existing dataspace.'() {
        when: 'attempt to create a data node in a non existing dataspace'
            objectUnderTest.storeDataNode('unknown dataspace', 'new anchor name', new DataNode())
        then:
            thrown(DataspaceNotFoundException)
    }

}
