/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2023 Nordix Foundation
 *  Modifications Copyright (C) 2020-2021 Pantheon.tech
 *  Modifications Copyright (C) 2020-2022 Bell Canada.
 *  Modifications Copyright (C) 2022 TechMahindra Ltd.
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

package org.onap.cps.api.impl

import org.onap.cps.TestUtils
import org.onap.cps.api.CpsAdminService
import org.onap.cps.spi.CpsModulePersistenceService
import org.onap.cps.spi.exceptions.DuplicatedYangResourceException
import org.onap.cps.spi.exceptions.ModelValidationException
import org.onap.cps.spi.exceptions.SchemaSetInUseException
import org.onap.cps.spi.model.ModuleDefinition
import org.onap.cps.spi.utils.CpsValidator
import org.onap.cps.spi.model.Anchor
import org.onap.cps.spi.model.ModuleReference
import org.onap.cps.spi.model.SchemaSet
import org.onap.cps.yang.TimedYangTextSchemaSourceSetBuilder
import org.onap.cps.yang.YangTextSchemaSourceSet
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import spock.lang.Specification
import static org.onap.cps.spi.CascadeDeleteAllowed.CASCADE_DELETE_ALLOWED
import static org.onap.cps.spi.CascadeDeleteAllowed.CASCADE_DELETE_PROHIBITED

class CpsModuleServiceImplSpec extends Specification {

    def mockCpsModulePersistenceService = Mock(CpsModulePersistenceService)
    def mockCpsAdminService = Mock(CpsAdminService)
    def mockYangTextSchemaSourceSetCache = Mock(YangTextSchemaSourceSetCache)
    def mockCpsValidator = Mock(CpsValidator)
    def timedYangTextSchemaSourceSetBuilder = new TimedYangTextSchemaSourceSetBuilder()

    def objectUnderTest = new CpsModuleServiceImpl(mockCpsModulePersistenceService, mockYangTextSchemaSourceSetCache, mockCpsAdminService, mockCpsValidator,timedYangTextSchemaSourceSetBuilder)

    def 'Create schema set.'() {
        when: 'Create schema set method is invoked'
            objectUnderTest.createSchemaSet('someDataspace', 'someSchemaSet', [:])
        then: 'Parameters are validated and processing is delegated to persistence service'
            1 * mockCpsModulePersistenceService.storeSchemaSet('someDataspace', 'someSchemaSet', [:])
        and: 'the CpsValidator is called on the dataspaceName and schemaSetName'
            1 * mockCpsValidator.validateNameCharacters('someDataspace', 'someSchemaSet')
    }

    def 'Create schema set from new modules and existing modules.'() {
        given: 'a list of existing modules module reference'
            def moduleReferenceForExistingModule = new ModuleReference('test',  '2021-10-12','test.org')
            def listOfExistingModulesModuleReference = [moduleReferenceForExistingModule]
        when: 'create schema set from modules method is invoked'
            objectUnderTest.createSchemaSetFromModules('someDataspaceName', 'someSchemaSetName', [newModule: 'newContent'], listOfExistingModulesModuleReference)
        then: 'processing is delegated to persistence service'
            1 * mockCpsModulePersistenceService.storeSchemaSetFromModules('someDataspaceName', 'someSchemaSetName', [newModule: 'newContent'], listOfExistingModulesModuleReference)
        and: 'the CpsValidator is called on the dataspaceName and schemaSetName'
            1 * mockCpsValidator.validateNameCharacters('someDataspaceName', 'someSchemaSetName')
    }

    def 'Create schema set from invalid resources'() {
        given: 'Invalid yang resource as name-to-content map'
            def yangResourcesNameToContentMap = TestUtils.getYangResourcesAsMap('invalid.yang')
        when: 'Create schema set method is invoked'
            objectUnderTest.createSchemaSet('someDataspace', 'someSchemaSet', yangResourcesNameToContentMap)
        then: 'Model validation exception is thrown'
            thrown(ModelValidationException)
    }

    def 'Create schema set with duplicate yang resource exception in persistence layer.'() {
        given: 'the persistence layer throws an duplicated yang resource exception'
            def originalException = new DuplicatedYangResourceException('name', '123', null)
            mockCpsModulePersistenceService.storeSchemaSet(*_) >> { throw originalException }
        when: 'attempt to create schema set'
            objectUnderTest.createSchemaSet('someDataspace', 'someSchemaSet', [:])
        then: 'the same duplicated yang resource exception is thrown (up)'
            def thrownUp = thrown(DuplicatedYangResourceException)
            assert thrownUp == originalException
        and: 'the exception message contains the relevant data'
            assert thrownUp.message.contains('name')
            assert thrownUp.message.contains('123')
    }

