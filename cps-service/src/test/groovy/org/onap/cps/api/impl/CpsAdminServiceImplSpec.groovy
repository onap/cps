/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2022 Nordix Foundation
 *  Modifications Copyright (C) 2020-2022 Bell Canada.
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

package org.onap.cps.api.impl

import org.onap.cps.api.CpsDataService
import org.onap.cps.spi.CpsAdminPersistenceService
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.spi.model.Anchor
import org.onap.cps.spi.model.CmHandleQueryParameters
import spock.lang.Specification
import java.time.OffsetDateTime

class CpsAdminServiceImplSpec extends Specification {
    def mockCpsAdminPersistenceService = Mock(CpsAdminPersistenceService)
    def mockCpsDataService = Mock(CpsDataService)
    def objectUnderTest = new CpsAdminServiceImpl(mockCpsAdminPersistenceService, mockCpsDataService)

    def 'Create dataspace method invokes persistence service.'() {
        when: 'create dataspace method is invoked'
            objectUnderTest.createDataspace('someDataspace')
        then: 'the persistence service method is invoked with same parameters'
            1 * mockCpsAdminPersistenceService.createDataspace('someDataspace')
    }

    def 'Create a dataspace with an invalid dataspace name.'() {
        when: 'create dataspace method is invoked with incorrectly named dataspace'
            objectUnderTest.createDataspace('Dataspace Name with spaces')
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'the persistence service method is not invoked'
            0 * mockCpsAdminPersistenceService.createDataspace(_)
    }

    def 'Create anchor method invokes persistence service.'() {
        when: 'create anchor method is invoked'
            objectUnderTest.createAnchor('someDataspace', 'someSchemaSet', 'someAnchorName')
        then: 'the persistence service method is invoked with same parameters'
            1 * mockCpsAdminPersistenceService.createAnchor('someDataspace', 'someSchemaSet', 'someAnchorName')
    }

    def 'Create an anchor with an invalid anchor name.'() {
        when: 'create anchor method is invoked with incorrectly named dataspace'
            objectUnderTest.createAnchor('someDataspace', 'someSchemaSet', 'Anchor Name With Spaces')
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'the persistence service method is not invoked'
            0 * mockCpsAdminPersistenceService.createAnchor(_, _, _)
    }

    def 'Retrieve all anchors for dataspace.'() {
        given: 'that anchor is associated with the dataspace'
            def anchors = [new Anchor()]
            mockCpsAdminPersistenceService.getAnchors('someDataspace') >> anchors
        expect: 'the collection provided by persistence service is returned as result'
            objectUnderTest.getAnchors('someDataspace') == anchors
    }

    def 'Retrieve all anchors with an invalid dataspace name.'() {
        when: 'get anchors is invoked with an invalid dataspace name'
            objectUnderTest.getAnchors('Dataspace name with spaces')
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'cps admin persistence get anchors is not invoked'
            0 * mockCpsAdminPersistenceService.getAnchors(_)
    }

    def 'Retrieve all anchors for schema-set.'() {
        given: 'that anchor is associated with the dataspace and schemaset'
            def anchors = [new Anchor()]
            mockCpsAdminPersistenceService.getAnchors('someDataspace', 'someSchemaSet') >> anchors
        expect: 'the collection provided by persistence service is returned as result'
            objectUnderTest.getAnchors('someDataspace', 'someSchemaSet') == anchors
    }
    def 'Retrieve all anchors for schema-set with invalid #scenario.'() {
        when: 'the collection provided by persistence service is returned as result'
            objectUnderTest.getAnchors(dataspaceName, schemaSetName)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'cps admin persistence get anchors is not invoked'
            0 * mockCpsAdminPersistenceService.getAnchors(_, _)
        where: 'the following parameters are used'
            scenario                         | dataspaceName                 | schemaSetName
            'dataspace name'                 | 'dataspace names with spaces' | 'schemaSetName'
            'schema set name'                | 'dataspaceName'               | 'schema set name with spaces'
            'dataspace and schema set name'  | 'dataspace name with spaces'  | 'schema set name with spaces'
    }


    def 'Retrieve anchor for dataspace and provided anchor name.'() {
        given: 'that anchor name is associated with the dataspace'
            Anchor anchor = new Anchor()
            mockCpsAdminPersistenceService.getAnchor('someDataspace','someAnchor') >>  anchor
        expect: 'the anchor provided by persistence service is returned as result'
            assert objectUnderTest.getAnchor('someDataspace','someAnchor') == anchor
    }

