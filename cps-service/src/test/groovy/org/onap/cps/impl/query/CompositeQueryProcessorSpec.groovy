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

class CompositeQueryProcessorSpec extends Specification {
    def mockCpsDataPersistenceService = Mock(CpsDataPersistenceService)
    def objectUnderTest = new CompositeQueryProcessor(mockCpsDataPersistenceService)
    static def DATASPACE = 'bookstoreDataspace'
    static def ANCHOR    = 'bookstoreAnchor'
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
    def 'Composite query with only a base cps path.'() {
        given: 'a query with no conditions'
            def query = CompositeQuery.builder().cpsPath("//categories[@name='Children']").build()
        and: 'persistence returns a category with two book children'
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, "//categories[@name='Children']",
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> [categoryChildren]
        when: 'the composite query is processed'
            def result = objectUnderTest.processCompositeQuery(DATASPACE, ANCHOR, query,
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
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
            def result = objectUnderTest.processCompositeQuery(DATASPACE, ANCHOR, query,
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
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
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> conditionNodes
        when: 'the composite query is processed'
            def result = objectUnderTest.processCompositeQuery(DATASPACE, ANCHOR, query,
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'only the expected number of results are returned'
            result.size() == expectedSize
        where: 'the following conditions are evaluated'
            scenario              | conditionPath         | conditionNodes || expectedSize
            'matching condition'  | '//books[@price=20]'  | [bookMatilda]  || 1
            'no matching results' | '//books[@price=999]' | []             || 0
    }
    def 'Composite query with OR operator returns union of matching categories.'() {
        given: 'a query fetching all categories OR Children OR Comedy'
            def condition1 = CompositeQuery.builder().cpsPath("//categories[@name='Children']").build()
            def condition2 = CompositeQuery.builder().cpsPath("//categories[@name='Comedy']").build()
            def query = CompositeQuery.builder().cpsPath('//categories').operator('or')
                .conditions([condition1, condition2]).build()
        and: 'persistence returns all categories for base; each condition returns one category'
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//categories',
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> [categoryChildren, categoryThriller, categoryComedy]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, "//categories[@name='Children']",
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> [categoryChildren]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, "//categories[@name='Comedy']",
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> [categoryComedy]
        when: 'the composite query is processed'
            def result = objectUnderTest.processCompositeQuery(DATASPACE, ANCHOR, query,
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'Children and Comedy categories are returned'
            result.size() == 2
            result.collect { it.xpath }.containsAll([
                "/bookstore/categories[@code='1']",
                "/bookstore/categories[@code='3']"
            ])
        and: 'the Thriller category is excluded'
            !result.collect { it.xpath }.contains("/bookstore/categories[@code='2']")
    }
    def 'Composite query with #scenario conditions returns all base nodes without filtering.'() {
        given: 'a query with null or empty conditions list'
            def query = CompositeQuery.builder().cpsPath('//categories').operator('and')
                .conditions(conditions).build()
        and: 'persistence returns all three categories'
            mockCpsDataPersistenceService.queryDataNodes(*_) >> [categoryChildren, categoryThriller, categoryComedy]
        when: 'the composite query is processed'
            def result = objectUnderTest.processCompositeQuery(DATASPACE, ANCHOR, query,
                FetchDescendantsOption.OMIT_DESCENDANTS)
        then: 'all three categories are returned because there is no condition to filter by'
            result.size() == 3
        where: 'the following conditions lists are used'
            scenario | conditions
            'null'   | null
            'empty'  | []
    }
    def 'Composite query with OMIT_DESCENDANTS returns nodes without children.'() {
        given: 'a query with no conditions'
            def query = CompositeQuery.builder().cpsPath('//categories').build()
        and: 'persistence returns all categories'
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//categories',
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> [categoryChildren, categoryThriller, categoryComedy]
        when: 'the composite query is processed with OMIT_DESCENDANTS'
            def result = objectUnderTest.processCompositeQuery(DATASPACE, ANCHOR, query,
                FetchDescendantsOption.OMIT_DESCENDANTS)
        then: 'all three categories are returned with no child data nodes'
            result.size() == 3
            result.every { it.childDataNodes.isEmpty() }
    }
    def 'Composite query includes ancestor as structural bridge when only a descendant matches.'() {
        given: 'a query with AND condition matching only a book (child of a category)'
            def condition = CompositeQuery.builder().cpsPath('//books[@price=20]').build()
            def query = CompositeQuery.builder().cpsPath('//categories').operator('and').conditions([condition]).build()
        and: 'base returns categoryChildren; condition returns bookMatilda'
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//categories',
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> [categoryChildren]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//books[@price=20]',
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> [bookMatilda]
        when: 'the composite query is processed with all descendants'
            def result = objectUnderTest.processCompositeQuery(DATASPACE, ANCHOR, query,
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the category is returned as a structural bridge'
            result.size() == 1
            result[0].xpath == "/bookstore/categories[@code='1']"
        and: 'bookMatilda is present as a child'
            result[0].childDataNodes.collect { it.xpath }
                .contains("/bookstore/categories[@code='1']/books[@title='Matilda']")
    }
    def 'Composite query returns all descendants of a matched node when no child condition matches.'() {
        given: 'a query with AND condition matching only the category itself'
            def condition = CompositeQuery.builder().cpsPath("//categories[@name='Children']").build()
            def query = CompositeQuery.builder().cpsPath('//categories').operator('and').conditions([condition]).build()
        and: 'base and condition both return categoryChildren'
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//categories',
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> [categoryChildren]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, "//categories[@name='Children']",
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> [categoryChildren]
        when: 'the composite query is processed with all descendants'
            def result = objectUnderTest.processCompositeQuery(DATASPACE, ANCHOR, query,
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the category is returned with all its books via walkAllDescendants'
            result.size() == 1
            result[0].childDataNodes.size() == 2
            result[0].childDataNodes.collect { it.xpath }.containsAll([
                "/bookstore/categories[@code='1']/books[@title='Matilda']",
                "/bookstore/categories[@code='1']/books[@title='The Gruffalo']"
            ])
    }
    def 'Composite query with depth-limited fetch option respects the depth.'() {
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
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> [category]
        when: 'the composite query is processed with direct children only'
            def result = objectUnderTest.processCompositeQuery(DATASPACE, ANCHOR, query,
                FetchDescendantsOption.DIRECT_CHILDREN_ONLY)
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
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> [categoryChildren]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//books[@price=20]',
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> [bookMatilda]
        when: 'the composite query is processed'
            def result = objectUnderTest.processCompositeQuery(DATASPACE, ANCHOR, query,
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
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
        and: 'base returns all categories; condition returns Children; sub-condition returns nothing'
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//categories',
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> [categoryChildren, categoryThriller, categoryComedy]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, "//categories[@name='Children']",
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> [categoryChildren]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//books[@price=999]',
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> []
        when: 'the composite query is processed'
            def result = objectUnderTest.processCompositeQuery(DATASPACE, ANCHOR, query,
                FetchDescendantsOption.OMIT_DESCENDANTS)
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
        and: 'base returns all categories; condition returns Children; sub-condition returns nothing'
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//categories',
                FetchDescendantsOption.OMIT_DESCENDANTS, 0) >> [categoryChildren, categoryThriller, categoryComedy]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, "//categories[@name='Children']",
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> [categoryChildren]
            mockCpsDataPersistenceService.queryDataNodes(DATASPACE, ANCHOR, '//books[@price=999]',
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS, 0) >> []
        when: 'the composite query is processed'
            def result = objectUnderTest.processCompositeQuery(DATASPACE, ANCHOR, query,
                FetchDescendantsOption.OMIT_DESCENDANTS)
        then: 'categoryChildren is returned because OR does not require all conditions to match'
            result.size() == 1
            result[0].xpath == "/bookstore/categories[@code='1']"
    }
    def 'Composite query handles data nodes with null child data nodes gracefully.'() {
        given: 'a data node with null children'
            def nodeWithNullChildren = new DataNode(xpath: "/bookstore/categories[@code='7']",
                leaves: [name: 'History'], dataspace: DATASPACE, anchorName: ANCHOR)
            nodeWithNullChildren.setChildDataNodes(null)
        and: 'a query with no conditions'
            def query = CompositeQuery.builder().cpsPath('//categories').build()
        and: 'persistence returns the node with null children'
            mockCpsDataPersistenceService.queryDataNodes(*_) >> [nodeWithNullChildren]
        when: 'the composite query is processed'
            def result = objectUnderTest.processCompositeQuery(DATASPACE, ANCHOR, query,
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the node is returned with an empty children list'
            result.size() == 1
            result[0].xpath == "/bookstore/categories[@code='7']"
            result[0].childDataNodes.isEmpty()
    }
}
