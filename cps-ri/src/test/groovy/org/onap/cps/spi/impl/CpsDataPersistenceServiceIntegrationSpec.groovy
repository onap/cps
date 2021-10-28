/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021 Bell Canada.
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
package org.onap.cps.spi.impl

import com.google.common.collect.ImmutableSet
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.entities.FragmentEntity
import org.onap.cps.spi.exceptions.AlreadyDefinedException
import org.onap.cps.spi.exceptions.AnchorNotFoundException
import org.onap.cps.spi.exceptions.DataNodeNotFoundException
import org.onap.cps.spi.exceptions.DataspaceNotFoundException
import org.onap.cps.spi.model.DataNode
import org.onap.cps.spi.model.DataNodeBuilder
import org.spockframework.util.CollectionUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql

import javax.validation.ConstraintViolationException
import java.util.stream.Collectors

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS

class CpsDataPersistenceServiceIntegrationSpec extends CpsPersistenceSpecBase {

    @Autowired
    CpsDataPersistenceService objectUnderTest

    static final Gson GSON = new GsonBuilder().create()

    static final String SET_DATA = '/data/fragment.sql'
    static final long ID_DATA_NODE_WITH_DESCENDANTS = 4001
    static final String XPATH_DATA_NODE_WITH_DESCENDANTS = '/parent-1'
    static final String XPATH_DATA_NODE_WITH_LEAVES = '/parent-100'
    static final long UPDATE_DATA_NODE_FRAGMENT_ID = 4202L
    static final long UPDATE_DATA_NODE_SUB_FRAGMENT_ID = 4203L
    static final long LIST_DATA_NODE_PARENT201_FRAGMENT_ID = 4206L
    static final long LIST_DATA_NODE_PARENT203_FRAGMENT_ID = 4214L
    static final long LIST_DATA_NODE_PARENT204_FRAGMENT_ID = 4219L
    static final long LIST_DATA_NODE_PARENT205_FRAGMENT_ID = 4221L
    static final long LIST_DATA_NODE_CHILD202_FRAGMENT_ID = 4204L
    static final long LIST_DATA_NODE_PARENT202_FRAGMENT_ID = 4211L

    static final DataNode newDataNode = new DataNodeBuilder().build()
    static DataNode existingDataNode
    static DataNode existingChildDataNode

    def expectedLeavesByXpathMap = [
            '/parent-100'                      : ['parent-leaf': 'parent-leaf value'],
            '/parent-100/child-001'            : ['first-child-leaf': 'first-child-leaf value'],
            '/parent-100/child-002'            : ['second-child-leaf': 'second-child-leaf value'],
            '/parent-100/child-002/grand-child': ['grand-child-leaf': 'grand-child-leaf value']
    ]

