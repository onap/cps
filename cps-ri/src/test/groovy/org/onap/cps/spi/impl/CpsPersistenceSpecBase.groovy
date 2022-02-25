/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021 Bell Canada.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the 'License');
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.spi.impl

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.DatabaseTestContainer
import org.onap.cps.spi.repository.AnchorRepository
import org.onap.cps.spi.repository.DataspaceRepository
import org.onap.cps.spi.repository.FragmentRepository
import org.onap.cps.spi.repository.YangResourceRepository
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification

@SpringBootTest
@Testcontainers
class CpsPersistenceSpecBase extends Specification {

    @Autowired
    DataspaceRepository dataspaceRepository

    @Autowired
    YangResourceRepository yangResourceRepository

    @Autowired
    AnchorRepository anchorRepository

    @Autowired
    FragmentRepository fragmentRepository

    @SpringBean
    JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

    static final String CLEAR_DATA = '/data/clear-all.sql'

    static final String DATASPACE_NAME = 'DATASPACE-001'
    static final String DATASPACE_NAME2 = 'DATASPACE-002'
    static final String SCHEMA_SET_NAME1 = 'SCHEMA-SET-001'
    static final String SCHEMA_SET_NAME2 = 'SCHEMA-SET-002'
    static final String ANCHOR_NAME1 = 'ANCHOR-001'
    static final String ANCHOR_NAME2 = 'ANCHOR-002'
    static final String ANCHOR_NAME3 = 'ANCHOR-003'
    static final String ANCHOR_FOR_DATA_NODES_WITH_LEAVES = 'ANCHOR-003'
    static final String ANCHOR_FOR_SHOP_EXAMPLE = 'ANCHOR-004'
}
