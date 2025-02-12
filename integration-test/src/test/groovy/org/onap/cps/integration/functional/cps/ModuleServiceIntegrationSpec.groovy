/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2025 Nordix Foundation
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

package org.onap.cps.integration.functional.cps

import org.onap.cps.api.CpsModuleService
import org.onap.cps.api.exceptions.AlreadyDefinedException
import org.onap.cps.api.exceptions.DataspaceNotFoundException
import org.onap.cps.api.exceptions.ModelValidationException
import org.onap.cps.api.exceptions.SchemaSetInUseException
import org.onap.cps.api.exceptions.SchemaSetNotFoundException
import org.onap.cps.api.model.ModuleDefinition
import org.onap.cps.api.model.ModuleReference
import org.onap.cps.api.parameters.CascadeDeleteAllowed
import org.onap.cps.integration.base.FunctionalSpecBase

class ModuleServiceIntegrationSpec extends FunctionalSpecBase {

    CpsModuleService objectUnderTest

    private static def originalNumberOfModuleReferences = 2 // bookstore has two modules
    private static def bookStoreModuleReference = new ModuleReference('stores','2024-02-08')
    private static def bookStoreModuleReferenceWithNamespace = new ModuleReference('stores','2024-02-08', 'org:onap:cps:sample')
    private static def bookStoreTypesModuleReference = new ModuleReference('bookstore-types','2024-01-30')
    private static def bookStoreTypesModuleReferenceWithNamespace = new ModuleReference('bookstore-types','2024-01-30', 'org:onap:cps:types:sample')
    static def NEW_RESOURCE_REVISION = '2023-05-10'
    static def NEW_RESOURCE_CONTENT = """
        module test_module {
            yang-version 1.1;
            namespace "org:onap:ccsdk:sample";
            prefix book-store;
            revision "2023-05-10" {
                description
                "Sample Model";
            }
        }
        """

    def yangResourceContentPerName = [:]
    def allModuleReferences = []
    def noNewModules = [:]
    def bookstoreModelFileContent = readResourceDataFile('bookstore/bookstore.yang')
    def bookstoreTypesFileContent = readResourceDataFile('bookstore/bookstore-types.yang')

    def setup() { objectUnderTest = cpsModuleService }

    /*
        C R E A T E   S C H E M A   S E T   U S E - C A S E S
     */

    def 'Create new schema set from yang resources with #scenario'() {
        given: 'a new schema set with #numberOfModules modules'
            populateYangResourceContentPerNameAndAllModuleReferences(numberOfNewModules)
        when: 'the new schema set is created'
            objectUnderTest.createSchemaSet(FUNCTIONAL_TEST_DATASPACE_1, 'newSchemaSet', yangResourceContentPerName)
        then: 'the number of module references has increased by #numberOfNewModules'
            def yangResourceModuleReferences = objectUnderTest.getYangResourceModuleReferences(FUNCTIONAL_TEST_DATASPACE_1)
            originalNumberOfModuleReferences + numberOfNewModules == yangResourceModuleReferences.size()
        cleanup:
            objectUnderTest.deleteSchemaSetsWithCascade(FUNCTIONAL_TEST_DATASPACE_1, [ 'newSchemaSet' ])
        where: 'the following parameters are used'
            scenario                       | numberOfNewModules
            'two valid new modules'        | 2
            'empty schema set'             | 0
            'over max batch size #modules' | 101
    }

    def 'Create new schema set with recommended filename format but invalid yang'() {
        given: 'a filename using RFC6020 recommended format (for coverage only)'
            def fileName = 'test@2023-05-11.yang'
        when: 'attempt to create a schema set with invalid Yang'
            objectUnderTest.createSchemaSet(FUNCTIONAL_TEST_DATASPACE_1, 'newSchemaSet', [(fileName) :'invalid yang'])
        then: 'a model validation exception'
            thrown(ModelValidationException)
    }

