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

import java.time.OffsetDateTime

class CpsDataServiceIntegrationSpec extends FunctionalSpecBase {

    CpsDataService objectUnderTest
    def originalCountBookstoreChildNodes

    def setup() {
        objectUnderTest = cpsDataService
        originalCountBookstoreChildNodes = countDataNodesInTree(objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore', FetchDescendantsOption.DIRECT_CHILDREN_ONLY))
    }

    def 'Read bookstore top-level container(s) using #fetchDescendantsOption.'() {
        when: 'get data nodes for bookstore container'
            def result = objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore', fetchDescendantsOption)
        then: 'the tree consist ouf of #expectNumberOfDataNodes data nodes'
            assert countDataNodesInTree(result) == expectNumberOfDataNodes
        and: 'the top level data node has the expected attribute and value'
            assert result.leaves['bookstore-name'] == ['Easons']
        and: 'they are from the correct dataspace'
            assert result.dataspace == [FUNCTIONAL_TEST_DATASPACE_1]
        and: 'they are from the correct anchor'
            assert result.anchorName == [BOOKSTORE_ANCHOR_1]
        where: 'the following option is used'
            fetchDescendantsOption                         || expectNumberOfDataNodes
            FetchDescendantsOption.OMIT_DESCENDANTS        || 1
            FetchDescendantsOption.DIRECT_CHILDREN_ONLY    || 6
            FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS || 17
            new FetchDescendantsOption(2)                  || 17
    }

    def 'Add and Delete a (container) datanode.'() {
        given: 'new (webinfo) datanode'
            def json = '{"webinfo": {"domain-name":"ourbookstore.com" ,"contact-email":"info@ourbookstore.com" }}'
        when: 'the new datanode is saved'
            objectUnderTest.saveData(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1 , '/bookstore', json, OffsetDateTime.now())
        then: 'it can be retrieved by its xpath'
            def result = objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/webinfo', FetchDescendantsOption.DIRECT_CHILDREN_ONLY)
            assert result.size() == 1
            assert result[0].xpath == '/bookstore/webinfo'
        and: 'there is now one extra datanode'
            assert originalCountBookstoreChildNodes + 1 == countDataNodesInTree(objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore', FetchDescendantsOption.DIRECT_CHILDREN_ONLY))
        when: 'the new datanode is deleted'
            objectUnderTest.deleteDataNode(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/webinfo', OffsetDateTime.now())
        then: 'the original number of datanodes is restored'
            assert originalCountBookstoreChildNodes == countDataNodesInTree(objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore', FetchDescendantsOption.DIRECT_CHILDREN_ONLY))
    }

    def 'Add and Delete list (element) datanodes.'() {
        given: 'two new (categories) datanodes'
            def json = '{"categories": [ {"code":"new1"}, {"code":"new2" } ] }'
        when: 'the new list elements are saved'
            objectUnderTest.saveListElements(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1 , '/bookstore', json, OffsetDateTime.now())
        then: 'they can be retrieved by their xpaths'
            objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/categories[@code="new1"]', FetchDescendantsOption.DIRECT_CHILDREN_ONLY).size() == 1
            objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/categories[@code="new2"]', FetchDescendantsOption.DIRECT_CHILDREN_ONLY).size() == 1
        and: 'there are now two extra datanodes'
            assert originalCountBookstoreChildNodes + 2 == countDataNodesInTree(objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore', FetchDescendantsOption.DIRECT_CHILDREN_ONLY))
        when: 'the new elements are deleted'
            objectUnderTest.deleteDataNode(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/categories[@code="new1"]', OffsetDateTime.now())
            objectUnderTest.deleteDataNode(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/categories[@code="new2"]', OffsetDateTime.now())
        then: 'the original number of datanodes is restored'
            assert originalCountBookstoreChildNodes == countDataNodesInTree(objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore', FetchDescendantsOption.DIRECT_CHILDREN_ONLY))
    }

    def 'Add and Delete a batch of lists (element) datanodes.'() {
        given: 'two new (categories) datanodes in two separate batches'
            def json1 = '{"categories": [ {"code":"new1"} ] }'
            def json2 = '{"categories": [ {"code":"new2"} ] }'
        when: 'the batches of new list element(s) are saved'
            objectUnderTest.saveListElementsBatch(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1 , '/bookstore', [json1, json2], OffsetDateTime.now())
        then: 'they can be retrieved by their xpaths'
            objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/categories[@code="new1"]', FetchDescendantsOption.DIRECT_CHILDREN_ONLY).size() == 1
            objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/categories[@code="new2"]', FetchDescendantsOption.DIRECT_CHILDREN_ONLY).size() == 1
        and: 'there are now two extra datanodes'
            assert originalCountBookstoreChildNodes + 2 == countDataNodesInTree(objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore', FetchDescendantsOption.DIRECT_CHILDREN_ONLY))
        when: 'the new elements are deleted'
            objectUnderTest.deleteDataNode(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/categories[@code="new1"]', OffsetDateTime.now())
            objectUnderTest.deleteDataNode(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/categories[@code="new2"]', OffsetDateTime.now())
        then: 'the original number of datanodes is restored'
            assert originalCountBookstoreChildNodes == countDataNodesInTree(objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore', FetchDescendantsOption.DIRECT_CHILDREN_ONLY))
    }

}
