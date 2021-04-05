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

import org.onap.cps.spi.CpsAdminPersistenceService
import org.onap.cps.spi.exceptions.AlreadyDefinedException
import org.onap.cps.spi.exceptions.AnchorNotFoundException
import org.onap.cps.spi.exceptions.DataspaceNotFoundException
import org.onap.cps.spi.exceptions.SchemaSetNotFoundException
import org.onap.cps.spi.model.Anchor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import spock.lang.Unroll

class CpsAdminPersistenceServiceSpec extends CpsPersistenceSpecBase {

    @Autowired
    CpsAdminPersistenceService objectUnderTest

    static final String SET_DATA = '/data/anchor.sql'
    static final String EMPTY_DATASPACE_NAME = 'DATASPACE-002'
    static final Integer DELETED_ANCHOR_ID = 3001;
    static final Long DELETED_FRAGMENT_ID = 4001;

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

    @Unroll
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

    @Unroll
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

    @Unroll
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
        then: 'anchor and associated data are deleted'
            assert anchorRepository.findById(DELETED_ANCHOR_ID).isEmpty()
            assert fragmentRepository.findById(DELETED_FRAGMENT_ID).isEmpty()
    }

    @Unroll
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
}
