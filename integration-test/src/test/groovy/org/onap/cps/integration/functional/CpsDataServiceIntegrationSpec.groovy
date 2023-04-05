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
import org.onap.cps.spi.exceptions.DataspaceNotFoundException
import org.onap.cps.spi.model.DataNode
import org.onap.cps.spi.model.DataNodeBuilder

import java.time.OffsetDateTime

class CpsDataServiceIntegrationSpec extends FunctionalSpecBase {

    CpsDataService objectUnderTest

    def setup() { objectUnderTest = cpsDataService }

    def 'Read bookstore top-level container(s) using #fetchDescendantsOption.'() {
        when: 'get data nodes for bookstore container'
            def result = objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE, BOOKSTORE_ANCHOR, '/bookstore', fetchDescendantsOption)
        then: 'the tree consist ouf of #expectNumberOfDataNodes data nodes'
            assert countDataNodesInTree(result) == expectNumberOfDataNodes
        and: 'the top level data node has the expected attribute and value'
            assert result.leaves['bookstore-name'] == ['Easons']
        where: 'the following option is used'
            fetchDescendantsOption                         || expectNumberOfDataNodes
            FetchDescendantsOption.OMIT_DESCENDANTS        || 1
            FetchDescendantsOption.DIRECT_CHILDREN_ONLY    || 4
            FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS || 8
            new FetchDescendantsOption(2)                  || 8
    }

    def 'Read bookstore top-level container(s) has correct dataspace and anchor.'() {
        when: 'get data nodes for bookstore container'
            def result = objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE, BOOKSTORE_ANCHOR, '/bookstore', FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the correct dataspace was queried'
            assert result.dataspace.toSet() == [FUNCTIONAL_TEST_DATASPACE].toSet()
        and: 'the correct anchor was queried'
            assert result.anchorName.toSet() == [BOOKSTORE_ANCHOR].toSet()
    }


    def 'Update multiple data node leaves.'() {
        given:
            def jsonData =  "{'book-store:books':{'lang':'English/French','price':100,'title':'Matilda','authors':['RoaldDahl'],'pub_year':1988}}"
        when: 'update is performed for leaves'
            objectUnderTest.updateNodeLeaves(FUNCTIONAL_TEST_DATASPACE, BOOKSTORE_ANCHOR, "/bookstore/categories[@code='1']", jsonData, OffsetDateTime.now())
        then: 'the updated data nodes are retrieved'
            def result = cpsDataService.getDataNodes(FUNCTIONAL_TEST_DATASPACE, BOOKSTORE_ANCHOR, "/bookstore/categories[@code=1]/books[@title='Matilda']", FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        and: 'the leaf values are updated as expected'
            assert result.leaves['lang'] == ['English/French']
            assert result.leaves['price'] == [100]
    }
}
