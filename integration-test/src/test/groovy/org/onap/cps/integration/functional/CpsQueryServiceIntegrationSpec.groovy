/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
 *  Modifications Copyright (C) 2023 TechMahindra Ltd
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

import java.time.OffsetDateTime
import org.onap.cps.api.CpsQueryService
import org.onap.cps.integration.base.FunctionalSpecBase
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.PaginationOption
import org.onap.cps.spi.exceptions.CpsPathException

import static org.onap.cps.spi.FetchDescendantsOption.DIRECT_CHILDREN_ONLY
import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS

class CpsQueryServiceIntegrationSpec extends FunctionalSpecBase {

    CpsQueryService objectUnderTest

    def setup() { objectUnderTest = cpsQueryService }

    def 'Query bookstore using CPS path where #scenario.'() {
        when: 'query data nodes for bookstore container'
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, cpsPath, INCLUDE_ALL_DESCENDANTS)
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

    def 'Cps Path query using combinations of OR operator #scenario.'() {
        when: 'a query is executed to get response by the given cps path'
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, cpsPath, OMIT_DESCENDANTS)
        then: 'the result contains expected number of nodes'
            assert result.size() == expectedResultSize
        and: 'the cps-path of queryDataNodes has the expectedLeaves'
            assert result.leaves.sort() == expectedLeaves.sort()
        where: 'the following data is used'
            scenario                                | cpsPath                                                          || expectedResultSize | expectedLeaves
            'the "OR" condition'                    | '//books[@lang="English" or @price=15]'                          || 6                  | [[lang: "English", price: 15, title: "Annihilation", authors: ["Jeff VanderMeer"], editions: [2014]],
                                                                                                                                                [lang: "English", price: 15, title: "The Gruffalo", authors: ["Julia Donaldson"], editions: [1999]],
                                                                                                                                                [lang: "English", price: 14, title: "The Light Fantastic", authors: ["Terry Pratchett"], editions: [1986]],
                                                                                                                                                [lang: "English", price: 13, title: "Good Omens", authors: ["Terry Pratchett", "Neil Gaiman"], editions: [2006]],
                                                                                                                                                [lang: "English", price: 12, title: "The Colour of Magic", authors: ["Terry Pratchett"], editions: [1983]],
                                                                                                                                                [lang: "English", price: 10, title: "Matilda", authors: ["Roald Dahl"], editions: [1988, 2000]]]
            'the "OR" condition with non-json data' | '//books[@title="xyz" or @price=15]'                             || 2                  | [[lang: "English", price: 15, title: "Annihilation", authors: ["Jeff VanderMeer"], editions: [2014]],
                                                                                                                                                [lang: "English", price: 15, title: "The Gruffalo", authors: ["Julia Donaldson"], editions: [1999]]]
            'combination of multiple AND'           | '//books[@lang="English" and @price=15 and @edition=1983]'       || 0                  | []
            'combination of multiple OR'            | '//books[ @title="Matilda" or @price=15 or @edition=1983]'       || 3                  | [[lang: "English", price: 15, title: "Annihilation", authors: ["Jeff VanderMeer"], editions: [2014]],
                                                                                                                                                [lang: "English", price: 10, title: "Matilda", authors: ["Roald Dahl"], editions: [1988, 2000]],
                                                                                                                                                [lang: "English", price: 15, title: "The Gruffalo", authors: ["Julia Donaldson"], editions: [1999]]]
            'combination of AND/OR'                 | '//books[@edition=1983 and @price=15 or @title="Good Omens"]'    || 1                  | [[lang: "English", price: 13, title: "Good Omens", authors: ["Terry Pratchett", "Neil Gaiman"], editions: [2006]]]
            'combination of OR/AND'                 | '//books[@title="Annihilation" or @price=39 and @lang="arabic"]' || 1                  | [[lang: "English", price: 15, title: "Annihilation", authors: ["Jeff VanderMeer"], editions: [2014]]]
    }

    def 'cps-path query using combinations of Comparative Operators #scenario.'() {
        when: 'a query is executed to get response by the given cpsPath'
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, cpsPath, OMIT_DESCENDANTS)
        then: 'the result contains expected number of nodes'
            assert result.size() == expectedResultSize
        and: 'xpaths of the retrieved data nodes are as expected'
            def bookTitles = result.collect { it.getLeaves().get('title') }
            assert bookTitles.sort() == expectedBookTitles.sort()
        where: 'the following data is used'
            scenario                                         | cpsPath                                                            || expectedResultSize | expectedBookTitles
            'the ">" condition'                              | '//books[@price>13 ]'                                              || 5                  | ['A Book with No Language', 'Annihilation', 'Debian GNU/Linux', 'The Gruffalo', 'The Light Fantastic']
            'the "<" condition '                             | '//books[@price<15]'                                               || 5                  | ['Good Omens', 'Logarithm tables', 'Matilda', 'The Colour of Magic', 'The Light Fantastic']
            'the "<=" condition'                             | '//books[@price<=15]'                                              || 7                  | ['Annihilation', 'Good Omens', 'Logarithm tables', 'Matilda', 'The Colour of Magic', 'The Gruffalo', 'The Light Fantastic']
            'the ">=" condition'                             | '//books[@price>=20]'                                              || 2                  | ['A Book with No Language', 'Debian GNU/Linux']
            'the "<" condition  where result does not exist' | '//books[@price<5]'                                                || 0                  | []
            'the ">" condition  where result does not exist' | '//books[@price>1000]'                                             || 0                  | []
            'the ">" condition with AND condition'           | '//books[@price>13 and @title="A Book with No Language"]'          || 1                  | ['A Book with No Language']
            'the "<" condition with OR condition'            | '//books[@price<10 or @lang="German"]'                             || 1                  | ['Debian GNU/Linux']
            'the "<=" condition with AND/OR condition'       | '//books[@price<=15 and @title="Annihilation" or @lang="Spanish"]' || 1                  | ['Annihilation']
            'the ">=" condition with OR/AND condition'       | '//books[@price>=13 or @lang="Spanish" and @title="Good Omens"]'   || 6                  | ['A Book with No Language', 'Annihilation', 'Good Omens', 'Debian GNU/Linux', 'The Gruffalo', 'The Light Fantastic']
            'Mix of integer and string condition '           | '//books[@lang="German" and @price>38]'                            || 1                  | ['Debian GNU/Linux']
    }

    def 'Cps Path query for leaf value(s) with #scenario.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, cpsPath, fetchDescendantsOption)
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
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, cpsPath, OMIT_DESCENDANTS)
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
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore/categories[@code="1"]', fetchDescendantsOption)
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
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '//books', OMIT_DESCENDANTS)
        then: 'the expected number of books are returned'
            assert result.size() == 9
    }

    def 'Cps Path query using descendant anywhere with #scenario.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, cpsPath, OMIT_DESCENDANTS)
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

    def 'Query for attribute by cps path using contains condition #scenario.'() {
        when: 'a query is executed to get response by the given cps path'
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, cpsPath, INCLUDE_ALL_DESCENDANTS)
        then: 'xpaths of the retrieved data nodes are as expected'
            def bookTitles = result.collect { it.getLeaves().get('title') }
            assert bookTitles.sort() == expectedBookTitles.sort()
        where: 'the following data is used'
            scenario                                 | cpsPath                           || expectedBookTitles
            'contains condition with leaf'           | '//books[contains(@title,"Mat")]' || ["Matilda"]
            'contains condition with case-sensitive' | '//books[contains(@title,"Ti")]'  || []
            'contains condition with Integer Value'  | '//books[contains(@price,"15")]'  || ["Annihilation", "The Gruffalo"]
            'contains condition with No-value'       | '//books[contains(@title,"")]'    || ["A Book with No Language", "Annihilation", "Debian GNU/Linux", "Good Omens", "Logarithm tables", "Matilda", "The Colour of Magic", "The Gruffalo", "The Light Fantastic"]
    }

    def 'Cps Path query using descendant anywhere with #scenario condition for a container element.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, cpsPath, OMIT_DESCENDANTS)
        then: 'book titles from the retrieved data nodes are as expected'
            def bookTitles = result.collect { it.getLeaves().get('title') }
            assert bookTitles.sort() == expectedBookTitles.sort()
        where: 'the following data is used'
            scenario                                                   | cpsPath                                                                || expectedBookTitles
            'one leaf'                                                 | '//books[@price=14]'                                                   || ['The Light Fantastic']
            'one leaf with ">" condition'                              | '//books[@price>14]'                                                   || ['A Book with No Language', 'Annihilation', 'Debian GNU/Linux', 'The Gruffalo']
            'one text'                                                 | '//books/authors[text()="Terry Pratchett"]'                            || ['Good Omens', 'The Colour of Magic', 'The Light Fantastic']
            'more than one leaf'                                       | '//books[@price=12 and @lang="English"]'                               || ['The Colour of Magic']
            'more than one leaf has "OR" condition'                    | '//books[@lang="English" or @price=15]'                                || ['Annihilation', 'Good Omens', 'Matilda', 'The Colour of Magic', 'The Gruffalo', 'The Light Fantastic']
            'more than one leaf has "OR" condition with non-json data' | '//books[@title="xyz" or @price=13]'                                   || ['Good Omens']
            'more than one leaf has multiple AND'                      | '//books[@lang="English" and @price=13 and @edition=1983]'             || []
            'more than one leaf has multiple OR'                       | '//books[ @title="Matilda" or @price=15 or @edition=2006]'             || ['Annihilation', 'Matilda', 'The Gruffalo']
            'leaves reversed in order'                                 | '//books[@lang="English" and @price=12]'                               || ['The Colour of Magic']
            'more than one leaf has combination of AND/OR'             | '//books[@edition=1983 and @price=13 or @title="Good Omens"]'          || ['Good Omens']
            'more than one leaf has OR/AND'                            | '//books[@title="The Light Fantastic" or @price=11 and @edition=1983]' || ['The Light Fantastic']
            'leaf and text'                                            | '//books[@price=14]/authors[text()="Terry Pratchett"]'                 || ['The Light Fantastic']
            'leaf and contains'                                        | '//books[contains(@price,"13")]'                                       || ['Good Omens']
    }

    def 'Cps Path query using descendant anywhere with #scenario condition(s) for a list element.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, cpsPath, INCLUDE_ALL_DESCENDANTS)
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
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, cpsPath, OMIT_DESCENDANTS)
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
            'ancestor combined with contains condition' | '//books[contains(@title,"Mat")]/ancestor::bookstore' || ["/bookstore"]
    }

    def 'Query for attribute by cps path of type ancestor with #scenario descendants.'() {
        when: 'the given cps path is parsed'
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '//books/ancestor::bookstore', fetchDescendantsOption)
        then: 'the xpaths of the retrieved data nodes are as expected'
            assert countDataNodesInTree(result) == expectedNumberOfNodes
        where: 'the following data is used'
            scenario | fetchDescendantsOption  || expectedNumberOfNodes
            'no'     | OMIT_DESCENDANTS        || 1
            'direct' | DIRECT_CHILDREN_ONLY    || 6
            'all'    | INCLUDE_ALL_DESCENDANTS || 17
    }

    def 'Cps Path query with #scenario throws a CPS Path Exception.'() {
        when: 'trying to execute a query with a syntax (parsing) error'
            objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, cpsPath, OMIT_DESCENDANTS)
        then: 'a cps path exception is thrown'
            thrown(CpsPathException)
        where: 'the following data is used'
            scenario                           | cpsPath
            'cpsPath that cannot be parsed'    | 'cpsPath that cannot be parsed'
            'String with comparative operator' | '//books[@lang>"German" and @price>10]'
    }

    def 'Cps Path query across anchors with #scenario.'() {
        when: 'a query is executed to get a data nodes across anchors by the given CpsPath'
            PaginationOption NO_PAGINATION = null
            def result = objectUnderTest.queryDataNodesAcrossAnchors(FUNCTIONAL_TEST_DATASPACE_1, cpsPath, OMIT_DESCENDANTS, NO_PAGINATION)
        then: 'the correct dataspace is queried'
            assert result.dataspace.toSet() == [FUNCTIONAL_TEST_DATASPACE_1].toSet()
        and: 'correct anchors are queried'
            assert result.anchorName.toSet() == [BOOKSTORE_ANCHOR_1, BOOKSTORE_ANCHOR_2].toSet()
        and: 'the correct number of nodes is returned'
            assert result.size() == expectedXpathsPerAnchor.size() * NUMBER_OF_ANCHORS_PER_DATASPACE_WITH_BOOKSTORE_DATA
        and: 'the queried nodes have expected xpaths'
            assert result.xpath.toSet() == expectedXpathsPerAnchor.toSet()
        where: 'the following data is used'
            scenario                                    | cpsPath                                               || expectedXpathsPerAnchor
            'container node'                            | '/bookstore'                                          || ["/bookstore"]
            'list node'                                 | '/bookstore/categories'                               || ["/bookstore/categories[@code='1']", "/bookstore/categories[@code='2']", "/bookstore/categories[@code='3']", "/bookstore/categories[@code='4']"]
            'integer leaf-condition'                    | '/bookstore/categories[@code="1"]/books[@price=15]'   || ["/bookstore/categories[@code='1']/books[@title='The Gruffalo']"]
            'multiple list-ancestors'                   | '//books/ancestor::categories'                        || ["/bookstore/categories[@code='1']", "/bookstore/categories[@code='2']", "/bookstore/categories[@code='3']", "/bookstore/categories[@code='4']"]
            'one ancestor with list value'              | '//books/ancestor::categories[@code="1"]'             || ["/bookstore/categories[@code='1']"]
            'list with index value in the xpath prefix' | '//categories[@code="1"]/books/ancestor::bookstore'   || ["/bookstore"]
            'ancestor with parent list'                 | '//books/ancestor::bookstore/categories'              || ["/bookstore/categories[@code='1']", "/bookstore/categories[@code='2']", "/bookstore/categories[@code='3']", "/bookstore/categories[@code='4']"]
            'ancestor with parent list element'         | '//books/ancestor::bookstore/categories[@code="2"]'   || ["/bookstore/categories[@code='2']"]
            'ancestor combined with text condition'     | '//books/title[text()="Matilda"]/ancestor::bookstore' || ["/bookstore"]
    }

    def 'Cps Path query across anchors with #scenario descendants.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodesAcrossAnchors(FUNCTIONAL_TEST_DATASPACE_1, '/bookstore', fetchDescendantsOption, PaginationOption.NO_PAGINATION)
        then: 'the correct dataspace was queried'
            assert result.dataspace.toSet() == [FUNCTIONAL_TEST_DATASPACE_1].toSet()
        and: 'correct number of datanodes are returned'
            assert countDataNodesInTree(result) == expectedNumberOfNodesPerAnchor * NUMBER_OF_ANCHORS_PER_DATASPACE_WITH_BOOKSTORE_DATA
        where: 'the following data is used'
            scenario | fetchDescendantsOption  || expectedNumberOfNodesPerAnchor
            'no'     | OMIT_DESCENDANTS        || 1
            'direct' | DIRECT_CHILDREN_ONLY    || 6
            'all'    | INCLUDE_ALL_DESCENDANTS || 17
    }

    def 'Cps Path query across anchors with ancestors and #scenario descendants.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodesAcrossAnchors(FUNCTIONAL_TEST_DATASPACE_1, '//books/ancestor::bookstore', fetchDescendantsOption, PaginationOption.NO_PAGINATION)
        then: 'the correct dataspace was queried'
            assert result.dataspace.toSet() == [FUNCTIONAL_TEST_DATASPACE_1].toSet()
        and: 'correct number of datanodes are returned'
            assert countDataNodesInTree(result) == expectedNumberOfNodesPerAnchor * NUMBER_OF_ANCHORS_PER_DATASPACE_WITH_BOOKSTORE_DATA
        where: 'the following data is used'
            scenario | fetchDescendantsOption  || expectedNumberOfNodesPerAnchor
            'no'     | OMIT_DESCENDANTS        || 1
            'direct' | DIRECT_CHILDREN_ONLY    || 6
            'all'    | INCLUDE_ALL_DESCENDANTS || 17
    }

    def 'Cps Path query across anchors with syntax error throws a CPS Path Exception.'() {
        when: 'trying to execute a query with a syntax (parsing) error'
            objectUnderTest.queryDataNodesAcrossAnchors(FUNCTIONAL_TEST_DATASPACE_1, 'cpsPath that cannot be parsed' , OMIT_DESCENDANTS, PaginationOption.NO_PAGINATION)
        then: 'a cps path exception is thrown'
            thrown(CpsPathException)
    }

    def 'Cps Path querys with all descendants including descendants that are list entries: #scenario.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, cpsPath, INCLUDE_ALL_DESCENDANTS)
        then: 'correct number of datanodes are returned'
            assert countDataNodesInTree(result) == expectedNumberOfDataNodes
        where:
            scenario                              | cpsPath                                 || expectedNumberOfDataNodes
            'absolute path all list entries'      | '/bookstore/categories'                 || 13
            'absolute path 1 list entry by key'   | '/bookstore/categories[@code="3"]'      || 5
            'absolute path 1 list entry by name'  | '/bookstore/categories[@name="Comedy"]' || 5
            'relative path all list entries'      | '//categories'                          || 13
            'relative path 1 list entry by key'   | '//categories[@code="3"]'               || 5
            'relative path 1 list entry by leaf'  | '//categories[@name="Comedy"]'          || 5
            'incomplete absolute path'            | '/categories'                           || 0
            'incomplete absolute 1 list entry'    | '/categories[@code="3"]'                || 0
    }

    def 'Cps Path query contains #wildcard.'() {
        when: 'a query is executed with a wildcard in the given cps path'
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, cpsPath, INCLUDE_ALL_DESCENDANTS)
        then: 'no results are returned, as Cps Path query does not interpret wildcard characters'
            assert result.isEmpty()
        where:
            wildcard                                   | cpsPath
            '  sql wildcard in parent path list index' | '/bookstore/categories[@code="%"]/books'
            'regex wildcard in parent path list index' | '/bookstore/categories[@code=".*"]/books'
            '  sql wildcard in leaf-condition'         | '/bookstore/categories[@code="1"]/books[@title="%"]'
            'regex wildcard in leaf-condition'         | '/bookstore/categories[@code="1"]/books[@title=".*"]'
            '  sql wildcard in text-condition'         | '/bookstore/categories[@code="1"]/books/title[text()="%"]'
            'regex wildcard in text-condition'         | '/bookstore/categories[@code="1"]/books/title[text()=".*"]'
            '  sql wildcard in contains-condition'     | '/bookstore/categories[@code="1"]/books[contains(@title, "%")]'
            'regex wildcard in contains-condition'     | '/bookstore/categories[@code="1"]/books[contains(@title, ".*")]'
    }

    def 'Cps Path query can return a data node containing [@ in xpath #scenario.'() {
        given: 'a book with special characters [@ and ] in title'
            cpsDataService.saveData(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, "/bookstore/categories[@code='1']", '{"books": [ {"title":"[@hello=world]"} ] }', OffsetDateTime.now())
        when: 'a query is executed'
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, cpsPath, OMIT_DESCENDANTS)
        then: 'the node is returned'
            assert result.size() == 1
        cleanup: 'the new datanode'
            cpsDataService.deleteDataNode(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, "/bookstore/categories[@code='1']/books[@title='[@hello=world]']", OffsetDateTime.now())
        where:
            scenario             || cpsPath
            'leaf-condition'     || "/bookstore/categories[@code='1']/books[@title='[@hello=world]']"
            'text-condition'     || "/bookstore/categories[@code='1']/books/title[text()='[@hello=world]']"
            'contains-condition' || "/bookstore/categories[@code='1']/books[contains(@title, '[@hello=world]')]"
    }

    def 'Cps Path query across anchors using pagination option with #scenario.'() {
        when: 'a query is executed to get a data nodes across anchors by the given CpsPath and pagination option'
            def result = objectUnderTest.queryDataNodesAcrossAnchors(FUNCTIONAL_TEST_DATASPACE_1, '/bookstore', OMIT_DESCENDANTS, new PaginationOption(pageIndex, pageSize))
        then: 'correct bookstore names are queried'
            def bookstoreNames = result.collect { it.getLeaves().get('bookstore-name') }
            assert bookstoreNames.toList() == expectedBookstoreNames
        and: 'the correct number of page size is returned'
            assert result.size() == expectedPageSize
        and: 'the queried nodes have expected bookstore names'
            assert result.anchorName.toSet() == expectedAnchors.toSet()
        where: 'the following data is used'
            scenario                       | pageIndex | pageSize || expectedPageSize || expectedAnchors                          || expectedBookstoreNames
            '1st page with one anchor'     | 1         | 1        || 1                || [BOOKSTORE_ANCHOR_1]                     || ['Easons-1']
            '1st page with two anchor'     | 1         | 2        || 2                || [BOOKSTORE_ANCHOR_1, BOOKSTORE_ANCHOR_2] || ['Easons-1', 'Easons-2']
            '2nd page'                     | 2         | 1        || 1                || [BOOKSTORE_ANCHOR_2]                     || ['Easons-2']
            'no 2nd page due to page size' | 2         | 2        || 0                || []                                       || []
    }
}
