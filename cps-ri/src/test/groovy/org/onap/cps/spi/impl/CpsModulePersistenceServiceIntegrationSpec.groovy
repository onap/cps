/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
 *  Modifications Copyright (C) 2021-2022 Bell Canada.
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

import org.onap.cps.spi.CpsAdminPersistenceService
import org.onap.cps.spi.CpsModulePersistenceService
import org.onap.cps.spi.entities.YangResourceEntity
import org.onap.cps.spi.exceptions.AlreadyDefinedException
import org.onap.cps.spi.exceptions.DataspaceNotFoundException
import org.onap.cps.spi.exceptions.SchemaSetNotFoundException
import org.onap.cps.spi.model.ModuleReference
import org.onap.cps.spi.repository.AnchorRepository
import org.onap.cps.spi.repository.SchemaSetRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql

class CpsModulePersistenceServiceIntegrationSpec extends CpsPersistenceSpecBase {

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
    static final String SCHEMA_SET_NAME_NEW = 'SCHEMA-SET-NEW'

    static final String NEW_RESOURCE_NAME = 'some new resource'
    static final String NEW_RESOURCE_CONTENT = 'module stores {\n' +
            '    yang-version 1.1;\n' +
            '    namespace "org:onap:ccsdk:sample";\n' +
            '\n' +
            '    prefix book-store;\n' +
            '\n' +
            '    revision "2020-09-15" {\n' +
            '        description\n' +
            '        "Sample Model";\n' +
            '    }' +
            '}'
    static final String NEW_RESOURCE_CHECKSUM = 'b13faef573ed1374139d02c40d8ce09c80ea1dc70e63e464c1ed61568d48d539'
    static final String NEW_RESOURCE_MODULE_NAME = 'stores'
    static final String NEW_RESOURCE_REVISION = '2020-09-15'
    static final ModuleReference newModuleReference = ModuleReference.builder().name(NEW_RESOURCE_MODULE_NAME)
            .revision(NEW_RESOURCE_REVISION).build()

    def newYangResourcesNameToContentMap = [(NEW_RESOURCE_NAME):NEW_RESOURCE_CONTENT]

    def dataspaceEntity

