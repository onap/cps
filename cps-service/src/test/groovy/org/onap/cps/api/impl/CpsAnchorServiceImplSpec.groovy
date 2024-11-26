/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation
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

import org.onap.cps.impl.utils.CpsValidator
import org.onap.cps.spi.CpsAdminPersistenceService
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.api.exceptions.ModuleNamesNotFoundException
import org.onap.cps.spi.api.model.Anchor
import spock.lang.Specification

class CpsAnchorServiceImplSpec extends Specification {

    def mockCpsAdminPersistenceService = Mock(CpsAdminPersistenceService)
    def mockCpsDataPersistenceService = Mock(CpsDataPersistenceService)
    def mockCpsValidator = Mock(CpsValidator)

    def objectUnderTest = new CpsAnchorServiceImpl(mockCpsAdminPersistenceService, mockCpsDataPersistenceService, mockCpsValidator)

    def 'Create anchor method invokes persistence service.'() {
        when: 'create anchor method is invoked'
            objectUnderTest.createAnchor('someDataspace', 'someSchemaSet', 'someAnchorName')
        then: 'the persistence service method is invoked with same parameters'
            1 * mockCpsAdminPersistenceService.createAnchor('someDataspace', 'someSchemaSet', 'someAnchorName')
        and: 'the CpsValidator is called on the dataspaceName, schemaSetName and anchorName'
            1 * mockCpsValidator.validateNameCharacters('someDataspace', 'someSchemaSet', 'someAnchorName')
    }

    def 'Retrieve all anchors for dataspace.'() {
        given: 'that an anchor is associated with the dataspace'
            def anchors = [new Anchor()]
            mockCpsAdminPersistenceService.getAnchors('someDataspace') >> anchors
        when: 'get Anchors is called for a dataspace name'
            def result = objectUnderTest.getAnchors('someDataspace')
        then: 'the collection provided by persistence service is returned as result'
            result == anchors
        and: 'the CpsValidator is called on the dataspaceName'
            1 * mockCpsValidator.validateNameCharacters('someDataspace')
    }

    def 'Retrieve all anchors for schema-set.'() {
        given: 'that anchor is associated with the dataspace and schemaset'
            def anchors = [new Anchor()]
            mockCpsAdminPersistenceService.getAnchorsBySchemaSetName('someDataspace', 'someSchemaSet') >> anchors
        when: 'get anchors is called for a dataspace name and schema set name'
            def result = objectUnderTest.getAnchorsBySchemaSetName('someDataspace', 'someSchemaSet')
        then: 'the collection provided by persistence service is returned as result'
            result == anchors
        and: 'the CpsValidator is called on the dataspaceName, schemaSetName'
            1 * mockCpsValidator.validateNameCharacters('someDataspace', 'someSchemaSet')
    }

    def 'Retrieve all anchors for multiple schema-sets.'() {
        given: 'that anchor is associated with the dataspace and schemasets'
            def anchors = [new Anchor(), new Anchor()]
            mockCpsAdminPersistenceService.getAnchorsBySchemaSetNames('someDataspace', _ as Collection<String>) >> anchors
        when: 'get anchors is called for a dataspace name and schema set names'
            def result = objectUnderTest.getAnchorsBySchemaSetNames('someDataspace', ['schemaSet1', 'schemaSet2'])
        then: 'the collection provided by persistence service is returned as result'
            result == anchors
        and: 'the CpsValidator is called on the dataspace name and schema-set names'
            1 * mockCpsValidator.validateNameCharacters('someDataspace')
            1 * mockCpsValidator.validateNameCharacters(_)
    }

    def 'Retrieve anchor for dataspace and provided anchor name.'() {
        given: 'that anchor name is associated with the dataspace'
            Anchor anchor = new Anchor()
            mockCpsAdminPersistenceService.getAnchor('someDataspace','someAnchor') >>  anchor
        when: 'get anchor is called for a dataspace name and anchor name'
            def result = objectUnderTest.getAnchor('someDataspace','someAnchor')
        then: 'the anchor provided by persistence service is returned as result'
            result == anchor
        and: 'the CpsValidator is called on the dataspaceName, anchorName'
            1 * mockCpsValidator.validateNameCharacters('someDataspace', 'someAnchor')
    }

