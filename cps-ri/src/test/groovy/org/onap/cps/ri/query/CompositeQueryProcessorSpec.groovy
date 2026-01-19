/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2026 Deutsche Telekom AG
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ri.query

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.model.CompositeQuery
import org.onap.cps.api.parameters.FetchDescendantsOption
import org.onap.cps.ri.models.AnchorEntity
import org.onap.cps.ri.models.DataspaceEntity
import org.onap.cps.ri.models.FragmentEntity
import org.onap.cps.ri.repository.FragmentRepository
import org.onap.cps.ri.repository.FragmentRepositoryCpsPathQuery
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

class CompositeQueryProcessorSpec extends Specification {

    def mockFragmentRepositoryCpsPathQuery = Mock(FragmentRepositoryCpsPathQuery)
    def mockFragmentRepository = Mock(FragmentRepository)
    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    def objectUnderTest = new CompositeQueryProcessor(mockFragmentRepositoryCpsPathQuery, mockFragmentRepository, jsonObjectMapper)

    def dataspaceEntity = new DataspaceEntity(id: 1, name: 'bookstoreDataspace')
    def anchorEntity = new AnchorEntity(id: 10, name: 'bookstoreAnchor', dataspace: dataspaceEntity)

    def bookMatilda      = new FragmentEntity(id: 4L, xpath: "/bookstore/categories[@code='1']/books[@title='Matilda']",     parentId: 2L, attributes: '{"price":20}', anchor: anchorEntity, childFragments: [] as Set)
    def bookGruffalo     = new FragmentEntity(id: 5L, xpath: "/bookstore/categories[@code='1']/books[@title='The Gruffalo']",parentId: 2L, attributes: '{"price":15}', anchor: anchorEntity, childFragments: [] as Set)
    def bookAnnihilation = new FragmentEntity(id: 6L, xpath: "/bookstore/categories[@code='2']/books[@title='Annihilation']",parentId: 3L, attributes: '{"price":15}', anchor: anchorEntity, childFragments: [] as Set)
    def bookGoodOmens    = new FragmentEntity(id: 8L, xpath: "/bookstore/categories[@code='3']/books[@title='Good Omens']",  parentId: 7L, attributes: '{"price":13}', anchor: anchorEntity, childFragments: [] as Set)

    def catChildren = new FragmentEntity(id: 2L, xpath: "/bookstore/categories[@code='1']", parentId: 1L, attributes: '{"name":"Children"}', anchor: anchorEntity, childFragments: [bookMatilda, bookGruffalo] as Set)
    def catThriller = new FragmentEntity(id: 3L, xpath: "/bookstore/categories[@code='2']", parentId: 1L, attributes: '{"name":"Thriller"}', anchor: anchorEntity, childFragments: [bookAnnihilation] as Set)
    def catComedy   = new FragmentEntity(id: 7L, xpath: "/bookstore/categories[@code='3']", parentId: 1L, attributes: '{"name":"Comedy"}',   anchor: anchorEntity, childFragments: [bookGoodOmens] as Set)

    def bookstore   = new FragmentEntity(id: 1L, xpath: '/bookstore', parentId: null, attributes: '{"bookstore-name":"Easons"}', anchor: anchorEntity, childFragments: [catChildren, catThriller, catComedy] as Set)

    def setup() {
        mockFragmentRepository.prefetchDescendantsOfFragmentEntities(_, _) >> { fetchDescendantsOption, fragments -> fragments }
    }

    def 'Composite query with no conditions returns all fetched descendants.'() {
        given: 'a query with only a base cpsPath'
            def query = CompositeQuery.builder().cpsPath("//categories[@name='Children']").build()
        and: 'the repository returns the expected base fragment'
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >> [catChildren]
        when: 'the composite query is processed'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'expected number of data nodes are returned'
            result.size() == 1
        and: 'the returned node has the correct xpath'
            result[0].xpath == "/bookstore/categories[@code='1']"
        and: 'and the data node has expected number of child data nodes'
            result[0].childDataNodes.size() == 2
    }

    def 'Process composite query when base cpsPath matches nothing.'() {
        given: 'a query targeting a category that does not exist'
            def query = CompositeQuery.builder().cpsPath("//categories[@name='NonExistent']").build()
        and: 'the repository returns no matching fragments'
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >> []
        when: 'the composite query is processed'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the result is empty'
            result.isEmpty()
    }