    def 'Create new schema set from modules with #scenario'() {
        given: 'a new schema set with #numberOfNewModules modules'
            populateYangResourceContentPerNameAndAllModuleReferences(numberOfNewModules)
        and: 'add existing module references (optional)'
            allModuleReferences.addAll(existingModuleReferences)
        when: 'the new schema set is created'
            def schemaSetName = "NewSchemaWith${numberOfNewModules}Modules"
            objectUnderTest.createSchemaSetFromModules(FUNCTIONAL_TEST_DATASPACE_1, schemaSetName, yangResourceContentPerName, allModuleReferences)
        and: 'associated with a new anchor'
            cpsAnchorService.createAnchor(FUNCTIONAL_TEST_DATASPACE_1, schemaSetName, 'newAnchor')
        then: 'the new anchor has the correct number of modules'
            def yangResourceModuleReferences = objectUnderTest.getYangResourcesModuleReferences(FUNCTIONAL_TEST_DATASPACE_1, 'newAnchor')
            assert expectedNumberOfModulesForAnchor == yangResourceModuleReferences.size()
        and: 'the schema set has the correct number of modules too'
            def dataspaceEntity = dataspaceRepository.getByName(FUNCTIONAL_TEST_DATASPACE_1)
            def schemaSetEntity = schemaSetRepository.getByDataspaceAndName(dataspaceEntity, schemaSetName)
            assert expectedNumberOfModulesForAnchor == schemaSetEntity.yangResources.size()
        cleanup:
            objectUnderTest.deleteSchemaSetsWithCascade(FUNCTIONAL_TEST_DATASPACE_1, [ schemaSetName.toString() ])
        where: 'the following module references are provided'
            scenario                        | numberOfNewModules | existingModuleReferences                          || expectedNumberOfModulesForAnchor
            'empty schema set'              | 0                  | [ ]                                               || 0
            'one existing module'           | 0                  | [ bookStoreModuleReference ]                      || 1
            'two new modules'               | 2                  | [ ]                                               || 2
            'two new modules, one existing' | 2                  | [ bookStoreModuleReference ]                      || 3
            'over max batch size #modules'  | 101                | [ ]                                               || 101
            'two valid, one invalid module' | 2                  | [ new ModuleReference('NOT EXIST','IRRELEVANT') ] || 2
    }

    def 'Duplicate schema content.'() {
        given: 'a map of yang resources'
            populateYangResourceContentPerNameAndAllModuleReferences(1)
        when: 'a new schema set is created'
            objectUnderTest.createSchemaSet(FUNCTIONAL_TEST_DATASPACE_1, 'newSchema1', yangResourceContentPerName)
        then: 'the dataspace has one new module (reference)'
            def numberOfModuleReferencesAfterFirstSchemaSetHasBeenAdded = objectUnderTest.getYangResourceModuleReferences(FUNCTIONAL_TEST_DATASPACE_1).size()
            assert numberOfModuleReferencesAfterFirstSchemaSetHasBeenAdded == originalNumberOfModuleReferences + 1
        when: 'a second new schema set is created'
            objectUnderTest.createSchemaSet(FUNCTIONAL_TEST_DATASPACE_1, 'newSchema2', yangResourceContentPerName)
        then: 'the dataspace has no additional module (reference)'
            assert numberOfModuleReferencesAfterFirstSchemaSetHasBeenAdded  == objectUnderTest.getYangResourceModuleReferences(FUNCTIONAL_TEST_DATASPACE_1).size()
        cleanup: 'the data created in this test'
            objectUnderTest.deleteSchemaSetsWithCascade(FUNCTIONAL_TEST_DATASPACE_1, [ 'newSchema1', 'newSchema2'])
    }

