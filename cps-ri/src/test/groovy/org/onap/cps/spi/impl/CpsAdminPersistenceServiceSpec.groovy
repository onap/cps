/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
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

package org.onap.cps.spi.impl

import org.onap.cps.spi.CpsAdminPersistenceService
import org.onap.cps.spi.exceptions.AlreadyDefinedException
import org.onap.cps.spi.exceptions.AnchorNotFoundException
import org.onap.cps.spi.exceptions.DataspaceNotFoundException
import org.onap.cps.spi.exceptions.SchemaSetNotFoundException
import org.onap.cps.spi.exceptions.ModuleNamesNotFoundException
import org.onap.cps.spi.model.Anchor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql

class CpsAdminPersistenceServiceSpec extends CpsPersistenceSpecBase {

    @Autowired
    CpsAdminPersistenceService objectUnderTest


    static final String SET_DATA = '/data/anchor.sql'
    static final String SAMPLE_DATA_FOR_ANCHORS_WITH_MODULES = '/data/anchors-schemaset-modules.sql'
    static final String EMPTY_DATASPACE_NAME = 'DATASPACE-002'
    static final Integer DELETED_ANCHOR_ID = 3001
    static final Long DELETED_FRAGMENT_ID = 4001

    @Sql(CLEAR_DATA)
    def 'Create and retrieve a new dataspace.'() {
        when: 'a new dataspace is created'
            def dataspaceName = 'some new dataspace'
            objectUnderTest.createDataspace(dataspaceName)
        then: 'that dataspace can be retrieved from the dataspace repository'
            def dataspaceEntity = dataspaceRepository.findByName(dataspaceName).orElseThrow()
            dataspaceEntity.id != null
            dataspaceEntity.name == dataspaceName
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Attempt to create a duplicate dataspace.'() {
        when: 'an attempt is made to create an already existing dataspace'
            objectUnderTest.createDataspace(DATASPACE_NAME)
        then: 'an exception that is is already defined is thrown with the correct details'
            def thrown = thrown(AlreadyDefinedException)
            thrown.details.contains(DATASPACE_NAME)
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Create and retrieve a new anchor.'() {
        when: 'a new anchor is created'
            def newAnchorName = 'my new anchor'
            objectUnderTest.createAnchor(DATASPACE_NAME, SCHEMA_SET_NAME1, newAnchorName)
        then: 'that anchor can be retrieved'
            def anchor = objectUnderTest.getAnchor(DATASPACE_NAME, newAnchorName)
            anchor.name == newAnchorName
            anchor.dataspaceName == DATASPACE_NAME
            anchor.schemaSetName == SCHEMA_SET_NAME1
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Create anchor error scenario: #scenario.'() {
        when: 'attempt to create new anchor named #anchorName in dataspace #dataspaceName with #schemaSetName'
            objectUnderTest.createAnchor(dataspaceName, schemaSetName, anchorName)
        then: 'an #expectedException is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                    | dataspaceName  | schemaSetName    | anchorName     || expectedException
            'dataspace does not exist'  | 'unknown'      | 'not-relevant'   | 'not-relevant' || DataspaceNotFoundException
            'schema set does not exist' | DATASPACE_NAME | 'unknown'        | 'not-relevant' || SchemaSetNotFoundException
            'anchor already exists'     | DATASPACE_NAME | SCHEMA_SET_NAME1 | ANCHOR_NAME1   || AlreadyDefinedException
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Get anchor error scenario: #scenario.'() {
        when: 'attempt to get anchor named #anchorName in dataspace #dataspaceName'
            objectUnderTest.getAnchor(dataspaceName, anchorName)
        then: 'an #expectedException is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                   | dataspaceName  | anchorName     || expectedException
            'dataspace does not exist' | 'unknown'      | 'not-relevant' || DataspaceNotFoundException
            'anchor does not exists'   | DATASPACE_NAME | 'unknown'      || AnchorNotFoundException
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Get all anchors in dataspace #dataspaceName.'() {
        when: 'all anchors are retrieved from #DATASPACE_NAME'
            def result = objectUnderTest.getAnchors(dataspaceName)
        then: 'the expected collection of anchors is returned'
            result.size() == expectedAnchors.size()
            result.containsAll(expectedAnchors)
        where: 'the following data is used'
            dataspaceName        || expectedAnchors
            DATASPACE_NAME       || [Anchor.builder().name(ANCHOR_NAME1).schemaSetName(SCHEMA_SET_NAME1).dataspaceName(DATASPACE_NAME).build(),
                                     Anchor.builder().name(ANCHOR_NAME2).schemaSetName(SCHEMA_SET_NAME2).dataspaceName(DATASPACE_NAME).build()]
            EMPTY_DATASPACE_NAME || []
    }

