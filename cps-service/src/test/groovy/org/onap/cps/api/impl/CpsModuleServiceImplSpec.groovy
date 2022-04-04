/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2022 Nordix Foundation
 *  Modifications Copyright (C) 2020-2021 Pantheon.tech
 *  Modifications Copyright (C) 2020-2022 Bell Canada.
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
import org.onap.cps.spi.CascadeDeleteAllowed
import org.onap.cps.spi.CpsModulePersistenceService
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.spi.exceptions.ModelValidationException
import org.onap.cps.spi.exceptions.SchemaSetInUseException
import org.onap.cps.spi.model.Anchor
import org.onap.cps.spi.model.ModuleReference
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import spock.lang.Specification
import static org.onap.cps.spi.CascadeDeleteAllowed.CASCADE_DELETE_ALLOWED
import static org.onap.cps.spi.CascadeDeleteAllowed.CASCADE_DELETE_PROHIBITED

class CpsModuleServiceImplSpec extends Specification {

    def mockCpsModulePersistenceService = Mock(CpsModulePersistenceService)
    def mockCpsAdminService = Mock(CpsAdminService)
    def mockYangTextSchemaSourceSetCache = Mock(YangTextSchemaSourceSetCache)

    def objectUnderTest = new CpsModuleServiceImpl(mockCpsModulePersistenceService, mockYangTextSchemaSourceSetCache, mockCpsAdminService)

    def 'Create schema set.'() {
        given: 'Valid yang resource as name-to-content map'
            def yangResourcesNameToContentMap = TestUtils.getYangResourcesAsMap('bookstore.yang')
        when: 'Create schema set method is invoked'
            objectUnderTest.createSchemaSet('someDataspace', 'someSchemaSet', yangResourcesNameToContentMap)
        then: 'Parameters are validated and processing is delegated to persistence service'
            1 * mockCpsModulePersistenceService.storeSchemaSet('someDataspace', 'someSchemaSet', yangResourcesNameToContentMap)
    }

    def 'Create a schema set with an invalid #scenario.'() {
        when: 'create dataspace method is invoked with incorrectly named dataspace'
            objectUnderTest.createSchemaSet(dataspaceName, schemaSetName, _ as Map<String, String>)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'the persistence service method is not invoked'
            0 * mockCpsModulePersistenceService.storeSchemaSet(_, _, _)
        where: 'the following parameters are used'
            scenario                         | dataspaceName                 | schemaSetName
            'dataspace name'                 | 'dataspace names with spaces' | 'schemaSetName'
            'schema set name name'           | 'dataspaceName'               | 'schema set name with spaces'
            'dataspace and schema set name'  | 'dataspace name with spaces'  | 'schema set name with spaces'
    }

    def 'Create schema set from new modules and existing modules.'() {
        given: 'a list of existing modules module reference'
            def moduleReferenceForExistingModule = new ModuleReference("test",  "2021-10-12","test.org")
            def listOfExistingModulesModuleReference = [moduleReferenceForExistingModule]
        when: 'create schema set from modules method is invoked'
            objectUnderTest.createSchemaSetFromModules("someDataspaceName", "someSchemaSetName", [newModule: "newContent"], listOfExistingModulesModuleReference)
        then: 'processing is delegated to persistence service'
            1 * mockCpsModulePersistenceService.storeSchemaSetFromModules("someDataspaceName", "someSchemaSetName", [newModule: "newContent"], listOfExistingModulesModuleReference)
    }

    def 'Create schema set from new modules and existing modules with invalid #scenario.'() {
        when: 'create dataspace method is invoked with incorrectly named dataspace'
            objectUnderTest.createSchemaSetFromModules(dataspaceName, schemaSetName, _ as Map<String, String>, _ as Collection<ModuleReference>)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'the persistence service method is not invoked'
            0 * mockCpsModulePersistenceService.storeSchemaSetFromModules(_, _, _)
        where: 'the following parameters are used'
            scenario                         | dataspaceName                 | schemaSetName
            'dataspace name'                 | 'dataspace names with spaces' | 'schemaSetName'
            'schema set name name'           | 'dataspaceName'               | 'schema set name with spaces'
            'dataspace and schema set name'  | 'dataspace name with spaces'  | 'schema set name with spaces'
    }

    def 'Create schema set from invalid resources'() {
        given: 'Invalid yang resource as name-to-content map'
            def yangResourcesNameToContentMap = TestUtils.getYangResourcesAsMap('invalid.yang')
        when: 'Create schema set method is invoked'
            objectUnderTest.createSchemaSet('someDataspace', 'someSchemaSet', yangResourcesNameToContentMap)
        then: 'Model validation exception is thrown'
            thrown(ModelValidationException.class)
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
    }

