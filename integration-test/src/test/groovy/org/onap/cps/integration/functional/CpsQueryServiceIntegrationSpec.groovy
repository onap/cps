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
import org.onap.cps.spi.FetchDescendantsOption

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

    def 'Cps Path query using descendant anywhere with #scenario.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE, BOOKSTORE_ANCHOR, cpsPath, OMIT_DESCENDANTS)
        then: 'xpaths of the retrieved data nodes are as expected'
            assert result.xpath.sort() == expectedXPaths.sort()
        where: 'the following data is used'
            scenario                                                  | cpsPath                                                           || expectedXPaths
            // TODO Move this case to a separate test
            'descendant name match end of other node'                 | '//books'                                                         || ["/bookstore/categories[@code='1']/books[@title='Matilda']",
                                                                                                                                             "/bookstore/categories[@code='1']/books[@title='The Gruffalo']",
                                                                                                                                             "/bookstore/categories[@code='2']/books[@title='Annihilation']",
                                                                                                                                             "/bookstore/categories[@code='3']/books[@title='Good Omens']",
                                                                                                                                             "/bookstore/categories[@code='3']/books[@title='A Book with No Language']",
                                                                                                                                             "/bookstore/categories[@code='4']/books[@title='Debian GNU/Linux']",
                                                                                                                                             "/bookstore/categories[@code='5']/books[@title='Catching Fire: The Hunger Games [Book 2]']"]
            'descendant with string leaf condition'                   | '//categories[@code="2"]'                                         || ["/bookstore/categories[@code='2']"]
            'descendant with text condition on leaf'                  | '//books/title[text()="Matilda"]'                                 || ["/bookstore/categories[@code='1']/books[@title='Matilda']"]
            'descendant with text condition case mismatch'            | '//books/title[text()="matilda"]'                                 || []
            'descendant with text condition on int leaf'              | '//books/price[text()="10"]'                                      || ["/bookstore/categories[@code='1']/books[@title='Matilda']"]
            'descendant with text condition on leaf-list'             | '//books/authors[text()="Terry Pratchett"]'                       || ["/bookstore/categories[@code='3']/books[@title='Good Omens']"]
            'descendant with text condition partial match'            | '//books/authors[text()="Terry"]'                                 || []
            'descendant with text condition (existing) empty string'  | '//books/lang[text()=""]'                                         || ["/bookstore/categories[@code='3']/books[@title='A Book with No Language']"]
            // TODO bookstore.yaml model does not have int leaf-list
            // 'descendant with text condition on int leaf-list'         | '//books/editions[text()="2000"]'                                 || ["/bookstore/categories[@code='2']/book"]
            'descendant name match of leaf containing /'              | '//books[@lang="N/A"]'                                            || ["/bookstore/categories[@code='4']/books[@title='Logarithm tables']"]
            'descendant with text condition on leaf containing /'     | '//books/lang[text()="N/A"]'                                      || ["/bookstore/categories[@code='4']/books[@title='Logarithm tables']"]
            'descendant name match of key containing /'               | '//books[@title="Debian GNU/Linux"]'                              || ["/bookstore/categories[@code='4']/books[@title='Debian GNU/Linux']"]
            'descendant with text condition on key containing /'      | '//books/title[text()="Debian GNU/Linux"]'                        || ["/bookstore/categories[@code='4']/books[@title='Debian GNU/Linux']"]
            // TODO These cases fail
            // 'descendant name match of leaf containing ['              | '//books[@author="Joe Bloggs [author]"]'                          || ["/bookstore/categories[@code='4']/books[@title='Logarithm tables']"]
            // 'descendant with text condition on leaf containing ['     | '//books/author[text()="Joe Bloggs [author]"]'                    || ["/bookstore/categories[@code='4']/books[@title='Logarithm tables']"]
            // 'descendant name match of key containing ['               | '//books[@title="Catching Fire: The Hunger Games [Book 2]"]'      || ["/bookstore/categories[@code='5']/books[@title='Catching Fire: The Hunger Games [Book 2]']"]
            // 'descendant with text condition on key containing ['      | '//books/title[text()="Catching Fire: The Hunger Games [Book 2]"]'|| ["/bookstore/categories[@code='5']/books[@title='Catching Fire: The Hunger Games [Book 2]']"]
    }

}
