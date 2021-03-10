/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the 'License');
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
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

import static org.onap.cps.spi.CascadeDeleteAllowed.CASCADE_DELETE_ALLOWED
import static org.onap.cps.spi.CascadeDeleteAllowed.CASCADE_DELETE_PROHIBITED

import org.onap.cps.spi.CpsAdminPersistenceService
import org.onap.cps.spi.CpsModulePersistenceService
import org.onap.cps.spi.entities.YangResourceEntity
import org.onap.cps.spi.exceptions.DataspaceNotFoundException
import org.onap.cps.spi.exceptions.SchemaSetAlreadyDefinedException
import org.onap.cps.spi.exceptions.SchemaSetInUseException
import org.onap.cps.spi.exceptions.SchemaSetNotFoundException
import org.onap.cps.spi.repository.AnchorRepository
import org.onap.cps.spi.repository.SchemaSetRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import spock.lang.Unroll

class CpsModulePersistenceServiceSpec extends CpsPersistenceSpecBase {

    @Autowired
    CpsModulePersistenceService objectUnderTest

    @Autowired
    AnchorRepository anchorRepository

    @Autowired
    SchemaSetRepository schemaSetRepository

    @Autowired
    CpsAdminPersistenceService cpsAdminPersistenceService

    static final String SET_DATA = '/data/schemaset.sql'
    static final String EXISTING_SCHEMA_SET_NAME = SCHEMA_SET_NAME1
    static final String SCHEMA_SET_NAME_NO_ANCHORS = 'SCHEMA-SET-100'
    static final String SCHEMA_SET_NAME_WITH_ANCHORS_AND_DATA = 'SCHEMA-SET-101'
    static final String SCHEMA_SET_NAME_NEW = 'SCHEMA-SET-NEW'

    static final Long NEW_RESOURCE_ABSTRACT_ID = 0L
    static final String NEW_RESOURCE_NAME = 'some new resource'
    static final String NEW_RESOURCE_CONTENT = 'some resource content'
    static final String NEW_RESOURCE_CHECKSUM = '09002da02ee2683898d2c81c67f9e22cdbf8577d8c2de16c84d724e4ae44a0a6'

    def newYangResourcesNameToContentMap = [(NEW_RESOURCE_NAME):NEW_RESOURCE_CONTENT]
    def dataspaceEntity

    def setup() {
        dataspaceEntity = dataspaceRepository.getByName(DATASPACE_NAME)
    }