    def setup() {
        dataspaceEntity = dataspaceRepository.getByName(DATASPACE_NAME)
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Store schema set error scenario: #scenario.'() {
        when: 'attempt to store schema set #schemaSetName in dataspace #dataspaceName'
            objectUnderTest.storeSchemaSet(dataspaceName, schemaSetName, newYangResourcesNameToContentMap)
        then: 'an #expectedException is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                    | dataspaceName  | schemaSetName            || expectedException
            'dataspace does not exist'  | 'unknown'      | 'not-relevant'           || DataspaceNotFoundException
            'schema set already exists' | DATASPACE_NAME | EXISTING_SCHEMA_SET_NAME || AlreadyDefinedException
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Store new schema set.'() {
        when: 'a new schemaset is stored'
            objectUnderTest.storeSchemaSet(DATASPACE_NAME, SCHEMA_SET_NAME_NEW, newYangResourcesNameToContentMap)
        then: 'the schema set is persisted correctly'
            assertSchemaSetPersisted(DATASPACE_NAME, SCHEMA_SET_NAME_NEW, NEW_RESOURCE_NAME,
                    NEW_RESOURCE_CONTENT, NEW_RESOURCE_CHECKSUM, NEW_RESOURCE_MODULE_NAME, NEW_RESOURCE_REVISION)
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Store and retrieve new schema set from new modules and existing modules.'() {
        given: 'map of new modules, a list of existing modules, module reference'
            def mapOfNewModules = [newModule1: 'module newmodule { yang-version 1.1; revision "2021-10-12" { } }']
            def moduleReferenceForExistingModule = new ModuleReference("test","2021-10-12")
            def listOfExistingModulesModuleReference = [moduleReferenceForExistingModule]
            def mapOfExistingModule = [test: 'module test { yang-version 1.1; revision "2021-10-12" { } }']
            objectUnderTest.storeSchemaSet(DATASPACE_NAME, "someSchemaSetName", mapOfExistingModule)
        when: 'a new schema set is created from these new modules and existing modules'
            objectUnderTest.storeSchemaSetFromModules(DATASPACE_NAME, "newSchemaSetName" , mapOfNewModules, listOfExistingModulesModuleReference)
        then: 'the schema set can be retrieved'
            def expectedYangResourcesMapAfterSchemaSetHasBeenCreated = mapOfNewModules + mapOfExistingModule
            def actualYangResourcesMapAfterSchemaSetHasBeenCreated =
                    objectUnderTest.getYangSchemaResources(DATASPACE_NAME, "newSchemaSetName")
        actualYangResourcesMapAfterSchemaSetHasBeenCreated == expectedYangResourcesMapAfterSchemaSetHasBeenCreated
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
    def 'Retrieving all yang resources module references for the given dataspace.'() {
        given: 'a dataspace name'
            def dataspaceName = 'DATASPACE-002'
        when: 'all yang resources module references are retrieved for the given dataspace name'
            def result = objectUnderTest.getYangResourceModuleReferences(dataspaceName)
        then: 'the correct resources are returned'
            result.sort() == [new ModuleReference(moduleName: 'MODULE-NAME-005', revision: 'REVISION-002'),
                              new ModuleReference(moduleName: 'MODULE-NAME-006', revision: 'REVISION-006')]
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Retrieving module names and revisions for the given anchor.'() {
        given: 'a dataspace name and anchor name'
            def dataspaceName = 'DATASPACE-001'
            def anchorName = 'ANCHOR1'
        when: 'all yang resources module references are retrieved for the given anchor'
            def result = objectUnderTest.getYangResourceModuleReferences(dataspaceName, anchorName)
        then: 'the correct module names and revisions are returned'
            result.sort() == [ new ModuleReference(moduleName: 'MODULE-NAME-003', revision: 'REVISION-002'),
                              new ModuleReference(moduleName: 'MODULE-NAME-004', revision: 'REVISION-004')]
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Storing duplicate schema content.'() {
        given: 'a new schema set with a resource with the same content as an existing resource'
            def existingResourceContent = 'module test { yang-version 1.1; revision "2020-09-15"; }'
            def newYangResourcesNameToContentMap = [(NEW_RESOURCE_NAME):existingResourceContent]
        when: 'the schema set with duplicate resource is stored'
            objectUnderTest.storeSchemaSet(DATASPACE_NAME, SCHEMA_SET_NAME_NEW, newYangResourcesNameToContentMap)
        then: 'the schema persisted (re)uses the existing name and has the same checksum'
            def existingResourceName = 'module1@2020-02-02.yang'
            def existingResourceChecksum = 'bea1afcc3d1517e7bf8cae151b3b6bfbd46db77a81754acdcb776a50368efa0a'
            def existingResourceModelName = 'test'
            def existingResourceRevision = '2020-09-15'
            assertSchemaSetPersisted(DATASPACE_NAME, SCHEMA_SET_NAME_NEW, existingResourceName,
                    existingResourceContent, existingResourceChecksum,
                    existingResourceModelName, existingResourceRevision)
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Delete schema set'() {
        when: 'a schema set is deleted with cascade-prohibited option'
            objectUnderTest.deleteSchemaSet(DATASPACE_NAME, SCHEMA_SET_NAME_NO_ANCHORS)
        then: 'the schema set has been deleted'
            schemaSetRepository.findByDataspaceAndName(dataspaceEntity, SCHEMA_SET_NAME_NO_ANCHORS).isPresent() == false
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Identifying new module references where #scenario'() {
        when: 'identifyNewModuleReferences is called'
            def result = objectUnderTest.identifyNewModuleReferences(moduleReferences)
        then: 'the correct module reference collection is returned'
            assert result == expectedResult
        where: 'the following data is used'
            scenario                              | moduleReferences                                                                                  || expectedResult
            'new module references exist'         | toModuleReference([['some module 1' : 'some revision 1'], ['some module 2' : 'some revision 2']]) || toModuleReference([['some module 1' : 'some revision 1'], ['some module 2' : 'some revision 2']])
            'no new module references exist'      | []                                                                                                || []
            'module references collection is null'| null                                                                                              || []
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Delete schema set error scenario: #scenario.'() {
        when: 'attempt to delete a schema set where #scenario'
            objectUnderTest.deleteSchemaSet(dataspaceName, schemaSetName)
        then: 'an #expectedException is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                                   | dataspaceName  | schemaSetName                         || expectedException
            'dataspace does not exist'                 | 'unknown'      | 'not-relevant'                        || DataspaceNotFoundException
            'schema set does not exists'               | DATASPACE_NAME | 'unknown'                             || SchemaSetNotFoundException
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Delete only orphan Yang Resources'() {
        given: 'a schema set is deleted and and yang resource is not used anymore'
            objectUnderTest.deleteSchemaSet(DATASPACE_NAME, SCHEMA_SET_NAME_NO_ANCHORS)
        when: 'orphan yang resources are deleted'
            objectUnderTest.deleteUnusedYangResourceModules()
        then: 'any orphaned (not used by any schema set anymore) yang resources are deleted'
            def orphanedResourceId = 3100L
            yangResourceRepository.findById(orphanedResourceId).isPresent() == false
        and: 'any shared (still in use by other schema set) yang resources still persists'
            def sharedResourceId = 3003L
            yangResourceRepository.findById(sharedResourceId).isPresent()
    }

    def assertSchemaSetPersisted(expectedDataspaceName,
                                 expectedSchemaSetName,
                                 expectedYangResourceName,
                                 expectedYangResourceContent,
                                 expectedYangResourceChecksum,
                                 expectedYangResourceModuleName,
                                 expectedYangResourceRevision) {
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
        yangResourceEntity.name == expectedYangResourceName
        yangResourceEntity.content == expectedYangResourceContent
        yangResourceEntity.checksum == expectedYangResourceChecksum
        yangResourceEntity.moduleName == expectedYangResourceModuleName
        yangResourceEntity.revision == expectedYangResourceRevision
    }

    def toModuleReference(moduleReferenceAsMap) {
        def moduleReferences = [].withDefault { [:] }
        moduleReferenceAsMap.forEach(property ->
            property.forEach((moduleName, revision) -> {
                moduleReferences.add(new ModuleReference('moduleName' : moduleName, 'revision' : revision))
            }))
        return moduleReferences
    }

}
