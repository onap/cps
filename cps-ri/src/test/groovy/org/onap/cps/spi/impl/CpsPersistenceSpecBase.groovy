/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation
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
import org.onap.cps.spi.model.DataNodeBuilder
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

    @Shared
    DatabaseTestContainer databaseTestContainer = DatabaseTestContainer.getInstance()

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
    static final String SCHEMA_SET_NAME1 = 'SCHEMA-SET-001'
    static final String SCHEMA_SET_NAME2 = 'SCHEMA-SET-002'
    static final String ANCHOR_NAME1 = 'ANCHOR-001'
    static final String ANCHOR_NAME2 = 'ANCHOR-002'
    static final String ANCHOR_NAME3 = 'ANCHOR-003'
    static final String ANCHOR_FOR_DATA_NODES_WITH_LEAVES = 'ANCHOR-003'
    static final String ANCHOR_FOR_SHOP_EXAMPLE = 'ANCHOR-004'
    static final String ANCHOR_HAVING_SINGLE_TOP_LEVEL_FRAGMENT = 'ANCHOR-005'

    def createLineage(cpsDataPersistenceService, numberOfChildren, numberOfGrandChildren) {
        (1..numberOfChildren).each {
            def childName = "perf-test-child-${it}".toString()
            def child = goForthAndMultiply(PERF_TEST_PARENT, childName, numberOfGrandChildren)
            cpsDataPersistenceService.addChildDataNode('PERF-DATASPACE', 'PERF-ANCHOR', PERF_TEST_PARENT, child)
        }
    }

    def goForthAndMultiply(parentXpath, childName, numberOfGrandChildren) {
        def grandChildren = []
        (1..numberOfGrandChildren).each {
            def grandChild = new DataNodeBuilder().withXpath("${parentXpath}/${childName}/perf-test-grand-child-${it}").build()
            grandChildren.add(grandChild)
        }
        return new DataNodeBuilder().withXpath("${parentXpath}/${childName}").withChildDataNodes(grandChildren).build()
    }

    def createLineageWithLists(cpsDataPersistenceService, numberOfLists, numberOfListElements) {
        (1..numberOfLists).each {
            def listName = "perf-test-list-${it}".toString()
            def listElements = makeListElements(PERF_TEST_PARENT, listName, numberOfListElements)
            cpsDataPersistenceService.addListElements('PERF-DATASPACE', 'PERF-ANCHOR', PERF_TEST_PARENT, listElements)
        }
    }

    def makeListElements(parentXpath, childName, numberOfListElements) {
        def listElements = []
        (1..numberOfListElements).each {
            def key = it.toString()
            def element = new DataNodeBuilder().withXpath("${parentXpath}/${childName}[@key='${key}']").build()
            listElements.add(element)
        }
        return listElements
    }
}
