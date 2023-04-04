package org.onap.cps.integration.functional

import org.onap.cps.api.CpsQueryService
import org.onap.cps.integration.base.FunctionalSpecBase
import org.onap.cps.spi.exceptions.CpsPathException

import static org.onap.cps.spi.FetchDescendantsOption.DIRECT_CHILDREN_ONLY
import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS

class CpsQueryServiceIntegrationSpec  extends FunctionalSpecBase {

    CpsQueryService objectUnderTest

    def setup() { objectUnderTest = cpsQueryService }

    def 'Cps Path query across anchors with #scenario.'() {
        when: 'a query is executed to get a data nodes across anchors by the given CpsPath'
            def result = objectUnderTest.queryDataNodesAcrossAnchors(FUNCTIONAL_TEST_DATASPACE, cpsPath, OMIT_DESCENDANTS)
        then: 'the correct queried nodes are returned'
            assert result.size() == expectedXpathsPerAnchor.size() * 2
            assert result.xpath.toSet().containsAll(expectedXpathsPerAnchor)
        and: 'correct anchors were queried'
            assert result.anchorName.toSet() == ['bookstoreAnchor1', 'bookstoreAnchor2'].toSet()
        where: 'the following data is used'
            scenario                                    | cpsPath                                               || expectedXpathsPerAnchor
            'container node'                            | '/bookstore'                                          || ["/bookstore"]
            'list node'                                 | '/bookstore/categories'                               || ["/bookstore/categories[@code='1']", "/bookstore/categories[@code='2']", "/bookstore/categories[@code='3']"]
            'string leaf-condition'                     | '/bookstore[@bookstore-name="Easons"]'                || ["/bookstore"]
            'integer leaf-condition'                    | '/bookstore/categories[@code="1"]/books[@price=15]'   || ["/bookstore/categories[@code='1']/books[@title='The Gruffalo']"]
            'multiple list-ancestors'                   | '//books/ancestor::categories'                        || ["/bookstore/categories[@code='1']", "/bookstore/categories[@code='2']", "/bookstore/categories[@code='3']"]
            'one ancestor with list value'              | '//books/ancestor::categories[@code="1"]'             || ["/bookstore/categories[@code='1']"]
            'list with index value in the xpath prefix' | '//categories[@code="1"]/books/ancestor::bookstore'   || ["/bookstore"]
            'ancestor with parent list'                 | '//books/ancestor::bookstore/categories'              || ["/bookstore/categories[@code='1']", "/bookstore/categories[@code='2']", "/bookstore/categories[@code='3']"]
            'ancestor with parent list element'         | '//books/ancestor::bookstore/categories[@code="2"]'   || ["/bookstore/categories[@code='2']"]
            'ancestor combined with text condition'     | '//books/title[text()="Matilda"]/ancestor::bookstore' || ["/bookstore"]
    }

    def 'Cps Path query across anchors with #scenario descendants.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodesAcrossAnchors(FUNCTIONAL_TEST_DATASPACE, '/bookstore', fetchDescendantsOption)
        then: 'correct number of datanodes are returned'
            assert countDataNodesInTree(result) == expectedNumberOfNodes
        where: 'the following data is used'
            scenario | fetchDescendantsOption  || expectedNumberOfNodes
            'no'     | OMIT_DESCENDANTS        || 2 * 1
            'direct' | DIRECT_CHILDREN_ONLY    || 2 * 4
            // FIXME The next case throws a NullPointerException. This will be fixed in CPS-1582. Uncomment after fix.
            // 'all'    | INCLUDE_ALL_DESCENDANTS || 2 * 8
    }

    def 'Cps Path query across anchors with ancestors and #scenario descendants.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodesAcrossAnchors(FUNCTIONAL_TEST_DATASPACE, '//books/ancestor::bookstore', fetchDescendantsOption)
        then: 'correct number of datanodes are returned'
            assert countDataNodesInTree(result) == expectedNumberOfNodes
        where: 'the following data is used'
            scenario | fetchDescendantsOption  || expectedNumberOfNodes
            'no'     | OMIT_DESCENDANTS        || 2 * 1
            'direct' | DIRECT_CHILDREN_ONLY    || 2 * 4
            'all'    | INCLUDE_ALL_DESCENDANTS || 2 * 8
    }

    def 'Cps Path query across anchors with syntax error throws a CPS Path Exception.'() {
        when: 'trying to execute a query with a syntax (parsing) error'
            objectUnderTest.queryDataNodesAcrossAnchors(FUNCTIONAL_TEST_DATASPACE, 'cpsPath that cannot be parsed' , OMIT_DESCENDANTS)
        then: 'a cps path exception is thrown'
            thrown(CpsPathException)
    }

}
