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

    def 'cps-path query using combinations of OR operator #scenario.'() {
        when: 'a query is executed to get response by the given cps path'
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE, BOOKSTORE_ANCHOR, cpspath, OMIT_DESCENDANTS)
        then: 'the result contains expected number of nodes'
            assert result.size() == expectedResultSize
        then: 'the cps-path of queryDataNodes has the expectedLeaves'
            assert result.leaves == expectedLeaves
        where: 'the following data is used'
            scenario                                | cpspath                                                        || expectedResultSize | expectedLeaves
            'the "OR" condition'                    | '//books[@lang="English" or @price=15]'                        || 4                  | [[lang: "English", price: 15, title: "The Gruffalo", authors: ["Julia Donaldson"], pub_year: 1999],
                                                                                                                                              [lang: "English", price: 10, title: "Matilda", authors: ["Roald Dahl"], pub_year: 1988],
                                                                                                                                              [lang: "English", price: 15, title: "Annihilation", authors: ["Jeff VanderMeer"], pub_year: 2014],
                                                                                                                                              [lang: "English", price: 13, title: "Good Omens", authors: ["Terry Pratchett", "Neil Gaiman"], pub_year: 2006]]
            'the "OR" condition with non-json data' | '//books[@title="xyz" or @price=15]'                           || 2                  | [[lang: "English", price: 15, title: "The Gruffalo", authors: ["Julia Donaldson"], pub_year: 1999],
                                                                                                                                              [lang: "English", price: 15, title: "Annihilation", authors: ["Jeff VanderMeer"], pub_year: 2014]]
            'combination of AND/OR'                 | '//books[@pub_year=2014 and @price=15 or @title="Good Omens"]' || 2                  | [[lang: "English", price: 15, title: "Annihilation", authors: ["Jeff VanderMeer"], pub_year: 2014],
                                                                                                                                              [lang: "English", price: 13, title: "Good Omens", authors: ["Terry Pratchett", "Neil Gaiman"], pub_year: 2006]]
            'combination of OR/AND'                 | '//books[@title="Good Omens" or @price=15 and @pub_year=1999]' || 1                  | [[lang: "English", price: 15, title: "The Gruffalo", authors: ["Julia Donaldson"], pub_year: 1999]]
            'combination of multiple AND'           | '//books[@lang="English" and @price=15 and @pub_year=2014 ]'   || 1                  | [[lang: "English", price: 15, title: "Annihilation", authors: ["Jeff VanderMeer"], pub_year: 2014]]
            'combination of multiple OR'            | '//books[ @title="Matilda" or @price=15 or @pub_year=2000]'    || 3                  | [[lang: "English", price: 15, title: "The Gruffalo", authors: ["Julia Donaldson"], pub_year: 1999],
                                                                                                                                              [lang: "English", price: 10, title: "Matilda", authors: ["Roald Dahl"], pub_year: 1988],
                                                                                                                                              [lang: "English", price: 15, title: "Annihilation", authors: ["Jeff VanderMeer"], pub_year: 2014]]
    }
}
