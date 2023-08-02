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

package org.onap.cps.integration.base

import java.time.OffsetDateTime

class FunctionalSpecBase extends CpsIntegrationSpecBase {

    def static FUNCTIONAL_TEST_DATASPACE_1 = 'functionalTestDataspace1'
    def static FUNCTIONAL_TEST_DATASPACE_2 = 'functionalTestDataspace2'
    def static FUNCTIONAL_TEST_DATASPACE_3 = 'functionalTestDataspace3'
    def static NUMBER_OF_ANCHORS_PER_DATASPACE_WITH_BOOKSTORE_DATA = 2
    def static NUMBER_OF_ANCHORS_PER_DATASPACE_WITH_BOOKSTORE_DELTA_DATA = 1
    def static BOOKSTORE_ANCHOR_1 = 'bookstoreAnchor1'
    def static BOOKSTORE_ANCHOR_2 = 'bookstoreAnchor2'
    def static BOOKSTORE_ANCHOR_3 = 'bookstoreReferenceAnchor1'
    def static BOOKSTORE_ANCHOR_4 = 'bookstoreComparandAnchor1'
    def static BOOKSTORE_ANCHOR_5 = 'copyOfReferenceAnchor1'
    def static EMPTY_BOOKSTORE_DATA = 'emptyData1'

    def static initialized = false
    def static bookstoreJsonData = readResourceDataFile('bookstore/bookstoreData.json')
    def static bookstoreJsonDataForDelta = readResourceDataFile('bookstore/bookstoreDeltaData.json')
    def static emptyBookstoreData = readResourceDataFile('bookstore/emptyData.json')

    def setup() {
        if (!initialized) {
            setupBookstoreInfraStructure()
            addBookstoreData()
            addDeltaData()
            initialized = true
        }
    }

    def setupBookstoreInfraStructure() {
        cpsAdminService.createDataspace(FUNCTIONAL_TEST_DATASPACE_1)
        cpsAdminService.createDataspace(FUNCTIONAL_TEST_DATASPACE_2)
        cpsAdminService.createDataspace(FUNCTIONAL_TEST_DATASPACE_3)
        def bookstoreYangModelAsString = readResourceDataFile('bookstore/bookstore.yang')
        cpsModuleService.createSchemaSet(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_SCHEMA_SET, [bookstore: bookstoreYangModelAsString])
        cpsModuleService.createSchemaSet(FUNCTIONAL_TEST_DATASPACE_2, BOOKSTORE_SCHEMA_SET, [bookstore: bookstoreYangModelAsString])
        cpsModuleService.createSchemaSet(FUNCTIONAL_TEST_DATASPACE_3, BOOKSTORE_SCHEMA_SET, [bookstore: bookstoreYangModelAsString])

    }

    def addBookstoreData() {
        addAnchorsWithData(NUMBER_OF_ANCHORS_PER_DATASPACE_WITH_BOOKSTORE_DATA, FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_SCHEMA_SET, 'bookstoreAnchor', bookstoreJsonData)
        addAnchorsWithData(NUMBER_OF_ANCHORS_PER_DATASPACE_WITH_BOOKSTORE_DATA, FUNCTIONAL_TEST_DATASPACE_2, BOOKSTORE_SCHEMA_SET, 'bookstoreAnchor', bookstoreJsonData)
    }

    def addDeltaData() {
        addAnchorsWithData(NUMBER_OF_ANCHORS_PER_DATASPACE_WITH_BOOKSTORE_DELTA_DATA, FUNCTIONAL_TEST_DATASPACE_3, BOOKSTORE_SCHEMA_SET, 'bookstoreReferenceAnchor', bookstoreJsonData)
        addAnchorsWithData(NUMBER_OF_ANCHORS_PER_DATASPACE_WITH_BOOKSTORE_DELTA_DATA, FUNCTIONAL_TEST_DATASPACE_3, BOOKSTORE_SCHEMA_SET, 'bookstoreComparandAnchor', bookstoreJsonDataForDelta)
        addAnchorsWithData(NUMBER_OF_ANCHORS_PER_DATASPACE_WITH_BOOKSTORE_DELTA_DATA, FUNCTIONAL_TEST_DATASPACE_3, BOOKSTORE_SCHEMA_SET, 'copyOfReferenceAnchor', bookstoreJsonData)
        addAnchorsWithData(NUMBER_OF_ANCHORS_PER_DATASPACE_WITH_BOOKSTORE_DELTA_DATA, FUNCTIONAL_TEST_DATASPACE_3, BOOKSTORE_SCHEMA_SET, 'emptyData', emptyBookstoreData)
    }

    def restoreBookstoreDataAnchor(anchorNumber) {
        def anchorName = 'bookstoreAnchor' + anchorNumber
        cpsAdminService.deleteAnchor(FUNCTIONAL_TEST_DATASPACE_1, anchorName)
        cpsAdminService.createAnchor(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_SCHEMA_SET, anchorName)
        cpsDataService.saveData(FUNCTIONAL_TEST_DATASPACE_1, anchorName, bookstoreJsonData.replace("Easons", "Easons-"+anchorNumber.toString()), OffsetDateTime.now())
    }

}