    def 'Attempt to create schema set, error scenario: #scenario.'() {
        when: 'attempt to store schema set #schemaSetName in dataspace #dataspaceName'
            populateYangResourceContentPerNameAndAllModuleReferences(0)
            objectUnderTest.createSchemaSet(dataspaceName, schemaSetName, yangResourceContentPerName)
        then: 'an #expectedException is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                    | dataspaceName               | schemaSetName        || expectedException
            'dataspace does not exist'  | 'unknown'                   | 'not-relevant'       || DataspaceNotFoundException
            'schema set already exists' | FUNCTIONAL_TEST_DATASPACE_1 | BOOKSTORE_SCHEMA_SET || AlreadyDefinedException
    }

    def 'Attempt to create duplicate schema set from modules.'() {
        when: 'attempt to store duplicate schema set from modules'
            objectUnderTest.createSchemaSetFromModules(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_SCHEMA_SET, yangResourceContentPerName, [])
        then: 'an Already Defined Exception is thrown'
            thrown(AlreadyDefinedException)
    }


    /*
        R E A D   S C H E M A   S E T   I N F O   U S E - C A S E S
     */

    def 'Retrieving module definitions by anchor.'() {
        when: 'the module definitions for an anchor are retrieved'
            def result = objectUnderTest.getModuleDefinitionsByAnchorName(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1)
        then: 'the correct module definitions are returned'
            assert result.size() == 2
            assert result.contains(new ModuleDefinition('stores','2024-02-08',bookstoreModelFileContent))
            assert result.contains(new ModuleDefinition('bookstore-types','2024-01-30', bookstoreTypesFileContent))
    }

    def 'Retrieving module definitions: #scenarios'() {
        when: 'module definitions for module name are retrieved'
            def result = objectUnderTest.getModuleDefinitionsByAnchorAndModule(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, moduleName, moduleRevision)
        then: 'the correct module definitions are returned'
            if (expectedNumberOfDefinitions > 0) {
                assert result.size() == expectedNumberOfDefinitions
                def expectedModuleDefinition = new ModuleDefinition('stores', '2024-02-08', bookstoreModelFileContent)
                assert result[0] == expectedModuleDefinition
            }
        where: 'following parameters are used'
            scenarios                          | moduleName | moduleRevision || expectedNumberOfDefinitions
            'correct module name and revision' | 'stores'   | '2024-02-08'   || 1
            'correct module name'              | 'stores'   | null           || 1
            'incorrect module name'            | 'other'    | null           || 0
            'incorrect revision'               | 'stores'   | '2025-11-22'   || 0
    }

    def 'Retrieving yang resource module references by anchor.'() {
        when: 'the yang resource module references for an anchor are retrieved'
            def result = objectUnderTest.getYangResourcesModuleReferences(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1)
        then: 'the correct module references are returned'
            assert result.size() == 2
            assert result.containsAll(bookStoreModuleReference, bookStoreTypesModuleReference)
    }

    def 'Identifying new module references with #scenario'() {
        when: 'identifyNewModuleReferences is called'
            def result = objectUnderTest.identifyNewModuleReferences(allModuleReferences)
        then: 'the correct module references are returned'
            assert result.size() == expectedResult.size()
            assert result.containsAll(expectedResult)
        where: 'the following data is used'
            scenario                                | allModuleReferences                                                    || expectedResult
            'just new module references'            | [new ModuleReference('new1', 'r1'), new ModuleReference('new2', 'r1')] || [new ModuleReference('new1', 'r1'), new ModuleReference('new2', 'r1')]
            'one new module,one existing reference' | [new ModuleReference('new1', 'r1'), bookStoreModuleReference]          || [new ModuleReference('new1', 'r1')]
            'no new module references'              | [bookStoreModuleReference]                                             || []
            'no module references'                  | []                                                                     || []
            'module references collection is null'  | null                                                                   || []
    }

