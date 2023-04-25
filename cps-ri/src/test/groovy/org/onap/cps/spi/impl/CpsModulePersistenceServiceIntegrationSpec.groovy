/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation
 *  Modifications Copyright (C) 2021-2022 Bell Canada.
 *  Modifications Copyright (C) 2022 TechMahindra Ltd.
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
import org.onap.cps.spi.model.ModuleDefinition
import org.onap.cps.spi.model.ModuleReference
import org.onap.cps.spi.model.SchemaSet
import org.onap.cps.spi.repository.SchemaSetRepository
import org.onap.cps.spi.repository.SchemaSetYangResourceRepositoryImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql

class CpsModulePersistenceServiceIntegrationSpec extends CpsPersistenceSpecBase {

    @Autowired
    CpsModulePersistenceService objectUnderTest

    @Autowired
    SchemaSetRepository schemaSetRepository

    @Autowired
    CpsAdminPersistenceService cpsAdminPersistenceService

    final static String SET_DATA = '/data/schemaset.sql'

    def static EXISTING_SCHEMA_SET_NAME = SCHEMA_SET_NAME1
    def SCHEMA_SET_NAME_NO_ANCHORS = 'SCHEMA-SET-100'
    def NEW_SCHEMA_SET_NAME = 'SCHEMA-SET-NEW'
    def NEW_RESOURCE_NAME = 'some new resource'
    def NEW_RESOURCE_CONTENT = 'module stores {\n' +
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
    def NEW_RESOURCE_CHECKSUM = 'b13faef573ed1374139d02c40d8ce09c80ea1dc70e63e464c1ed61568d48d539'
    def NEW_RESOURCE_MODULE_NAME = 'stores'
    def NEW_RESOURCE_REVISION = '2020-09-15'
    def newYangResourcesNameToContentMap = [(NEW_RESOURCE_NAME):NEW_RESOURCE_CONTENT]

    def dataspaceEntity

