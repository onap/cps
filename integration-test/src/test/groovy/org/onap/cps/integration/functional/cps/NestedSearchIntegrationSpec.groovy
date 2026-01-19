package org.onap.cps.integration.functional.cps

import static org.onap.cps.api.parameters.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS

import org.onap.cps.api.CpsQueryService
import org.onap.cps.api.model.NestedSearchQuery
import org.onap.cps.integration.base.FunctionalSpecBase

class NestedSearchIntegrationSpec extends FunctionalSpecBase {

    CpsQueryService objectUnderTest

    def setup() { objectUnderTest = cpsQueryService }

    def 'Nested search returns categories (Children or Comedy) with only books with price > 14 included.'() {
        given: 'a nested search query (outer categories, inner books)'
            def payload = NestedSearchQuery.builder()
                .cpsPath('//categories[@name="Children" or @name="Comedy"]')
                .operator('and')
                .conditions([
                    NestedSearchQuery.builder()
                        .cpsPath('//books[@price>14]')
                        .operator('and')
                        .build()
                ])
                .build()

        when: 'search is executed'
            def result = objectUnderTest.searchDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1,
                payload, INCLUDE_ALL_DESCENDANTS)

        then: 'both categories are returned'
            def categoryNames = result.collect { it.leaves.get('name') }
            assert categoryNames.contains('Children')
            assert categoryNames.contains('Comedy')
        and: 'print json'
            print(jsonObjectMapper.asJsonString(result))

        and: 'Children contains only books > 14 (20 and 15)'
            def children = result.find { it.leaves.get('name') == 'Children' }
            def childrenBookPrices = children.childDataNodes.findAll { it.xpath.contains('/books') }
                .collect { it.leaves.get('price') }
            assert childrenBookPrices.contains(20)
            assert childrenBookPrices.contains(15)

        and: 'Comedy contains only the book > 14 (20)'
            def comedy = result.find { it.leaves.get('name') == 'Comedy' }
            def comedyBookPrices = comedy.childDataNodes.findAll { it.xpath.contains('/books') }
                .collect { it.leaves.get('price') }
            assert comedyBookPrices == [20]


    }
}