    def 'Retrieve schema set.'() {
        when: 'a specific schema set is retrieved'
            def result = objectUnderTest.getSchemaSet(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_SCHEMA_SET)
        then: 'the result has the correct name and module(s)'
            assert result.name == 'bookstoreSchemaSet'
            assert result.moduleReferences.size() == 2
            assert result.moduleReferences.containsAll(bookStoreModuleReferenceWithNamespace, bookStoreTypesModuleReferenceWithNamespace)
        and: 'the yang resource is stored with the normalized filename'
            def fileName = cpsModulePersistenceService.getYangSchemaResources(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_SCHEMA_SET).keySet()[0]
            assert fileName == 'bookstore-types@2024-01-30.yang'
    }

    def 'Retrieve all schema sets.'() {
        given: 'an extra schema set is stored'
            populateYangResourceContentPerNameAndAllModuleReferences(1)
            objectUnderTest.createSchemaSet(FUNCTIONAL_TEST_DATASPACE_1, 'newSchema1', yangResourceContentPerName)
        when: 'all schema sets are retrieved'
            def result = objectUnderTest.getSchemaSets(FUNCTIONAL_TEST_DATASPACE_1)
        then: 'the result contains all expected schema sets'
            assert result.name.size() == 2
            assert result.name.containsAll('bookstoreSchemaSet', 'newSchema1')
        cleanup: 'the data created in this test'
            objectUnderTest.deleteSchemaSetsWithCascade(FUNCTIONAL_TEST_DATASPACE_1, ['newSchema1'])
    }

    def 'Create schema set with duplicate module filename [CPS-138].'() {
        given: 'store the original number of sets and modules'
            def numberOfSchemaSets = objectUnderTest.getSchemaSets(FUNCTIONAL_TEST_DATASPACE_1).size()
            def numberOfModuleReferences = objectUnderTest.getYangResourceModuleReferences(FUNCTIONAL_TEST_DATASPACE_1).size()
        and: 'create a new schema set using a module with filename identical to a previously stored module (e.g. bookstore)'
            populateYangResourceContentPerNameAndAllModuleReferences('otherModule', 1)
            def otherModuleContent = yangResourceContentPerName.values()[0]
            def mapWithDuplicateName = ['bookstore' : otherModuleContent]
            objectUnderTest.createSchemaSet(FUNCTIONAL_TEST_DATASPACE_1, 'newSchema', mapWithDuplicateName)
        when: 'the yang resource details are retrieved'
            def yangSchemaResources = cpsModulePersistenceService.getYangSchemaResources(FUNCTIONAL_TEST_DATASPACE_1, 'newSchema')
        then: 'the file name of the resource has been normalized'
            def fileName = yangSchemaResources.keySet()[0]
            assert fileName == 'otherModule_0@2000-01-01.yang'
        and: 'the yang resource has the correct content'
            assert yangSchemaResources.get(fileName) == otherModuleContent
        and: 'the number of schema sets and modules has increased as expected'
            assert objectUnderTest.getSchemaSets(FUNCTIONAL_TEST_DATASPACE_1).size() == numberOfSchemaSets + 1
            assert objectUnderTest.getYangResourceModuleReferences(FUNCTIONAL_TEST_DATASPACE_1).size() == numberOfModuleReferences + 1
        cleanup: 'the data created in this test'
            objectUnderTest.deleteSchemaSetsWithCascade(FUNCTIONAL_TEST_DATASPACE_1, ['newSchema'])
    }

    def 'Create schema set with RFC-6020 filename pattern but incorrect details [CPS-138].'() {
        given: 'create a new schema set using a module with filename identical to a previously stored module (e.g. bookstore)'
            populateYangResourceContentPerNameAndAllModuleReferences('otherModule', 1)
            def otherModuleContent = yangResourceContentPerName.values()[0]
            def mapIncorrectName = ['wrongModuleAndRevision@1999-08-08.yang': otherModuleContent]
            objectUnderTest.createSchemaSet(FUNCTIONAL_TEST_DATASPACE_1, 'newSchema', mapIncorrectName)
        when: 'the yang resource details are retrieved'
            def yangSchemaResources = cpsModulePersistenceService.getYangSchemaResources(FUNCTIONAL_TEST_DATASPACE_1, 'newSchema')
        then: 'the file name of the resource has been normalized'
            def fileName = yangSchemaResources.keySet()[0]
            assert fileName == 'otherModule_0@2000-01-01.yang'
        cleanup: 'the data created in this test'
            objectUnderTest.deleteSchemaSetsWithCascade(FUNCTIONAL_TEST_DATASPACE_1, ['newSchema'])
    }


