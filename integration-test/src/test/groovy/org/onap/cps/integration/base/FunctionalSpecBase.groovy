/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
 *  Modifications Copyright (C) 2023 TechMahindra Ltd.
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

package org.onap.cps.integration.base

import java.time.OffsetDateTime

class FunctionalSpecBase extends CpsIntegrationSpecBase {

    def static initialized = false

    def setup() {
        if (!initialized) {
            setupBookstoreInfraStructure()
            addBookstoreData()
            initialized = true
        }
    }

    def setupBookstoreInfraStructure() {
        cpsAdminService.createDataspace(FUNCTIONAL_TEST_DATASPACE)
        def bookstoreYangModelAsString = readResourceDataFile('bookstore/bookstore.yang')
        cpsModuleService.createSchemaSet(FUNCTIONAL_TEST_DATASPACE, BOOKSTORE_SCHEMA_SET, [bookstore: bookstoreYangModelAsString])
        cpsAdminService.createAnchor(FUNCTIONAL_TEST_DATASPACE, BOOKSTORE_SCHEMA_SET, BOOKSTORE_ANCHOR)
        cpsAdminService.createAnchor(FUNCTIONAL_TEST_DATASPACE, BOOKSTORE_SCHEMA_SET, BOOKSTORE_ANCHOR_2)
    }

    def addBookstoreData() {
        def bookstoreJsonData = readResourceDataFile('bookstore/bookstoreData.json')
        cpsDataService.saveData(FUNCTIONAL_TEST_DATASPACE, BOOKSTORE_ANCHOR, bookstoreJsonData, OffsetDateTime.now())
        cpsDataService.saveData(FUNCTIONAL_TEST_DATASPACE, BOOKSTORE_ANCHOR_2, bookstoreJsonData, OffsetDateTime.now())
    }

}