    def 'Retrieve multiple anchors for dataspace and provided anchor names.'() {
        given: 'multiple anchors names to get'
            def anchorNames = ['anchor1', 'anchor2']
        and: 'that anchors are associated with the dataspace and anchor names'
            def anchors = [new Anchor(), new Anchor()]
            mockCpsAdminPersistenceService.getAnchors('someDataspace', anchorNames) >> anchors
        when: 'get anchors is called for a dataspace name and anchor names'
            def result = objectUnderTest.getAnchors('someDataspace', anchorNames)
        then: 'the collection provided by persistence service is returned as result'
            result == anchors
        and: 'the CpsValidator is called on the dataspace name and anchor names'
            1 * mockCpsValidator.validateNameCharacters('someDataspace')
            1 * mockCpsValidator.validateNameCharacters(anchorNames)
    }

    def 'Delete anchor.'() {
        when: 'delete anchor is invoked'
            objectUnderTest.deleteAnchor('someDataspace','someAnchor')
        then: 'delete data nodes is invoked on the data service with expected parameters'
            1 * mockCpsDataPersistenceService.deleteDataNodes('someDataspace','someAnchor')
        and: 'the persistence service method is invoked with same parameters to delete anchor'
            1 * mockCpsAdminPersistenceService.deleteAnchor('someDataspace','someAnchor')
        and: 'the CpsValidator is called on the dataspaceName, anchorName'
            1 * mockCpsValidator.validateNameCharacters('someDataspace', 'someAnchor')
    }

    def 'Delete multiple anchors.'() {
        given: 'multiple anchors to delete'
            def anchorNames = ['anchor1', 'anchor2']
        when: 'delete anchors is invoked'
            objectUnderTest.deleteAnchors('someDataspace', anchorNames)
        then: 'delete data nodes is invoked on the data service with expected parameters'
            1 * mockCpsDataPersistenceService.deleteDataNodes('someDataspace', anchorNames)
        and: 'the persistence service method is invoked with same parameters to delete anchor'
            1 * mockCpsAdminPersistenceService.deleteAnchors('someDataspace', anchorNames)
        and: 'the CpsValidator is called on the dataspace name and anchor names'
            1 * mockCpsValidator.validateNameCharacters('someDataspace')
            1 * mockCpsValidator.validateNameCharacters(anchorNames)
    }

    def 'Query all anchor identifiers for a dataspace and module names.'() {
        given: 'the persistence service is invoked with the expected parameters and returns a list of anchors'
            mockCpsAdminPersistenceService.queryAnchorNames('some-dataspace-name', ['some-module-name']) >> ['some-anchor-identifier']
        when: 'query anchor names is called using a dataspace name and module name'
            def result = objectUnderTest.queryAnchorNames('some-dataspace-name', ['some-module-name'])
        then: 'get anchor identifiers returns the same anchor identifier returned by the persistence layer'
            result == ['some-anchor-identifier']
        and: 'the CpsValidator is called on the dataspaceName'
            1 * mockCpsValidator.validateNameCharacters('some-dataspace-name')
    }

    def 'Query all anchors with Module Names Not Found Exception in persistence layer.'() {
        given: 'the persistence layer throws a Module Names Not Found Exception'
            def originalException = new ModuleNamesNotFoundException('exception-ds', ['m1', 'm2'])
            mockCpsAdminPersistenceService.queryAnchorNames(*_) >> { throw originalException}
        when: 'attempt query anchors'
            objectUnderTest.queryAnchorNames('some-dataspace-name', [])
        then: 'the same exception is thrown (up)'
            def thrownUp = thrown(ModuleNamesNotFoundException)
            assert thrownUp == originalException
        and: 'the exception details contains the relevant data'
            assert thrownUp.details.contains('exception-ds')
            assert thrownUp.details.contains('m1')
            assert thrownUp.details.contains('m2')
    }

    def 'Update anchor schema set.'() {
        when: 'update anchor is invoked'
            objectUnderTest.updateAnchorSchemaSet('someDataspace', 'someAnchor', 'someSchemaSetName')
        then: 'associated persistence service method is invoked with correct parameter'
            1 * mockCpsAdminPersistenceService.updateAnchorSchemaSet('someDataspace', 'someAnchor', 'someSchemaSetName')
    }

}