    def 'Retrieve anchor with invalid #scenario.'() {
        when: 'get anchors is invoked with an invalid dataspace name'
            objectUnderTest.getAnchor(dataspaceName, anchorName)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'cps admin persistence get anchor is not invoked'
            0 * mockCpsAdminPersistenceService.getAnchor(_, _)
        where: 'the following parameters are used'
            scenario                     | dataspaceName                 | anchorName
            'dataspace name'             | 'dataspace names with spaces' | 'anchorName'
            'anchor name'                | 'dataspaceName'               | 'anchor name with spaces'
            'dataspace and anchor name'  | 'dataspace name with spaces'  | 'anchor name with spaces'
    }

    def 'Delete anchor.'() {
        when: 'delete anchor is invoked'
            objectUnderTest.deleteAnchor('someDataspace','someAnchor')
        then: 'delete data nodes is invoked on the data service with expected parameters'
            1 * mockCpsDataService.deleteDataNodes('someDataspace','someAnchor', _ as OffsetDateTime )
        and: 'the persistence service method is invoked with same parameters to delete anchor'
             1 * mockCpsAdminPersistenceService.deleteAnchor('someDataspace','someAnchor')
    }

    def 'Delete anchor with invalid #scenario.'() {
        when: 'delete anchor is invoked'
            objectUnderTest.deleteAnchor(dataspaceName, anchorName)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'delete data nodes is invoked on the data service with expected parameters'
            0 * mockCpsDataService.deleteDataNodes(_,_, _ as OffsetDateTime )
        and: 'the persistence service method is invoked with same parameters to delete anchor'
            0 * mockCpsAdminPersistenceService.deleteAnchor(_,_)
        where: 'the following parameters are used'
            scenario                     | dataspaceName                 | anchorName
            'dataspace name'             | 'dataspace names with spaces' | 'anchorName'
            'anchor name'                | 'dataspaceName'               | 'anchor name with spaces'
            'dataspace and anchor name'  | 'dataspace name with spaces'  | 'anchor name with spaces'
    }

    def 'Query all anchor identifiers for a dataspace and module names.'() {
        given: 'the persistence service is invoked with the expected parameters and returns a list of anchors'
            mockCpsAdminPersistenceService.queryAnchors('some-dataspace-name', ['some-module-name']) >> [new Anchor(name:'some-anchor-identifier')]
        expect: 'get anchor identifiers returns the same anchor identifier returned by the persistence layer'
            objectUnderTest.queryAnchorNames('some-dataspace-name', ['some-module-name']) == ['some-anchor-identifier']

    }

    def 'Query all anchor identifiers for a dataspace and module names with an invalid dataspace name.'() {
        when: 'delete anchor is invoked'
            objectUnderTest.queryAnchorNames('some dataspace name', _ as Collection<String>)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'delete data nodes is not invoked'
            0 * mockCpsAdminPersistenceService.queryAnchors(_, _)
    }

    def 'Delete dataspace.'() {
        when: 'delete dataspace is invoked'
            objectUnderTest.deleteDataspace('someDataspace')
        then: 'associated persistence service method is invoked with correct parameter'
            1 * mockCpsAdminPersistenceService.deleteDataspace('someDataspace')
    }

    def 'Query CM Handles.'() {
        given: 'a cm handle query'
            def cmHandleQueryParameters = new CmHandleQueryParameters()
        when: 'query cm handles is invoked'
            objectUnderTest.queryCmHandles(cmHandleQueryParameters)
        then: 'associated persistence service method is invoked with correct parameter'
            1 * mockCpsAdminPersistenceService.queryCmHandles(cmHandleQueryParameters)
    }

    def 'Query Advised Cm Handles.'() {
        when: 'query advised cm handles is invoked'
            objectUnderTest.queryAdvisedCmHandle()
        then: 'the associated persistence service method is invoked'
            1 * mockCpsAdminPersistenceService.queryAdvisedCmHandle()
    }

    def 'Delete dataspace with invalid dataspace id.'() {
        when: 'delete dataspace is invoked'
            objectUnderTest.deleteDataspace('some dataspace name')
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'associated persistence service method is not invoked'
            0 * mockCpsAdminPersistenceService.deleteDataspace(_)
    }

}
