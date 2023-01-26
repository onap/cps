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

package org.onap.cps.integration

import org.onap.cps.spi.FetchDescendantsOption

class CpsPersistenceSpec extends CpsIntegrationSpecBase{

    def 'Test creation of test data'() {
        when: 'A dataspace, schema set and anchor are persisted'
            createDataspaceSchemaSetAnchor(TEST_DATASPACE, BOOKSTORE_SCHEMA_SET, 'bookstore.yang', TEST_ANCHOR)
        and: 'data nodes are persisted under the created anchor'
            saveDataNodes(TEST_DATASPACE, TEST_ANCHOR, '/', 'BookstoreDataNodes.json')
        then: 'The dataspace has been persisted successfully'
            cpsAdminService.getDataspace(TEST_DATASPACE).getName() == TEST_DATASPACE
        and: 'The schema set has been persisted successfully'
            cpsModuleService.getSchemaSet(TEST_DATASPACE, BOOKSTORE_SCHEMA_SET).getName() == BOOKSTORE_SCHEMA_SET
        and: 'The anchor has been persisted successfully'
            cpsAdminService.getAnchor(TEST_DATASPACE, TEST_ANCHOR).getName() == TEST_ANCHOR
        and: 'The data nodes have been persisted successfully'
            cpsDataService.getDataNode(TEST_DATASPACE, TEST_ANCHOR, '/bookstore', FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS).xpath == '/bookstore'
    }

    def 'Test deletion of all test data'() {
        when: 'delete all from test dataspace method is called'
            deleteAllFromTestDataspace()
        and: 'the test dataspace is deleted'
            cpsAdminService.deleteDataspace(TEST_DATASPACE)
        then: 'there is no test dataspace'
            !cpsAdminService.getAllDataspaces().contains(TEST_DATASPACE)
    }

    def 'Read test for persisted data nodes'() {
        given:'There is a test dataspace created'
            cpsAdminService.createDataspace(TEST_DATASPACE)
        and: 'There is a schema set and anchor for the test dataspace'
            createSchemaSetAnchor(TEST_DATASPACE, 'bookstoreSchemaSet', 'bookstore.yang', TEST_ANCHOR)
        when: 'data is persisted to the database'
            saveDataNodes(TEST_DATASPACE, TEST_ANCHOR, "/", "BookstoreDataNodes.json")
        then: 'the correct data is saved'
            cpsDataService.getDataNode(TEST_DATASPACE, TEST_ANCHOR, '/bookstore', FetchDescendantsOption.OMIT_DESCENDANTS).leaves['bookstore-name'] == 'Easons'
    }
}
