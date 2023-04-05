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

package org.onap.cps.integration.functional

import org.onap.cps.api.CpsDataService
import org.onap.cps.integration.base.FunctionalSpecBase
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.exceptions.AnchorNotFoundException
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.spi.exceptions.DataspaceNotFoundException

import java.time.OffsetDateTime

class CpsDataServiceIntegrationSpec extends FunctionalSpecBase {

    CpsDataService objectUnderTest

    def setup() { objectUnderTest = cpsDataService }

    def 'Read bookstore top-level container(s) using #fetchDescendantsOption.'() {
        when: 'get data nodes for bookstore container'
            def result = objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore', fetchDescendantsOption)
        then: 'the tree consist ouf of #expectNumberOfDataNodes data nodes'
            assert countDataNodesInTree(result) == expectNumberOfDataNodes
        and: 'the top level data node has the expected attribute and value'
            assert result.leaves['bookstore-name'] == ['Easons']
        where: 'the following option is used'
            fetchDescendantsOption                         || expectNumberOfDataNodes
            FetchDescendantsOption.OMIT_DESCENDANTS        || 1
            FetchDescendantsOption.DIRECT_CHILDREN_ONLY    || 6
            FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS || 17
            new FetchDescendantsOption(2)                  || 17
    }

    def 'Read bookstore top-level container(s) has correct dataspace and anchor.'() {
        when: 'get data nodes for bookstore container'
            def result = objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore', FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the correct dataspace was queried'
            assert result.dataspace.toSet() == [FUNCTIONAL_TEST_DATASPACE_1].toSet()
        and: 'the correct anchor was queried'
            assert result.anchorName.toSet() == [BOOKSTORE_ANCHOR_1].toSet()
    }


    def 'Update multiple data node leaves.'() {
        given: 'Updated json for bookstore data'
            def jsonData =  "{'book-store:books':{'lang':'English/French','price':100,'title':'Matilda','authors':['RoaldDahl'],'pub_year':1988}}"
        when: 'update is performed for leaves'
            objectUnderTest.updateNodeLeaves(FUNCTIONAL_TEST_DATASPACE, BOOKSTORE_ANCHOR, "/bookstore/categories[@code='1']", jsonData, OffsetDateTime.now())
        then: 'the updated data nodes are retrieved'
            def result = cpsDataService.getDataNodes(FUNCTIONAL_TEST_DATASPACE, BOOKSTORE_ANCHOR, "/bookstore/categories[@code=1]/books[@title='Matilda']", FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        and: 'the leaf values are updated as expected'
            assert result.leaves['lang'] == ['English/French']
            assert result.leaves['price'] == [100]
    }

    def 'Update multiple data leaves error scenario: #scenario.'() {
        given: 'Updated json for bookstore data'
            def jsonData =  "{'book-store:books':{'lang':'English/French','price':100,'title':'Matilda','authors':['RoaldDahl'],'pub_year':1988}}"
        when: 'attempt to update data node for #scenario'
            objectUnderTest.updateNodeLeaves(dataspaceName, anchorName, xpath, jsonData, OffsetDateTime.now())
        then: 'a #expectedException is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                 | dataspaceName                | anchorName                 | xpath                 || expectedException
            'invalid dataspace name' | 'INVALID DATAsPACE'          | 'not-relevant'             | '/not relevant'       || DataValidationException
            'invalid anchor name'    | FUNCTIONAL_TEST_DATASPACE    | 'INVALID ANCHOR'           | '/not relevant'       || DataValidationException
            'non-existing dataspace' | 'non-existing-dataspace'     | 'not-relevant'             | '/not relevant'       || DataspaceNotFoundException
            'non-existing anchor'    | FUNCTIONAL_TEST_DATASPACE    | 'non-existing-anchor'      | '/not relevant'       || AnchorNotFoundException
    }
}
