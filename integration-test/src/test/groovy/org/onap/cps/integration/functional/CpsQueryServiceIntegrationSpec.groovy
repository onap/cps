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

    def 'cps-path query using combinations of operator #scenario.'() {
        when: 'a query is executed to get response by the given cps path'
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE, BOOKSTORE_ANCHOR, cpspath, OMIT_DESCENDANTS)
        then: 'the cps-path of queryDataNodes has the expectedLeaves'
            assert result.size() == expectedResultsize
        where: 'the following data is used'
            scenario                        | cpspath                           | expectedResultsize
            'The "contains" condition'      | '//books[contains(@title,"Mat")]' | 1
            '"contains" with leaf-list'     | '//books[contains(@title,"ti")]'  | 2
            '"contains" with Integer Value' | '//books[contains(@price,"15")]'  | 2
    }
}
