/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2022 Bell Canada
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

import org.mockito.Mock
import org.onap.cps.spi.CpsAdminPersistenceService
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.exceptions.AlreadyDefinedException
import org.onap.cps.spi.exceptions.AnchorNotFoundException
import org.onap.cps.spi.exceptions.DataspaceInUseException
import org.onap.cps.spi.exceptions.DataspaceNotFoundException
import org.onap.cps.spi.exceptions.SchemaSetNotFoundException
import org.onap.cps.spi.exceptions.ModuleNamesNotFoundException
import org.onap.cps.spi.model.Anchor
import org.onap.cps.spi.model.CmHandleQueryParameters
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper

class CpsAdminPersistenceServiceSpec extends CpsPersistenceSpecBase {

    @Autowired
    CpsAdminPersistenceService objectUnderTest

    @Mock
    ObjectMapper objectMapper

    def cpsAdminPersistenceService = Mock(CpsDataPersistenceService)

    static final String SET_DATA = '/data/anchor.sql'
    static final String SET_FRAGMENT_DATA = '/data/fragment.sql'
    static final String SAMPLE_DATA_FOR_ANCHORS_WITH_MODULES = '/data/anchors-schemaset-modules.sql'
    static final String DATASPACE_WITH_NO_DATA = 'DATASPACE-002-NO-DATA'
    static final Integer DELETED_ANCHOR_ID = 3002

    @Sql(CLEAR_DATA)
    def 'Create and retrieve a new dataspace.'() {
        when: 'a new dataspace is created'
            def dataspaceName = 'some-new-dataspace'
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
            def newAnchorName = 'my-new-anchor'
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
            dataspaceName          || expectedAnchors
            DATASPACE_NAME         || [Anchor.builder().name(ANCHOR_NAME1).schemaSetName(SCHEMA_SET_NAME1).dataspaceName(DATASPACE_NAME).build(),
                                       Anchor.builder().name(ANCHOR_NAME2).schemaSetName(SCHEMA_SET_NAME2).dataspaceName(DATASPACE_NAME).build()]
            DATASPACE_WITH_NO_DATA || []
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Get all anchors associated with schemaset in a dataspace.'() {
        when: 'anchors are retrieved by dataspace and schema-set'
            def anchors = objectUnderTest.getAnchors(dataspace, schemasetName)
        then: ' the response contains expected anchors'
            anchors == expectedAnchors
        where:
            scenario     | dataspace       | schemasetName               || expectedAnchors
            'no-anchors' | 'DATASPACE-003' | 'SCHEMA-SET-002-NO-ANCHORS' || Collections.emptySet()
            'one-anchor' | 'DATASPACE-001' | 'SCHEMA-SET-001'            || Set.of(new Anchor('ANCHOR-001', 'DATASPACE-001', 'SCHEMA-SET-001'))
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Error Handling: Get all anchors associated with schemaset in a dataspace.'() {
        when: 'anchors are retrieved by dataspace and schema-set'
            def anchors = objectUnderTest.getAnchors(dataspace, schemasetName)
        then: ' an expected expception is thrown'
            thrown(expectedException)
        where:
            scenario            | dataspace       | schemasetName               || expectedException
            'unknown-dataspace' | 'unknown'       | 'SCHEMA-SET-002-NO-ANCHORS' || DataspaceNotFoundException
            'unknown-schemaset' | 'DATASPACE-001' | 'unknown-schema-set'        || SchemaSetNotFoundException
    }