    static {
        existingDataNode = createDataNodeTree(XPATH_DATA_NODE_WITH_DESCENDANTS)
        existingChildDataNode = createDataNodeTree('/parent-1/child-1')
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'StoreDataNode with descendants.'() {
        when: 'a fragment with descendants is stored'
            def parentXpath = "/parent-new"
            def childXpath = "/parent-new/child-new"
            def grandChildXpath = "/parent-new/child-new/grandchild-new"
            objectUnderTest.storeDataNode(DATASPACE_NAME, ANCHOR_NAME1,
                    createDataNodeTree(parentXpath, childXpath, grandChildXpath))
        then: 'it can be retrieved by its xpath'
            def parentFragment = getFragmentByXpath(DATASPACE_NAME, ANCHOR_NAME1, parentXpath)
        and: 'it contains the children'
            parentFragment.childFragments.size() == 1
            def childFragment = parentFragment.childFragments[0]
            childFragment.xpath == childXpath
        and: "and its children's children"
            childFragment.childFragments.size() == 1
            def grandchildFragment = childFragment.childFragments[0]
            grandchildFragment.xpath == grandChildXpath
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Store data node for multiple anchors using the same schema.'() {
        def xpath = "/parent-new"
        given: 'a fragment is stored for an anchor'
            objectUnderTest.storeDataNode(DATASPACE_NAME, ANCHOR_NAME1, createDataNodeTree(xpath))
        when: 'another fragment is stored for an other anchor, using the same schema set'
            objectUnderTest.storeDataNode(DATASPACE_NAME, ANCHOR_NAME3, createDataNodeTree(xpath))
        then: 'both fragments can be retrieved by their xpath'
            def fragment1 = getFragmentByXpath(DATASPACE_NAME, ANCHOR_NAME1, xpath)
            fragment1.anchor.name == ANCHOR_NAME1
            fragment1.xpath == xpath
            def fragment2 = getFragmentByXpath(DATASPACE_NAME, ANCHOR_NAME3, xpath)
            fragment2.anchor.name == ANCHOR_NAME3
            fragment2.xpath == xpath
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Store datanode error scenario: #scenario.'() {
        when: 'attempt to store a data node with #scenario'
            objectUnderTest.storeDataNode(dataspaceName, anchorName, dataNode)
        then: 'a #expectedException is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                    | dataspaceName  | anchorName     | dataNode         || expectedException
            'dataspace does not exist'  | 'unknown'      | 'not-relevant' | newDataNode      || DataspaceNotFoundException
            'schema set does not exist' | DATASPACE_NAME | 'unknown'      | newDataNode      || AnchorNotFoundException
            'anchor already exists'     | DATASPACE_NAME | ANCHOR_NAME1   | newDataNode      || ConstraintViolationException
            'datanode already exists'   | DATASPACE_NAME | ANCHOR_NAME1   | existingDataNode || AlreadyDefinedException
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Add a child to a Fragment that already has a child.'() {
        given: ' a new child node'
            def newChild = createDataNodeTree('xpath for new child')
        when: 'the child is added to an existing parent with 1 child'
            objectUnderTest.addChildDataNode(DATASPACE_NAME, ANCHOR_NAME1, XPATH_DATA_NODE_WITH_DESCENDANTS, newChild)
        then: 'the parent is now has to 2 children'
            def expectedExistingChildPath = '/parent-1/child-1'
            def parentFragment = fragmentRepository.findById(ID_DATA_NODE_WITH_DESCENDANTS).orElseThrow()
            parentFragment.getChildFragments().size() == 2
        and: 'it still has the old child'
            parentFragment.getChildFragments().find({ it.xpath == expectedExistingChildPath })
        and: 'it has the new child'
            parentFragment.getChildFragments().find({ it.xpath == newChild.xpath })
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Add child error scenario: #scenario.'() {
        when: 'attempt to add a child data node with #scenario'
            objectUnderTest.addChildDataNode(DATASPACE_NAME, ANCHOR_NAME1, parentXpath, dataNode)
        then: 'a #expectedException is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                 | parentXpath                      | dataNode              || expectedException
            'parent does not exist'  | 'unknown'                        | newDataNode           || DataNodeNotFoundException
            'already existing child' | XPATH_DATA_NODE_WITH_DESCENDANTS | existingChildDataNode || AlreadyDefinedException
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Add list-node fragment with multiple elements including an element with a child datanode.'() {
        given: 'two new data nodes for an existing list'
            def listNodeXpaths = ['/parent-201/child-204[@key="B"]', '/parent-201/child-204[@key="C"]']
            def listNodeCollection = buildDataNodeCollection(listNodeXpaths)
        and: 'a child node for one of the new data nodes'
            def childDataNode = buildDataNode('/parent-201/child-204[@key="C"]/grand-child-204[@key2="Z"]', [leave:'value'], [])
            listNodeCollection.iterator().next().childDataNodes = [childDataNode]
        when: 'the data nodes (list elements) are added to existing parent node'
            objectUnderTest.addListDataNodes(DATASPACE_NAME, ANCHOR_NAME3, '/parent-201', listNodeCollection)
        then: 'new entries successfully persisted, parent node now contains 5 children (2 new + 3 existing before)'
            def parentFragment = fragmentRepository.getById(LIST_DATA_NODE_PARENT201_FRAGMENT_ID)
            def allChildXpaths = parentFragment.getChildFragments().collect { it.getXpath() }
            assert allChildXpaths.size() == 5
            assert allChildXpaths.containsAll(listNodeXpaths)
        and: 'the child node of the new list entry is also present'
            def dataspaceEntity = dataspaceRepository.getByName(DATASPACE_NAME)
            def anchorEntity = anchorRepository.getByDataspaceAndName(dataspaceEntity, ANCHOR_NAME3)
            def listElementChild = fragmentRepository.findByDataspaceAndAnchorAndXpath(dataspaceEntity, anchorEntity, childDataNode.xpath)
            assert listElementChild.isPresent()
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Add list-node fragment error scenario: #scenario.'() {
        given: 'list node data fragment as a collection of data nodes'
            def listNodeCollection = buildDataNodeCollection(listNodeXpaths)
        when: 'list-node elements added to existing parent node'
            objectUnderTest.addListDataNodes(DATASPACE_NAME, ANCHOR_NAME3, parentNodeXpath, listNodeCollection)
        then: 'a #expectedException is thrown'
            thrown(expectedException)
        where: 'following parameters were used'
            scenario                     | parentNodeXpath | listNodeXpaths                      || expectedException
            'parent node does not exist' | '/unknown'      | ['irrelevant']                      || DataNodeNotFoundException
            'already existing fragment'  | '/parent-201'   | ['/parent-201/child-204[@key="A"]'] || AlreadyDefinedException

    }

    static def createDataNodeTree(String... xpaths) {
        def dataNodeBuilder = new DataNodeBuilder().withXpath(xpaths[0])
        if (xpaths.length > 1) {
            def xPathsDescendant = Arrays.copyOfRange(xpaths, 1, xpaths.length)
            def childDataNode = createDataNodeTree(xPathsDescendant)
            dataNodeBuilder.withChildDataNodes(ImmutableSet.of(childDataNode))
        }
        dataNodeBuilder.build()
    }

    def getFragmentByXpath(dataspaceName, anchorName, xpath) {
        def dataspace = dataspaceRepository.getByName(dataspaceName)
        def anchor = anchorRepository.getByDataspaceAndName(dataspace, anchorName)
        return fragmentRepository.findByDataspaceAndAnchorAndXpath(dataspace, anchor, xpath).orElseThrow()
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Get data node by xpath without descendants.'() {
        when: 'data node is requested'
            def result = objectUnderTest.getDataNode(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES,
                    inputXPath, OMIT_DESCENDANTS)
        then: 'data node is returned with no descendants'
            assert result.getXpath() == XPATH_DATA_NODE_WITH_LEAVES
        and: 'expected leaves'
            assert result.getChildDataNodes().size() == 0
            assertLeavesMaps(result.getLeaves(), expectedLeavesByXpathMap[XPATH_DATA_NODE_WITH_LEAVES])
        where: 'the following data is used'
            scenario      | inputXPath
            'some xpath'  | '/parent-100'
            'root xpath'  | '/'
            'empty xpath' | ''
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Get data node by xpath with all descendants.'() {
        when: 'data node is requested with all descendants'
            def result = objectUnderTest.getDataNode(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES,
                    inputXPath, INCLUDE_ALL_DESCENDANTS)
            def mappedResult = treeToFlatMapByXpath(new HashMap<>(), result)
        then: 'data node is returned with all the descendants populated'
            assert mappedResult.size() == 4
            assert result.getChildDataNodes().size() == 2
            assert mappedResult.get('/parent-100/child-001').getChildDataNodes().size() == 0
            assert mappedResult.get('/parent-100/child-002').getChildDataNodes().size() == 1
        and: 'extracted leaves maps are matching expected'
            mappedResult.forEach(
                    (xPath, dataNode) -> assertLeavesMaps(dataNode.getLeaves(), expectedLeavesByXpathMap[xPath]))
        where: 'the following data is used'
            scenario      | inputXPath
            'some xpath'  | '/parent-100'
            'root xpath'  | '/'
            'empty xpath' | ''
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Get data node error scenario: #scenario.'() {
        when: 'attempt to get data node with #scenario'
            objectUnderTest.getDataNode(dataspaceName, anchorName, xpath, OMIT_DESCENDANTS)
        then: 'a #expectedException is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                 | dataspaceName  | anchorName                        | xpath          || expectedException
            'non-existing dataspace' | 'NO DATASPACE' | 'not relevant'                    | 'not relevant' || DataspaceNotFoundException
            'non-existing anchor'    | DATASPACE_NAME | 'NO ANCHOR'                       | 'not relevant' || AnchorNotFoundException
            'non-existing xpath'     | DATASPACE_NAME | ANCHOR_FOR_DATA_NODES_WITH_LEAVES | 'NO XPATH'     || DataNodeNotFoundException
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Update data node leaves.'() {
        when: 'update is performed for leaves'
            objectUnderTest.updateDataLeaves(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES,
                    "/parent-200/child-201", ['leaf-value': 'new'])
        then: 'leaves are updated for selected data node'
            def updatedFragment = fragmentRepository.getById(UPDATE_DATA_NODE_FRAGMENT_ID)
            def updatedLeaves = getLeavesMap(updatedFragment)
            assert updatedLeaves.size() == 1
            assert updatedLeaves.'leaf-value' == 'new'
        and: 'existing child entry remains as is'
            def childFragment = updatedFragment.getChildFragments().iterator().next()
            def childLeaves = getLeavesMap(childFragment)
            assert childFragment.getId() == UPDATE_DATA_NODE_SUB_FRAGMENT_ID
            assert childLeaves.'leaf-value' == 'original'
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Update data leaves error scenario: #scenario.'() {
        when: 'attempt to update data node for #scenario'
            objectUnderTest.updateDataLeaves(dataspaceName, anchorName, xpath, ['leaf-name': 'leaf-value'])
        then: 'a #expectedException is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                 | dataspaceName  | anchorName                        | xpath                || expectedException
            'non-existing dataspace' | 'NO DATASPACE' | 'not relevant'                    | 'not relevant'       || DataspaceNotFoundException
            'non-existing anchor'    | DATASPACE_NAME | 'NO ANCHOR'                       | 'not relevant'       || AnchorNotFoundException
            'non-existing xpath'     | DATASPACE_NAME | ANCHOR_FOR_DATA_NODES_WITH_LEAVES | 'NON-EXISTING XPATH' || DataNodeNotFoundException
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Replace data node tree with descendants removal.'() {
        given: 'data node object with leaves updated, no children'
            def submittedDataNode = buildDataNode("/parent-200/child-201", ['leaf-value': 'new'], [])
        when: 'replace data node tree is performed'
            objectUnderTest.replaceDataNodeTree(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES, submittedDataNode)
        then: 'leaves have been updated for selected data node'
            def updatedFragment = fragmentRepository.getById(UPDATE_DATA_NODE_FRAGMENT_ID)
            def updatedLeaves = getLeavesMap(updatedFragment)
            assert updatedLeaves.size() == 1
            assert updatedLeaves.'leaf-value' == 'new'
        and: 'updated entry has no children'
            updatedFragment.getChildFragments().isEmpty()
        and: 'previously attached child entry is removed from database'
            fragmentRepository.findById(UPDATE_DATA_NODE_SUB_FRAGMENT_ID).isEmpty()
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Replace data node tree with descendants.'() {
        given: 'data node object with leaves updated, having child with old content'
            def submittedDataNode = buildDataNode("/parent-200/child-201", ['leaf-value': 'new'], [
                  buildDataNode("/parent-200/child-201/grand-child", ['leaf-value': 'original'], [])
            ])
        when: 'update is performed including descendants'
            objectUnderTest.replaceDataNodeTree(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES, submittedDataNode)
        then: 'leaves have been updated for selected data node'
            def updatedFragment = fragmentRepository.getById(UPDATE_DATA_NODE_FRAGMENT_ID)
            def updatedLeaves = getLeavesMap(updatedFragment)
            assert updatedLeaves.size() == 1
            assert updatedLeaves.'leaf-value' == 'new'
        and: 'existing child entry is not updated as content is same'
            def childFragment = updatedFragment.getChildFragments().iterator().next()
            childFragment.getXpath() == '/parent-200/child-201/grand-child'
            def childLeaves = getLeavesMap(childFragment)
            assert childLeaves.'leaf-value' == 'original'
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Replace data node tree with same descendants but changed leaf value.'() {
        given: 'data node object with leaves updated, having child with old content'
            def submittedDataNode = buildDataNode("/parent-200/child-201", ['leaf-value': 'new'], [
                    buildDataNode("/parent-200/child-201/grand-child", ['leaf-value': 'new'], [])
            ])
        when: 'update is performed including descendants'
            objectUnderTest.replaceDataNodeTree(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES, submittedDataNode)
        then: 'leaves have been updated for selected data node'
            def updatedFragment = fragmentRepository.getById(UPDATE_DATA_NODE_FRAGMENT_ID)
            def updatedLeaves = getLeavesMap(updatedFragment)
            assert updatedLeaves.size() == 1
            assert updatedLeaves.'leaf-value' == 'new'
        and: 'existing child entry is updated with the new content'
            def childFragment = updatedFragment.getChildFragments().iterator().next()
            childFragment.getXpath() == '/parent-200/child-201/grand-child'
            def childLeaves = getLeavesMap(childFragment)
            assert childLeaves.'leaf-value' == 'new'
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Replace data node tree with different descendants xpath'() {
        given: 'data node object with leaves updated, having child with old content'
            def submittedDataNode = buildDataNode("/parent-200/child-201", ['leaf-value': 'new'], [
                    buildDataNode("/parent-200/child-201/grand-child-new", ['leaf-value': 'new'], [])
            ])
        when: 'update is performed including descendants'
            objectUnderTest.replaceDataNodeTree(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES, submittedDataNode)
        then: 'leaves have been updated for selected data node'
            def updatedFragment = fragmentRepository.getById(UPDATE_DATA_NODE_FRAGMENT_ID)
            def updatedLeaves = getLeavesMap(updatedFragment)
            assert updatedLeaves.size() == 1
            assert updatedLeaves.'leaf-value' == 'new'
        and: 'previously attached child entry is removed from database'
            fragmentRepository.findById(UPDATE_DATA_NODE_SUB_FRAGMENT_ID).isEmpty()
        and: 'new child entry is persisted'
            def childFragment = updatedFragment.getChildFragments().iterator().next()
            childFragment.getXpath() == '/parent-200/child-201/grand-child-new'
            def childLeaves = getLeavesMap(childFragment)
            assert childLeaves.'leaf-value' == 'new'
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Replace data node tree error scenario: #scenario.'() {
        given: 'data node object'
            def submittedDataNode = buildDataNode(xpath, ['leaf-name': 'leaf-value'], [])
        when: 'attempt to update data node for #scenario'
            objectUnderTest.replaceDataNodeTree(dataspaceName, anchorName, submittedDataNode)
        then: 'a #expectedException is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                 | dataspaceName  | anchorName                        | xpath                || expectedException
            'non-existing dataspace' | 'NO DATASPACE' | 'not relevant'                    | 'not relevant'       || DataspaceNotFoundException
            'non-existing anchor'    | DATASPACE_NAME | 'NO ANCHOR'                       | 'not relevant'       || AnchorNotFoundException
            'non-existing xpath'     | DATASPACE_NAME | ANCHOR_FOR_DATA_NODES_WITH_LEAVES | 'NON-EXISTING XPATH' || DataNodeNotFoundException
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Replace list-node content of #scenario.'() {
        given: 'list node data fragment as a collection of data nodes'
            def listNodeCollection = buildDataNodeCollection(listNodeXpaths)
        when: 'list-node elements replaced within the existing parent node'
            objectUnderTest.replaceListElement(DATASPACE_NAME, ANCHOR_NAME3, parentXpath, listNodeCollection)
        then: 'child list elements are updated as expected, non-list element remains as is'
            def parentFragment = fragmentRepository.getById(listNodeFragmentID)
            def allChildXpaths = parentFragment.getChildFragments().collect { it.getXpath() }
            assert allChildXpaths.size() == expectedChildXpaths.size()
            assert allChildXpaths.containsAll(expectedChildXpaths)
        where: 'following parameters were used'
            scenario                                                       | listNodeXpaths                                                             |parentXpath              |listNodeFragmentID                     || expectedChildXpaths
            'existing list node with non existing key'                     | ['/parent-201/child-204[@key="B"]']                                        | '/parent-201'           | LIST_DATA_NODE_PARENT201_FRAGMENT_ID  || ['/parent-201/child-203', '/parent-201/child-204[@key="B"]']
            'non existing list node with non existing key'                 | ['/parent-201/child-205[@key="1"]']                                        | '/parent-201'           | LIST_DATA_NODE_PARENT201_FRAGMENT_ID  || ['/parent-201/child-203', '/parent-201/child-204[@key="A"]', '/parent-201/child-204[@key="X"]', '/parent-201/child-205[@key="1"]']
            'existing list node with 1 existing key'                       | ['/parent-201/child-204[@key="X"]']                                        | '/parent-201'           | LIST_DATA_NODE_PARENT201_FRAGMENT_ID  || ['/parent-201/child-203', '/parent-201/child-204[@key="X"]']
            'existing list-node with combined keys'                        | ['/parent-202/child-205[@key="A"]']                                        | '/parent-202'           | LIST_DATA_NODE_PARENT202_FRAGMENT_ID  || ['/parent-202/child-206[@key="A"]', '/parent-202/child-205[@key="A"]']
            'existing grandchild list-node'                                | ['/parent-200/child-202/grand-child-202[@key="E"]']                        | '/parent-200/child-202' | LIST_DATA_NODE_CHILD202_FRAGMENT_ID   || ['/parent-200/child-202/grand-child-202[@key="E"]']
            'existing list node with two list nodes'                       | ['/parent-201/child-204[@key="new X"]', '/parent-201/child-204[@key="Y"]'] | '/parent-201'           | LIST_DATA_NODE_PARENT201_FRAGMENT_ID  || ['/parent-201/child-203', '/parent-201/child-204[@key="new X"]', '/parent-201/child-204[@key="Y"]']
            'existing list node with compounded list node'                 | ['/parent-202/child-205[@key="A" and @key2="B"]']                          | '/parent-202'           | LIST_DATA_NODE_PARENT202_FRAGMENT_ID  || ['/parent-202/child-206[@key="A"]', '/parent-202/child-205[@key="A" and @key2="B"]']
            'existing list node with list node with parent with key value' | ['/parent-204[@key="L"]/child-210[@key="N"]']                              | '/parent-204[@key="L"]' | LIST_DATA_NODE_PARENT204_FRAGMENT_ID  || ['/parent-204[@key="L"]/child-210[@key="N"]']
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Replace list-node that has children with #scenario'() {
        given: 'list node data fragment with child data node fragments'
            def grandChildDataNodes = buildDataNodeCollection(grandChildXpaths)
            def listNode = new DataNodeBuilder().withXpath(childXpath).withChildDataNodes(grandChildDataNodes).build()
        when: 'list-node elements replaced within the existing parent node'
            objectUnderTest.replaceListElement(DATASPACE_NAME, ANCHOR_NAME3, parentXpath, [ listNode ])
        then: 'child list elements are updated as expected with non-list elements remaining as is'
            def parentFragment = fragmentRepository.getById(listNodeFragmentId)
            def allChildXpaths = parentFragment.getChildFragments().collect { it.getXpath() }
            assert allChildXpaths.size() == expectedChildXpaths.size()
            assert allChildXpaths.containsAll(expectedChildXpaths)
        and: 'grandchild list elements are updated as expected'
            def allGrandChildXpaths = parentFragment.getChildFragments().collect(){
                it.getChildFragments().collect(){
                    it.getXpath()}}
            allGrandChildXpaths.removeIf(list -> list.isEmpty())
            def grandChildXpathsToList = allGrandChildXpaths.stream().flatMap(List::stream).collect(Collectors.toList())
            def expectedGrandChildXpaths = grandChildXpaths
            assert grandChildXpathsToList.size() == expectedGrandChildXpaths.size()
            assert grandChildXpathsToList.containsAll(expectedGrandChildXpaths)
        where: 'the following parameters are used'
            scenario                                                    | parentXpath   | childXpath                        | grandChildXpaths                                                                                | expectedChildXpaths                                            | listNodeFragmentId
            'existing grandchild of list node'                          | '/parent-203' | '/parent-203/child-204[@key="X"]' | ['/parent-203/child-204/grandchild[@key="2"]']                                                  | ['/parent-203/child-203', '/parent-203/child-204[@key="X"]']   | LIST_DATA_NODE_PARENT203_FRAGMENT_ID
            'existing grandchild of list node with two new nodes'       | '/parent-203' | '/parent-203/child-204[@key="X"]' | ['/parent-203/child-204/grandchild[@key="2"]' , '/parent-203/child-204/grandchild[@key="3"]']   | ['/parent-203/child-203', '/parent-203/child-204[@key="X"]']   | LIST_DATA_NODE_PARENT203_FRAGMENT_ID
            'existing grandchild with compound list node'               | '/parent-205' | '/parent-205/child-205[@key="X"]' | ['/parent-205/child-205/grand-child-206[@key="Y" and @key2="Z"]']                               | ['/parent-205/child-205', '/parent-205/child-205[@key="X"]']   | LIST_DATA_NODE_PARENT205_FRAGMENT_ID
            'two existing list node with a new node'                    | '/parent-205' | '/parent-205/child-205[@key="X"]' | ['/parent-205/child-205/grandchild[@key="A"]']                                                  | ['/parent-205/child-205', '/parent-205/child-205[@key="X"]']   | LIST_DATA_NODE_PARENT205_FRAGMENT_ID
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Replace list-node content of #scenario with grandchildren.'() {
        given: 'list node data fragment as a collection of data nodes'
            def listNodeCollection = buildDataNodeCollection(listNodeXpaths)
        when: 'list-node elements replaced within the existing parent node'
            objectUnderTest.replaceListElement(DATASPACE_NAME, ANCHOR_NAME3, parentXpath, listNodeCollection)
        then: 'child list elements are updated as expected with non-list elements remaining as is'
            def parentFragment = fragmentRepository.getById(listNodeFragmentID)
            def allChildXpaths = parentFragment.getChildFragments().collect { it.getXpath() }
            assert allChildXpaths.size() == expectedChildXpaths.size()
            assert allChildXpaths.containsAll(expectedChildXpaths)
        and: 'grandchild list elements are updated as expected'
            def allGrandChildXpaths = parentFragment.getChildFragments().collect {
                it.getChildFragments().collect {
                    it.getXpath()}}
            allGrandChildXpaths.removeIf(list -> list.isEmpty())
            assert allGrandChildXpaths.size() == expectedGrandChildXpaths.size()
            assert allGrandChildXpaths.containsAll(expectedGrandChildXpaths)
        where: 'following parameters were used'
            scenario                                                       | listNodeXpaths                      | parentXpath   | listNodeFragmentID                   || expectedChildXpaths                                          | expectedGrandChildXpaths
            'existing list node with existing keys'                        | ['/parent-203/child-204[@key="X"]'] | '/parent-203' | LIST_DATA_NODE_PARENT203_FRAGMENT_ID || ['/parent-203/child-203', '/parent-203/child-204[@key="X"]'] | []
            'non existing list node with existing keys'                    | ['/parent-203/child-204[@key="V"]'] | '/parent-203' | LIST_DATA_NODE_PARENT203_FRAGMENT_ID || ['/parent-203/child-203', '/parent-203/child-204[@key="V"]'] | []
    }


    @Sql([CLEAR_DATA, SET_DATA])
    def 'Replace list-node fragment error scenario: #scenario.'() {
        given: 'list node data fragment as a collection of data nodes'
            def listNodeCollection = buildDataNodeCollection(listNodeXpaths)
        when: 'list-node elements were replaced under existing parent node'
            objectUnderTest.replaceListElement(DATASPACE_NAME, ANCHOR_NAME3, parentNodeXpath, listNodeCollection)
        then: 'a #expectedException is thrown'
            thrown(expectedException)
        where: 'following parameters were used'
            scenario                     | parentNodeXpath | listNodeXpaths || expectedException
            'parent node does not exist' | '/unknown'      | ['irrelevant'] || DataNodeNotFoundException
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Delete list-node content of #scenario.'() {
        given: 'list node data fragments are present in database'
        when: 'list-node elements deleted within the existing parent node'
            objectUnderTest.deleteListElement(DATASPACE_NAME, ANCHOR_NAME3, listNodeXpaths)
        then: 'child list elements are removed as expected, non-list element remains as is'
            def parentFragment = fragmentRepository.getById(listNodeFragmentID)
            def allChildXpaths = parentFragment.getChildFragments().collect { it.getXpath() }
            assert allChildXpaths.size() == expectedChildXpaths.size()
            assert allChildXpaths.containsAll(expectedChildXpaths)
        where: 'following parameters were used'
            scenario                                          | listNodeXpaths                                               | listNodeFragmentID                   || expectedChildXpaths
            'existing list-node with key'                     | '/parent-203/child-204[@key="A"]'                            | LIST_DATA_NODE_PARENT203_FRAGMENT_ID || ['/parent-203/child-203', '/parent-203/child-204[@key="X"]']
            'existing list-node with key'                     | '/parent-203/child-204[@key="X"]'                            | LIST_DATA_NODE_PARENT203_FRAGMENT_ID || ['/parent-203/child-203', '/parent-203/child-204[@key="A"]']
            'existing grand-child list node with keys'        | '/parent-203/child-204[@key="X"]/grand-child-204[@key2="Y"]' | LIST_DATA_NODE_PARENT203_FRAGMENT_ID || ['/parent-203/child-203', '/parent-203/child-204[@key="X"]', '/parent-203/child-204[@key="A"]']
            'existing list-node with combined keys'           | '/parent-202/child-205[@key="A" and @key2="B"]'              | LIST_DATA_NODE_PARENT202_FRAGMENT_ID || ['/parent-202/child-206[@key="A"]']
            'existing node with list node variants to delete' | '/parent-203/child-204'                                      | LIST_DATA_NODE_PARENT203_FRAGMENT_ID || ['/parent-203/child-203']
            'existing grandchild list-node'                   | '/parent-200/child-202/grand-child-202[@key="D"]'            | LIST_DATA_NODE_CHILD202_FRAGMENT_ID  || []
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Delete list-node fragment error scenario: #scenario.'() {
        given: 'list node data fragments are present in database'
        when: 'list-node elements are deleted under existing parent node'
            objectUnderTest.deleteListElement(DATASPACE_NAME, ANCHOR_NAME3, listNodeXpaths)
        then: 'a #expectedException is thrown'
            thrown(expectedException)
        where: 'following parameters were used'
            scenario                                            | listNodeXpaths                                    || expectedException
            'list parent node does not exist'                   | '/unknown/unknown'                                || DataNodeNotFoundException
            'list child nodes do not exist'                     | '/parent-200/unknown'                             || DataNodeNotFoundException
            'list child nodes with key does not exist'          | '/parent-200/unknown[@key="C"]'                   || DataNodeNotFoundException
            'list grandchild nodes parent does not exist'       | '/parent-200/unknown/unknown'                     || DataNodeNotFoundException
            'non-existing parent with existing list-node'       | '/unknown/child-204'                              || DataNodeNotFoundException
            'non-existing parent with existing list-node & key' | '/unknown/child-204[@key="A"]'                    || DataNodeNotFoundException
            'valid with non existing key'                       | '/parent-200/child-202/grand-child-202[@key="A"]' || DataNodeNotFoundException
            'child list node without key'                       | '/parent-200/child-204/grand-child-204'           || DataNodeNotFoundException
            'valid list node with invalid key'                  | '/parent-203/child-204[@key="C"]'                 || DataNodeNotFoundException

    }

    static Collection<DataNode> buildDataNodeCollection(xpaths) {
        return xpaths.collect { new DataNodeBuilder().withXpath(it).build() }
    }

    static DataNode buildDataNode(xpath, leaves, childDataNodes) {
        return new DataNodeBuilder().withXpath(xpath).withLeaves(leaves).withChildDataNodes(childDataNodes).build()
    }

    static Map<String, Object> getLeavesMap(FragmentEntity fragmentEntity) {
        return GSON.fromJson(fragmentEntity.getAttributes(), Map<String, Object>.class)
    }

    def static assertLeavesMaps(actualLeavesMap, expectedLeavesMap) {
        expectedLeavesMap.forEach((key, value) -> {
            def actualValue = actualLeavesMap[key]
            if (value instanceof Collection<?> && actualValue instanceof Collection<?>) {
                assert value.size() == actualValue.size()
                assert value.containsAll(actualValue)
            } else {
                assert value == actualValue
            }
        })
        return true
    }

    def static treeToFlatMapByXpath(Map<String, DataNode> flatMap, DataNode dataNodeTree) {
        flatMap.put(dataNodeTree.getXpath(), dataNodeTree)
        dataNodeTree.getChildDataNodes()
                .forEach(childDataNode -> treeToFlatMapByXpath(flatMap, childDataNode))
        return flatMap
    }

}