    def 'Get a schema set with an invalid #scenario'() {
        when: 'create dataspace method is invoked with incorrectly named dataspace'
            objectUnderTest.getSchemaSet(dataspaceName, schemaSetName)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'the yang resource cache is not invoked'
            0 * mockYangTextSchemaSourceSetCache.get(_, _)
        where: 'the following parameters are used'
            scenario                     | dataspaceName                 | schemaSetName
            'dataspace name'             | 'dataspace names with spaces' | 'schemaSetName'
            'schema set name'            | 'dataspaceName'               | 'schema set name with spaces'
            'dataspace and anchor name'  | 'dataspace name with spaces'  | 'schema set name with spaces'
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
    }

    def 'Delete schema-set when cascade is prohibited and schema-set has anchors.'() {
        given: '2 anchors are associated with schemaset'
            mockCpsAdminService.getAnchors('my-dataspace', 'my-schemaset') >> createAnchors(2)
        when: 'schema set deletion is requested with cascade allowed'
            objectUnderTest.deleteSchemaSet('my-dataspace', 'my-schemaset', CASCADE_DELETE_PROHIBITED)
        then: 'Schema-Set in Use exception is thrown'
            thrown(SchemaSetInUseException)
    }

    def 'Delete a schema set with an invalid #scenario.'() {
        when: 'create dataspace method is invoked with incorrectly named dataspace'
            objectUnderTest.deleteSchemaSet(dataspaceName, schemaSetName, CASCADE_DELETE_ALLOWED)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'anchor deletion is called 0 times'
            0 * mockCpsAdminService.deleteAnchor(_, _)
        and: 'the delete schema set persistence service method is not invoked'
            0 * mockCpsModulePersistenceService.deleteSchemaSet(_, _, _)
        and: 'schema set will be removed from the cache is not invoked'
            0 * mockYangTextSchemaSourceSetCache.removeFromCache(_, _)
        and: 'orphan yang resources are deleted is not invoked'
            0 * mockCpsModulePersistenceService.deleteUnusedYangResourceModules()
        where: 'the following parameters are used'
            scenario                         | dataspaceName                 | schemaSetName
            'dataspace name'                 | 'dataspace names with spaces' | 'schemaSetName'
            'schema set name name'           | 'dataspaceName'               | 'schema set name with spaces'
            'dataspace and schema set name'  | 'dataspace name with spaces'  | 'schema set name with spaces'
    }

    def createAnchors(int anchorCount) {
        def anchors = []
        (0..<anchorCount).each { anchors.add(new Anchor("my-anchor-$it", 'my-dataspace', 'my-schemaset')) }
        return anchors
    }

    def 'Get all yang resources module references.'() {
        given: 'an already present module reference'
            def moduleReferences = [new ModuleReference('some module name','some revision name')]
            mockCpsModulePersistenceService.getYangResourceModuleReferences('someDataspaceName') >> moduleReferences
        expect: 'the list provided by persistence service is returned as result'
            objectUnderTest.getYangResourceModuleReferences('someDataspaceName') == moduleReferences
    }

    def 'Get all yang resources module references given an invalid dataspace name.'() {
        when: 'the get yang resources module references method is invoked with an invalid dataspace name'
            objectUnderTest.getYangResourceModuleReferences('dataspace name with spaces')
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'the persistence service method is not invoked'
            0 * mockCpsModulePersistenceService.getYangResourceModuleReferences(_)
    }


    def 'Get all yang resources module references for the given dataspace name and anchor name.'() {
        given: 'the module store service service returns a list module references'
            def moduleReferences = [new ModuleReference()]
            mockCpsModulePersistenceService.getYangResourceModuleReferences('someDataspaceName', 'someAnchorName') >> moduleReferences
        expect: 'the list provided by persistence service is returned as result'
            objectUnderTest.getYangResourcesModuleReferences('someDataspaceName', 'someAnchorName') == moduleReferences
    }

    def 'Get all yang resources module references given an invalid #scenario.'() {
        when: 'the get yang resources module references method is invoked with invalid #scenario'
            objectUnderTest.getYangResourcesModuleReferences(dataspaceName, anchorName)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'the persistence service method is not invoked'
            0 * mockCpsModulePersistenceService.getYangResourceModuleReferences(_, _)
        where: 'the following parameters are used'
            scenario                     | dataspaceName                 | anchorName
            'dataspace name'             | 'dataspace names with spaces' | 'anchorName'
            'anchor name'                | 'dataspaceName'               | 'anchor name with spaces'
            'dataspace and anchor name'  | 'dataspace name with spaces'  | 'anchor name with spaces'
    }

    def 'Identifying new module references'(){
        given: 'module references from cm handle'
            def moduleReferencesToCheck = [new ModuleReference('some-module', 'some-revision')]
        when: 'identifyNewModuleReferences is called'
            objectUnderTest.identifyNewModuleReferences(moduleReferencesToCheck)
        then: 'cps module persistence service is called with module references to check'
            1 * mockCpsModulePersistenceService.identifyNewModuleReferences(moduleReferencesToCheck);
    }
}