    @Sql(CLEAR_DATA)
    def 'Get all anchors in unknown dataspace.'() {
        when: 'attempt to get all anchors in an unknown dataspace'
            objectUnderTest.getAnchors('unknown dataspace')
        then: 'an DataspaceNotFoundException is thrown'
            thrown(DataspaceNotFoundException)
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Delete anchor'() {
        when: 'delete anchor action is invoked'
            objectUnderTest.deleteAnchor(DATASPACE_NAME, ANCHOR_NAME1)
        then: 'anchor and associated data fragment are deleted'
            assert anchorRepository.findById(DELETED_ANCHOR_ID).isEmpty()
            assert fragmentRepository.findById(DELETED_FRAGMENT_ID).isEmpty()
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'delete anchor error scenario: #scenario'(){
        when: 'delete anchor attempt is performed'
            objectUnderTest.deleteAnchor(dataspaceName, anchorName)
        then: 'an #expectedException is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                   | dataspaceName  | anchorName     || expectedException
            'dataspace does not exist' | 'unknown'      | 'not-relevant' || DataspaceNotFoundException
            'anchor does not exists'   | DATASPACE_NAME | 'unknown'      || AnchorNotFoundException
    }

    @Sql([CLEAR_DATA, SAMPLE_DATA_FOR_ANCHORS_WITH_MODULES])
    def 'Query anchors that have #scenario.'() {
        when: 'all anchor are retrieved for the given dataspace name and module names'
            def anchors = objectUnderTest.queryAnchors('DATASPACE-001', inputModuleNames)
        then: 'the expected anchors are returned'
            anchors.size() == expectedAnchors.size()
            anchors.containsAll(expectedAnchors)
        where: 'the following data is used'
            scenario                                | inputModuleNames                       || expectedAnchors
            'one module'                            | ['MODULE-NAME-001']                    || [buildAnchor('ANCHOR1', 'DATASPACE-001', 'SCHEMA-SET-001')]
            'two modules'                           | ['MODULE-NAME-001', 'MODULE-NAME-002'] || [buildAnchor('ANCHOR1', 'DATASPACE-001', 'SCHEMA-SET-001'), buildAnchor('ANCHOR2', 'DATASPACE-001', 'SCHEMA-SET-002'), buildAnchor('ANCHOR3', 'DATASPACE-001', 'SCHEMA-SET-004')]
            'a module attached to multiple anchors' | ['MODULE-NAME-003']                    || [buildAnchor('ANCHOR1', 'DATASPACE-001', 'SCHEMA-SET-001'), buildAnchor('ANCHOR2', 'DATASPACE-001', 'SCHEMA-SET-002')]
            'same module with different revisions'  | ['MODULE-NAME-002']                    || [buildAnchor('ANCHOR2', 'DATASPACE-001', 'SCHEMA-SET-002'), buildAnchor('ANCHOR3', 'DATASPACE-001', 'SCHEMA-SET-004')]
    }

    @Sql([CLEAR_DATA, SAMPLE_DATA_FOR_ANCHORS_WITH_MODULES])
    def 'Query all anchors for an #scenario.'() {
        when: 'attempt to query anchors'
            objectUnderTest.queryAnchors(dataspaceName, moduleNames)
        then: 'the correct exception is thrown with the relevant details'
            def thrownException = thrown(expectedException)
            thrownException.details.contains(expectedMessageDetails)
        where: 'the following data is used'
            scenario                                                   | dataspaceName       | moduleNames                                  || expectedException            | expectedMessageDetails
            'existing module in an unknown dataspace'                  | 'db-does-not-exist' | ['does-not-matter']                          || DataspaceNotFoundException   | 'db-does-not-exist'
            'unknown module in an existing dataspace'                  | 'DATASPACE-001'     | ['module-does-not-exist']                    || ModuleNamesNotFoundException | 'module-does-not-exist'
            'unknown module and known module in an existing dataspace' | 'DATASPACE-001'     | ['MODULE-NAME-001', 'module-does-not-exist'] || ModuleNamesNotFoundException | 'module-does-not-exist'
    }

    def buildAnchor(def anchorName, def dataspaceName, def SchemaSetName) {
        return Anchor.builder().name(anchorName).dataspaceName(dataspaceName).schemaSetName(SchemaSetName).build()
    }
}
