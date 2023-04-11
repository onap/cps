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
import org.onap.cps.spi.exceptions.CpsPathException

import static org.onap.cps.spi.FetchDescendantsOption.DIRECT_CHILDREN_ONLY
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

    def 'Cps Path query for leaf value(s) with #scenario.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE, BOOKSTORE_ANCHOR, cpsPath, fetchDescendantsOption)
        then: 'the correct number of parent nodes are returned'
            assert result.size() == expectedNumberOfParentNodes
        and: 'the correct total number of data nodes are returned'
            assert countDataNodesInTree(result) == expectedTotalNumberOfNodes
        where: 'the following data is used'
            scenario                               | cpsPath                                                    | fetchDescendantsOption         || expectedNumberOfParentNodes | expectedTotalNumberOfNodes
            'string and no descendants'            | '/bookstore/categories[@code="1"]/books[@title="Matilda"]' | OMIT_DESCENDANTS               || 1                           | 1
            'integer and descendants'              | '/bookstore/categories[@code="1"]/books[@price=15]'        | INCLUDE_ALL_DESCENDANTS        || 1                           | 1
            'no condition and no descendants'      | '/bookstore/categories'                                    | OMIT_DESCENDANTS               || 4                           | 4
            'no condition and level 1 descendants' | '/bookstore'                                               | new FetchDescendantsOption(1)  || 1                           | 6
            'no condition and level 2 descendants' | '/bookstore'                                               | new FetchDescendantsOption(2)  || 1                           | 17
    }

    def 'Query for attribute by cps path with cps paths that return no data because of #scenario.'() {
        when: 'a query is executed to get data nodes for the given cps path'
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE, BOOKSTORE_ANCHOR, cpsPath, OMIT_DESCENDANTS)
        then: 'no data is returned'
            assert result.isEmpty()
        where: 'following cps queries are performed'
            scenario                         | cpsPath
            'cps path is incomplete'         | '/bookstore[@title="Matilda"]'
            'leaf value does not exist'      | '/bookstore/categories[@code="1"]/books[@title=\'does not exist\']'
            'incomplete end of xpath prefix' | '/bookstore/categories/books[@price=15]'
    }

    def 'Cps Path query using descendant anywhere and #type (further) descendants.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE, BOOKSTORE_ANCHOR, '/bookstore/categories[@code="1"]', fetchDescendantsOption)
        then: 'the data node has the correct number of children'
            assert result[0].childDataNodes.xpath.sort() == expectedChildNodes.sort()
        where: 'the following data is used'
            type      | fetchDescendantsOption   || expectedChildNodes
            'omit'    | OMIT_DESCENDANTS         || []
            'include' | INCLUDE_ALL_DESCENDANTS  || ["/bookstore/categories[@code='1']/books[@title='Matilda']",
                                                     "/bookstore/categories[@code='1']/books[@title='The Gruffalo']"]
    }

    def 'Cps Path query for all books.'() {
        when: 'a query is executed to get all books'
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE, BOOKSTORE_ANCHOR, '//books', OMIT_DESCENDANTS)
        then: 'the expected number of books are returned'
            assert result.size() == 9
    }

    def 'Cps Path query using descendant anywhere with #scenario.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE, BOOKSTORE_ANCHOR, cpsPath, OMIT_DESCENDANTS)
        then: 'xpaths of the retrieved data nodes are as expected'
            def bookTitles = result.collect { it.getLeaves().get('title') }
            assert bookTitles.sort() == expectedBookTitles.sort()
        where: 'the following data is used'
            scenario                                 | cpsPath                                     || expectedBookTitles
            'string leaf condition'                  | '//books[@title="Matilda"]'                 || ["Matilda"]
            'text condition on leaf'                 | '//books/title[text()="Matilda"]'           || ["Matilda"]
            'text condition case mismatch'           | '//books/title[text()="matilda"]'           || []
            'text condition on int leaf'             | '//books/price[text()="10"]'                || ["Matilda"]
            'text condition on leaf-list'            | '//books/authors[text()="Terry Pratchett"]' || ["Good Omens", "The Colour of Magic", "The Light Fantastic"]
            'text condition partial match'           | '//books/authors[text()="Terry"]'           || []
            'text condition (existing) empty string' | '//books/lang[text()=""]'                   || ["A Book with No Language"]
            'text condition on int leaf-list'        | '//books/editions[text()="2000"]'           || ["Matilda"]
            'match of leaf containing /'             | '//books[@lang="N/A"]'                      || ["Logarithm tables"]
            'text condition on leaf containing /'    | '//books/lang[text()="N/A"]'                || ["Logarithm tables"]
            'match of key containing /'              | '//books[@title="Debian GNU/Linux"]'        || ["Debian GNU/Linux"]
            'text condition on key containing /'     | '//books/title[text()="Debian GNU/Linux"]'  || ["Debian GNU/Linux"]
    }

    def 'Cps Path query using descendant anywhere with #scenario condition for a container element.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE, BOOKSTORE_ANCHOR, cpsPath, OMIT_DESCENDANTS)
        then: 'book titles from the retrieved data nodes are as expected'
            def bookTitles = result.collect { it.getLeaves().get('title') }
            assert bookTitles.sort() == expectedBookTitles.sort()
        where: 'the following data is used'
            scenario                   | cpsPath                                                || expectedBookTitles
            'one leaf'                 | '//books[@price=14]'                                   || ['The Light Fantastic']
            'one text'                 | '//books/authors[text()="Terry Pratchett"]'            || ['Good Omens', 'The Colour of Magic', 'The Light Fantastic']
            'more than one leaf'       | '//books[@price=12 and @lang="English"]'               || ['The Colour of Magic']
            'leaves reversed in order' | '//books[@lang="English" and @price=12]'               || ['The Colour of Magic']
            'leaf and text'            | '//books[@price=14]/authors[text()="Terry Pratchett"]' || ['The Light Fantastic']
    }

    def 'Cps Path query using descendant anywhere with #scenario condition(s) for a list element.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE, BOOKSTORE_ANCHOR, cpsPath, INCLUDE_ALL_DESCENDANTS)
        then: 'xpaths of the retrieved data nodes are as expected'
            result.xpath.toList() == ["/bookstore/premises/addresses[@house-number='2' and @street='Main Street']"]
        where: 'the following data is used'
            scenario                              | cpsPath
            'full composite key'                  | '//addresses[@house-number=2 and @street="Main Street"]'
            'one partial key leaf'                | '//addresses[@house-number=2]'
            'one non key leaf'                    | '//addresses[@county="Kildare"]'
            'mix of partial key and non key leaf' | '//addresses[@street="Main Street" and @county="Kildare"]'
    }

    def 'Query for attribute by cps path of type ancestor with #scenario.'() {
        when: 'the given cps path is parsed'
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE, BOOKSTORE_ANCHOR, cpsPath, OMIT_DESCENDANTS)
        then: 'the xpaths of the retrieved data nodes are as expected'
            assert result.xpath.sort() == expectedXPaths.sort()
        where: 'the following data is used'
            scenario                                    | cpsPath                                               || expectedXPaths
            'multiple list-ancestors'                   | '//books/ancestor::categories'                        || ["/bookstore/categories[@code='1']", "/bookstore/categories[@code='2']", "/bookstore/categories[@code='3']", "/bookstore/categories[@code='4']"]
            'one ancestor with list value'              | '//books/ancestor::categories[@code="1"]'             || ["/bookstore/categories[@code='1']"]
            'top ancestor'                              | '//books/ancestor::bookstore'                         || ["/bookstore"]
            'list with index value in the xpath prefix' | '//categories[@code="1"]/books/ancestor::bookstore'   || ["/bookstore"]
            'ancestor with parent list'                 | '//books/ancestor::bookstore/categories'              || ["/bookstore/categories[@code='1']", "/bookstore/categories[@code='2']", "/bookstore/categories[@code='3']", "/bookstore/categories[@code='4']"]
            'ancestor with parent'                      | '//books/ancestor::bookstore/categories[@code="2"]'   || ["/bookstore/categories[@code='2']"]
            'ancestor combined with text condition'     | '//books/title[text()="Matilda"]/ancestor::bookstore' || ["/bookstore"]
            'ancestor with parent that does not exist'  | '//books/ancestor::parentDoesNoExist/categories'      || []
            'ancestor does not exist'                   | '//books/ancestor::ancestorDoesNotExist'              || []
    }

    def 'Query for attribute by cps path of type ancestor with #scenario descendants.'() {
        when: 'the given cps path is parsed'
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE, BOOKSTORE_ANCHOR, '//books/ancestor::bookstore', fetchDescendantsOption)
        then: 'the xpaths of the retrieved data nodes are as expected'
            assert countDataNodesInTree(result) == expectedNumberOfNodes
        where: 'the following data is used'
            scenario | fetchDescendantsOption  || expectedNumberOfNodes
            'no'     | OMIT_DESCENDANTS        || 1
            'direct' | DIRECT_CHILDREN_ONLY    || 6
            'all'    | INCLUDE_ALL_DESCENDANTS || 17
    }

    def 'Cps Path query with syntax error throws a CPS Path Exception.'() {
        when: 'trying to execute a query with a syntax (parsing) error'
            objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE, BOOKSTORE_ANCHOR, 'cpsPath that cannot be parsed' , OMIT_DESCENDANTS)
        then: 'a cps path exception is thrown'
            thrown(CpsPathException)
    }

}
