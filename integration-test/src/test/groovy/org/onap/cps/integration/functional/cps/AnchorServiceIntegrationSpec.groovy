/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation
 *  Modifications Copyright (C) 2024 TechMahindra Ltd.
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

import java.time.OffsetDateTime

import org.onap.cps.api.CpsAnchorService
import org.onap.cps.integration.base.FunctionalSpecBase
import org.onap.cps.api.parameters.FetchDescendantsOption
import org.onap.cps.api.exceptions.AlreadyDefinedException
import org.onap.cps.api.exceptions.AnchorNotFoundException
import org.onap.cps.utils.ContentType

class AnchorServiceIntegrationSpec extends FunctionalSpecBase {

    CpsAnchorService objectUnderTest

    def setup() { objectUnderTest = cpsAnchorService }

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
            createStandardBookStoreSchemaSet(GENERAL_TEST_DATASPACE, 'otherSchemaSet')
            objectUnderTest.createAnchor(GENERAL_TEST_DATASPACE, 'otherSchemaSet', 'anchor3')
        then: 'there are 4 anchors in the general test database'
            assert objectUnderTest.getAnchors(GENERAL_TEST_DATASPACE).size() == 4
        and: 'there are 3 anchors associated with bookstore schema set'
            assert objectUnderTest.getAnchorsBySchemaSetName(GENERAL_TEST_DATASPACE, BOOKSTORE_SCHEMA_SET).size() == 3
        and: 'there is 1 anchor associated with other schema set'
            assert objectUnderTest.getAnchorsBySchemaSetName(GENERAL_TEST_DATASPACE, 'otherSchemaSet').size() == 1
    }

    def 'Querying anchor(name)s (depends on previous test!).'() {
        expect: 'there are now 4 anchors using the "stores" module (both schema sets use the same modules) '
            assert objectUnderTest.queryAnchorNames(GENERAL_TEST_DATASPACE, ['stores', 'bookstore-types']).size() == 4
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

    def 'Query anchors without any known modules'() {
        when: 'querying for anchors with #scenario'
            def result = objectUnderTest.queryAnchorNames(GENERAL_TEST_DATASPACE, ['unknownModule'])
        then: 'an empty result is returned (no error)'
            assert result == []
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
            def branchName = cpsDataService.getDataNodes(GENERAL_TEST_DATASPACE, 'anchor4', "/test-tree/branch", FetchDescendantsOption.DIRECT_CHILDREN_ONLY)[0].leaves['name']
            assert branchName == 'left'
        and: 'a another schema set with updated tree yang model is created'
            def updatedTreeYangModelAsString = readResourceDataFile('tree/updated-test-tree.yang')
            cpsModuleService.createSchemaSet(GENERAL_TEST_DATASPACE, 'anotherTreeSchemaSet', [tree: updatedTreeYangModelAsString])
        and: 'anchor4 schema set is updated with another schema set successfully'
            objectUnderTest.updateAnchorSchemaSet(GENERAL_TEST_DATASPACE, 'anchor4', 'anotherTreeSchemaSet')
        when: 'updated tree data node with new leaves'
            def updatedTreeJsonData = readResourceDataFile('tree/updated-test-tree.json')
            cpsDataService.updateNodeLeaves(GENERAL_TEST_DATASPACE, "anchor4", "/test-tree/branch[@name='left']", updatedTreeJsonData, OffsetDateTime.now(), ContentType.JSON)
        then: 'updated tree data node can be retrieved by its normalized xpath'
            def birdsName = cpsDataService.getDataNodes(GENERAL_TEST_DATASPACE, 'anchor4',"/test-tree/branch[@name='left']/nest", FetchDescendantsOption.DIRECT_CHILDREN_ONLY)[0].leaves['birds'] as List
            assert birdsName.size() == 3
            assert birdsName.containsAll('Night Owl', 'Raven', 'Crow')
    }
}
