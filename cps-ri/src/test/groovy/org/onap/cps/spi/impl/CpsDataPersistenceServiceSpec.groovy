/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021 Bell Canada.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
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
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.entities.FragmentEntity
import org.onap.cps.spi.exceptions.AlreadyDefinedException
import org.onap.cps.spi.exceptions.AnchorNotFoundException
import org.onap.cps.spi.exceptions.DataNodeNotFoundException
import org.onap.cps.spi.exceptions.DataspaceNotFoundException
import org.onap.cps.spi.model.DataNode
import org.onap.cps.spi.model.DataNodeBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.jdbc.Sql
import spock.lang.Unroll

import javax.validation.ConstraintViolationException

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS

class CpsDataPersistenceServiceSpec extends CpsPersistenceSpecBase {

    @Autowired
    CpsDataPersistenceService objectUnderTest

    static final Gson GSON = new GsonBuilder().create()

    static final String SET_DATA = '/data/fragment.sql'
    static final long ID_DATA_NODE_WITH_DESCENDANTS = 4001
    static final String XPATH_DATA_NODE_WITH_DESCENDANTS = '/parent-1'
    static final String XPATH_DATA_NODE_WITH_LEAVES = '/parent-100'
    static final long UPDATE_DATA_NODE_FRAGMENT_ID = 4202L
    static final long UPDATE_DATA_NODE_SUB_FRAGMENT_ID = 4203L

    static final DataNode newDataNode = new DataNodeBuilder().build()
    static DataNode existingDataNode
    static DataNode existingChildDataNode