    @Unroll
    @Sql([CLEAR_DATA, SET_DATA])
    def 'Store schema set error scenario: #scenario.'() {
        when: 'attempt to store schema set #schemaSetName in dataspace #dataspaceName'
            objectUnderTest.storeSchemaSet(dataspaceName, schemaSetName, newYangResourcesNameToContentMap)
        then: 'an #expectedException is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                    | dataspaceName  | schemaSetName            || expectedException
            'dataspace does not exist'  | 'unknown'      | 'not-relevant'           || DataspaceNotFoundException
            'schema set already exists' | DATASPACE_NAME | EXISTING_SCHEMA_SET_NAME || SchemaSetAlreadyDefinedException
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Store new schema set.'() {
        when: 'a new schemaset is stored'
            objectUnderTest.storeSchemaSet(DATASPACE_NAME, SCHEMA_SET_NAME_NEW, newYangResourcesNameToContentMap)
        then: 'the schema set is persisted correctly'
            assertSchemaSetPersisted(DATASPACE_NAME, SCHEMA_SET_NAME_NEW, NEW_RESOURCE_ABSTRACT_ID, NEW_RESOURCE_NAME,
                    NEW_RESOURCE_CONTENT, NEW_RESOURCE_CHECKSUM)
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Retrieving schema set (resources) by anchor.'() {
        given: 'a new schema set is stored'
            objectUnderTest.storeSchemaSet(DATASPACE_NAME, SCHEMA_SET_NAME_NEW, newYangResourcesNameToContentMap)
        and: 'an anchor is created with that schema set'
            cpsAdminPersistenceService.createAnchor(DATASPACE_NAME, SCHEMA_SET_NAME_NEW, ANCHOR_NAME1)
        when: 'the schema set resources for that anchor is retrieved'
            def result = objectUnderTest.getYangSchemaSetResources(DATASPACE_NAME, ANCHOR_NAME1)
        then: 'the correct resources are returned'
             result == newYangResourcesNameToContentMap
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Storing duplicate schema content.'() {
        given: 'a new schema set with a resource with the same content as an existing resource'
            def existingResourceContent = 'CONTENT-001'
            def newYangResourcesNameToContentMap = [(NEW_RESOURCE_NAME):existingResourceContent]
        when: 'the schema set with duplicate resource is stored'
            objectUnderTest.storeSchemaSet(DATASPACE_NAME, SCHEMA_SET_NAME_NEW, newYangResourcesNameToContentMap)
        then: 'the schema persisted (re)uses the existing id, name and has the same checksum'
            def existingResourceId = 3001L
            def existingResourceName = 'module1@2020-02-02.yang'
            def existingResourceChecksum = 'e8bdda931099310de66532e08c3fafec391db29f55c81927b168f6aa8f81b73b'
            assertSchemaSetPersisted(DATASPACE_NAME, SCHEMA_SET_NAME_NEW,
                    existingResourceId, existingResourceName, existingResourceContent, existingResourceChecksum)
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Delete schema set with cascade delete prohibited but no anchors using it'() {
        when: 'a schema set is deleted with cascade-prohibited option'
            objectUnderTest.deleteSchemaSet(DATASPACE_NAME, SCHEMA_SET_NAME_NO_ANCHORS,
                    CASCADE_DELETE_PROHIBITED)
        then: 'the schema set has been deleted'
            schemaSetRepository.findByDataspaceAndName(dataspaceEntity, SCHEMA_SET_NAME_NO_ANCHORS).isPresent() == false
        and: 'any orphaned (not used by any schema set anymore) yang resources are deleted'
            def orphanedResourceId = 3100L
            yangResourceRepository.findById(orphanedResourceId).isPresent() == false
        and: 'any shared (still in use by other schema set) yang resources still persists'
            def sharedResourceId = 3003L
            yangResourceRepository.findById(sharedResourceId).isPresent()
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Delete schema set with cascade allowed.'() {
        when: 'a schema set is deleted with cascade-allowed option'
            objectUnderTest.deleteSchemaSet(DATASPACE_NAME, SCHEMA_SET_NAME_WITH_ANCHORS_AND_DATA,
                    CASCADE_DELETE_ALLOWED)
        then: 'the schema set has been deleted'
            schemaSetRepository
                    .findByDataspaceAndName(dataspaceEntity, SCHEMA_SET_NAME_WITH_ANCHORS_AND_DATA).isPresent() == false
        and: 'the associated anchors are removed'
            def associatedAnchorsIds = [ 6001, 6002 ]
            associatedAnchorsIds.each {anchorRepository.findById(it).isPresent() == false }
        and: 'the fragment(s) under those anchors are removed'
            def fragmentUnderAnchor1Id = 7001L
            fragmentRepository.findById(fragmentUnderAnchor1Id).isPresent() == false
        and: 'the shared resources still persist'
            def sharedResourceIds = [ 3003L, 3004L ]
            sharedResourceIds.each {yangResourceRepository.findById(it).isPresent() }
    }

    @Unroll
    @Sql([CLEAR_DATA, SET_DATA])
    def 'Delete schema set error scenario: #scenario.'() {
        when: 'attempt to delete a schema set where #scenario'
            objectUnderTest.deleteSchemaSet(dataspaceName, schemaSetName, CASCADE_DELETE_PROHIBITED)
        then: 'an #expectedException is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                                   | dataspaceName  | schemaSetName                         || expectedException
            'dataspace does not exist'                 | 'unknown'      | 'not-relevant'                        || DataspaceNotFoundException
            'schema set does not exists'               | DATASPACE_NAME | 'unknown'                             || SchemaSetNotFoundException
            'cascade prohibited but schema set in use' | DATASPACE_NAME | SCHEMA_SET_NAME_WITH_ANCHORS_AND_DATA || SchemaSetInUseException
    }

    def assertSchemaSetPersisted(expectedDataspaceName,
                             expectedSchemaSetName,
                             expectedYangResourceId,
                             expectedYangResourceName,
                             expectedYangResourceContent,
                             expectedYangResourceChecksum) {
        // assert the schema set is persisted
        def schemaSetEntity = schemaSetRepository
                .findByDataspaceAndName(dataspaceEntity, expectedSchemaSetName).orElseThrow()
        assert schemaSetEntity.name == expectedSchemaSetName
        assert schemaSetEntity.dataspace.name == expectedDataspaceName

        // assert the attached yang resource is persisted
        def yangResourceEntities = schemaSetEntity.getYangResources()
        yangResourceEntities.size() == 1

        // assert the attached yang resource content
        YangResourceEntity yangResourceEntity = yangResourceEntities.iterator().next()
        assert yangResourceEntity.id != null
        if (expectedYangResourceId != NEW_RESOURCE_ABSTRACT_ID) {
            // existing resource with known id
            assert yangResourceEntity.id == expectedYangResourceId
        }
        yangResourceEntity.name == expectedYangResourceName
        yangResourceEntity.content == expectedYangResourceContent
        yangResourceEntity.checksum == expectedYangResourceChecksum
    }

}
