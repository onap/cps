/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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

package org.onap.cps.integration.functional

import org.onap.cps.api.CpsAdminService
import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.exceptions.AlreadyDefinedException
import org.onap.cps.spi.exceptions.AnchorNotFoundException
import org.onap.cps.spi.exceptions.DataspaceInUseException
import org.onap.cps.spi.exceptions.DataspaceNotFoundException
import java.time.OffsetDateTime

class CpsAdminServiceIntegrationSpec extends CpsIntegrationSpecBase {

    CpsAdminService objectUnderTest

    def setup() { objectUnderTest = cpsAdminService }

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

    def 'Delete dataspace with error; #scenario.'() {
        setup: 'add some anchors if needed'
            numberOfAnchors.times {
                objectUnderTest.createAnchor(GENERAL_TEST_DATASPACE, BOOKSTORE_SCHEMA_SET, 'anchor' + it)
            }
        when: 'attempt to delete dataspace'
            objectUnderTest.deleteDataspace(dataspaceName)
        then: 'the correct exception is thrown with the relevant details'
            def thrownException = thrown(expectedException)
            thrownException.details.contains(expectedMessageDetails)
        cleanup:
            numberOfAnchors.times {
                objectUnderTest.deleteAnchor(GENERAL_TEST_DATASPACE, 'anchor' + it)
            }
        where: 'the following data is used'
            scenario                        | dataspaceName          | numberOfAnchors || expectedException          | expectedMessageDetails
            'dataspace name does not exist' | 'unknown'              | 0               || DataspaceNotFoundException | 'unknown does not exist'
            'dataspace contains schemasets' | GENERAL_TEST_DATASPACE | 0               || DataspaceInUseException    | 'contains 1 schemaset(s)'
            'dataspace contains anchors'    | GENERAL_TEST_DATASPACE | 2               || DataspaceInUseException    | 'contains 2 anchor(s)'
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

    def 'Anchor CRUD operations.'() {
        when: 'an anchor is created'
            objectUnderTest.createAnchor(GENERAL_TEST_DATASPACE, BOOKSTORE_SCHEMA_SET, 'newAnchor')
        then: 'the anchor be read'
            assert objectUnderTest.getAnchor(GENERAL_TEST_DATASPACE, 'newAnchor').name == 'newAnchor'
        and: 'it can be deleted'
            objectUnderTest.deleteAnchor(GENERAL_TEST_DATASPACE,'newAnchor')
        then: 'the anchor no longer exists i.e. an exception is thrown if an attempt is made to retrieve it'
            def thrown = null
            try {
                objectUnderTest.getAnchor(GENERAL_TEST_DATASPACE, 'newAnchor')
            } catch(Exception exception) {
                thrown = exception
            }
            assert thrown instanceof AnchorNotFoundException
    }

    def 'Filtering multiple anchors.'() {
        when: '2 anchors with bookstore schema set are created'
            objectUnderTest.createAnchor(GENERAL_TEST_DATASPACE, BOOKSTORE_SCHEMA_SET, 'anchor1')
            objectUnderTest.createAnchor(GENERAL_TEST_DATASPACE, BOOKSTORE_SCHEMA_SET, 'anchor2')
        and: '1 anchor with "other" schema set is created'
            def bookstoreModelFileContent = readResourceDataFile('bookstore/bookstore.yang')
            cpsModuleService.createSchemaSet(GENERAL_TEST_DATASPACE, 'otherSchemaSet', [someFileName: bookstoreModelFileContent])
            objectUnderTest.createAnchor(GENERAL_TEST_DATASPACE, 'otherSchemaSet', 'anchor3')
        then: 'there are 3 anchors in the general test database'
            assert objectUnderTest.getAnchors(GENERAL_TEST_DATASPACE).size() == 3
        and: 'there are 2 anchors associated with bookstore schema set'
            assert objectUnderTest.getAnchors(GENERAL_TEST_DATASPACE, BOOKSTORE_SCHEMA_SET).size() == 2
        and: 'there is 1 anchor associated with other schema set'
            assert objectUnderTest.getAnchors(GENERAL_TEST_DATASPACE, 'otherSchemaSet').size() == 1
    }

    def 'Querying anchor(name)s (depends on previous test!).'() {
        expect: 'there are now 3 anchors using the "stores" module (both schema sets use the same modules) '
            assert objectUnderTest.queryAnchorNames(GENERAL_TEST_DATASPACE, ['stores']).size() == 3
        and: 'there are no anchors using both "stores" and a "unused-model"'
            assert objectUnderTest.queryAnchorNames(GENERAL_TEST_DATASPACE, ['stores', 'unused-model']).size() == 0
    }

    def 'Duplicate anchors.'() {
        given: 'an anchor is created'
            objectUnderTest.createAnchor(GENERAL_TEST_DATASPACE, BOOKSTORE_SCHEMA_SET, 'newAnchor')
        when: 'attempt to create another anchor with the same name'
            objectUnderTest.createAnchor(GENERAL_TEST_DATASPACE, BOOKSTORE_SCHEMA_SET, 'newAnchor')
        then: 'an exception is thrown that the anchor already is defined'
            thrown(AlreadyDefinedException)
        cleanup:
            objectUnderTest.deleteAnchor(GENERAL_TEST_DATASPACE, 'newAnchor')
    }

    def 'Query anchors without any known modules and #scenario'() {
        when: 'querying for anchors with #scenario'
            def result = objectUnderTest.queryAnchorNames(dataspaceName, ['unknownModule'])
        then: 'an empty result is returned (no error)'
            assert result == []
        where:
           scenario                 | dataspaceName
           'non existing database'  | 'nonExistingDataspace'
           'just unknown module(s)' | GENERAL_TEST_DATASPACE
    }

    def 'Update anchor schema set.'() {
        when: 'a new schema set with tree yang model is created'
            def newTreeYangModelAsString = readResourceDataFile('tree/new-test-tree.yang')
            cpsModuleService.createSchemaSet(GENERAL_TEST_DATASPACE, 'newTreeSchemaSet', [tree: newTreeYangModelAsString])
        then: 'an anchor with new schema set is created'
            objectUnderTest.createAnchor(GENERAL_TEST_DATASPACE, 'newTreeSchemaSet', 'anchor4')
        and: 'the new tree datanode is saved'
            def treeJsonData = readResourceDataFile('tree/new-test-tree.json')
            cpsDataService.saveData(GENERAL_TEST_DATASPACE, 'anchor4', treeJsonData, OffsetDateTime.now())
        and: 'saved tree data node can be retrieved by its normalized xpath'
            def branchName = cpsDataService.getDataNodes(GENERAL_TEST_DATASPACE, 'anchor4', "/test-tree/branch",
                    FetchDescendantsOption.DIRECT_CHILDREN_ONLY)[0].leaves['name']
            assert branchName == 'left'
        and: 'a another schema set with updated tree yang model is created'
            def updatedTreeYangModelAsString = readResourceDataFile('tree/updated-test-tree.yang')
            cpsModuleService.createSchemaSet(GENERAL_TEST_DATASPACE, 'anotherTreeSchemaSet', [tree: updatedTreeYangModelAsString])
        and: 'anchor4 schema set is updated with another schema set successfully'
            def isAnchorUpdated = objectUnderTest.updateAnchorSchemaSet(GENERAL_TEST_DATASPACE, 'anchor4', 'anotherTreeSchemaSet')
            assert isAnchorUpdated
        when: 'updated tree data node is saved'
            def updatedTreeJsonData = readResourceDataFile('tree/updated-test-tree.json')
            cpsDataService.updateNodeLeaves(GENERAL_TEST_DATASPACE, "anchor4", "/test-tree/branch[@name='left']", updatedTreeJsonData, OffsetDateTime.now())
        then: 'updated tree data node can be retrieved by its normalized xpath'
            def resultAfterUpdate = cpsDataService.getDataNodes(GENERAL_TEST_DATASPACE, 'anchor4', "/test-tree/branch[@name='left']/nest", FetchDescendantsOption.DIRECT_CHILDREN_ONLY)[0].leaves['birds']
            assert resultAfterUpdate as String == '[Raven, Night Owl, Crow]'
    }
}