    def expectedLeavesByXpathMap = [
            '/parent-100'                      : ['parent-leaf': 'parent-leaf-value'],
            '/parent-100/child-001'            : ['first-child-leaf': 'first-child-leaf-value'],
            '/parent-100/child-002'            : ['second-child-leaf': 'second-child-leaf-value'],
            '/parent-100/child-002/grand-child': ['grand-child-leaf': 'grand-child-leaf-value']
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

    @Unroll
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

    @Unroll
    @Sql([CLEAR_DATA, SET_DATA])
    def 'Add child error scenario: #scenario.'() {
        when: 'attempt to add a child data node with #scenario'
            objectUnderTest.addChildDataNode(DATASPACE_NAME, ANCHOR_NAME1, parentXpath, dataNode)
        then: 'a #expectedException is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                 | parentXpath                      | dataNode              || expectedException
            'parent does not exist'  | 'unknown'                        | newDataNode           || DataNodeNotFoundException
            'already existing child' | XPATH_DATA_NODE_WITH_DESCENDANTS | existingChildDataNode || DataIntegrityViolationException
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
                    XPATH_DATA_NODE_WITH_LEAVES, OMIT_DESCENDANTS)
        then: 'data node is returned with no descendants'
            assert result.getXpath() == XPATH_DATA_NODE_WITH_LEAVES
        and: 'expected leaves'
            assert result.getChildDataNodes().size() == 0
            assertLeavesMaps(result.getLeaves(), expectedLeavesByXpathMap[XPATH_DATA_NODE_WITH_LEAVES])
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Get data node by xpath with all descendants.'() {
        when: 'data node is requested with all descendants'
            def result = objectUnderTest.getDataNode(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES,
                    XPATH_DATA_NODE_WITH_LEAVES, INCLUDE_ALL_DESCENDANTS)
            def mappedResult = treeToFlatMapByXpath(new HashMap<>(), result)
        then: 'data node is returned with all the descendants populated'
            assert mappedResult.size() == 4
            assert result.getChildDataNodes().size() == 2
            assert mappedResult.get('/parent-100/child-001').getChildDataNodes().size() == 0
            assert mappedResult.get('/parent-100/child-002').getChildDataNodes().size() == 1
        and: 'extracted leaves maps are matching expected'
            mappedResult.forEach(
                    (xpath, dataNode) ->
                            assertLeavesMaps(dataNode.getLeaves(), expectedLeavesByXpathMap[xpath])
            )
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
        }
        )
        return true
    }

    def static treeToFlatMapByXpath(Map<String, DataNode> flatMap, DataNode dataNodeTree) {
        flatMap.put(dataNodeTree.getXpath(), dataNodeTree)
        dataNodeTree.getChildDataNodes()
                .forEach(childDataNode -> treeToFlatMapByXpath(flatMap, childDataNode))
        return flatMap
    }

    @Unroll
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
            def updatedFragment = fragmentRepository.getOne(UPDATE_DATA_NODE_FRAGMENT_ID)
            def updatedLeaves = getLeavesMap(updatedFragment)
            assert updatedLeaves.size() == 1
            assert updatedLeaves.'leaf-value' == 'new'
        and: 'existing child entry remains as is'
            def childFragment = updatedFragment.getChildFragments().iterator().next()
            def childLeaves = getLeavesMap(childFragment)
            assert childFragment.getId() == UPDATE_DATA_NODE_SUB_FRAGMENT_ID
            assert childLeaves.'leaf-value' == 'original'
    }

    @Unroll
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
            def updatedFragment = fragmentRepository.getOne(UPDATE_DATA_NODE_FRAGMENT_ID)
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
            def updatedFragment = fragmentRepository.getOne(UPDATE_DATA_NODE_FRAGMENT_ID)
            def updatedLeaves = getLeavesMap(updatedFragment)
            assert updatedLeaves.size() == 1
            assert updatedLeaves.'leaf-value' == 'new'
        and: 'previously attached child entry is removed from database'
            fragmentRepository.findById(UPDATE_DATA_NODE_SUB_FRAGMENT_ID).isEmpty()
        and: 'new child entry with same content is created'
            def childFragment = updatedFragment.getChildFragments().iterator().next()
            def childLeaves = getLeavesMap(childFragment)
            assert childFragment.getId() != UPDATE_DATA_NODE_SUB_FRAGMENT_ID
            assert childLeaves.'leaf-value' == 'original'
    }

    @Unroll
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

    static DataNode buildDataNode(xpath, leaves, childDataNodes) {
        return new DataNodeBuilder().withXpath(xpath).withLeaves(leaves).withChildDataNodes(childDataNodes).build()
    }

    static Map<String, Object> getLeavesMap(FragmentEntity fragmentEntity) {
        return GSON.fromJson(fragmentEntity.getAttributes(), Map<String, Object>.class)
    }

    @Unroll
    @Sql([CLEAR_DATA, SET_DATA])
    def 'Cps Path query for single leaf value with type: #type.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodes(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES, cpsPath, includeDescendantsOption)
        then: 'the correct data is returned'
            def leaves = '[common-leaf-name:common-leaf-value, common-leaf-name-int:5.0]'
            DataNode dataNode = result.stream().findFirst().get()
            dataNode.getLeaves().toString() == leaves
            dataNode.getChildDataNodes().size() == expectedNumberOfChidlNodes
        where: 'the following data is used'
            type                        | cpsPath                                                          | includeDescendantsOption || expectedNumberOfChidlNodes
            'String and no descendants' | '/parent-200/child-202[@common-leaf-name=\'common-leaf-value\']' | OMIT_DESCENDANTS         || 0
            'Integer and descendants'   | '/parent-200/child-202[@common-leaf-name-int=5]'                 | INCLUDE_ALL_DESCENDANTS  || 1
    }

    @Unroll
    @Sql([CLEAR_DATA, SET_DATA])
    def 'Query for attribute by cps path with cps paths that return no data because of #scenario.'() {
        when: 'a query is executed to get datanodes for the given cps path'
            def result = objectUnderTest.queryDataNodes(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES, cpsPath, FetchDescendantsOption.OMIT_DESCENDANTS)
        then: 'no data is returned'
            result.isEmpty()
        where: 'following cps queries are performed'
            scenario                           | cpsPath
            'cps path is incomplete'           | '/parent-200[@common-leaf-name-int=5]'
            'leaf value does not exist'        | '/parent-200/child-202[@common-leaf-name=\'does not exist\']'
            'incomplete end of xpath prefix'   | '/parent-200/child-20[@common-leaf-name-int=5]'
    }

    @Unroll
    @Sql([CLEAR_DATA, SET_DATA])
    def 'Cps Path query using descendant anywhere and #type (further) descendants.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def cpsPath = '//child-202'
            def result = objectUnderTest.queryDataNodes(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES, cpsPath, includeDescendantsOption)
        then: 'the data node has the correct number of children'
            DataNode dataNode = result.stream().findFirst().get()
            dataNode.getChildDataNodes().size() == expectedNumberOfChildNodes
        where: 'the following data is used'
            type      | includeDescendantsOption || expectedNumberOfChildNodes
            'omit'    | OMIT_DESCENDANTS         || 0
            'include' | INCLUDE_ALL_DESCENDANTS  || 1
    }

    @Unroll
    @Sql([CLEAR_DATA, SET_DATA])
    def 'Cps Path query using descendant anywhere with %scenario '() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodes(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES, cpsPath, OMIT_DESCENDANTS)
        then: 'Only one data node is returned'
            result.size() == expectedResultSize
        where: 'the following data is used'
            scenario                                  | cpsPath             || expectedResultSize
            'fully unique descendant name'            | '//grand-child-202' || 1
            'descendant name match end of other node' | '//child-202'       || 2
    }

    @Unroll
    @Sql([CLEAR_DATA, SET_DATA])
    def 'Cps Path query using descendant anywhere ends with yang list containing %scenario '() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodes(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES, cpsPath, OMIT_DESCENDANTS)
        then: 'Only one data node is returned'
            result.size() == expectedResultSize
        where: 'the following data is used'
            scenario                   | cpsPath                                                                          || expectedResultSize
            'one attribute'            | '//child-202[@common-leaf-name-int=5]'                                           ||  2
            'more than one attribute'  | '//child-202[@common-leaf-name-int=5 and @common-leaf-name="common-leaf-value"]' ||  1

    }
}