    def 'Process composite query with AND operator.'() {
        given: 'a query on bookstore data categories with an AND condition to fetch books with price=20'
            def condition = CompositeQuery.builder().cpsPath("//books[@price=20]").build()
            def query = CompositeQuery.builder().cpsPath('//categories').operator('and').conditions([condition]).build()
        and: 'the base query returns expected fragments'
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >>> [[catChildren, catThriller, catComedy], [bookMatilda]]
        when: 'the composite query is processed'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'expected books are returned'
            result.size() == 1
            result[0].xpath == "/bookstore/categories[@code='1']"
    }

    def 'Process composite query with AND operator on non-existing data.'() {
        given: 'a query on bookstore data categories with an AND condition to fetch non-existing book'
            def condition = CompositeQuery.builder().cpsPath("//books[@price=999]").build()
            def query = CompositeQuery.builder().cpsPath('//categories').operator('and').conditions([condition]).build()
        and: 'the base query returns all categories, but the condition matches no books'
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >>> [[catChildren, catThriller, catComedy], []]
        when: 'the composite query is processed'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the result is empty because no category has a book with price=999'
            result.isEmpty()
    }

    def 'Process composite query with OR operator.'() {
        given: 'a query on bookstore data categories with OR conditions for Children or Comedy'
            def condition1 = CompositeQuery.builder().cpsPath("//categories[@name='Children']").build()
            def condition2   = CompositeQuery.builder().cpsPath("//categories[@name='Comedy']").build()
            def query = CompositeQuery.builder().cpsPath('//categories').operator('or').conditions([condition1, condition2]).build()
        and: 'the base query returns all categories; conditions return Children and Comedy respectively'
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >>> [[catChildren, catThriller, catComedy], [catChildren], [catComedy]]
        when: 'the composite query is processed'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'expected nodes are returned'
            result.size() == 2
            result.collect { it.xpath }.containsAll(["/bookstore/categories[@code='1']", "/bookstore/categories[@code='3']"])
        and: 'the data node with category "Thriller" is not returned'
            !result.collect { it.xpath }.contains("/bookstore/categories[@code='2']")
    }

    def 'Process composite query with no descendants.'() {
        given: 'a query with no conditions'
            def query = CompositeQuery.builder().cpsPath('//categories').build()
        and: 'the repository returns expected fragments'
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >> [catChildren, catThriller, catComedy]
        when: 'the composite query is processed with OMIT_DESCENDANTS'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.OMIT_DESCENDANTS)
        then: 'all three categories are returned'
            result.size() == 3
        and: 'none have child data nodes'
            result.every { it.childDataNodes.isEmpty() }
    }

    def 'Process composite query with a fragment with no attributes.'() {
        given: 'a category fragment with no attributes'
            def catNoAttributes = new FragmentEntity(id: 99L, xpath: "/bookstore/categories[@code='9']", parentId: 1L, attributes: null, anchor: anchorEntity, childFragments: [] as Set)
        and: 'a query with no conditions'
            def query = CompositeQuery.builder().cpsPath('//categories').build()
        and: 'the repository returns the fragment with null attributes'
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >> [catNoAttributes]
        when: 'the composite query is processed'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.OMIT_DESCENDANTS)
        then: 'the result has one node'
            result.size() == 1
        and: 'the node has no leaves'
            result[0].leaves.isEmpty()
    }

    def 'Process composite query where fragment have no child fragments.'() {
        given: 'a category fragment with no child fragments'
            def catNullChildren = new FragmentEntity(id: 20L, xpath: "/bookstore/categories[@code='5']", parentId: 1L, attributes: '{"name":"Science"}', anchor: anchorEntity, childFragments: null)
        and: 'a query with no conditions'
            def query = CompositeQuery.builder().cpsPath('//categories').build()
        and: 'the repository returns the fragment with null childFragments'
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >> [catNullChildren]
        when: 'the composite query is processed'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the result has one node'
            result.size() == 1
        and: 'the node xpath is correct'
            result[0].xpath == "/bookstore/categories[@code='5']"
    }

    def 'Process composite query where a matched node is also an ancestor of another match.'() {
        given: 'a query with AND condition'
            def condition = CompositeQuery.builder().cpsPath("//books[@price=20]").build()
            def query = CompositeQuery.builder().cpsPath('//categories').operator('and').conditions([condition]).build()
        and: 'the base query returns category Children; the condition query returns book Matilda (child of category Children)'
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >>> [[catChildren], [bookMatilda]]
        when: 'the composite query is processed'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'expected node is returned'
            result.size() == 1
            result[0].xpath == "/bookstore/categories[@code='1']"
    }

}