    @Sql(CLEAR_DATA)
    def 'Get all anchors in unknown dataspace.'() {
        when: 'attempt to get all anchors in an unknown dataspace'
            objectUnderTest.getAnchors('unknown-dataspace')
        then: 'an DataspaceNotFoundException is thrown'
            thrown(DataspaceNotFoundException)
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Delete anchor'() {
        when: 'delete anchor action is invoked'
            objectUnderTest.deleteAnchor(DATASPACE_NAME, ANCHOR_NAME2)
        then: 'anchor is deleted'
            assert anchorRepository.findById(DELETED_ANCHOR_ID).isEmpty()
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'delete anchor error scenario: #scenario'() {
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
            def anchors = objectUnderTest.queryAnchors('dataspace-1', inputModuleNames)
        then: 'the expected anchors are returned'
            anchors.size() == expectedAnchors.size()
            anchors.containsAll(expectedAnchors)
        where: 'the following data is used'
            scenario                           | inputModuleNames                                    || expectedAnchors
            'one module'                       | ['module-name-1']                                   || [buildAnchor('anchor-2', 'dataspace-1', 'schema-set-2'), buildAnchor('anchor-1', 'dataspace-1', 'schema-set-1')]
            'two modules'                      | ['module-name-1', 'module-name-2']                  || [buildAnchor('anchor-2', 'dataspace-1', 'schema-set-2'), buildAnchor('anchor-1', 'dataspace-1', 'schema-set-1')]
            'no anchors for all three modules' | ['module-name-1', 'module-name-2', 'module-name-3'] || []
    }

    @Sql([CLEAR_DATA, SAMPLE_DATA_FOR_ANCHORS_WITH_MODULES])
    def 'Query all anchors for an #scenario.'() {
        when: 'attempt to query anchors'
            objectUnderTest.queryAnchors(dataspaceName, moduleNames)
        then: 'the correct exception is thrown with the relevant details'
            def thrownException = thrown(expectedException)
            thrownException.details.contains(expectedMessageDetails)
        where: 'the following data is used'
            scenario                          | dataspaceName       | moduleNames                                || expectedException            | expectedMessageDetails  | messageDoesNotContain
            'unknown dataspace'               | 'db-does-not-exist' | ['does-not-matter']                        || DataspaceNotFoundException   | 'db-does-not-exist'     | 'does-not-matter'
            'unknown module and known module' | 'dataspace-1'       | ['module-name-1', 'module-does-not-exist'] || ModuleNamesNotFoundException | 'module-does-not-exist' | 'module-name-1'
    }

    def buildAnchor(def anchorName, def dataspaceName, def SchemaSetName) {
        return Anchor.builder().name(anchorName).dataspaceName(dataspaceName).schemaSetName(SchemaSetName).build()
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Delete dataspace.'() {
        when: 'delete dataspace action is invoked'
            objectUnderTest.deleteDataspace(DATASPACE_WITH_NO_DATA)
        then: 'dataspace is deleted'
            assert dataspaceRepository.findByName(DATASPACE_WITH_NO_DATA).isEmpty();
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Delete dataspace when #scenario.'() {
        when: 'delete dataspace action is invoked'
            objectUnderTest.deleteDataspace(dataspaceName)
        then: 'the correct exception is thrown with the relevant details'
            def thrownException = thrown(expectedException)
            thrownException.details.contains(expectedMessageDetails)
        where: 'the following data is used'
            scenario                        | dataspaceName   || expectedException          | expectedMessageDetails
            'dataspace name does not exist' | 'unknown'       || DataspaceNotFoundException | 'unknown does not exist'
            'dataspace contains an anchor'  | 'DATASPACE-001' || DataspaceInUseException    | 'contains 2 anchor(s)'
            'dataspace contains schemasets' | 'DATASPACE-003' || DataspaceInUseException    | 'contains 1 schemaset(s)'
    }

    @Sql([CLEAR_DATA, SET_FRAGMENT_DATA])
    def 'Retrieve cm handle ids when #scenario.'() {
        when: 'the service is invoked'
            def cmHandleQueryParameters = new CmHandleQueryParameters()
            cmHandleQueryParameters.setPublicProperties(publicProperties)
            def returnedCmHandles = objectUnderTest.queryCmHandles(cmHandleQueryParameters)
        then: 'the correct expected cm handles are returned'
            returnedCmHandles == expectedCmHandleIds
        where: 'the following data is used'
            scenario                                       | publicProperties                                                                              || expectedCmHandleIds
            'single matching property'                     | ['Contact' : 'newemailforstore@bookstore.com']                                                || ['PNFDemo2', 'PNFDemo', 'PNFDemo4'] as Set
            'public property dont match'                   | ['wont_match' : 'wont_match']                                                                 || [] as Set
            '2 properties, only one match (and)'           | ['Contact' : 'newemailforstore@bookstore.com', 'Contact2': 'newemailforstore2@bookstore.com'] || ['PNFDemo4'] as Set
            '2 properties, no match (and)'                 | ['Contact' : 'newemailforstore@bookstore.com', 'Contact2': '']                                || [] as Set
            'No public properties - return all cm handles' | [ : ]                                                                                         || ['PNFDemo3', 'PNFDemo', 'PNFDemo2', 'PNFDemo4'] as Set
    }

    //ToDo Complete this test.
    @Sql([CLEAR_DATA, SET_FRAGMENT_DATA])
    def 'Retrieve cm handles with #scenario state' () {
        when: 'query advised cm handles is invoked'
            def cmHandles = ['PNFDemo5'] as Set
            def returnedCmHandles = objectUnderTest.queryAdvisedCmHandle(cmHandles)
        then: 'cps data peristence service get datanode is invoked'
            1 * cpsAdminPersistenceService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry', '/dmi-registry/cm-handles[@id="PNFDemo5"]', FetchDescendantsOption.OMIT_DESCENDANTS)
    }
}
