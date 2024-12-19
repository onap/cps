/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation
 *  Modifications Copyright (C) 2022-2023 TechMahindra Ltd.
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

import org.onap.cps.utils.ContentType

import java.time.OffsetDateTime

abstract class FunctionalSpecBase extends CpsIntegrationSpecBase {

    def static FUNCTIONAL_TEST_DATASPACE_1 = 'functionalTestDataspace1'
    def static FUNCTIONAL_TEST_DATASPACE_2 = 'functionalTestDataspace2'
    def static FUNCTIONAL_TEST_DATASPACE_3 = 'functionalTestDataspace3'
    def static FUNCTIONAL_TEST_DATASPACE_4 = 'functionalTestDataspace4'
    def static NUMBER_OF_ANCHORS_PER_DATASPACE_WITH_BOOKSTORE_DATA = 2
    def static NUMBER_OF_ANCHORS_PER_DATASPACE_WITH_BOOKSTORE_XML_DATA = 1
    def static NUMBER_OF_ANCHORS_PER_DATASPACE_WITH_BOOKSTORE_DELTA_DATA = 1
    def static BOOKSTORE_ANCHOR_1 = 'bookstoreAnchor1'
    def static BOOKSTORE_ANCHOR_2 = 'bookstoreAnchor2'
    def static BOOKSTORE_ANCHOR_3 = 'bookstoreSourceAnchor1'
    def static BOOKSTORE_ANCHOR_4 = 'copyOfSourceAnchor1'
    def static BOOKSTORE_ANCHOR_5 = 'bookstoreAnchorForDeltaReport1'
    def static BOOKSTORE_ANCHOR_6 = 'bookstoreAnchorXml1'

    def static initialized = false
    def static bookstoreJsonData = readResourceDataFile('bookstore/bookstoreData.json')
    def static bookstoreXmlData = readResourceDataFile('bookstore/bookstoreDataForXml.xml')
    def static bookstoreJsonDataForDeltaReport = readResourceDataFile('bookstore/bookstoreDataForDeltaReport.json')

    def setup() {
        if (!initialized) {
            setupBookstoreInfraStructure()
            addBookstoreData()
            addDeltaData()
            addBookstoreXmlData()
            initialized = true
        }
    }

    def setupBookstoreInfraStructure() {
        cpsDataspaceService.createDataspace(FUNCTIONAL_TEST_DATASPACE_1)
        cpsDataspaceService.createDataspace(FUNCTIONAL_TEST_DATASPACE_2)
        cpsDataspaceService.createDataspace(FUNCTIONAL_TEST_DATASPACE_3)
        cpsDataspaceService.createDataspace(FUNCTIONAL_TEST_DATASPACE_4)
        createStandardBookStoreSchemaSet(FUNCTIONAL_TEST_DATASPACE_1)
        createStandardBookStoreSchemaSet(FUNCTIONAL_TEST_DATASPACE_2)
        createStandardBookStoreSchemaSet(FUNCTIONAL_TEST_DATASPACE_3)
        createStandardBookStoreSchemaSet(FUNCTIONAL_TEST_DATASPACE_4)
    }

    def addBookstoreData() {
        addAnchorsWithData(NUMBER_OF_ANCHORS_PER_DATASPACE_WITH_BOOKSTORE_DATA, FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_SCHEMA_SET, 'bookstoreAnchor', bookstoreJsonData)
        addAnchorsWithData(NUMBER_OF_ANCHORS_PER_DATASPACE_WITH_BOOKSTORE_DATA, FUNCTIONAL_TEST_DATASPACE_2, BOOKSTORE_SCHEMA_SET, 'bookstoreAnchor', bookstoreJsonData)
    }

    def addDeltaData() {
        addAnchorsWithData(NUMBER_OF_ANCHORS_PER_DATASPACE_WITH_BOOKSTORE_DELTA_DATA, FUNCTIONAL_TEST_DATASPACE_3, BOOKSTORE_SCHEMA_SET, 'bookstoreSourceAnchor', bookstoreJsonData)
        addAnchorsWithData(NUMBER_OF_ANCHORS_PER_DATASPACE_WITH_BOOKSTORE_DELTA_DATA, FUNCTIONAL_TEST_DATASPACE_3, BOOKSTORE_SCHEMA_SET, 'copyOfSourceAnchor', bookstoreJsonData)
        addAnchorsWithData(NUMBER_OF_ANCHORS_PER_DATASPACE_WITH_BOOKSTORE_DELTA_DATA, FUNCTIONAL_TEST_DATASPACE_3, BOOKSTORE_SCHEMA_SET, 'bookstoreAnchorForDeltaReport', bookstoreJsonDataForDeltaReport)
    }

    def addBookstoreXmlData() {
        addAnchorsWithData(NUMBER_OF_ANCHORS_PER_DATASPACE_WITH_BOOKSTORE_XML_DATA, FUNCTIONAL_TEST_DATASPACE_4, BOOKSTORE_SCHEMA_SET, 'bookstoreAnchorXml', bookstoreXmlData)
    }

    def restoreBookstoreDataAnchor(anchorNumber) {
        def anchorName = 'bookstoreAnchor' + anchorNumber
        cpsAnchorService.deleteAnchor(FUNCTIONAL_TEST_DATASPACE_1, anchorName)
        cpsAnchorService.createAnchor(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_SCHEMA_SET, anchorName)
        cpsDataService.saveData(FUNCTIONAL_TEST_DATASPACE_1, anchorName, bookstoreJsonData.replace('Easons', 'Easons-'+anchorNumber.toString()), OffsetDateTime.now())
    }

    def restoreBookstoreXmlDataAnchor(anchorNumber) {
        def anchorName = 'bookstoreAnchorXml' + anchorNumber
        cpsAnchorService.deleteAnchor(FUNCTIONAL_TEST_DATASPACE_4, anchorName)
        cpsAnchorService.createAnchor(FUNCTIONAL_TEST_DATASPACE_4, BOOKSTORE_SCHEMA_SET, anchorName)
        cpsDataService.saveData(FUNCTIONAL_TEST_DATASPACE_4,
            anchorName,
            bookstoreXmlData,
            OffsetDateTime.now(),
            ContentType.XML)
    }

}
