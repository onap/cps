/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 Deutsche Telekom AG
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.impl.query
import org.onap.cps.api.model.CompositeQuery
import org.onap.cps.api.model.DataNode
import org.onap.cps.api.parameters.FetchDescendantsOption
import org.onap.cps.spi.CpsDataPersistenceService
import spock.lang.Shared
import spock.lang.Specification

class CompositeQueryExecutionSpec extends Specification {
    def mockCpsDataPersistenceService = Mock(CpsDataPersistenceService)
    @Shared def DATASPACE = 'bookstoreDataspace'
    @Shared def ANCHOR    = 'bookstoreAnchor'
    @Shared
    def bookMatilda  = new DataNode(xpath: "/bookstore/categories[@code='1']/books[@title='Matilda']",
        leaves: [price: 20], dataspace: DATASPACE, anchorName: ANCHOR, childDataNodes: [])
    @Shared
    def bookGruffalo = new DataNode(xpath: "/bookstore/categories[@code='1']/books[@title='The Gruffalo']",
        leaves: [price: 15], dataspace: DATASPACE, anchorName: ANCHOR, childDataNodes: [])
    @Shared
    def categoryChildren = new DataNode(xpath: "/bookstore/categories[@code='1']",
        leaves: [name: 'Children'], dataspace: DATASPACE, anchorName: ANCHOR,
        childDataNodes: [bookMatilda, bookGruffalo])
    @Shared
    def categoryThriller = new DataNode(xpath: "/bookstore/categories[@code='2']",
        leaves: [name: 'Thriller'], dataspace: DATASPACE, anchorName: ANCHOR, childDataNodes: [])
    @Shared
    def categoryComedy   = new DataNode(xpath: "/bookstore/categories[@code='3']",
        leaves: [name: 'Comedy'], dataspace: DATASPACE, anchorName: ANCHOR, childDataNodes: [])
    @Shared
    def categoryChildrenNoDescendants = new DataNode(xpath: "/bookstore/categories[@code='1']",
        leaves: [name: 'Children'], dataspace: DATASPACE, anchorName: ANCHOR, childDataNodes: [])
    @Shared
    def categoryThrillerNoDescendants = new DataNode(xpath: "/bookstore/categories[@code='2']",
        leaves: [name: 'Thriller'], dataspace: DATASPACE, anchorName: ANCHOR, childDataNodes: [])
    @Shared
    def categoryComedyNoDescendants   = new DataNode(xpath: "/bookstore/categories[@code='3']",
        leaves: [name: 'Comedy'], dataspace: DATASPACE, anchorName: ANCHOR, childDataNodes: [])

    def processCompositeQuery(compositeQuery, fetchDescendantsOption) {
        CompositeQueryExecution.processCompositeQuery(mockCpsDataPersistenceService,
            DATASPACE, ANCHOR, compositeQuery, fetchDescendantsOption)
    }

