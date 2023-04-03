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

import org.onap.cps.integration.base.FunctionalSpecBase
import org.onap.cps.spi.FetchDescendantsOption

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS

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

    def 'Query bookstore across anchors using CPS path where #scenario.'() {
        when: 'query data nodes for bookstore container'
            def result = objectUnderTest.queryDataNodesAcrossAnchors(FUNCTIONAL_TEST_DATASPACE, cpsPath, includeDescendantsOption)
        then: 'the result contains expected number of nodes'
            assert result.size() == expectedResultSize
        and : 'correct anchors are queried'
            assert result.anchorName.containsAll(expectedAnchors)
        where:
        scenario                                        | cpsPath                                   | includeDescendantsOption  || expectedResultSize || expectedAnchors
        'the and condition is used'                     | '//books[@lang="English" and @price=15]'  | INCLUDE_ALL_DESCENDANTS   || 4                  || ['bookstoreAnchor', 'bookstoreAnchor2']
        'the and condition is used with no descendants' | '//books[@lang="English" and @price=15]'  | OMIT_DESCENDANTS          || 4                  || ['bookstoreAnchor', 'bookstoreAnchor2']
        'root level node is queried'                    | '/bookstore'                              | INCLUDE_ALL_DESCENDANTS   || 2                  || ['bookstoreAnchor', 'bookstoreAnchor2']
        'root level node is queried with no descendants'| '/bookstore'                              | OMIT_DESCENDANTS          || 2                  || ['bookstoreAnchor', 'bookstoreAnchor2']
        'the and is used where result does not exist'   | '//books[@lang="English" and @price=1000]'| INCLUDE_ALL_DESCENDANTS   || 0                  || []
    }
}
