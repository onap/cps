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

import org.onap.cps.api.CpsQueryService
import org.onap.cps.integration.base.FunctionalSpecBase

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS

class CpsQueryServiceIntegrationSpec extends FunctionalSpecBase {

    CpsQueryService objectUnderTest

    def setup() { objectUnderTest = cpsQueryService }

    def 'Query bookstore using CPS path where #scenario.'() {
        when: 'query data nodes for bookstore container'
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE, BOOKSTORE_ANCHOR, cpsPath, INCLUDE_ALL_DESCENDANTS)
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
            'the AND condition is used'                   | '//books[@lang="English" and @price=15]'   || 2                  | [lang:"English", price:15]
            'the AND is used where result does not exist' | '//books[@lang="English" and @price=1000]' || 0                  | []
    }

    def 'Cps Path query using descendant anywhere with #scenario condition for a container element.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE, BOOKSTORE_ANCHOR, cpsPath, OMIT_DESCENDANTS)
        then: 'xpaths of the retrieved data nodes are as expected'
            assert result.xpath.sort() == expectedXPaths.sort()
        where: 'the following data is used'
            scenario                   | cpsPath                                                     || expectedXPaths
            'one leaf'                 | '//books[@pub_year=1986]'                                   || ["/bookstore/categories[@code='3']/books[@title='The Light Fantastic']"]
            'one text'                 | '//books/authors[text()="Terry Pratchett"]'                 || ["/bookstore/categories[@code='3']/books[@title='Good Omens']",
                                                                                                         "/bookstore/categories[@code='3']/books[@title='The Colour of Magic']",
                                                                                                         "/bookstore/categories[@code='3']/books[@title='The Light Fantastic']"]
            'more than one leaf'       | '//books[@price=12 and @pub_year=1983]'                     || ["/bookstore/categories[@code='3']/books[@title='The Colour of Magic']"]
            'leaves reversed in order' | '//books[@pub_year=1983 and @price=12]'                     || ["/bookstore/categories[@code='3']/books[@title='The Colour of Magic']"]
            'leaf and text'            | '//books[@pub_year=1986]/authors[text()="Terry Pratchett"]' || ["/bookstore/categories[@code='3']/books[@title='The Light Fantastic']"]
    }

}
