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

import org.onap.cps.integration.base.FunctionalSpecBase
import org.onap.cps.spi.FetchDescendantsOption

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS

class CpsQueryServiceIntegrationSpec extends FunctionalSpecBase {

    def objectUnderTest

    def setup() { objectUnderTest = cpsQueryService }

    def 'Query bookstore using CPS path where #scenario.'() {
        when: 'query data nodes for bookstore container'
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE, BOOKSTORE_ANCHOR, cpsPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the result contains expected number of nodes'
            assert result.size() == expectedResultSize
        and: 'the result contains the expected leaf values'
            result.leaves.forEach( dataNodeLeaves -> {
                expectedLeaves.forEach( (expectedLeafKey,expectedLeafValue) -> {
                    assert dataNodeLeaves[expectedLeafKey] == expectedLeafValue
                })
            })
        where:
            scenario                                      | cpsPath                                    || expectedResultSize | expectedLeaves
            'the and condition is used'                   | '//books[@lang="English" and @price=15]'   || 2                  | [lang:"English", price:15]
            'the and is used where result does not exist' | '//books[@lang="English" and @price=1000]' || 0                  | []
    }

    def 'Cps Path queries with all descendants including descendants that are list entries: #scenario.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE, BOOKSTORE_ANCHOR, cpsPath, INCLUDE_ALL_DESCENDANTS)
        then: 'correct number of datanodes are returned'
            assert countDataNodesInTree(result) == expectedNumberOfDataNodes
        where: 'the following cps paths are used'
            scenario                              | cpsPath                                 || expectedNumberOfDataNodes
            'absolute path all list entries'      | '/bookstore/categories'                 || 7
            'absolute path 1 list entry by key'   | '/bookstore/categories[@code="3"]'      || 2
            'absolute path 1 list entry by leaf'  | '/bookstore/categories[@name="Comedy"]' || 2
            'relative path all list entries'      | '//categories'                          || 7
            'relative path 1 list entry by key'   | '//categories[@code="3"]'               || 2
            'relative path 1 list entry by leaf'  | '//categories[@name="Comedy"]'          || 2
            'incomplete absolute path'            | '/categories'                           || 0
            'incomplete absolute path list entry' | '/categories[@code="3"]'                || 0
    }
}
