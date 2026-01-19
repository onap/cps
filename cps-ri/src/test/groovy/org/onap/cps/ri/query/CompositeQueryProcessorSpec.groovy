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

    def 'Process composite query with no conditions returns all fetched descendants.'() {
        given: 'a query with only a base cpsPath targeting the Children category'
            def query = CompositeQuery.builder().cpsPath("//categories[@name='Children']").build()
        and: 'the repository returns the Children category as the base fragment'
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >> [catChildren]
        when: 'the composite query is processed with all descendants'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'one DataNode is returned for the Children category'
            result.size() == 1
        and: 'the returned node has the correct xpath'
            result[0].xpath == "/bookstore/categories[@code='1']"
        and: 'both books are present as child nodes'
            result[0].childDataNodes.size() == 2
    }

    def 'Process composite query returns empty result when base cpsPath matches nothing.'() {
        given: 'a query targeting a category that does not exist'
            def query = CompositeQuery.builder().cpsPath("//categories[@name='NonExistent']").build()
        and: 'the repository returns no matching fragments'
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >> []
        when: 'the composite query is processed'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the result is empty'
            result.isEmpty()
    }

    def 'Process composite query with AND operator returns categories that have a matching book.'() {
        given: 'a query on categories with an AND condition requiring a book with price=20'
            def condition = CompositeQuery.builder().cpsPath("//books[@price=20]").build()
            def query = CompositeQuery.builder().cpsPath('//categories').operator('and').conditions([condition]).build()
        and: 'the base query returns all three categories'
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >>> [[catChildren, catThriller, catComedy], [bookMatilda]]
        when: 'the composite query is processed'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'only the Children category is returned because it contains Matilda (price=20)'
            result.size() == 1
            result[0].xpath == "/bookstore/categories[@code='1']"
    }

    def 'Process composite query with AND operator returns empty when no category satisfies the condition.'() {
        given: 'a query on categories with an AND condition for a price that no book has'
            def condition = CompositeQuery.builder().cpsPath("//books[@price=999]").build()
            def query = CompositeQuery.builder().cpsPath('//categories').operator('and').conditions([condition]).build()
        and: 'the base query returns all categories, but the condition matches no books'
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >>> [[catChildren, catThriller, catComedy], []]
        when: 'the composite query is processed'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the result is empty because no category has a book with price=999'
            result.isEmpty()
    }

    def 'Process composite query with OR operator returns categories matching any condition.'() {
        given: 'a query on categories with OR conditions for Children or Comedy'
            def conditionChildren = CompositeQuery.builder().cpsPath("//categories[@name='Children']").build()
            def conditionComedy   = CompositeQuery.builder().cpsPath("//categories[@name='Comedy']").build()
            def query = CompositeQuery.builder().cpsPath('//categories').operator('or').conditions([conditionChildren, conditionComedy]).build()
        and: 'the base query returns all categories; conditions return Children and Comedy respectively'
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >>> [[catChildren, catThriller, catComedy], [catChildren], [catComedy]]
        when: 'the composite query is processed'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'Children and Comedy categories are returned'
            result.size() == 2
            result.collect { it.xpath }.containsAll(["/bookstore/categories[@code='1']", "/bookstore/categories[@code='3']"])
        and: 'the Thriller category is not returned'
            !result.collect { it.xpath }.contains("/bookstore/categories[@code='2']")
    }

    def 'Process composite query with OMIT_DESCENDANTS returns nodes without child nodes.'() {
        given: 'a query with no conditions on categories'
            def query = CompositeQuery.builder().cpsPath('//categories').build()
        and: 'the repository returns all three categories'
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >> [catChildren, catThriller, catComedy]
        when: 'the composite query is processed with OMIT_DESCENDANTS'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.OMIT_DESCENDANTS)
        then: 'all three categories are returned'
            result.size() == 3
        and: 'none have child data nodes'
            result.every { it.childDataNodes.isEmpty() }
    }

    def 'Process composite query with a fragment that has null attributes returns empty leaves.'() {
        given: 'a category fragment with null attributes (covers parseLeaves null branch)'
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

    def 'Process composite query where fragment has null childFragments covers addFragmentAndDescendantsToMap null branch.'() {
        given: 'a category fragment with null childFragments'
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

    def 'Process composite query where a matched node is also an ancestor of another match covers findAncestorIds parent-already-in-matchingIds branch.'() {
        given: 'a nested structure: bookstore contains catChildren which contains bookMatilda'
        and: 'a query with AND condition that matches both catChildren and bookMatilda'
            def condition = CompositeQuery.builder().cpsPath("//books[@price=20]").build()
            def query = CompositeQuery.builder().cpsPath('//categories').operator('and').conditions([condition]).build()
        and: 'the base query returns catChildren; the condition query returns bookMatilda (child of catChildren)'
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >>> [[catChildren], [bookMatilda]]
        when: 'the composite query is processed'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'one result is returned for catChildren'
            result.size() == 1
            result[0].xpath == "/bookstore/categories[@code='1']"
    }

    def 'Process composite query with AND condition covering getFilteredChildDataNodes when node is in matchingIds but has no matched descendants (returns all children).'() {
        given: 'a query on bookstore with AND condition requiring Children category'
            def condition = CompositeQuery.builder().cpsPath("//categories[@name='Children']").build()
            def query = CompositeQuery.builder().cpsPath('/bookstore').operator('and').conditions([condition]).build()
        and: 'the base query returns the bookstore; the condition returns catChildren'
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >>> [[bookstore], [catChildren]]
        when: 'the composite query is processed with all descendants'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'one result is returned for the bookstore'
            result.size() == 1
            result[0].xpath == '/bookstore'
        and: 'catChildren is present as a child of bookstore'
            result[0].childDataNodes.collect { it.xpath }.contains("/bookstore/categories[@code='1']")
    }

    def 'Process composite query with AND condition where matching node itself has matching descendants covers getFilteredChildDataNodes hasMatchedDescendants-true branch.'() {
        given: 'a query on categories with AND condition requiring a matching book'
            def condition = CompositeQuery.builder().cpsPath("//books[@price=20]").build()
            def query = CompositeQuery.builder().cpsPath('//categories').operator('and').conditions([condition]).build()
        and: 'base query returns catChildren and catThriller; condition returns bookMatilda which is child of catChildren'
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >>> [[catChildren, catThriller], [bookMatilda]]
        when: 'the composite query is processed with all descendants'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'only catChildren is returned because it has bookMatilda'
            result.size() == 1
            result[0].xpath == "/bookstore/categories[@code='1']"
        and: 'bookMatilda is present as a child'
            result[0].childDataNodes.collect { it.xpath }.contains("/bookstore/categories[@code='1']/books[@title='Matilda']")
    }

    def 'Process composite query with nested condition using OR operator and non-empty nested matches covers evaluateCondition nested non-empty path.'() {
        given: 'a deeply-nested condition: categories with an OR condition requiring children categories having a specific book'
            def innerCondition = CompositeQuery.builder().cpsPath("//books[@price=15]").build()
            def condition = CompositeQuery.builder().cpsPath('//categories').operator('or').conditions([innerCondition]).build()
            def query = CompositeQuery.builder().cpsPath('/bookstore').operator('and').conditions([condition]).build()
        and: 'first call returns bookstore; second call returns catChildren and catThriller; third call returns bookGruffalo and bookAnnihilation (price=15)'
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >>> [
                [bookstore],
                [catChildren, catThriller],
                [bookGruffalo, bookAnnihilation]
            ]
        when: 'the composite query is processed'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the bookstore is returned'
            result.size() == 1
            result[0].xpath == '/bookstore'
    }

    def 'Process composite query with nested condition using AND operator with empty nested matches covers evaluateCondition AND empty path.'() {
        given: 'a condition with AND nested condition that will produce empty nested matches'
            def innerCondition = CompositeQuery.builder().cpsPath("//books[@price=999]").build()
            def condition = CompositeQuery.builder().cpsPath('//categories').operator('and').conditions([innerCondition]).build()
            def query = CompositeQuery.builder().cpsPath('/bookstore').operator('and').conditions([condition]).build()
        and: 'base returns bookstore; second call returns catChildren; inner condition returns empty (no books at price 999)'
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >>> [
                [bookstore],
                [catChildren, catThriller, catComedy],
                []
            ]
        when: 'the composite query is processed'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the result is empty because AND with no match fails'
            result.isEmpty()
    }

    def 'Process composite query with nested condition using OR operator and empty nested matches covers evaluateCondition OR empty path.'() {
        given: 'a condition with OR nested condition that will produce empty nested matches'
            def innerCondition = CompositeQuery.builder().cpsPath("//books[@price=999]").build()
            def condition = CompositeQuery.builder().cpsPath('//categories').operator('or').conditions([innerCondition]).build()
            def query = CompositeQuery.builder().cpsPath('/bookstore').operator('and').conditions([condition]).build()
        and: 'base returns bookstore; second call returns categories; inner condition returns empty (no books at price 999)'
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >>> [
                [bookstore],
                [catChildren, catThriller, catComedy],
                []
            ]
        when: 'the composite query is processed'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'OR operator returns fragment ids even when nested is empty'
            !result.isEmpty()
    }

    def 'Process composite query with hasMatchingDescendant covers direct child-in-relevantIds branch.'() {
        given: 'a query with AND condition that returns a book (leaf node) as a match'
            def condition = CompositeQuery.builder().cpsPath("//books[@price=15]").build()
            def query = CompositeQuery.builder().cpsPath('//categories').operator('and').conditions([condition]).build()
        and: 'base query returns catThriller; condition returns bookAnnihilation which is a direct child of catThriller'
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >>> [[catThriller], [bookAnnihilation]]
        when: 'the composite query is processed'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'catThriller is returned'
            result.size() == 1
            result[0].xpath == "/bookstore/categories[@code='2']"
        and: 'bookAnnihilation is present as a child'
            result[0].childDataNodes.collect { it.xpath }.contains("/bookstore/categories[@code='2']/books[@title='Annihilation']")
    }

    def 'Process composite query where root has null childFragments covers hasMatchingDescendant and getAllChildDataNodes null-children branches.'() {
        given: 'a root fragment with null childFragments'
            def rootNullChildren = new FragmentEntity(id: 50L, xpath: '/bookstore', parentId: null, attributes: '{"name":"Store"}', anchor: anchorEntity, childFragments: null)
        and: 'a query with no conditions'
            def query = CompositeQuery.builder().cpsPath('/bookstore').build()
        and: 'the repository returns the fragment'
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >> [rootNullChildren]
        when: 'the composite query is processed with all descendants'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the root node is returned'
            result.size() == 1
        and: 'the node has no children'
            result[0].childDataNodes.isEmpty()
    }

    def 'Process composite query with deeply nested structure covers collectDescendantsFromFragment recursion.'() {
        given: 'a three-level hierarchy: bookstore -> catChildren -> bookMatilda -> (leaf fragment with no children)'
            def leafFragment = new FragmentEntity(id: 100L, xpath: "/bookstore/categories[@code='1']/books[@title='Matilda']/chapter[@num='1']", parentId: 4L, attributes: '{"title":"The Chocolatier"}', anchor: anchorEntity, childFragments: [] as Set)
            def deepBook = new FragmentEntity(id: 4L, xpath: "/bookstore/categories[@code='1']/books[@title='Matilda']", parentId: 2L, attributes: '{"price":20}', anchor: anchorEntity, childFragments: [leafFragment] as Set)
            def deepCat = new FragmentEntity(id: 2L, xpath: "/bookstore/categories[@code='1']", parentId: 1L, attributes: '{"name":"Children"}', anchor: anchorEntity, childFragments: [deepBook] as Set)
            def deepStore = new FragmentEntity(id: 1L, xpath: '/bookstore', parentId: null, attributes: '{"bookstore-name":"Easons"}', anchor: anchorEntity, childFragments: [deepCat] as Set)
        and: 'a query with AND condition on a deep leaf chapter'
            def condition = CompositeQuery.builder().cpsPath("//chapter[@num='1']").build()
            def query = CompositeQuery.builder().cpsPath('/bookstore').operator('and').conditions([condition]).build()
        and: 'base returns bookstore; condition returns the leaf chapter'
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >>> [[deepStore], [leafFragment]]
        when: 'the composite query is processed with all descendants'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the bookstore is returned'
            result.size() == 1
            result[0].xpath == '/bookstore'
    }

    // ── getAllChildDataNodes: !hasNext() branch ───────────────────────────────
    def 'getAllChildDataNodes stops recursion when OMIT_DESCENDANTS is passed into toDataNodeWithAllDescendants.'() {
        given: 'a matching node whose child is a matched node with its own children'
            def grandchild = new FragmentEntity(id: 9L, xpath: "/bookstore/categories[@code='1']/books[@title='Matilda']/page[@num='1']", parentId: 4L, attributes: '{}', anchor: anchorEntity, childFragments: [] as Set)
            def bookWithChild = new FragmentEntity(id: 4L, xpath: "/bookstore/categories[@code='1']/books[@title='Matilda']", parentId: 2L, attributes: '{"price":20}', anchor: anchorEntity, childFragments: [grandchild] as Set)
            def cat = new FragmentEntity(id: 2L, xpath: "/bookstore/categories[@code='1']", parentId: null, attributes: '{"name":"Children"}', anchor: anchorEntity, childFragments: [bookWithChild] as Set)
        and: 'a query with AND condition matching the category (no matched descendants of cat, so toDataNodeWithAllDescendants is called)'
            def condition = CompositeQuery.builder().cpsPath("//categories[@name='Children']").build()
            def query = CompositeQuery.builder().cpsPath('//categories').operator('and').conditions([condition]).build()
        and: 'base returns cat; condition returns cat itself (so cat is in matchingIds with no matched child)'
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >>> [[cat], [cat]]
        when: 'processed with DIRECT_CHILDREN_ONLY (depth=1) so that next() produces depth=0 → !hasNext())'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.DIRECT_CHILDREN_ONLY)
        then: 'the category is returned'
            result.size() == 1
            result[0].xpath == "/bookstore/categories[@code='1']"
        and: 'the direct child book is included but its grandchild page is not (getAllChildDataNodes returned empty due to !hasNext())'
            result[0].childDataNodes.size() == 1
            result[0].childDataNodes[0].xpath == "/bookstore/categories[@code='1']/books[@title='Matilda']"
            result[0].childDataNodes[0].childDataNodes.isEmpty()
    }

    // ── hasMatchingDescendant: empty children set (not null) returns false ────
    def 'hasMatchingDescendant returns false when a fragment has an empty (not null) children set and is not in relevantIds.'() {
        given: 'a root that has two children: one with empty children set and one that matches'
            def nonMatchingLeaf  = new FragmentEntity(id: 20L, xpath: '/bookstore/other', parentId: 1L, attributes: '{}', anchor: anchorEntity, childFragments: [] as Set)
            def matchingLeaf     = new FragmentEntity(id: 21L, xpath: '/bookstore/match',  parentId: 1L, attributes: '{}', anchor: anchorEntity, childFragments: [] as Set)
            def root = new FragmentEntity(id: 1L, xpath: '/bookstore', parentId: null, attributes: '{}', anchor: anchorEntity, childFragments: [nonMatchingLeaf, matchingLeaf] as Set)
        and: 'a no-conditions query so all flattened ids become matchingIds'
            def query = CompositeQuery.builder().cpsPath('/bookstore').build()
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >> [root]
        when: 'processed with all descendants'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'both children are present – confirming the empty-children branch was traversed without error'
            result.size() == 1
            result[0].childDataNodes.size() == 2
    }

    // ── hasMatchingDescendant: match via grandchild (recursive true branch) ──
    def 'hasMatchingDescendant returns true when only a grandchild of the root is in relevantIds.'() {
        given: 'root is NOT in matchingIds but its grandchild is'
            def grandchild = new FragmentEntity(id: 30L, xpath: "/bookstore/categories[@code='1']/books[@title='Matilda']", parentId: 20L, attributes: '{"price":20}', anchor: anchorEntity, childFragments: [] as Set)
            def child      = new FragmentEntity(id: 20L, xpath: "/bookstore/categories[@code='1']", parentId: 1L, attributes: '{}', anchor: anchorEntity, childFragments: [grandchild] as Set)
            def root       = new FragmentEntity(id: 1L,  xpath: '/bookstore', parentId: null, attributes: '{}', anchor: anchorEntity, childFragments: [child] as Set)
        and: 'AND condition that matches only the grandchild'
            def condition = CompositeQuery.builder().cpsPath("//books[@price=20]").build()
            def query = CompositeQuery.builder().cpsPath('/bookstore').operator('and').conditions([condition]).build()
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >>> [[root], [grandchild]]
        when: 'processed with all descendants'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'root is included because hasMatchingDescendant found the grandchild via recursion'
            result.size() == 1
            result[0].xpath == '/bookstore'
    }

    // ── collectDescendantsFromFragment: null children early-return ────────────
    def 'collectDescendantsFromFragment returns immediately when a fragment has null childFragments.'() {
        given: 'a fragment with null children used as a condition match inside evaluateCondition'
            def leafNull = new FragmentEntity(id: 40L, xpath: "/bookstore/categories[@code='X']", parentId: 1L, attributes: '{}', anchor: anchorEntity, childFragments: null)
            def root     = new FragmentEntity(id: 1L, xpath: '/bookstore', parentId: null, attributes: '{}', anchor: anchorEntity, childFragments: [leafNull] as Set)
        and: 'AND condition whose evaluateCondition scopes via getScopedFragmentsFromFlatMap → collectDescendantsFromFragment(leafNull, …)'
            def innerCond = CompositeQuery.builder().cpsPath("//books[@price=20]").build()
            def condition = CompositeQuery.builder().cpsPath("//categories").operator('and').conditions([innerCond]).build()
            def query     = CompositeQuery.builder().cpsPath('/bookstore').operator('and').conditions([condition]).build()
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >>> [[root], [leafNull], []]
        when: 'processed – no exception means the null-children guard was hit correctly'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'result is empty because the inner AND condition found no books'
            result.isEmpty()
    }

    // ── collectDescendantsFromFragment: duplicate-id guard (add returns false) ─
    def 'collectDescendantsFromFragment skips already-visited child ids to prevent infinite loops.'() {
        given: 'a fragment whose child appears twice in the children set (simulated via two entries sharing an id concept via nested AND conditions that revisit same scoped fragments)'
            def sharedChild = new FragmentEntity(id: 50L, xpath: "/bookstore/categories[@code='1']/books[@title='Matilda']", parentId: 2L, attributes: '{"price":20}', anchor: anchorEntity, childFragments: [] as Set)
            def cat  = new FragmentEntity(id: 2L, xpath: "/bookstore/categories[@code='1']", parentId: 1L, attributes: '{"name":"Children"}', anchor: anchorEntity, childFragments: [sharedChild] as Set)
            def root = new FragmentEntity(id: 1L, xpath: '/bookstore', parentId: null, attributes: '{}', anchor: anchorEntity, childFragments: [cat] as Set)
        and: 'two AND conditions both pointing to the same book, forcing getScopedFragmentsFromFlatMap to call collectDescendants twice for same parent'
            def cond1 = CompositeQuery.builder().cpsPath("//books[@price=20]").build()
            def cond2 = CompositeQuery.builder().cpsPath("//books[@price=20]").build()
            def condition = CompositeQuery.builder().cpsPath('//categories').operator('and').conditions([cond1, cond2]).build()
            def query     = CompositeQuery.builder().cpsPath('/bookstore').operator('and').conditions([condition]).build()
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >>> [[root], [cat], [sharedChild], [sharedChild]]
        when: 'processed without error'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'bookstore is returned'
            result.size() == 1
            result[0].xpath == '/bookstore'
    }

    // ── evaluateCondition: repo fragment filtered out (not in flatMap) ─────────
    def 'evaluateCondition filters out fragments returned by repository that are not in the flatMap.'() {
        given: 'the condition repo call returns a fragment whose id is NOT among the base fragments'
            def outsiderBook = new FragmentEntity(id: 999L, xpath: "/other/books[@title='X']", parentId: null, attributes: '{}', anchor: anchorEntity, childFragments: [] as Set)
            def condition = CompositeQuery.builder().cpsPath("//books[@price=20]").build()
            def query     = CompositeQuery.builder().cpsPath('//categories').operator('and').conditions([condition]).build()
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >>> [[catChildren], [outsiderBook]]
        when: 'processed'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'result is empty because the outsider fragment was filtered and AND intersection is empty'
            result.isEmpty()
    }

    // ── findAncestorIds: parent IS in matchingIds (line-77 false branch) ───────
    def 'findAncestorIds does not add parentId to ancestorIds when the parent is itself a matching fragment.'() {
        given: 'both catChildren (id=2) and bookMatilda (id=4) are in matchingIds'
            def condition = CompositeQuery.builder().cpsPath("//books[@price=20]").build()
            def query     = CompositeQuery.builder().cpsPath('//categories').operator('and').conditions([condition]).build()
        and: 'base returns catChildren; condition also returns bookMatilda (child), so both end up in matchingIds'
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >>> [[catChildren], [bookMatilda]]
        when: 'processed'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'catChildren is returned (parentId of bookMatilda IS in matchingIds so it is not added to ancestorIds separately)'
            result.size() == 1
            result[0].xpath == "/bookstore/categories[@code='1']"
    }

    // ── findAncestorIds: fragment not in flatMap (null lookup on line 80) ──────
    def 'findAncestorIds stops walking when a parentId has no corresponding entry in the flatMap.'() {
        given: 'bookMatilda references parentId=2 but catChildren is NOT in the prefetched flat map'
            def isolatedBook = new FragmentEntity(id: 4L, xpath: "/bookstore/categories[@code='1']/books[@title='Matilda']", parentId: 2L, attributes: '{"price":20}', anchor: anchorEntity, childFragments: [] as Set)
        and: 'a no-conditions query that returns only the isolated book (parent not in flatMap)'
            def query = CompositeQuery.builder().cpsPath("//books[@price=20]").build()
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >> [isolatedBook]
        when: 'processed'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the isolated book is still returned'
            result.size() == 1
            result[0].xpath == "/bookstore/categories[@code='1']/books[@title='Matilda']"
    }

    // ── getScopedFragmentsFromFlatMap: parentId not in flatMap ────────────────
    def 'getScopedFragmentsFromFlatMap handles a parentId that is absent from the flatMap without error.'() {
        given: 'condition repo returns a fragment with id=999 which is not present in the flat map built from catChildren'
            def ghostBook = new FragmentEntity(id: 999L, xpath: "/other/books[@title='Ghost']", parentId: null, attributes: '{}', anchor: anchorEntity, childFragments: [] as Set)
            def condition = CompositeQuery.builder().cpsPath("//books[@price=20]").build()
            def query     = CompositeQuery.builder().cpsPath('//categories').operator('and').conditions([condition]).build()
        and: 'base returns catChildren; condition returns ghostBook (not in flatMap → fragmentIds is empty → scoped is empty)'
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >>> [[catChildren], [ghostBook]]
        when: 'processed'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'result is empty because AND retains nothing'
            result.isEmpty()
    }

    // ── applyConditionsOnFlatMap: null conditions branch ─────────────────────
    def 'applyConditionsOnFlatMap returns all flatMap keys when conditions list is null.'() {
        given: 'a CompositeQuery built with null conditions (builder default) — covers the null-conditions early-return'
            def query = CompositeQuery.builder().cpsPath('//categories').conditions(null).build()
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >> [catChildren, catThriller]
        when: 'processed'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.OMIT_DESCENDANTS)
        then: 'all base fragments are returned unchanged'
            result.size() == 2
            result.collect { it.xpath }.containsAll(["/bookstore/categories[@code='1']", "/bookstore/categories[@code='2']"])
    }

    // ── convertToDataNodes: matchingIds empty ────────────────────────────────
    def 'convertToDataNodes returns empty collection immediately when no fragments satisfy the AND condition.'() {
        given: 'AND condition that matches nothing, so after applyConditionsOnFlatMap matchingIds is empty'
            def condition = CompositeQuery.builder().cpsPath("//books[@price=999]").build()
            def query     = CompositeQuery.builder().cpsPath('//categories').operator('and').conditions([condition]).build()
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >>> [[catChildren, catThriller, catComedy], []]
        when: 'processed'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the collection is empty (convertToDataNodes early-return triggered)'
            result.isEmpty()
    }

    // ── getFilteredChildDataNodes: null childFragments branch ────────────────
    def 'getFilteredChildDataNodes returns empty list when a matching fragment has null childFragments.'() {
        given: 'a fragment with null childFragments that is in matchingIds'
            def nullChildCat = new FragmentEntity(id: 2L, xpath: "/bookstore/categories[@code='1']", parentId: null, attributes: '{"name":"Children"}', anchor: anchorEntity, childFragments: null)
        and: 'no-conditions query so it lands in matchingIds'
            def query = CompositeQuery.builder().cpsPath('//categories').build()
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >> [nullChildCat]
        when: 'processed with all descendants'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'node is returned but has no children (null-childFragments guard in getFilteredChildDataNodes)'
            result.size() == 1
            result[0].childDataNodes.isEmpty()
    }

    // ── getFilteredChildDataNodes: !hasNext() branch ─────────────────────────
    def 'getFilteredChildDataNodes returns empty list when fetchDescendantsOption has no next level.'() {
        given: 'catChildren with books as children, queried with OMIT_DESCENDANTS'
            def query = CompositeQuery.builder().cpsPath('//categories').build()
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >> [catChildren]
        when: 'processed with OMIT_DESCENDANTS (!hasNext() is true)'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.OMIT_DESCENDANTS)
        then: 'category is returned without any children'
            result.size() == 1
            result[0].childDataNodes.isEmpty()
    }

    // ── getFilteredChildDataNodes: fragment in matchingIds with a grandchild match (hasMatchedDescendants via recursion) ──
    def 'getFilteredChildDataNodes detects a matched descendant via recursion when the direct child is not matched but grandchild is.'() {
        given: 'grandchild is the only match; direct child and category are ancestors'
            def grandchild = new FragmentEntity(id: 9L,  xpath: "/bookstore/categories[@code='1']/books[@title='Matilda']/page[@num='1']", parentId: 4L, attributes: '{"num":1}', anchor: anchorEntity, childFragments: [] as Set)
            def book       = new FragmentEntity(id: 4L,  xpath: "/bookstore/categories[@code='1']/books[@title='Matilda']",                 parentId: 2L, attributes: '{"price":20}', anchor: anchorEntity, childFragments: [grandchild] as Set)
            def cat        = new FragmentEntity(id: 2L,  xpath: "/bookstore/categories[@code='1']",                                         parentId: null, attributes: '{"name":"Children"}', anchor: anchorEntity, childFragments: [book] as Set)
        and: 'AND condition that matches only the grandchild'
            def condition = CompositeQuery.builder().cpsPath("//page[@num=1]").build()
            def query     = CompositeQuery.builder().cpsPath('//categories').operator('and').conditions([condition]).build()
            mockFragmentRepositoryCpsPathQuery.findByAnchorAndCpsPath(_, _, _) >>> [[cat], [grandchild]]
        when: 'processed with all descendants'
            def result = objectUnderTest.processCompositeQuery(anchorEntity, query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'category is returned and the grandchild is reachable via the child'
            result.size() == 1
            result[0].xpath == "/bookstore/categories[@code='1']"
            result[0].childDataNodes[0].xpath == "/bookstore/categories[@code='1']/books[@title='Matilda']"
            result[0].childDataNodes[0].childDataNodes[0].xpath == "/bookstore/categories[@code='1']/books[@title='Matilda']/page[@num='1']"
    }
}
