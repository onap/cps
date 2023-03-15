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
import org.onap.cps.spi.exceptions.AlreadyDefinedException
import org.onap.cps.spi.exceptions.AnchorNotFoundException
import org.onap.cps.spi.exceptions.DataspaceNotFoundException

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
            } catch(Exception e) {
                thrown = e
            }
           assert thrown instanceof DataspaceNotFoundException
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
        when: 'a anchor is created'
            objectUnderTest.createAnchor(GENERAL_TEST_DATASPACE, BOOKSTORE_SCHEMA_SET, 'newAnchor')
        then: 'the anchor be read'
            assert objectUnderTest.getAnchor(GENERAL_TEST_DATASPACE, 'newAnchor').name == 'newAnchor'
        and: 'it can be deleted'
            objectUnderTest.deleteAnchor(GENERAL_TEST_DATASPACE,'newAnchor')
        then: 'the anchor no longer exists i.e. an exception is thrown if an attempt is made to retrieve it'
            def thrown = null
            try {
                objectUnderTest.getAnchor(GENERAL_TEST_DATASPACE, 'newAnchor')
            } catch(Exception e) {
                thrown = e
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

    def 'Get anchors from multiple schema set names limit exceeded: 32,766 (~ 2^15) schema set names.'() {
        given: 'more than 32,766 schema set names'
            def schemaSetNames = (0..40_000).collect { "size-of-this-name-does-not-matter-for-limit-" + it }
        when: 'single get is executed to get all the anchors'
            objectUnderTest.getAnchors(GENERAL_TEST_DATASPACE, schemaSetNames)
        then: 'a database exception is not thrown'
            noExceptionThrown()
    }

    def 'Querying anchor names limit exceeded: 32,766 (~ 2^15) modules.'() {
        given: 'more than 32,766 module names'
            def moduleNames = (0..40_000).collect { "size-of-this-name-does-not-matter-for-limit-" + it }
        when: 'single query is executed to get all the anchors'
            objectUnderTest.queryAnchorNames(GENERAL_TEST_DATASPACE, moduleNames)
        then: 'a database exception is not thrown'
            noExceptionThrown()
    }

}