    /*
        D E L E T E   S C H E M A   S E T   U S E - C A S E S
     */

    def 'Delete schema sets with(out) cascade.'() {
        given: 'a schema set'
            populateYangResourceContentPerNameAndAllModuleReferences(1)
            objectUnderTest.createSchemaSet(FUNCTIONAL_TEST_DATASPACE_1, 'newSchemaSet', yangResourceContentPerName)
        and: 'optionally create anchor for the schema set'
            if (associateWithAnchor) {
                cpsAnchorService.createAnchor(FUNCTIONAL_TEST_DATASPACE_1, 'newSchemaSet', 'newAnchor')
            }
        when: 'attempt to delete the schema set'
            try {
                objectUnderTest.deleteSchemaSet(FUNCTIONAL_TEST_DATASPACE_1, 'newSchemaSet', cascadeDeleteAllowedOption)
            }
            catch (Exception e) {  // only accept correct exception when schema set cannot be deleted
                assert e instanceof SchemaSetInUseException && expectSchemaSetStillPresent
            }
        then: 'check if the dataspace still contains the new schema set or not'
            def remainingSchemaSetNames = objectUnderTest.getSchemaSets(FUNCTIONAL_TEST_DATASPACE_1).name
            assert remainingSchemaSetNames.contains('newSchemaSet') == expectSchemaSetStillPresent
        cleanup: 'the data created in this test'
            objectUnderTest.deleteSchemaSetsWithCascade(FUNCTIONAL_TEST_DATASPACE_1, ['newSchemaSet'])
        where: 'the following options are used'
            associateWithAnchor | cascadeDeleteAllowedOption                     || expectSchemaSetStillPresent
            false               | CascadeDeleteAllowed.CASCADE_DELETE_ALLOWED    || false
            false               | CascadeDeleteAllowed.CASCADE_DELETE_PROHIBITED || false
            true                | CascadeDeleteAllowed.CASCADE_DELETE_ALLOWED    || false
            true                | CascadeDeleteAllowed.CASCADE_DELETE_PROHIBITED || true
    }

    def 'Delete schema sets with shared resources.'() {
        given: 'a new schema set'
            populateYangResourceContentPerNameAndAllModuleReferences(1)
            objectUnderTest.createSchemaSet(FUNCTIONAL_TEST_DATASPACE_1, 'newSchemaSet1', yangResourceContentPerName)
        and: 'another schema set which shares one yang resource (module)'
            populateYangResourceContentPerNameAndAllModuleReferences(2)
            objectUnderTest.createSchemaSet(FUNCTIONAL_TEST_DATASPACE_1, 'newSchemaSet2', yangResourceContentPerName)
        when: 'all schema sets are retrieved'
            def moduleRevisions = objectUnderTest.getYangResourceModuleReferences(FUNCTIONAL_TEST_DATASPACE_1).revision
        then: 'both modules (revisions) are present'
            assert moduleRevisions.containsAll(['2000-01-01', '2000-01-01'])
        when: 'delete the second schema set that has two resources  one of which is a shared resource'
            objectUnderTest.deleteSchemaSetsWithCascade(FUNCTIONAL_TEST_DATASPACE_1, ['newSchemaSet2'])
        then: 'only the second schema set is deleted'
            def remainingSchemaSetNames = objectUnderTest.getSchemaSets(FUNCTIONAL_TEST_DATASPACE_1).name
            assert remainingSchemaSetNames.contains('newSchemaSet1')
            assert !remainingSchemaSetNames.contains('newSchemaSet2')
        and: 'only the shared module (revision) remains'
            def remainingModuleRevisions = objectUnderTest.getYangResourceModuleReferences(FUNCTIONAL_TEST_DATASPACE_1).revision
            assert remainingModuleRevisions.contains('2000-01-01')
            assert !remainingModuleRevisions.contains('2001-01-01')
        cleanup: 'the data created in this test'
            objectUnderTest.deleteSchemaSetsWithCascade(FUNCTIONAL_TEST_DATASPACE_1, ['newSchemaSet1'])
    }

