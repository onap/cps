/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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

import static org.onap.cps.api.parameters.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.api.parameters.PaginationOption.NO_PAGINATION

import org.onap.cps.api.CpsDataspaceService
import java.time.OffsetDateTime
import org.onap.cps.api.exceptions.DataNodeNotFoundException
import org.onap.cps.integration.base.FunctionalSpecBase
import org.onap.cps.api.exceptions.AlreadyDefinedException
import org.onap.cps.api.exceptions.DataspaceInUseException
import org.onap.cps.api.exceptions.DataspaceNotFoundException
import org.onap.cps.ri.repository.FragmentRepository
import org.onap.cps.utils.ContentType

class DataspaceServiceIntegrationSpec extends FunctionalSpecBase {

    CpsDataspaceService objectUnderTest

    def setup() { objectUnderTest = cpsDataspaceService }

    def 'Dataspace CRUD operations.'() {
        when: 'a dataspace is created'
            objectUnderTest.createDataspace('newDataspace')
        then: 'the dataspace can be read'
            assert objectUnderTest.getDataspace('newDataspace').name == 'newDataspace'
        and: 'it can be deleted'
            objectUnderTest.deleteDataspace('newDataspace')
        then: 'the dataspace no longer exists i.e. an exception is thrown if an attempt is made to retrieve it'
            def thrown = null
            try {
                objectUnderTest.getDataspace('newDataspace')
            } catch(Exception exception) {
                thrown = exception
            }
           assert thrown instanceof DataspaceNotFoundException
    }

    def 'Attempt to delete a non-existing dataspace'() {
        when: 'attempt to delete a non-existing dataspace'
            objectUnderTest.deleteDataspace('non-existing-name')
        then: 'a not found exception is thrown with the relevant dataspace name'
            def thrownException = thrown(DataspaceNotFoundException)
            assert thrownException.details.contains('non-existing-name does not exist')
    }

    def 'Attempt Delete dataspace with a schema set and anchor'() {
        setup: 'a dataspace with a schema set and anchor'
            objectUnderTest.createDataspace('targetDataspace')
            cpsModuleService.createSchemaSet('targetDataspace','someSchemaSet',[:])
            cpsAnchorService.createAnchor('targetDataspace', 'someSchemaSet', 'some_anchor')
        when: 'attempt to delete dataspace'
            objectUnderTest.deleteDataspace('targetDataspace')
        then: 'an in-use exception is thrown mentioning anchors'
            def thrownException = thrown(DataspaceInUseException)
            assert thrownException.details.contains('contains 1 anchor(s)')
        cleanup:
            cpsModuleService.deleteSchemaSetsWithCascade('targetDataspace',['someSchemaSet'])
            objectUnderTest.deleteDataspace('targetDataspace')
    }

    def 'Attempt to delete dataspace with just a schema set'() {
        setup: 'a dataspace with a schema set'
            objectUnderTest.createDataspace('targetDataspace')
            cpsModuleService.createSchemaSet('targetDataspace','someSchemaSet',[:])
        when: 'attempt to delete dataspace'
            objectUnderTest.deleteDataspace('targetDataspace')
        then: 'an in-use exception is thrown mentioning schema sets'
            def thrownException = thrown(DataspaceInUseException)
            assert thrownException.details.contains('contains 1 schema set(s)')
        cleanup:
            cpsModuleService.deleteSchemaSetsWithCascade('targetDataspace',['someSchemaSet'])
            objectUnderTest.deleteDataspace('targetDataspace')
    }

    def 'Retrieve all dataspaces (depends on total test suite).'() {
        given: 'two addtional dataspaces are created'
            objectUnderTest.createDataspace('dataspace1')
            objectUnderTest.createDataspace('dataspace2')
        when: 'all datespaces are retreived'
            def result = objectUnderTest.getAllDataspaces()
        then: 'there are at least 3 dataspaces (2 new ones plus the general test dataspace)'
            result.size() >= 3
            assert result.name.containsAll([GENERAL_TEST_DATASPACE, 'dataspace1', 'dataspace2'])
    }

    def 'Duplicate dataspaces.'() {
        when: 'attempting to create a dataspace with the same name as an existing one'
            objectUnderTest.createDataspace(GENERAL_TEST_DATASPACE)
        then: 'an exception is thrown indicating the dataspace already exists'
            thrown(AlreadyDefinedException)
    }

    def 'Delete all orphaned data in a dataspace.'() {
        setup: 'an anchor'
            cpsAnchorService.createAnchor(GENERAL_TEST_DATASPACE, BOOKSTORE_SCHEMA_SET, 'testAnchor')
        and: 'orphaned data'
            def jsonDataMap = [:]
            jsonDataMap.put('/bookstore/categories[@code=\'3\']', '{"books":[{"title": "Matilda"}]}')
            jsonDataMap.put('/bookstore/categories[@code=\'3\']', '{"sub-categories":{"code":"1","additional-info":{"info-name":"sample"}}}')
            cpsDataService.updateDataNodesAndDescendants(GENERAL_TEST_DATASPACE, 'testAnchor', jsonDataMap,  OffsetDateTime.now(), ContentType.JSON)
            def dataNodes = cpsDataService.getDataNodes(GENERAL_TEST_DATASPACE, 'testAnchor','/', INCLUDE_ALL_DESCENDANTS)
            assert dataNodes.size() == 1
            assert dataNodes.childDataNodes.size() == 1
        and: 'parent node does not exist'
            assert cpsQueryService.queryDataNodesAcrossAnchors(GENERAL_TEST_DATASPACE, '/bookstore', INCLUDE_ALL_DESCENDANTS, NO_PAGINATION).size() == 0
        when: 'deleting all orphaned data in a dataspace'
            objectUnderTest.deleteAllOrphanedData(GENERAL_TEST_DATASPACE)
        and: 'get data nodes in dataspace'
            cpsDataService.getDataNodes(GENERAL_TEST_DATASPACE, 'testAnchor','/', INCLUDE_ALL_DESCENDANTS)
        then: 'there will be no more data nodes available'
            thrown(DataNodeNotFoundException)
        cleanup:
            cpsAnchorService.deleteAnchor(GENERAL_TEST_DATASPACE, 'testAnchor')
    }

}
