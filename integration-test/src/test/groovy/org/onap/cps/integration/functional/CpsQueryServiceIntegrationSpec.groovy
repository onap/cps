package org.onap.cps.integration.functional

import org.onap.cps.integration.base.FunctionalSpecBase

import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS

class CpsQueryServiceIntegrationSpec extends FunctionalSpecBase {
    def objectUnderTest

    def setup() { objectUnderTest = cpsQueryService }

    def 'cps-path query using combinations of operator #scenario.'() {
        when: 'a query is executed to get response by the given cps path'
            def result = objectUnderTest.queryDataNodes(FUNCTIONAL_TEST_DATASPACE, BOOKSTORE_ANCHOR, cpspath, OMIT_DESCENDANTS)
        then: 'the cps-path of queryDataNodes has the expectedLeaves'
            assert result.leaves == expectedLeaves
        where: 'the following data is used'
            scenario                                | cpspath                                                        | expectedLeaves
            'the "OR" condition'                    | '//books[@lang="English" or @price=15]'                        | [[lang: "English", price: 15, title: "The Gruffalo", authors: ["Julia Donaldson"], pub_year: 1999],
                                                                                                                        [lang: "English", price: 10, title: "Matilda", authors: ["Roald Dahl"], pub_year: 1988],
                                                                                                                        [lang: "English", price: 15, title: "Annihilation", authors: ["Jeff VanderMeer"], pub_year: 2014],
                                                                                                                        [lang: "English", price: 13, title: "Good Omens", authors: ["Terry Pratchett", "Neil Gaiman"], pub_year: 2006]]
            'the "OR" condition with non-json data' | '//books[@title="xyz" or @price=15]'                           | [[lang: "English", price: 15, title: "The Gruffalo", authors: ["Julia Donaldson"], pub_year: 1999],
                                                                                                                        [lang: "English", price: 15, title: "Annihilation", authors: ["Jeff VanderMeer"], pub_year: 2014]]
            'combination of AND/OR'                 | '//books[@pub_year=2014 and @price=15 or @title="Good Omens"]' | [[lang: "English", price: 15, title: "Annihilation", authors: ["Jeff VanderMeer"], pub_year: 2014],
                                                                                                                        [lang: "English", price: 13, title: "Good Omens", authors: ["Terry Pratchett", "Neil Gaiman"], pub_year: 2006]]
            'combination of OR/AND'                 | '//books[@title="Good Omens" or @price=15 and @pub_year=1999]' | [[lang: "English", price: 15, title: "The Gruffalo", authors: ["Julia Donaldson"], pub_year: 1999]]
            'combination of multiple AND'           | '//books[@lang="English" and @price=15 and @pub_year=2014 ]'   | [[lang: "English", price: 15, title: "Annihilation", authors: ["Jeff VanderMeer"], pub_year: 2014]]
            'combination of multiple OR'            | '//books[ @title="Matilda" or @price=15 or @pub_year=2000]'    | [[lang: "English", price: 15, title: "The Gruffalo", authors: ["Julia Donaldson"], pub_year: 1999],
                                                                                                                        [lang: "English", price: 10, title: "Matilda", authors: ["Roald Dahl"], pub_year: 1988],
                                                                                                                        [lang: "English", price: 15, title: "Annihilation", authors: ["Jeff VanderMeer"], pub_year: 2014]]
    }
}