    def 'Delete schema set error scenario: #scenario.'() {
        when: 'attempt to delete a schema set where #scenario'
            objectUnderTest.deleteSchemaSet(dataspaceName, schemaSetName, CascadeDeleteAllowed.CASCADE_DELETE_PROHIBITED)
        then: 'an #expectedException is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                     | dataspaceName               | schemaSetName   || expectedException
            'dataspace does not exist'   | 'unknown'                   | 'not-relevant'  || DataspaceNotFoundException
            'schema set does not exists' | FUNCTIONAL_TEST_DATASPACE_1 | 'unknown'       || SchemaSetNotFoundException
    }

    /*
        U P G R A D E
     */

    def 'Upgrade schema set [with existing and new modules, no matching module set tag in NCMP]'() {
        given: 'an anchor and schema set with 2 modules (to be upgraded)'
            populateYangResourceContentPerNameAndAllModuleReferences('original', 2)
            objectUnderTest.createSchemaSetFromModules(FUNCTIONAL_TEST_DATASPACE_1, 'targetSchema', yangResourceContentPerName, allModuleReferences)
            cpsAnchorService.createAnchor(FUNCTIONAL_TEST_DATASPACE_1, 'targetSchema', 'targetAnchor')
            def yangResourceModuleReferencesBeforeUpgrade = objectUnderTest.getYangResourcesModuleReferences(FUNCTIONAL_TEST_DATASPACE_1, 'targetAnchor')
            assert yangResourceModuleReferencesBeforeUpgrade.size() == 2
            assert yangResourceModuleReferencesBeforeUpgrade.containsAll([new ModuleReference('original_0','2000-01-01'),new ModuleReference('original_1','2001-01-01')])
        and: 'two new 2 modules (from node)'
            populateYangResourceContentPerNameAndAllModuleReferences('new', 2)
            def newModuleReferences = [new ModuleReference('new_0','2000-01-01'),new ModuleReference('new_1','2001-01-01')]
        and: 'a list of all module references (normally retrieved from node)'
            def allOtherModuleReferences = []
            allOtherModuleReferences.add(bookStoreModuleReference)
            allOtherModuleReferences.addAll(newModuleReferences)
        when: 'the schema set is upgraded'
            objectUnderTest.upgradeSchemaSetFromModules(FUNCTIONAL_TEST_DATASPACE_1, 'targetSchema', yangResourceContentPerName, allOtherModuleReferences)
        then: 'the new anchor has the correct new and existing modules'
            def yangResourceModuleReferencesAfterUpgrade = objectUnderTest.getYangResourcesModuleReferences(FUNCTIONAL_TEST_DATASPACE_1, 'targetAnchor')
            assert yangResourceModuleReferencesAfterUpgrade.size() == 3
            assert yangResourceModuleReferencesAfterUpgrade.contains(bookStoreModuleReference)
            assert yangResourceModuleReferencesAfterUpgrade.containsAll(newModuleReferences);
        cleanup: 'the data created in this test'
            objectUnderTest.deleteSchemaSetsWithCascade(FUNCTIONAL_TEST_DATASPACE_1, ['targetSchema'])
    }

