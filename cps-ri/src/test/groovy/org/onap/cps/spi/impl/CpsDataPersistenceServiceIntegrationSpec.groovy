/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
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

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.ImmutableSet
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.entities.FragmentEntity
import org.onap.cps.spi.exceptions.AlreadyDefinedException
import org.onap.cps.spi.exceptions.AnchorNotFoundException
import org.onap.cps.spi.exceptions.DataNodeNotFoundException
import org.onap.cps.spi.exceptions.DataspaceNotFoundException
import org.onap.cps.spi.model.DataNode
import org.onap.cps.spi.model.DataNodeBuilder
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql

import javax.validation.ConstraintViolationException
import java.util.stream.Collectors

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS

class CpsDataPersistenceServiceIntegrationSpec extends CpsPersistenceSpecBase {

    @Autowired
    CpsDataPersistenceService objectUnderTest

    static final JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

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
    def 'Add multiple list elements including an element with a child datanode.'() {
        given: 'two new data nodes for an existing list'
            def listElementXpaths = ['/parent-201/child-204[@key="B"]', '/parent-201/child-204[@key="C"]']
            def listElements = toDataNodes(listElementXpaths)
        and: 'a child node for one of the new data nodes'
            def childDataNode = buildDataNode('/parent-201/child-204[@key="C"]/grand-child-204[@key2="Z"]', [leave:'value'], [])
            listElements[0].childDataNodes = [childDataNode]
        when: 'the data nodes (list elements) are added to existing parent node'
            objectUnderTest.addListElements(DATASPACE_NAME, ANCHOR_NAME3, '/parent-201', listElements)
        then: 'new entries successfully persisted, parent node now contains 5 children (2 new + 3 existing before)'
            def parentFragment = fragmentRepository.getById(LIST_DATA_NODE_PARENT201_FRAGMENT_ID)
            def allChildXpaths = parentFragment.getChildFragments().collect { it.getXpath() }
            assert allChildXpaths.size() == 5
            assert allChildXpaths.containsAll(listElementXpaths)
        and: 'the child node of the new list entry is also present'
            def dataspaceEntity = dataspaceRepository.getByName(DATASPACE_NAME)
            def anchorEntity = anchorRepository.getByDataspaceAndName(dataspaceEntity, ANCHOR_NAME3)
            def listElementChild = fragmentRepository.findByDataspaceAndAnchorAndXpath(dataspaceEntity, anchorEntity, childDataNode.xpath)
            assert listElementChild.isPresent()
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Add list element error scenario: #scenario.'() {
        given: 'list element as a collection of data nodes'
            def listElementCollection = toDataNodes(listElementXpaths)
        when: 'attempt to add list elements to parent node'
            objectUnderTest.addListElements(DATASPACE_NAME, ANCHOR_NAME3, parentNodeXpath, listElementCollection)
        then: 'a #expectedException is thrown'
            thrown(expectedException)
        where: 'following parameters were used'
            scenario                     | parentNodeXpath | listElementXpaths                   || expectedException
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
    def 'Replace list content of #scenario.'() {
        given: 'list element as a collection of data nodes'
            def listElementCollection = toDataNodes(listElementXpaths)
        when: 'list elements are replaced within the existing parent node'
            objectUnderTest.replaceListContent(DATASPACE_NAME, ANCHOR_NAME3, parentXpath, listElementCollection)
        then: 'list elements are updated as expected, non-list element remains as is'
            def parentFragment = fragmentRepository.getById(listElementFragmentID)
            def allChildXpaths = parentFragment.getChildFragments().collect { it.getXpath() }
            assert allChildXpaths.size() == expectedChildXpaths.size()
            assert allChildXpaths.containsAll(expectedChildXpaths)
        where: 'following parameters were used'
            scenario                                                    | listElementXpaths                                                          | parentXpath             | listElementFragmentID                 || expectedChildXpaths
            'existing list element with non existing key'               | ['/parent-201/child-204[@key="B"]']                                        | '/parent-201'           | LIST_DATA_NODE_PARENT201_FRAGMENT_ID  || ['/parent-201/child-203', '/parent-201/child-204[@key="B"]']
            'non existing list element with non existing key'           | ['/parent-201/child-205[@key="1"]']                                        | '/parent-201'           | LIST_DATA_NODE_PARENT201_FRAGMENT_ID  || ['/parent-201/child-203', '/parent-201/child-204[@key="A"]', '/parent-201/child-204[@key="X"]', '/parent-201/child-205[@key="1"]']
            'list element with 1 existing key'                          | ['/parent-201/child-204[@key="X"]']                                        | '/parent-201'           | LIST_DATA_NODE_PARENT201_FRAGMENT_ID  || ['/parent-201/child-203', '/parent-201/child-204[@key="X"]']
            'list element with combined keys'                           | ['/parent-202/child-205[@key="A"]']                                        | '/parent-202'           | LIST_DATA_NODE_PARENT202_FRAGMENT_ID  || ['/parent-202/child-206[@key="A"]', '/parent-202/child-205[@key="A"]']
            'grandchild list element'                                   | ['/parent-200/child-202/grand-child-202[@key="E"]']                        | '/parent-200/child-202' | LIST_DATA_NODE_CHILD202_FRAGMENT_ID   || ['/parent-200/child-202/grand-child-202[@key="E"]']
            'list element with two list elements'                       | ['/parent-201/child-204[@key="new X"]', '/parent-201/child-204[@key="Y"]'] | '/parent-201'           | LIST_DATA_NODE_PARENT201_FRAGMENT_ID  || ['/parent-201/child-203', '/parent-201/child-204[@key="new X"]', '/parent-201/child-204[@key="Y"]']
            'list element with compounded list element'                 | ['/parent-202/child-205[@key="A" and @key2="B"]']                          | '/parent-202'           | LIST_DATA_NODE_PARENT202_FRAGMENT_ID  || ['/parent-202/child-206[@key="A"]', '/parent-202/child-205[@key="A" and @key2="B"]']
            'list element with list element with parent with key value' | ['/parent-204[@key="L"]/child-210[@key="N"]']                              | '/parent-204[@key="L"]' | LIST_DATA_NODE_PARENT204_FRAGMENT_ID  || ['/parent-204[@key="L"]/child-210[@key="N"]']
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Replace list content that has #scenario'() {
        given: 'list element with child list element as a collection of data nodes'
            def grandChildDataNodes = toDataNodes(grandChildXpaths)
            def listElementCollection = new DataNodeBuilder().withXpath(childXpath).withChildDataNodes(grandChildDataNodes).build()
        when: 'list elements replaced within the existing parent node'
            objectUnderTest.replaceListContent(DATASPACE_NAME, ANCHOR_NAME3, parentXpath, [listElementCollection ])
        then: 'list elements are updated as expected with non-list elements remaining as is'
            def parentFragment = fragmentRepository.getById(listElementFragmentId)
            def allChildXpaths = parentFragment.getChildFragments().collect { it.getXpath() }
            assert allChildXpaths.size() == expectedRemainingChildXpaths.size()
            assert allChildXpaths.containsAll(expectedRemainingChildXpaths)
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
            scenario                                  | parentXpath   | childXpath                        | grandChildXpaths                                                                              | listElementFragmentId                || expectedRemainingChildXpaths
            'grandchild of list'                      | '/parent-203' | '/parent-203/child-204[@key="X"]' | ['/parent-203/child-204/grandchild[@key="2"]']                                                | LIST_DATA_NODE_PARENT203_FRAGMENT_ID || ['/parent-203/child-203', '/parent-203/child-204[@key="X"]']
            'grandchild of list with two new element' | '/parent-203' | '/parent-203/child-204[@key="X"]' | ['/parent-203/child-204/grandchild[@key="2"]' , '/parent-203/child-204/grandchild[@key="3"]'] | LIST_DATA_NODE_PARENT203_FRAGMENT_ID || ['/parent-203/child-203', '/parent-203/child-204[@key="X"]']
            'grandchild with compound list elements'  | '/parent-205' | '/parent-205/child-205[@key="X"]' | ['/parent-205/child-205/grand-child-206[@key="Y" and @key2="Z"]']                             | LIST_DATA_NODE_PARENT205_FRAGMENT_ID || ['/parent-205/child-205', '/parent-205/child-205[@key="X"]']
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Replace list content of #scenario with grandchildren.'() {
        given: 'list element as a collection of data nodes'
            def listElementCollection = toDataNodes(listElementXpaths)
        when: 'list elements are replaced within the existing parent node'
            objectUnderTest.replaceListContent(DATASPACE_NAME, ANCHOR_NAME3, parentXpath, listElementCollection)
        then: 'child list elements are updated as expected with non-list elements remaining as is'
            def parentFragment = fragmentRepository.getById(listElementFragmentID)
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
            scenario                                       | listElementXpaths                   | parentXpath   | listElementFragmentID                || expectedChildXpaths                                          | expectedGrandChildXpaths
            'existing list element with existing keys'     | ['/parent-203/child-204[@key="X"]'] | '/parent-203' | LIST_DATA_NODE_PARENT203_FRAGMENT_ID || ['/parent-203/child-203', '/parent-203/child-204[@key="X"]'] | []
            'non existing list element with existing keys' | ['/parent-203/child-204[@key="V"]'] | '/parent-203' | LIST_DATA_NODE_PARENT203_FRAGMENT_ID || ['/parent-203/child-203', '/parent-203/child-204[@key="V"]'] | []
    }


    @Sql([CLEAR_DATA, SET_DATA])
    def 'Replace content error scenario: #scenario.'() {
        given: 'list element as a collection of data nodes'
            def listElementCollection = toDataNodes(listElementXpaths)
        when: 'list elements were replaced under existing parent node'
            objectUnderTest.replaceListContent(DATASPACE_NAME, ANCHOR_NAME3, parentNodeXpath, listElementCollection)
        then: 'a #expectedException is thrown'
            thrown(expectedException)
        where: 'following parameters were used'
            scenario                     | parentNodeXpath | listElementXpaths || expectedException
            'parent node does not exist' | '/unknown'      | ['irrelevant'] || DataNodeNotFoundException
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Delete list scenario: #scenario.'() {
        when: 'deleting list is executed for: #scenario.'
            objectUnderTest.deleteListDataNode(DATASPACE_NAME, ANCHOR_NAME3, targetXpaths)
        then: 'only the expected children remain'
            def parentFragment = fragmentRepository.getById(parentFragmentId)
            def remainingChildXpaths = parentFragment.getChildFragments().collect { it.getXpath() }
            assert remainingChildXpaths.size() == expectedRemainingChildXpaths.size()
            assert remainingChildXpaths.containsAll(expectedRemainingChildXpaths)
        where: 'following parameters were used'
            scenario                          | targetXpaths                                                 | parentFragmentId                     || expectedRemainingChildXpaths
            'list element with key'           | '/parent-203/child-204[@key="A"]'                            | LIST_DATA_NODE_PARENT203_FRAGMENT_ID || ['/parent-203/child-203', '/parent-203/child-204[@key="X"]']
            'list element with combined keys' | '/parent-202/child-205[@key="A" and @key2="B"]'              | LIST_DATA_NODE_PARENT202_FRAGMENT_ID || ['/parent-202/child-206[@key="A"]']
            'whole list'                      | '/parent-203/child-204'                                      | LIST_DATA_NODE_PARENT203_FRAGMENT_ID || ['/parent-203/child-203']
            'list element under list element' | '/parent-203/child-204[@key="X"]/grand-child-204[@key2="Y"]' | LIST_DATA_NODE_PARENT203_FRAGMENT_ID || ['/parent-203/child-203', '/parent-203/child-204[@key="X"]', '/parent-203/child-204[@key="A"]']
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Delete list error scenario: #scenario.'() {
        when: 'attempting to delete scenario: #scenario.'
            objectUnderTest.deleteListDataNode(DATASPACE_NAME, ANCHOR_NAME3, targetXpaths)
        then: 'a DataNodeNotFoundException is thrown'
            thrown(DataNodeNotFoundException)
        where: 'following parameters were used'
            scenario                                   | targetXpaths
            'whole list, parent node does not exist'   | '/unknown/some-child'
            'list element, parent node does not exist' | '/unknown/child-204[@key="A"]'
            'whole list does not exist'                | '/parent-200/unknown'
            'list element, list does not exist'        | '/parent-200/unknown[@key="C"]'
            'list element, element does not exist'     | '/parent-203/child-204[@key="C"]'
            'valid datanode but not a list'            | '/parent-200/child-202'
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Confirm deletion of #scenario.'() {
        given: 'a valid data node'
            def dataNode
            def dataNodeXpath
        when: 'data nodes are deleted'
            objectUnderTest.deleteDataNode(DATASPACE_NAME, ANCHOR_NAME3, xpathForDeletion)
        then: 'verify data nodes are removed'
            try {
                dataNode = objectUnderTest.getDataNode(DATASPACE_NAME, ANCHOR_NAME3, getDataNodesXpaths, INCLUDE_ALL_DESCENDANTS)
                dataNodeXpath = dataNode.getXpath()
                assert dataNodeXpath == expectedXpaths
            } catch (DataNodeNotFoundException) {
                assert dataNodeXpath == expectedXpaths
            }
        where: 'following parameters were used'
            scenario                                | xpathForDeletion                                   | getDataNodesXpaths                                || expectedXpaths
            'child of target'                       | '/parent-206/child-206'                            | '/parent-206/child-206'                           || null
            'child data node, parent still exists'  | '/parent-206/child-206'                            | '/parent-206'                                     || '/parent-206'
            'list element'                          | '/parent-206/child-206/grand-child-206[@key="A"]'  | '/parent-206/child-206/grand-child-206[@key="A"]' || null
            'list element, sibling still exists'    | '/parent-206/child-206/grand-child-206[@key="A"]'  | '/parent-206/child-206/grand-child-206[@key="X"]' || '/parent-206/child-206/grand-child-206[@key="X"]'
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Delete data node with #scenario.'() {
        when: 'data node is deleted'
            objectUnderTest.deleteDataNode(DATASPACE_NAME, ANCHOR_NAME3, datanodeXpath)
        then: 'a #expectedException is thrown'
            thrown(DataNodeNotFoundException)
        where: 'the following parameters were used'
            scenario                                        | datanodeXpath
            'valid data node, non existent child node'      | '/parent-203/child-non-existent'
            'invalid list element'                          | '/parent-206/child-206/grand-child-206@key="A"]'
    }

    static Collection<DataNode> toDataNodes(xpaths) {
        return xpaths.collect { new DataNodeBuilder().withXpath(it).build() }
    }

    static DataNode buildDataNode(xpath, leaves, childDataNodes) {
        return new DataNodeBuilder().withXpath(xpath).withLeaves(leaves).withChildDataNodes(childDataNodes).build()
    }

    static Map<String, Object> getLeavesMap(FragmentEntity fragmentEntity) {
        return jsonObjectMapper.convertJsonString(fragmentEntity.getAttributes(), Map<String, Object>.class)
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