    def 'Get schema set by name and dataspace.'() {
        given: 'an already present schema set'
            def yangResourcesNameToContentMap = TestUtils.getYangResourcesAsMap('bookstore.yang')
        and: 'yang resource cache returns the expected schema set'
            mockYangTextSchemaSourceSetCache.get('someDataspace', 'someSchemaSet') >> YangTextSchemaSourceSetBuilder.of(yangResourcesNameToContentMap)
        when: 'get schema set method is invoked'
            def result = objectUnderTest.getSchemaSet('someDataspace', 'someSchemaSet')
        then: 'the correct schema set is returned'
            result.getName().contains('someSchemaSet')
            result.getDataspaceName().contains('someDataspace')
            result.getModuleReferences().contains(new ModuleReference('stores', '2020-09-15', 'org:onap:ccsdk:sample'))
        and: 'the CpsValidator is called on the dataspaceName and schemaSetName'
            1 * mockCpsValidator.validateNameCharacters('someDataspace', 'someSchemaSet')
    }

    def 'Get schema sets by dataspace name.'() {
        given: 'two already present schema sets'
            def moduleReference = new ModuleReference('sample1', '2022-12-07')
            def sampleSchemaSet1 = new SchemaSet('testSchemaSet1', 'testDataspace', [moduleReference])
            def sampleSchemaSet2 = new SchemaSet('testSchemaSet2', 'testDataspace', [moduleReference])
        and: 'the persistence service returns the created schema sets'
            mockCpsModulePersistenceService.getSchemaSetsByDataspaceName('testDataspace') >> [sampleSchemaSet1, sampleSchemaSet2]
        and: 'yang resource cache always returns a schema source set'
            def mockYangTextSchemaSourceSet = Mock(YangTextSchemaSourceSet)
            mockYangTextSchemaSourceSetCache.get('testDataspace', _) >> mockYangTextSchemaSourceSet
        when: 'get schema sets method is invoked'
            def result = objectUnderTest.getSchemaSets('testDataspace')
        then: 'the correct schema sets are returned'
            assert result.size() == 2
            assert result.containsAll(sampleSchemaSet1, sampleSchemaSet2)
        and: 'the Cps Validator is called on the dataspaceName'
            1 * mockCpsValidator.validateNameCharacters('testDataspace')
    }

    def 'Delete schema-set when cascade is allowed.'() {
        given: '#numberOfAnchors anchors are associated with schemaset'
            def associatedAnchors = createAnchors(numberOfAnchors)
            mockCpsAdminService.getAnchors('my-dataspace', 'my-schemaset') >> associatedAnchors
        when: 'schema set deletion is requested with cascade allowed'
            objectUnderTest.deleteSchemaSet('my-dataspace', 'my-schemaset', CASCADE_DELETE_ALLOWED)
        then: 'anchor deletion is called #numberOfAnchors times'
            numberOfAnchors * mockCpsAdminService.deleteAnchor('my-dataspace', _)
        and: 'persistence service method is invoked with same parameters'
            1 * mockCpsModulePersistenceService.deleteSchemaSet('my-dataspace', 'my-schemaset')
        and: 'schema set will be removed from the cache'
            1 * mockYangTextSchemaSourceSetCache.removeFromCache('my-dataspace', 'my-schemaset')
        and: 'orphan yang resources are deleted'
            1 * mockCpsModulePersistenceService.deleteUnusedYangResourceModules()
        and: 'the CpsValidator is called on the dataspaceName and schemaSetName'
            1 * mockCpsValidator.validateNameCharacters('my-dataspace', _)
        where: 'following parameters are used'
            numberOfAnchors << [0, 3]
    }

    def 'Delete schema-set when cascade is prohibited.'() {
        given: 'no anchors are associated with schemaset'
            mockCpsAdminService.getAnchors('my-dataspace', 'my-schemaset') >> Collections.emptyList()
        when: 'schema set deletion is requested with cascade allowed'
            objectUnderTest.deleteSchemaSet('my-dataspace', 'my-schemaset', CASCADE_DELETE_PROHIBITED)
        then: 'no anchors are deleted'
            0 * mockCpsAdminService.deleteAnchor(_, _)
        and: 'persistence service method is invoked with same parameters'
            1 * mockCpsModulePersistenceService.deleteSchemaSet('my-dataspace', 'my-schemaset')
        and: 'schema set will be removed from the cache'
            1 * mockYangTextSchemaSourceSetCache.removeFromCache('my-dataspace', 'my-schemaset')
        and: 'orphan yang resources are deleted'
            1 * mockCpsModulePersistenceService.deleteUnusedYangResourceModules()
        and: 'the CpsValidator is called on the dataspaceName and schemaSetName'
            1 * mockCpsValidator.validateNameCharacters('my-dataspace', 'my-schemaset')
    }