    def 'Upgrade existing schema set from another anchor [used in NCMP for matching module set tag]'() {
        given: 'an anchor and schema set with 1 module (target)'
            populateYangResourceContentPerNameAndAllModuleReferences('target', 1)
            objectUnderTest.createSchemaSetFromModules(FUNCTIONAL_TEST_DATASPACE_1, 'targetSchema', yangResourceContentPerName, allModuleReferences)
            cpsAnchorService.createAnchor(FUNCTIONAL_TEST_DATASPACE_1, 'targetSchema', 'targetAnchor')
            def moduleReferencesBeforeUpgrade = objectUnderTest.getYangResourcesModuleReferences(FUNCTIONAL_TEST_DATASPACE_1, 'targetAnchor')
            assert moduleReferencesBeforeUpgrade.size() == 1
        and: 'another anchor and schema set with 2 other modules (source for upgrade)'
            populateYangResourceContentPerNameAndAllModuleReferences('source', 2)
            objectUnderTest.createSchemaSetFromModules(FUNCTIONAL_TEST_DATASPACE_1, 'sourceSchema', yangResourceContentPerName, allModuleReferences)
            cpsAnchorService.createAnchor(FUNCTIONAL_TEST_DATASPACE_1, 'sourceSchema', 'sourceAnchor')
            def yangResourcesModuleReferences = objectUnderTest.getYangResourcesModuleReferences(FUNCTIONAL_TEST_DATASPACE_1, 'sourceAnchor')
            assert yangResourcesModuleReferences.size() == 2
        when: 'the target schema is upgraded using the module references from the source anchor'
            def moduleReferencesFromSourceAnchor = objectUnderTest.getYangResourcesModuleReferences(FUNCTIONAL_TEST_DATASPACE_1, 'sourceAnchor')
            objectUnderTest.upgradeSchemaSetFromModules(FUNCTIONAL_TEST_DATASPACE_1, 'targetSchema', noNewModules, moduleReferencesFromSourceAnchor)
        then: 'the target schema now refers to the source modules (with namespace) modules'
            def schemaSetModuleReferencesAfterUpgrade = getObjectUnderTest().getSchemaSet(FUNCTIONAL_TEST_DATASPACE_1, 'targetSchema').moduleReferences
            assert schemaSetModuleReferencesAfterUpgrade.containsAll([new ModuleReference('source_0','2000-01-01','org:onap:ccsdk:sample'),new ModuleReference('source_1','2001-01-01','org:onap:ccsdk:sample')]);
        and: 'the associated target anchor has the same module references (without namespace but that is a legacy issue)'
            def anchorModuleReferencesAfterUpgrade = objectUnderTest.getYangResourcesModuleReferences(FUNCTIONAL_TEST_DATASPACE_1, 'targetAnchor')
            assert anchorModuleReferencesAfterUpgrade.containsAll([new ModuleReference('source_0','2000-01-01'),new ModuleReference('source_1','2001-01-01')]);
        cleanup: 'the data created in this test'
            objectUnderTest.deleteSchemaSetsWithCascade(FUNCTIONAL_TEST_DATASPACE_1, ['sourceSchema', 'targetSchema'])
    }

    /*
        H E L P E R   M E T H O D S
     */

    def populateYangResourceContentPerNameAndAllModuleReferences(numberOfModules) {
        populateYangResourceContentPerNameAndAllModuleReferences('name', numberOfModules)
    }

    def populateYangResourceContentPerNameAndAllModuleReferences(namePrefix, numberOfModules) {
        yangResourceContentPerName.clear()
        allModuleReferences.clear()
        numberOfModules.times {
            def uniqueName = namePrefix + '_' + it
            def uniqueRevision = String.valueOf(2000 + it) + '-01-01'
            allModuleReferences.add(new ModuleReference(uniqueName, uniqueRevision))
            def uniqueContent = NEW_RESOURCE_CONTENT.replace(NEW_RESOURCE_REVISION, uniqueRevision).replace('module test_module', 'module '+uniqueName)
            yangResourceContentPerName.put(uniqueName, uniqueContent)
        }
    }

}