    def setup() {
        dataspaceEntity = dataspaceRepository.getByName(DATASPACE_NAME)
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Getting yang resource ids from module references'() {
        when: 'getting yang resources for #scenario'
            def result = yangResourceRepository.getResourceIdsByModuleReferences(moduleReferences)
        then: 'the result contains the expected number entries'
            assert result.size() == expectedResultSize
        where: 'the following module references are provided'
            scenario                                 | moduleReferences                                                                                                 || expectedResultSize
            '2 valid module references'              | [ new ModuleReference('MODULE-NAME-002','REVISION-002'), new ModuleReference('MODULE-NAME-003','REVISION-002') ] || 2
            '1 invalid module reference'             | [ new ModuleReference('NOT EXIST','IRRELEVANT') ]                                                                || 0
            '1 valid and 1 invalid module reference' | [ new ModuleReference('MODULE-NAME-002','REVISION-002'), new ModuleReference('NOT EXIST','IRRELEVANT') ]         || 1
            'no module references'                   | []                                                                                                               || 0
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
    def 'Store new schema set with one module'() {
        when: 'a new schema set with one module is stored'
            objectUnderTest.storeSchemaSet(DATASPACE_NAME, NEW_SCHEMA_SET_NAME, newYangResourcesNameToContentMap)
        then: 'the schema set is persisted correctly'
           assertSchemaSetWithOneModuleIsPersistedCorrectly(NEW_RESOURCE_NAME,
               NEW_RESOURCE_MODULE_NAME, NEW_RESOURCE_REVISION, NEW_RESOURCE_CONTENT, NEW_RESOURCE_CHECKSUM)
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Store new schema set with multiple modules.'() {
        given: 'a new schema set with #numberOfModules modules'
            def numberOfModules =  2
            String schemaSetName = "NewSchemaWith${numberOfModules}Modules"
            def newYangResourcesNameToContentMap = [:]
            (1..numberOfModules).each {
                def uniqueRevision = String.valueOf(2000 + it) + '-01-01'
                def uniqueContent = NEW_RESOURCE_CONTENT.replace(NEW_RESOURCE_REVISION, uniqueRevision)
                newYangResourcesNameToContentMap.put(uniqueRevision, uniqueContent)
            }
        when: 'the new schema set is stored'
            objectUnderTest.storeSchemaSet(DATASPACE_NAME, schemaSetName, newYangResourcesNameToContentMap)
        then: 'the correct number of modules are persisted'
            assert getYangResourceEntities(schemaSetName).size() == numberOfModules
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Store and retrieve new schema set from new modules and existing modules.'() {
        given: 'a new module'
            def mapOfNewModules = [newModule: 'module newmodule { yang-version 1.1; revision "2022-08-19" { } }']
        and: 'there are more existing modules in the db than the batch size (100)'
            def listOfExistingModulesModuleReference = []
            def mapOfExistingModule = [:]
            def numberOfModules =  1 + SchemaSetYangResourceRepositoryImpl.MAX_INSERT_BATCH_SIZE
            (1..numberOfModules).each {
                def uniqueRevision = String.valueOf(2000 + it) + '-01-01'
                def uniqueContent = "module test { yang-version 1.1; revision \"${uniqueRevision}\" { } }".toString()
                mapOfNewModules.put( 'test' + it, uniqueContent)
                listOfExistingModulesModuleReference.add(new ModuleReference('test',uniqueRevision))
            }
            objectUnderTest.storeSchemaSet(DATASPACE_NAME, 'existing schema set ', mapOfExistingModule)
        when: 'a new schema set is created from these new modules and existing modules'
            objectUnderTest.storeSchemaSetFromModules(DATASPACE_NAME, NEW_SCHEMA_SET_NAME , mapOfNewModules, listOfExistingModulesModuleReference)
        then: 'the schema set can be retrieved'
            def actualYangResourcesMapAfterSchemaSetHasBeenCreated =
                    objectUnderTest.getYangSchemaResources(DATASPACE_NAME, NEW_SCHEMA_SET_NAME)
        and: 'it has all the new and existing modules'
            def expectedYangResourcesMapAfterSchemaSetHasBeenCreated = mapOfNewModules + mapOfExistingModule
            assert actualYangResourcesMapAfterSchemaSetHasBeenCreated == expectedYangResourcesMapAfterSchemaSetHasBeenCreated
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Retrieving schema set (resources) by anchor.'() {
        given: 'a new schema set is stored'
            objectUnderTest.storeSchemaSet(DATASPACE_NAME, NEW_SCHEMA_SET_NAME, newYangResourcesNameToContentMap)
        and: 'an anchor is created with that schema set'
            cpsAdminPersistenceService.createAnchor(DATASPACE_NAME, NEW_SCHEMA_SET_NAME, ANCHOR_NAME1)
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
            objectUnderTest.storeSchemaSet(DATASPACE_NAME, NEW_SCHEMA_SET_NAME, newYangResourcesNameToContentMap)
        then: 'the schema persisted has the new name and has the same checksum'
            def existingResourceChecksum = 'bea1afcc3d1517e7bf8cae151b3b6bfbd46db77a81754acdcb776a50368efa0a'
            def existingResourceModuleName = 'test'
            def existingResourceRevision = '2020-09-15'
            assertSchemaSetWithOneModuleIsPersistedCorrectly(NEW_RESOURCE_NAME, existingResourceModuleName,
                existingResourceRevision, existingResourceContent, existingResourceChecksum)
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Retrieve schema sets for a given dataspace name'() {
        when: 'the schema set resources for a given dataspace name is retrieved'
            def result = objectUnderTest.getSchemaSetsByDataspaceName(DATASPACE_NAME)
        then: 'the correct resources are returned'
             result.contains(new SchemaSet(name: 'SCHEMA-SET-001', dataspaceName: 'DATASPACE-001'))
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Delete schema set'() {
        when: 'a schema set is deleted with cascade-prohibited option'
            objectUnderTest.deleteSchemaSet(DATASPACE_NAME, SCHEMA_SET_NAME_NO_ANCHORS)
        then: 'the schema set has been deleted'
            !schemaSetRepository.findByDataspaceAndName(dataspaceEntity, SCHEMA_SET_NAME_NO_ANCHORS).isPresent()
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Delete schema sets'() {
        when: 'schema sets are deleted'
            objectUnderTest.deleteSchemaSets(DATASPACE_NAME, ['SCHEMA-SET-001', 'SCHEMA-SET-002'])
        then: 'the schema sets have been deleted'
            !schemaSetRepository.findByDataspaceAndName(dataspaceEntity, 'SCHEMA-SET-001').isPresent()
            !schemaSetRepository.findByDataspaceAndName(dataspaceEntity, 'SCHEMA-SET-002').isPresent()
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Identifying new module references where #scenario'() {
        when: 'identifyNewModuleReferences is called'
            def result = objectUnderTest.identifyNewModuleReferences(moduleReferences)
        then: 'the correct module references are returned'
            assert result.size() == expectedResult.size()
            assert result.containsAll(expectedResult)
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

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Retrieving all yang resources module definition for the given dataspace and anchor name.'() {
        when: 'all yang resources module definitions are retrieved for the given dataspace and anchor name'
            def result = objectUnderTest.getYangResourceDefinitions('DATASPACE-001', 'ANCHOR3')
        then: 'the correct module definitions (moduleName, revision and yang resource content) are returned'
            result.sort() == [new ModuleDefinition('MODULE-NAME-004', 'REVISION-004', 'CONTENT-004')]
    }

    def assertSchemaSetWithOneModuleIsPersistedCorrectly(expectedYangResourceName,
                                                         expectedYangResourceModuleName,
                                                         expectedYangResourceRevision,
                                                         expectedYangResourceContent,
                                                         expectedYangResourceChecksum) {

        // assert the attached yang resource is persisted
        def yangResourceEntities = getYangResourceEntities(NEW_SCHEMA_SET_NAME)
        assert yangResourceEntities.size() == 1

        // assert the attached yang resource content
        YangResourceEntity yangResourceEntity = yangResourceEntities.iterator().next()
        assert yangResourceEntity.id != null
        assert yangResourceEntity.fileName == expectedYangResourceName
        assert yangResourceEntity.moduleName == expectedYangResourceModuleName
        assert yangResourceEntity.revision == expectedYangResourceRevision
        assert yangResourceEntity.content == expectedYangResourceContent
        assert yangResourceEntity.checksum == expectedYangResourceChecksum
        return true
    }

    def getYangResourceEntities(schemaSetName) {
        def schemaSetEntity = schemaSetRepository
            .findByDataspaceAndName(dataspaceEntity, schemaSetName).orElseThrow()
        return schemaSetEntity.getYangResources()
    }

    def toModuleReference(moduleReferenceAsMap) {
        def moduleReferences = [].withDefault { [:] }
        moduleReferenceAsMap.forEach(property ->
            property.forEach((moduleName, revision) -> {
                moduleReferences.add(new ModuleReference(moduleName, revision))
            }))
        return moduleReferences
    }

}