    def 'Delete schema-set when cascade is prohibited and schema-set has anchors.'() {
        given: '2 anchors are associated with schemaset'
            mockCpsAdminService.getAnchors('my-dataspace', 'my-schemaset') >> createAnchors(2)
        when: 'schema set deletion is requested with cascade allowed'
            objectUnderTest.deleteSchemaSet('my-dataspace', 'my-schemaset', CASCADE_DELETE_PROHIBITED)
        then: 'Schema-Set in Use exception is thrown'
            thrown(SchemaSetInUseException)
    }

    def createAnchors(int anchorCount) {
        def anchors = []
        (0..<anchorCount).each { anchors.add(new Anchor("my-anchor-$it", 'my-dataspace', 'my-schemaset')) }
        return anchors
    }

    def 'Delete multiple schema-sets when cascade is allowed.'() {
        given: '#numberOfAnchors anchors are associated with each schemaset'
            mockCpsAdminService.getAnchors('my-dataspace', ['my-schemaset1', 'my-schemaset2']) >> createAnchors(numberOfAnchors * 2)
        when: 'schema set deletion is requested with cascade allowed'
            objectUnderTest.deleteSchemaSetsWithCascade('my-dataspace', ['my-schemaset1', 'my-schemaset2'])
        then: 'anchor deletion is called #numberOfAnchors times'
            mockCpsAdminService.deleteAnchors('my-dataspace', _)
        and: 'persistence service method is invoked with same parameters'
            mockCpsModulePersistenceService.deleteSchemaSets('my-dataspace', _)
        and: 'schema sets will be removed from the cache'
            2 * mockYangTextSchemaSourceSetCache.removeFromCache('my-dataspace', _)
        and: 'orphan yang resources are deleted'
            1 * mockCpsModulePersistenceService.deleteUnusedYangResourceModules()
        and: 'the CpsValidator is called on the dataspaceName'
            1 * mockCpsValidator.validateNameCharacters('my-dataspace')
        and: 'the CpsValidator is called on the schemaSetNames'
            1 * mockCpsValidator.validateNameCharacters(_)
        where: 'following parameters are used'
            numberOfAnchors << [0, 3]
    }

    def 'Get all yang resources module references.'() {
        given: 'an already present module reference'
            def moduleReferences = [new ModuleReference('some module name','some revision name')]
            mockCpsModulePersistenceService.getYangResourceModuleReferences('someDataspaceName') >> moduleReferences
        when: 'get yang resource module references is called'
            def result = objectUnderTest.getYangResourceModuleReferences('someDataspaceName')
        then: 'the list provided by persistence service is returned as result'
            result == moduleReferences
        and: 'the CpsValidator is called on the dataspaceName and schemaSetName'
            1 * mockCpsValidator.validateNameCharacters('someDataspaceName')
    }

    def 'Get all yang resources module references for the given dataspace name and anchor name.'() {
        given: 'the module store service service returns a list module references'
            def moduleReferences = [new ModuleReference()]
            mockCpsModulePersistenceService.getYangResourceModuleReferences('someDataspaceName', 'someAnchorName') >> moduleReferences
        when: 'get yang resource module references is called for dataspace name and anchor name'
            def result = objectUnderTest.getYangResourcesModuleReferences('someDataspaceName', 'someAnchorName')
        then: 'the list provided by persistence service is returned as result'
            result == moduleReferences
        and: 'the CpsValidator is called on the dataspaceName and schemaSetName'
            1 * mockCpsValidator.validateNameCharacters('someDataspaceName', 'someAnchorName')
    }

    def 'Identifying new module references.'(){
        given: 'module references from cm handle'
            def moduleReferencesToCheck = [new ModuleReference('some-module', 'some-revision')]
        when: 'identifyNewModuleReferences is called'
            objectUnderTest.identifyNewModuleReferences(moduleReferencesToCheck)
        then: 'cps module persistence service is called with module references to check'
            1 * mockCpsModulePersistenceService.identifyNewModuleReferences(moduleReferencesToCheck)
    }

    def 'Getting module definitions.'() {
        given: 'the module persistence service returns a collection of module definitions'
            def moduleDefinitionsFromPersistenceService = [ new ModuleDefinition('name', 'revision', 'content' ) ]
            mockCpsModulePersistenceService.getYangResourceDefinitions('some-dataspace-name', 'some-anchor-name')  >> moduleDefinitionsFromPersistenceService
        when: 'get module definitions method is called with a valid dataspace and anchor name'
            def result = objectUnderTest.getModuleDefinitionsByAnchorName('some-dataspace-name', 'some-anchor-name')
        then: 'the result is the same collection returned by the persistence service'
            assert result == moduleDefinitionsFromPersistenceService
        and: 'the CpsValidator is called on the dataspaceName and schemaSetName'
            1 * mockCpsValidator.validateNameCharacters('some-dataspace-name', 'some-anchor-name')
    }
}
