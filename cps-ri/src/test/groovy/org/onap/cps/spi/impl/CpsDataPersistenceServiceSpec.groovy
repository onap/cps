/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.exceptions.AnchorNotFoundException
import org.onap.cps.spi.exceptions.DataNodeNotFoundException
import org.onap.cps.spi.exceptions.DataspaceNotFoundException
import org.onap.cps.spi.model.DataNode
import org.onap.cps.spi.model.DataNodeBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.jdbc.Sql
import spock.lang.Unroll

import static org.onap.cps.spi.FetchChildrenOption.OMIT_CHILDREN
import static org.onap.cps.spi.FetchChildrenOption.INCLUDE_ALL_CHILDREN

class CpsDataPersistenceServiceSpec extends CpsPersistenceSpecBase {

    @Autowired
    CpsDataPersistenceService objectUnderTest

    static final String SET_DATA = '/data/fragment.sql'
    static final long ID_DATA_NODE_WITH_DESCENDANTS = 4001
    static final String XPATH_DATA_NODE_WITH_DESCENDANTS = '/parent-1'
    static final String DATA_NODE_XPATH = '/PARENT-100'

    static final DataNode newDataNode = new DataNodeBuilder().build()
    static DataNode existingDataNode
    static DataNode existingChildDataNode

    static Map<String, Map<String, Object>> expectedLeavesByXpathMap = [
            '/PARENT-100'                      : ["x": "y"],
            '/PARENT-100/CHILD-001'            : ["a": "b", "c": ["d", "e", "f"]],
            '/PARENT-100/CHILD-002'            : ["g": "h", "i": ["j", "k"]],
            '/PARENT-100/CHILD-002/GRAND-CHILD': ["l": "m", "n": ["o", "p"]]
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
    def 'Get data node by xpath with no children.'() {
        when: 'data node is requested'
            def result = objectUnderTest.getDataNode(DATASPACE_NAME, ANCHOR_NAME3, DATA_NODE_XPATH, OMIT_CHILDREN)
        then: 'data node is returned with no children'
            assert result.getXpath() == DATA_NODE_XPATH
        and: 'expected leaves'
            assert result.getChildDataNodes().size() == 0
            assertLeavesMaps(result.getLeaves(), expectedLeavesByXpathMap[DATA_NODE_XPATH])
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Get data node by xpath with all the children.'() {
        when: 'data node is requested with all the children'
            def result = objectUnderTest.getDataNode(DATASPACE_NAME, ANCHOR_NAME3, DATA_NODE_XPATH, INCLUDE_ALL_CHILDREN)
            def mappedResult = treeToFlatMapByXpath(new HashMap<>(), result)
        then: 'data node is returned with all the children populated'
            assert mappedResult.size() == 4
            assert result.getChildDataNodes().size() == 2
            assert mappedResult.get('/PARENT-100/CHILD-001').getChildDataNodes().size() == 0
            assert mappedResult.get('/PARENT-100/CHILD-002').getChildDataNodes().size() == 1
        and: 'extracted leaves maps are matching expected'
            mappedResult.each {
                xpath, dataNode -> assertLeavesMaps(dataNode.getLeaves(), expectedLeavesByXpathMap[xpath])
            }
    }

    def static assertLeavesMaps(actualLeavesMap, expectedLeavesMap) {
        expectedLeavesMap.each { key, value ->
            {
                def actualValue = actualLeavesMap[key]
                if (value instanceof Collection<?> && actualValue instanceof Collection<?>) {
                    assert value.size() == actualValue.size()
                    assert value.containsAll(actualValue)
                } else {
                    assert value == actualValue
                }
            }
        }
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
            objectUnderTest.getDataNode(dataspaceName, anchorName, xpath, OMIT_CHILDREN)
        then: 'a #expectedException is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                 | dataspaceName  | anchorName   | xpath           || expectedException
            'non-existing dataspace' | 'NO DATASPACE' | ANCHOR_NAME3 | DATA_NODE_XPATH || DataspaceNotFoundException
            'non-existing anchor'    | DATASPACE_NAME | 'NO ANCHOR'  | DATA_NODE_XPATH || AnchorNotFoundException
            'non-existing xpath'     | DATASPACE_NAME | ANCHOR_NAME3 | 'NO XPATH'      || DataNodeNotFoundException
    }
}
