/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
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

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS
import static org.onap.cps.spi.UpdateDescendantsOption.UPDATE_LEAVES_AND_DESCENDANT_NODES
import static org.onap.cps.spi.UpdateDescendantsOption.UPDATE_LEAVES_ONLY

import com.google.common.collect.ImmutableSet
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.entities.FragmentEntity
import org.onap.cps.spi.exceptions.AnchorNotFoundException
import org.onap.cps.spi.exceptions.DataNodeNotFoundException
import org.onap.cps.spi.exceptions.DataspaceNotFoundException
import org.onap.cps.spi.model.DataNode
import org.onap.cps.spi.model.DataNodeBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.jdbc.Sql
import spock.lang.Unroll

class CpsDataPersistenceServiceSpec extends CpsPersistenceSpecBase {

    @Autowired
    CpsDataPersistenceService objectUnderTest

    static final Gson GSON = new GsonBuilder().create()

    static final String SET_DATA = '/data/fragment.sql'
    static final long ID_DATA_NODE_WITH_DESCENDANTS = 4001
    static final String XPATH_DATA_NODE_WITH_DESCENDANTS = '/parent-1'
    static final String XPATH_DATA_NODE_WITH_LEAVES = '/parent-100'

    static final DataNode newDataNode = new DataNodeBuilder().build()
    static DataNode existingDataNode
    static DataNode existingChildDataNode

    static Map<String, Map<String, Object>> expectedLeavesByXpathMap = [
            '/parent-100'                      : ["x": "y"],
            '/parent-100/child-001'            : ["a": "b", "c": ["d", "e", "f"]],
            '/parent-100/child-002'            : ["g": "h", "i": ["j", "k"]],
            '/parent-100/child-002/grand-child': ["l": "m", "n": ["o", "p"]]
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
    def 'Store datanode error scenario: #scenario.'() {
        when: 'attempt to store a data node with #scenario'
            objectUnderTest.storeDataNode(dataspaceName, anchorName, dataNode)
        then: 'a #expectedException is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                    | dataspaceName  | anchorName     | dataNode         || expectedException
            'dataspace does not exist'  | 'unknown'      | 'not-relevant' | newDataNode      || DataspaceNotFoundException
            'schema set does not exist' | DATASPACE_NAME | 'unknown'      | newDataNode      || AnchorNotFoundException
            'anchor already exists'     | DATASPACE_NAME | ANCHOR_NAME1   | existingDataNode || DataIntegrityViolationException
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
    def 'Update data node leaves only'() {
        given: 'data node object with leaves updated, no children defined'
            def dataNode = buildDataNode("/parent-200/child-201", ['leaf-value': 'new'], [])
        when: 'update is performed for leaves only'
            objectUnderTest.updateDataNode(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES,
                    dataNode, UPDATE_LEAVES_ONLY)
        then: 'leaves only is updated for selected data node'
            def updatedFragment = fragmentRepository.getOne(4202L)
            def updatedLeaves = getLeavesMap(updatedFragment)
            assert updatedLeaves.size() == 1
            assert updatedLeaves['leaf-value'] == 'new'
        and: 'child entry remains as is'
            def childFragment = updatedFragment.getChildFragments().iterator().next()
            def childLeaves = getLeavesMap(childFragment)
            assert childFragment.getId() == 4203L
            assert childLeaves['leaf-value'] == 'original'
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Update data node with children removal'() {
        given: 'data node object with leaves updated, no children defined'
            def dataNode = buildDataNode("/parent-200/child-201", ['leaf-value': 'new'], [])
        when: 'update is performed with children update requested'
            objectUnderTest.updateDataNode(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES,
                    dataNode, UPDATE_LEAVES_AND_DESCENDANT_NODES)
        then: 'leaves only is updated for selected data node'
            def updatedFragment = fragmentRepository.getOne(4202L)
            def updatedLeaves = getLeavesMap(updatedFragment)
            assert updatedLeaves.size() == 1
            assert updatedLeaves['leaf-value'] == 'new'
        and: 'updated entry has no children'
            updatedFragment.getChildFragments().isEmpty()
        and: 'previously attached child entry is removed from database'
            fragmentRepository.findById(4203L).isEmpty()
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Update data node with descendants replacement'() {
        given: 'data node object with leaves updated, having child with old content'
            def dataNode = buildDataNode("/parent-200/child-201", ['leaf-value': 'new'], [
                    buildDataNode("/parent-200/child-201/grand-child", ['leaf-value': 'original'], [])
            ])
        when: 'update is performed for leaves only'
            objectUnderTest.updateDataNode(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES,
                    dataNode, UPDATE_LEAVES_AND_DESCENDANT_NODES)
        then: 'leaves content updated for selected data node'
            def updatedFragment = fragmentRepository.getOne(4202L)
            def updatedLeaves = getLeavesMap(updatedFragment)
            assert updatedLeaves.size() == 1
            assert updatedLeaves['leaf-value'] == 'new'
        and: 'previously attached child entry is removed from database'
            fragmentRepository.findById(4203L).isEmpty()
        and: 'new child entry with same content is created'
            def childFragment = updatedFragment.getChildFragments().iterator().next()
            def childLeaves = getLeavesMap(childFragment)
            assert childFragment.getId() != 4203L
            assert childLeaves['leaf-value'] == 'original'
    }

    @Unroll
    @Sql([CLEAR_DATA, SET_DATA])
    def 'Update data node error scenario: #scenario.'() {
        given: 'data node object'
            def dataNode = buildDataNode(xpath, ['not': 'relevant'], [])
        when: 'attempt to update data node for #scenario'
            objectUnderTest.updateDataNode(dataspaceName, anchorName, dataNode, UPDATE_LEAVES_ONLY)
        then: 'a #expectedException is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                 | dataspaceName  | anchorName                        | xpath          || expectedException
            'non-existing dataspace' | 'NO DATASPACE' | 'not relevant'                    | 'not relevant' || DataspaceNotFoundException
            'non-existing anchor'    | DATASPACE_NAME | 'NO ANCHOR'                       | 'not relevant' || AnchorNotFoundException
            'non-existing xpath'     | DATASPACE_NAME | ANCHOR_FOR_DATA_NODES_WITH_LEAVES | 'NO XPATH'     || DataNodeNotFoundException
    }

    static DataNode buildDataNode(xpath, leaves, childDataNodes) {
        return new DataNodeBuilder().withXpath(xpath).withLeaves(leaves).withChildDataNodes(childDataNodes).build()
    }

    static Map<String, Object> getLeavesMap(FragmentEntity fragmentEntity) {
        return GSON.fromJson(fragmentEntity.getAttributes(), Map<String, Object>.class)
    }
}