    def 'Composite query with only a base cps path.'() {
        given: 'a query with no conditions'
            def query = CompositeQuery.builder().cpsPath("//categories[@name='Children']").build()
        and: 'persistence returns a category with two book children'
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, "//categories[@name='Children']",
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> [categoryChildren]
        when: 'the composite query is processed'
            def result = processCompositeQuery(query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the category node is returned with both book children'
            result.size() == 1
            result[0].xpath == "/bookstore/categories[@code='1']"
            result[0].childDataNodes.size() == 2
    }

    def 'Composite query where base cps path has no results.'() {
        given: 'a query targeting a path that does not exist'
            def query = CompositeQuery.builder().cpsPath("//categories[@name='NonExistent']").build()
        and: 'persistence returns nothing'
            mockCpsDataPersistenceService.queryDataNodes(*_) >> []
        when: 'the composite query is processed'
            def result = processCompositeQuery(query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the result is empty'
            result.isEmpty()
    }

    def 'Composite query with AND operator #scenario.'() {
        given: 'a query fetching all categories AND books matching a price condition'
            def condition = CompositeQuery.builder().cpsPath(conditionPath).build()
            def query = CompositeQuery.builder().cpsPath('//categories').operator('and').conditions([condition]).build()
        and: 'persistence returns all categories for the base path and matching nodes for the condition'
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//categories',
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> [categoryChildren, categoryThriller, categoryComedy]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, conditionPath,
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> conditionNodes
        when: 'the composite query is processed'
            def result = processCompositeQuery(query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'only the expected number of results are returned'
            result.size() == expectedSize
        where: 'the following conditions are evaluated'
            scenario              | conditionPath         | conditionNodes || expectedSize
            'matching condition'  | '//books[@price=20]'  | [bookMatilda]  || 1
            'no matching results' | '//books[@price=999]' | []             || 0
    }

    def 'Composite query with OR operator with multiple matching conditions.'() {
        given: 'a query fetching all categories OR Children OR Comedy'
            def condition1 = CompositeQuery.builder().cpsPath("//categories[@name='Children']").build()
            def condition2 = CompositeQuery.builder().cpsPath("//categories[@name='Comedy']").build()
            def query = CompositeQuery.builder().cpsPath('//categories').operator('or')
                .conditions([condition1, condition2]).build()
        and: 'persistence returns all categories for base; each condition returns one category'
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//categories',
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> [categoryChildren, categoryThriller, categoryComedy]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, "//categories[@name='Children']",
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> [categoryChildren]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, "//categories[@name='Comedy']",
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> [categoryComedy]
        when: 'the composite query is processed'
            def result = processCompositeQuery(query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'Children and Comedy categories are returned'
            result.size() == 2
            result.collect { it.xpath }.containsAll([
                "/bookstore/categories[@code='1']",
                "/bookstore/categories[@code='3']"
            ])
        and: 'the Thriller category is excluded'
            !result.collect { it.xpath }.contains("/bookstore/categories[@code='2']")
    }

    def 'Composite query with #scenario conditions.'() {
        given: 'a query with null or empty conditions list'
            def query = CompositeQuery.builder().cpsPath('//categories').operator('and')
                .conditions(conditions).build()
        and: 'persistence returns all three categories'
            mockCpsDataPersistenceService.queryDataNodes(*_) >> [categoryChildren, categoryThriller, categoryComedy]
        when: 'the composite query is processed'
            def result = processCompositeQuery(query, FetchDescendantsOption.OMIT_DESCENDANTS)
        then: 'all three categories are returned because there is no condition to filter by'
            result.size() == 3
        where: 'the following conditions lists are used'
            scenario | conditions
            'null'   | null
            'empty'  | []
    }

    def 'Composite query with OMIT_DESCENDANTS fetch option.'() {
        given: 'a query with no conditions'
            def query = CompositeQuery.builder().cpsPath('//categories').build()
        and: 'persistence returns all categories'
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//categories',
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> [categoryChildren, categoryThriller, categoryComedy]
        when: 'the composite query is processed with OMIT_DESCENDANTS'
            def result = processCompositeQuery(query, FetchDescendantsOption.OMIT_DESCENDANTS)
        then: 'all three categories are returned with no child data nodes'
            result.size() == 3
            result.every { it.childDataNodes.isEmpty() }
    }

    def 'Composite query with AND condition where only a descendant of the base node matches.'() {
        given: 'a query with AND condition matching only a book (child of a category)'
            def condition = CompositeQuery.builder().cpsPath('//books[@price=20]').build()
            def query = CompositeQuery.builder().cpsPath('//categories').operator('and').conditions([condition]).build()
        and: 'base returns categoryChildren; condition returns bookMatilda'
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//categories',
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> [categoryChildren]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//books[@price=20]',
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> [bookMatilda]
        when: 'the composite query is processed with all descendants'
            def result = processCompositeQuery(query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the category is returned even though only its descendant matched the condition'
            result.size() == 1
            result[0].xpath == "/bookstore/categories[@code='1']"
        and: 'bookMatilda is present as a child'
            result[0].childDataNodes.collect { it.xpath }
                .contains("/bookstore/categories[@code='1']/books[@title='Matilda']")
    }

    def 'Composite query with AND condition where the base node itself matches the condition.'() {
        given: 'a query with AND condition matching only the category itself'
            def condition = CompositeQuery.builder().cpsPath("//categories[@name='Children']").build()
            def query = CompositeQuery.builder().cpsPath('//categories').operator('and').conditions([condition]).build()
        and: 'base and condition both return categoryChildren'
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//categories',
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> [categoryChildren]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, "//categories[@name='Children']",
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> [categoryChildren]
        when: 'the composite query is processed with all descendants'
            def result = processCompositeQuery(query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the category is returned with all its books'
            result.size() == 1
            result[0].childDataNodes.size() == 2
            result[0].childDataNodes.collect { it.xpath }.containsAll([
                "/bookstore/categories[@code='1']/books[@title='Matilda']",
                "/bookstore/categories[@code='1']/books[@title='The Gruffalo']"
            ])
    }

    def 'Composite query with depth-limited fetch option.'() {
        given: 'a three-level hierarchy: category -> book -> leaf'
            def leaf         = new DataNode(xpath: "/bookstore/categories[@code='1']/books[@title='Matilda']/leaf",
                leaves: [type: 'special'], dataspace: DATASPACE, anchorName: ANCHOR, childDataNodes: [])
            def bookWithLeaf = new DataNode(xpath: "/bookstore/categories[@code='1']/books[@title='Matilda']",
                leaves: [price: 20], dataspace: DATASPACE, anchorName: ANCHOR, childDataNodes: [leaf])
            def category     = new DataNode(xpath: "/bookstore/categories[@code='1']",
                leaves: [name: 'Children'], dataspace: DATASPACE, anchorName: ANCHOR, childDataNodes: [bookWithLeaf])
        and: 'a query with AND condition where the category itself matches'
            def condition = CompositeQuery.builder().cpsPath("//categories[@name='Children']").build()
            def query = CompositeQuery.builder().cpsPath('//categories').operator('and').conditions([condition]).build()
        and: 'base and condition both return the category'
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//categories',
                FetchDescendantsOption.DIRECT_CHILDREN_ONLY, 0) >> [category]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, "//categories[@name='Children']",
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> [category]
        when: 'the composite query is processed with direct children only'
            def result = processCompositeQuery(query, FetchDescendantsOption.DIRECT_CHILDREN_ONLY)
        then: 'the category is returned with the book as a direct child'
            result.size() == 1
            result[0].childDataNodes.size() == 1
            result[0].childDataNodes[0].xpath == "/bookstore/categories[@code='1']/books[@title='Matilda']"
        and: 'the leaf is omitted because the fetch depth is exhausted'
            result[0].childDataNodes[0].childDataNodes.isEmpty()
    }

    def 'Composite query with nested AND conditions scoped to parent match.'() {
        given: 'a three-level nested query: categories AND (Children AND books[@price=20])'
            def subCondition = CompositeQuery.builder().cpsPath('//books[@price=20]').build()
            def condition    = CompositeQuery.builder().cpsPath("//categories[@name='Children']")
                .operator('and').conditions([subCondition]).build()
            def query        = CompositeQuery.builder().cpsPath('//categories').operator('and')
                .conditions([condition]).build()
        and: 'base returns all categories; condition returns Children; sub-condition returns bookMatilda'
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//categories',
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> [categoryChildren, categoryThriller, categoryComedy]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, "//categories[@name='Children']",
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> [categoryChildren]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//books[@price=20]',
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> [bookMatilda]
        when: 'the composite query is processed'
            def result = processCompositeQuery(query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'only the Children category is returned'
            result.size() == 1
            result[0].xpath == "/bookstore/categories[@code='1']"
    }

    def 'Composite query with nested AND conditions where deepest condition has no match.'() {
        given: 'a nested query: categories AND (Children AND books[@price=999])'
            def subCondition = CompositeQuery.builder().cpsPath('//books[@price=999]').build()
            def condition    = CompositeQuery.builder().cpsPath("//categories[@name='Children']")
                .operator('and').conditions([subCondition]).build()
            def query        = CompositeQuery.builder().cpsPath('//categories').operator('and')
                .conditions([condition]).build()
        and: 'base returns categories with NO children — accurately reflecting OMIT_DESCENDANTS from DB'
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//categories',
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> [categoryChildrenNoDescendants,
                                                                categoryThrillerNoDescendants,
                                                                categoryComedyNoDescendants]
        and: 'the condition returns Children and the sub-condition returns nothing'
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, "//categories[@name='Children']",
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> [categoryChildren]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//books[@price=999]',
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> []
        when: 'the composite query is processed'
            def result = processCompositeQuery(query, FetchDescendantsOption.OMIT_DESCENDANTS)
        then: 'result is empty because AND requires all levels to match'
            result.isEmpty()
    }

    def 'Composite query with nested OR conditions where deepest condition has no match.'() {
        given: 'a nested query: categories OR (Children OR books[@price=999])'
            def subCondition = CompositeQuery.builder().cpsPath('//books[@price=999]').build()
            def condition    = CompositeQuery.builder().cpsPath("//categories[@name='Children']")
                .operator('or').conditions([subCondition]).build()
            def query        = CompositeQuery.builder().cpsPath('//categories').operator('or')
                .conditions([condition]).build()
        and: 'base returns categories with NO children — accurately reflecting OMIT_DESCENDANTS from DB'
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//categories',
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> [categoryChildrenNoDescendants,
                                                                categoryThrillerNoDescendants,
                                                                categoryComedyNoDescendants]
        and: 'the condition returns Children and the sub-condition returns nothing'
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, "//categories[@name='Children']",
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> [categoryChildren]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//books[@price=999]',
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> []
        when: 'the composite query is processed'
            def result = processCompositeQuery(query, FetchDescendantsOption.OMIT_DESCENDANTS)
        then: 'categoryChildren is returned because OR does not require all conditions to match'
            result.size() == 1
            result[0].xpath == "/bookstore/categories[@code='1']"
    }

    def 'Composite query with four-level hierarchy and all descendants fetch option.'() {
        given: 'a four-level hierarchy: category -> book -> chapter -> page where only the category matches the condition'
            def page    = new DataNode(xpath: '/lib/category/book/chapter/page',
                leaves: [:], dataspace: DATASPACE, anchorName: ANCHOR, childDataNodes: [])
            def chapter = new DataNode(xpath: '/lib/category/book/chapter',
                leaves: [:], dataspace: DATASPACE, anchorName: ANCHOR, childDataNodes: [page])
            def book    = new DataNode(xpath: '/lib/category/book',
                leaves: [:], dataspace: DATASPACE, anchorName: ANCHOR, childDataNodes: [chapter])
            def category = new DataNode(xpath: '/lib/category',
                leaves: [name: 'lib'], dataspace: DATASPACE, anchorName: ANCHOR, childDataNodes: [book])
        and: 'a query where only the category matches the AND condition'
            def condition = CompositeQuery.builder().cpsPath('/lib/category').build()
            def query = CompositeQuery.builder().cpsPath('/lib/category').operator('and').conditions([condition]).build()
        and: 'persistence returns the full four-level tree for both the base path and the condition path'
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '/lib/category',
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> [category]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '/lib/category',
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> [category]
        when: 'the composite query is processed with all descendants'
            def result = processCompositeQuery(query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the category is returned and the full descendant hierarchy is preserved'
            result.size() == 1
            result[0].xpath == '/lib/category'
            result[0].childDataNodes[0].xpath == '/lib/category/book'
            result[0].childDataNodes[0].childDataNodes[0].xpath == '/lib/category/book/chapter'
    }

    def 'Composite query with DIRECT_CHILDREN_ONLY fetch option and null condition list.'() {
        given: 'a query with a null conditions list so evaluateCondition skips the nested block'
            def condition = CompositeQuery.builder().cpsPath("//categories[@name='Children']").conditions(null).build()
            def query = CompositeQuery.builder().cpsPath('//categories').operator('and').conditions([condition]).build()
        and: 'persistence returns the Children category for base and condition; books are children of the category'
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//categories',
                FetchDescendantsOption.DIRECT_CHILDREN_ONLY, 0) >> [categoryChildren]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, "//categories[@name='Children']",
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> [categoryChildren]
        when: 'the composite query is processed with DIRECT_CHILDREN_ONLY'
            def result = processCompositeQuery(query, FetchDescendantsOption.DIRECT_CHILDREN_ONLY)
        then: 'the category is returned but its book children have no further descendants'
            result.size() == 1
            result[0].xpath == "/bookstore/categories[@code='1']"
            result[0].childDataNodes.every { it.childDataNodes.isEmpty() }
    }

    def 'Composite query with AND condition where sub-condition returns no results and null children are present.'() {
        given: 'a three-level hierarchy where grandchild has null children'
            def grandchild = new DataNode(xpath: '/ns/parent/child/grandchild',
                leaves: [:], dataspace: DATASPACE, anchorName: ANCHOR)
            grandchild.setChildDataNodes(null)
            def child  = new DataNode(xpath: '/ns/parent/child',
                leaves: [:], dataspace: DATASPACE, anchorName: ANCHOR, childDataNodes: [grandchild])
            def parent = new DataNode(xpath: '/ns/parent',
                leaves: [:], dataspace: DATASPACE, anchorName: ANCHOR, childDataNodes: [child])
        and: 'a nested AND query where the condition itself has a sub-condition'
            def subCondition = CompositeQuery.builder().cpsPath('/ns/parent/child/grandchild[@x=1]').build()
            def condition    = CompositeQuery.builder().cpsPath('/ns/parent').operator('and').conditions([subCondition]).build()
            def query        = CompositeQuery.builder().cpsPath('/ns/parent').operator('and').conditions([condition]).build()
        and: 'the base path returns the full tree; the condition path returns parent and child; the sub-condition returns no results'
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '/ns/parent',
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> [parent]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '/ns/parent',
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> [parent, child]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '/ns/parent/child/grandchild[@x=1]',
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> []
        when: 'the composite query is processed'
            def result = processCompositeQuery(query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'no results are returned because the deepest sub-condition did not match'
            result.isEmpty()
    }

    def 'Composite query with null childDataNodes on intermediate node.'() {
        given: 'a category node matching the condition, whose book child has null childDataNodes'
            def bookWithNullChildren = new DataNode(xpath: "/bookstore/categories[@code='1']/books[@title='Matilda']",
                leaves: [price: 20], dataspace: DATASPACE, anchorName: ANCHOR)
            bookWithNullChildren.setChildDataNodes(null)
            def categoryWithNullGrandchildren = new DataNode(xpath: "/bookstore/categories[@code='1']",
                leaves: [name: 'Children'], dataspace: DATASPACE, anchorName: ANCHOR,
                childDataNodes: [bookWithNullChildren])
        and: 'a query with AND condition matching only the category itself'
            def condition = CompositeQuery.builder().cpsPath("//categories[@name='Children']").build()
            def query = CompositeQuery.builder().cpsPath('//categories').operator('and').conditions([condition]).build()
        and: 'both base and condition paths return the category'
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//categories',
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> [categoryWithNullGrandchildren]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, "//categories[@name='Children']",
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> [categoryWithNullGrandchildren]
        when: 'the composite query is processed with all descendants'
            def result = processCompositeQuery(query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the category is returned with the book child present but having no further children'
            result.size() == 1
            result[0].xpath == "/bookstore/categories[@code='1']"
            result[0].childDataNodes.size() == 1
            result[0].childDataNodes[0].xpath == "/bookstore/categories[@code='1']/books[@title='Matilda']"
            result[0].childDataNodes[0].childDataNodes.isEmpty()
    }

    def 'Composite query with deeply nested conditions including sub-condition match.'() {
        given: 'a three-level hierarchy: category -> book -> review'
            def review = new DataNode(xpath: "/bookstore/categories[@code='1']/books[@title='Matilda']/review",
                leaves: [rating: 5], dataspace: DATASPACE, anchorName: ANCHOR, childDataNodes: [])
            def bookWithReview = new DataNode(xpath: "/bookstore/categories[@code='1']/books[@title='Matilda']",
                leaves: [price: 20], dataspace: DATASPACE, anchorName: ANCHOR, childDataNodes: [review])
            def categoryWithBooks = new DataNode(xpath: "/bookstore/categories[@code='1']",
                leaves: [name: 'Children'], dataspace: DATASPACE, anchorName: ANCHOR, childDataNodes: [bookWithReview])
        and: 'a nested query: categories AND (books[@price=20] AND review[@rating=5])'
            def subCondition = CompositeQuery.builder().cpsPath('//review[@rating=5]').build()
            def condition    = CompositeQuery.builder().cpsPath('//books[@price=20]')
                .operator('and').conditions([subCondition]).build()
            def query        = CompositeQuery.builder().cpsPath('//categories').operator('and')
                .conditions([condition]).build()
        and: 'persistence returns the full tree for the base path, the book for the condition, and the review for the sub-condition'
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//categories',
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> [categoryWithBooks]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//books[@price=20]',
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> [bookWithReview]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//review[@rating=5]',
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> [review]
        when: 'the composite query is processed'
            def result = processCompositeQuery(query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the category is returned with the matched book and review as descendants'
            result.size() == 1
            result[0].xpath == "/bookstore/categories[@code='1']"
            result[0].childDataNodes[0].xpath == "/bookstore/categories[@code='1']/books[@title='Matilda']"
            result[0].childDataNodes[0].childDataNodes[0].xpath ==
                "/bookstore/categories[@code='1']/books[@title='Matilda']/review"
    }

    def 'Composite query with null child data nodes on top-level node.'() {
        given: 'a data node with null children'
            def nodeWithNullChildren = new DataNode(xpath: "/bookstore/categories[@code='7']",
                leaves: [name: 'History'], dataspace: DATASPACE, anchorName: ANCHOR)
            nodeWithNullChildren.setChildDataNodes(null)
        and: 'a query with no conditions'
            def query = CompositeQuery.builder().cpsPath('//categories').build()
        and: 'persistence returns the node with null children'
            mockCpsDataPersistenceService.queryDataNodes(*_) >> [nodeWithNullChildren]
        when: 'the composite query is processed'
            def result = processCompositeQuery(query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the node is returned with an empty children list'
            result.size() == 1
            result[0].xpath == "/bookstore/categories[@code='7']"
            result[0].childDataNodes.isEmpty()
    }

    def 'Composite query with a nested condition where the matched node has a child with null childDataNodes.'() {
        given: 'a parent node with one child, where that child has null child data nodes'
            def childWithNullChildren = new DataNode(xpath: '/ns/parent/child', leaves: [:], dataspace: DATASPACE, anchorName: ANCHOR)
            childWithNullChildren.setChildDataNodes(null)
            def parentNode = new DataNode(xpath: '/ns/parent', leaves: [:], dataspace: DATASPACE, anchorName: ANCHOR, childDataNodes: [childWithNullChildren])
        and: 'a nested AND query where the sub-condition does not match'
            def subCondition = CompositeQuery.builder().cpsPath('/ns/parent/child[@x=1]').build()
            def condition    = CompositeQuery.builder().cpsPath('/ns/parent').operator('and').conditions([subCondition]).build()
            def query        = CompositeQuery.builder().cpsPath('/ns/parent').operator('and').conditions([condition]).build()
        and: 'persistence returns the parent tree for the base path; the condition returns the parent; the sub-condition returns nothing'
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '/ns/parent',
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> [parentNode]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '/ns/parent',
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> [parentNode]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '/ns/parent/child[@x=1]',
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> []
        when: 'the composite query is processed'
            def result = processCompositeQuery(query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'no results are returned because the sub-condition did not match'
            result.isEmpty()
    }

    def 'Composite query with a nested condition where the matched node has a duplicated child.'() {
        given: 'a child node that appears twice in the parent childDataNodes list'
            def duplicateChild = new DataNode(xpath: '/ns/parent/child', leaves: [:], dataspace: DATASPACE, anchorName: ANCHOR, childDataNodes: [])
            def parentWithDuplicateChildren = new DataNode(xpath: '/ns/parent', leaves: [:], dataspace: DATASPACE, anchorName: ANCHOR, childDataNodes: [duplicateChild, duplicateChild])
        and: 'a nested AND query where the sub-condition does not match'
            def subCondition = CompositeQuery.builder().cpsPath('/ns/parent/child[@x=1]').build()
            def condition    = CompositeQuery.builder().cpsPath('/ns/parent').operator('and').conditions([subCondition]).build()
            def query        = CompositeQuery.builder().cpsPath('/ns/parent').operator('and').conditions([condition]).build()
        and: 'persistence returns the parent for base and condition; the sub-condition returns nothing'
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '/ns/parent',
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> [parentWithDuplicateChildren]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '/ns/parent',
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> [parentWithDuplicateChildren]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '/ns/parent/child[@x=1]',
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> []
        when: 'the composite query is processed'
            def result = processCompositeQuery(query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'no results are returned because the sub-condition did not match'
            result.isEmpty()
    }

    def 'Composite query where two conditions use the same cps path.'() {
        given: 'an OR query where both conditions use the same cps path'
            def condition1 = CompositeQuery.builder().cpsPath('//books[@price=20]').build()
            def condition2 = CompositeQuery.builder().cpsPath('//books[@price=20]').build()
            def query = CompositeQuery.builder().cpsPath('//categories').operator('or')
                .conditions([condition1, condition2]).build()
        and: 'persistence returns a category with two book children for the base path'
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//categories',
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> [categoryChildren]
        when: 'the composite query is processed'
            def result = processCompositeQuery(query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the shared condition cps path is queried exactly once'
            1 * mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//books[@price=20]',
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> [bookMatilda]
        and: 'the matching category is returned'
            result.size() == 1
            result[0].xpath == "/bookstore/categories[@code='1']"
    }

    def 'Composite query with AND operator and a condition that selects nothing.'() {
        given: 'an AND query with two conditions where the first has no matches'
            def condition1 = CompositeQuery.builder().cpsPath('//books[@price=999]').build()
            def condition2 = CompositeQuery.builder().cpsPath('//books[@price=20]').build()
            def query = CompositeQuery.builder().cpsPath('//categories').operator('and')
                .conditions([condition1, condition2]).build()
        and: 'persistence returns a category with two book children for the base path'
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//categories',
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> [categoryChildren]
        when: 'the composite query is processed'
            def result = processCompositeQuery(query, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the first condition is queried and returns nothing'
            1 * mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//books[@price=999]',
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> []
        and: 'the second condition is never queried because AND cannot be satisfied anymore'
            0 * mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//books[@price=20]', *_)
        and: 'the result is empty'
            result.isEmpty()
    }
}
