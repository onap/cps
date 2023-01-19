/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021-2022 Bell Canada.
 *  Modifications Copyright (C) 2022 TechMahindra Ltd.
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
import org.onap.cps.cpspath.parser.PathParsingException
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.entities.FragmentEntity
import org.onap.cps.spi.exceptions.AlreadyDefinedExceptionBatch
import org.onap.cps.spi.exceptions.AnchorNotFoundException
import org.onap.cps.spi.exceptions.CpsAdminException
import org.onap.cps.spi.exceptions.CpsPathException
import org.onap.cps.spi.exceptions.DataNodeNotFoundException
import org.onap.cps.spi.exceptions.DataspaceNotFoundException
import org.onap.cps.spi.model.DataNode
import org.onap.cps.spi.model.DataNodeBuilder
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql

import javax.validation.ConstraintViolationException

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS

class CpsDataPersistenceServiceIntegrationSpec extends CpsPersistenceSpecBase {

    @Autowired
    CpsDataPersistenceService objectUnderTest

    static JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    static DataNodeBuilder dataNodeBuilder = new DataNodeBuilder()

    static final String SET_DATA = '/data/fragment.sql'
    static int DATASPACE_1001_ID = 1001L
    static int ANCHOR_3003_ID = 3003L
    static long ID_DATA_NODE_WITH_DESCENDANTS = 4001
    static String XPATH_DATA_NODE_WITH_DESCENDANTS = '/parent-1'
    static String XPATH_DATA_NODE_WITH_LEAVES = '/parent-207'
    static long DATA_NODE_202_FRAGMENT_ID = 4202L
    static long CHILD_OF_DATA_NODE_202_FRAGMENT_ID = 4203L
    static long LIST_DATA_NODE_PARENT201_FRAGMENT_ID = 4206L
    static long LIST_DATA_NODE_PARENT203_FRAGMENT_ID = 4214L
    static long LIST_DATA_NODE_PARENT202_FRAGMENT_ID = 4211L
    static long PARENT_3_FRAGMENT_ID = 4003L

    static Collection<DataNode> newDataNodes = [new DataNodeBuilder().build()]
    static Collection<DataNode> existingDataNodes = [createDataNodeTree(XPATH_DATA_NODE_WITH_DESCENDANTS)]
    static Collection<DataNode> existingChildDataNodes = [createDataNodeTree('/parent-1/child-1')]

    def static deleteTestParentXPath = '/parent-200'
    def static deleteTestChildXpath = "${deleteTestParentXPath}/child-with-slash[@key='a/b']"
    def static deleteTestGrandChildXPath = "${deleteTestChildXpath}/grandChild"

    def expectedLeavesByXpathMap = [
            '/parent-207'                      : ['parent-leaf': 'parent-leaf value'],
            '/parent-207/child-001'            : ['first-child-leaf': 'first-child-leaf value'],
            '/parent-207/child-002'            : ['second-child-leaf': 'second-child-leaf value'],
            '/parent-207/child-002/grand-child': ['grand-child-leaf': 'grand-child-leaf value']
    ]

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Get existing datanode with descendants.'() {
        when: 'the node is retrieved by its xpath'
            def dataNode = objectUnderTest.getDataNode(DATASPACE_NAME, ANCHOR_NAME1, '/parent-1', INCLUDE_ALL_DESCENDANTS)
        then: 'the path and prefix are populated correctly'
            assert dataNode.xpath == '/parent-1'
        and: 'dataNode has no prefix (to be addressed by CPS-1301'
            assert dataNode.moduleNamePrefix == null
        and: 'the child node has the correct path'
            assert dataNode.childDataNodes[0].xpath == '/parent-1/child-1'
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Storing and Retrieving a new DataNodes with descendants.'() {
        when: 'a fragment with descendants is stored'
            def parentXpath = '/parent-new'
            def childXpath = '/parent-new/child-new'
            def grandChildXpath = '/parent-new/child-new/grandchild-new'
            def dataNodes = [createDataNodeTree(parentXpath, childXpath, grandChildXpath)]
            objectUnderTest.storeDataNodes(DATASPACE_NAME, ANCHOR_NAME1, dataNodes)
        then: 'it can be retrieved by its xpath'
            def dataNode = objectUnderTest.getDataNode(DATASPACE_NAME, ANCHOR_NAME1, parentXpath, INCLUDE_ALL_DESCENDANTS)
            assert dataNode.xpath == parentXpath
        and: 'it has the correct child'
            assert dataNode.childDataNodes.size() == 1
            def childDataNode = dataNode.childDataNodes[0]
            assert childDataNode.xpath == childXpath
        and: 'and its grandchild'
            assert childDataNode.childDataNodes.size() == 1
            def grandChildDataNode = childDataNode.childDataNodes[0]
            assert grandChildDataNode.xpath == grandChildXpath
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Store data node for multiple anchors using the same schema.'() {
        def xpath = '/parent-new'
        given: 'a fragment is stored for an anchor'
            objectUnderTest.storeDataNodes(DATASPACE_NAME, ANCHOR_NAME1, [createDataNodeTree(xpath)])
        when: 'another fragment is stored for an other anchor, using the same schema set'
            objectUnderTest.storeDataNodes(DATASPACE_NAME, ANCHOR_NAME3, [createDataNodeTree(xpath)])
        then: 'both fragments can be retrieved by their xpath'
            def fragment1 = getFragmentByXpath(DATASPACE_NAME, ANCHOR_NAME1, xpath)
            fragment1.anchor.name == ANCHOR_NAME1
            fragment1.xpath == xpath
            def fragment2 = getFragmentByXpath(DATASPACE_NAME, ANCHOR_NAME3, xpath)
            fragment2.anchor.name == ANCHOR_NAME3
            fragment2.xpath == xpath
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Store datanodes error scenario: #scenario.'() {
        when: 'attempt to store a data node with #scenario'
            objectUnderTest.storeDataNodes(dataspaceName, anchorName, dataNodes)
        then: 'a #expectedException is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                    | dataspaceName  | anchorName     | dataNodes          || expectedException
            'dataspace does not exist'  | 'unknown'      | 'not-relevant' | newDataNodes       || DataspaceNotFoundException
            'schema set does not exist' | DATASPACE_NAME | 'unknown'      | newDataNodes       || AnchorNotFoundException
            'anchor already exists'     | DATASPACE_NAME | ANCHOR_NAME1   | newDataNodes       || ConstraintViolationException
            'datanode already exists'   | DATASPACE_NAME | ANCHOR_NAME1   | existingDataNodes  || AlreadyDefinedExceptionBatch
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Add children to a Fragment that already has a child.'() {
        given: 'collection of new child data nodes'
            def newChild1 = createDataNodeTree('/parent-1/child-2')
            def newChild2 = createDataNodeTree('/parent-1/child-3')
            def newChildrenCollection = [newChild1, newChild2]
        when: 'the child is added to an existing parent with 1 child'
            objectUnderTest.addChildDataNodes(DATASPACE_NAME, ANCHOR_NAME1, XPATH_DATA_NODE_WITH_DESCENDANTS, newChildrenCollection)
        then: 'the parent is now has to 3 children'
            def expectedExistingChildPath = '/parent-1/child-1'
            def parentFragment = fragmentRepository.findById(ID_DATA_NODE_WITH_DESCENDANTS).orElseThrow()
            parentFragment.childFragments.size() == 3
        and: 'it still has the old child'
            parentFragment.childFragments.find({ it.xpath == expectedExistingChildPath })
        and: 'it has the new children'
            parentFragment.childFragments.find({ it.xpath == newChildrenCollection[0].xpath })
            parentFragment.childFragments.find({ it.xpath == newChildrenCollection[1].xpath })
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Add child error scenario: #scenario.'() {
        when: 'attempt to add a child data node with #scenario'
            objectUnderTest.addChildDataNodes(DATASPACE_NAME, ANCHOR_NAME1, parentXpath, dataNodes)
        then: 'a #expectedException is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                 | parentXpath                      | dataNodes               || expectedException
            'parent does not exist'  | '/unknown'                       | newDataNodes            || DataNodeNotFoundException
            'already existing child' | XPATH_DATA_NODE_WITH_DESCENDANTS | existingChildDataNodes  || AlreadyDefinedExceptionBatch
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Add collection of multiple new list elements including an element with a child datanode.'() {
        given: 'two new child list elements for an existing parent'
            def listElementXpaths = ['/parent-201/child-204[@key="NEW1"]', '/parent-201/child-204[@key="NEW2"]']
            def listElements = toDataNodes(listElementXpaths)
        and: 'a (grand)child data node for one of the new list elements'
            def grandChild = buildDataNode('/parent-201/child-204[@key="NEW1"]/grand-child-204[@key2="NEW1-CHILD"]', [leave:'value'], [])
            listElements[0].childDataNodes = [grandChild]
        when: 'the new data node (list elements) are added to an existing parent node'
            objectUnderTest.addMultipleLists(DATASPACE_NAME, ANCHOR_NAME3, '/parent-201', [listElements])
        then: 'new entries are successfully persisted, parent node now contains 5 children (2 new + 3 existing before)'
            def parentFragment = fragmentRepository.getById(LIST_DATA_NODE_PARENT201_FRAGMENT_ID)
            def allChildXpaths = parentFragment.childFragments.collect { it.xpath }
            assert allChildXpaths.size() == 5
            assert allChildXpaths.containsAll(listElementXpaths)
        and: 'the (grand)child node of the new list entry is also present'
            def dataspaceEntity = dataspaceRepository.getByName(DATASPACE_NAME)
            def anchorEntity = anchorRepository.getByDataspaceAndName(dataspaceEntity, ANCHOR_NAME3)
            def grandChildFragmentEntity = fragmentRepository.findByDataspaceAndAnchorAndXpath(dataspaceEntity, anchorEntity, grandChild.xpath)
            assert grandChildFragmentEntity.isPresent()
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Add multiple list with a mix of existing and new elements'() {
        given: 'two new child list elements for an existing parent'
            def existingDataNode = dataNodeBuilder.withXpath('/parent-207/child-001').withLeaves(['id': '001']).build()
            def newDataNode1 = dataNodeBuilder.withXpath('/parent-207/child-new1').withLeaves(['id': 'new1']).build()
            def newDataNode2 = dataNodeBuilder.withXpath('/parent-200/child-new2').withLeaves(['id': 'new2']).build()
            def dataNodeList1 = [existingDataNode, newDataNode1]
            def dataNodeList2 = [newDataNode2]
        when: 'duplicate data node is requested to be added'
            objectUnderTest.addMultipleLists(DATASPACE_NAME, ANCHOR_HAVING_SINGLE_TOP_LEVEL_FRAGMENT, '/', [dataNodeList1, dataNodeList2])
        then: 'already defined batch exception is thrown'
            def thrown = thrown(AlreadyDefinedExceptionBatch)
        and: 'it only contains the xpath(s) of the duplicated elements'
            assert thrown.alreadyDefinedXpaths.size() == 1
            assert thrown.alreadyDefinedXpaths.contains('/parent-207/child-001')
        and: 'it does NOT contains the xpaths of the new element that were not combined with existing elements'
            assert !thrown.alreadyDefinedXpaths.contains('/parent-207/child-new1')
            assert !thrown.alreadyDefinedXpaths.contains('/parent-207/child-new1')
        and: 'the new entity is inserted correctly'
            def dataspaceEntity = dataspaceRepository.getByName(DATASPACE_NAME)
            def anchorEntity = anchorRepository.getByDataspaceAndName(dataspaceEntity, ANCHOR_HAVING_SINGLE_TOP_LEVEL_FRAGMENT)
            fragmentRepository.findByDataspaceAndAnchorAndXpath(dataspaceEntity, anchorEntity, '/parent-200/child-new2').isPresent()
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Add list element error scenario: #scenario.'() {
        given: 'list element as a collection of data nodes'
            def listElements = toDataNodes(listElementXpaths)
        when: 'attempt to add list elements to parent node'
            objectUnderTest.addListElements(DATASPACE_NAME, ANCHOR_NAME3, parentNodeXpath, listElements)
        then: 'a #expectedException is thrown'
            thrown(expectedException)
        where: 'following parameters were used'
            scenario                        | parentNodeXpath | listElementXpaths                   || expectedException
            'parent node does not exist'    | '/unknown'      | ['irrelevant']                      || DataNodeNotFoundException
            'data fragment already exists'  | '/parent-201'   | ["/parent-201/child-204[@key='A']"] || AlreadyDefinedExceptionBatch
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Get data node by xpath without descendants.'() {
        when: 'data node is requested'
            def result = objectUnderTest.getDataNode(DATASPACE_NAME, ANCHOR_HAVING_SINGLE_TOP_LEVEL_FRAGMENT,
                    inputXPath, OMIT_DESCENDANTS)
        then: 'data node is returned with no descendants'
            assert result.xpath == XPATH_DATA_NODE_WITH_LEAVES
        and: 'expected leaves'
            assert result.childDataNodes.size() == 0
            assertLeavesMaps(result.leaves, expectedLeavesByXpathMap[XPATH_DATA_NODE_WITH_LEAVES])
        where: 'the following data is used'
            scenario      | inputXPath
            'some xpath'  | '/parent-207'
            'root xpath'  | '/'
            'empty xpath' | ''
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Cps Path query with syntax error throws a CPS Path Exception.'() {
        when: 'trying to execute a query with a syntax (parsing) error'
            objectUnderTest.getDataNode(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES, 'invalid-cps-path/child' , OMIT_DESCENDANTS)
        then: 'exception is thrown'
            def exceptionThrown = thrown(CpsPathException)
            assert exceptionThrown.getDetails().contains('failed to parse at line 1 due to extraneous input \'invalid-cps-path\' expecting \'/\'')
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Get data node by xpath with all descendants.'() {
        when: 'data node is requested with all descendants'
            def result = objectUnderTest.getDataNode(DATASPACE_NAME, ANCHOR_HAVING_SINGLE_TOP_LEVEL_FRAGMENT,
                    inputXPath, INCLUDE_ALL_DESCENDANTS)
            def mappedResult = treeToFlatMapByXpath(new HashMap<>(), result)
        then: 'data node is returned with all the descendants populated'
            assert mappedResult.size() == 4
            assert result.childDataNodes.size() == 2
            assert mappedResult.get('/parent-207/child-001').childDataNodes.size() == 0
            assert mappedResult.get('/parent-207/child-002').childDataNodes.size() == 1
        and: 'extracted leaves maps are matching expected'
            mappedResult.forEach(
                    (xPath, dataNode) -> assertLeavesMaps(dataNode.leaves, expectedLeavesByXpathMap[xPath]))
        where: 'the following data is used'
            scenario      | inputXPath
            'some xpath'  | '/parent-207'
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
            scenario                 | dataspaceName  | anchorName                        | xpath           || expectedException
            'non-existing dataspace' | 'NO DATASPACE' | 'not relevant'                    | '/not relevant' || DataspaceNotFoundException
            'non-existing anchor'    | DATASPACE_NAME | 'NO ANCHOR'                       | '/not relevant' || AnchorNotFoundException
            'non-existing xpath'     | DATASPACE_NAME | ANCHOR_FOR_DATA_NODES_WITH_LEAVES | '/NO-XPATH'     || DataNodeNotFoundException
            'invalid xpath'          | DATASPACE_NAME | ANCHOR_FOR_DATA_NODES_WITH_LEAVES | 'INVALID XPATH' || CpsPathException
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Get multiple data nodes by xpath.'() {
        when: 'fetch #scenario.'
            def results = objectUnderTest.getDataNodes(DATASPACE_NAME, ANCHOR_NAME3, inputXpaths, OMIT_DESCENDANTS)
        then: 'the expected number of data nodes are returned'
            assert results.size() == expectedResultSize
        where: 'following parameters were used'
            scenario                               | inputXpaths                                     || expectedResultSize
            '1 node'                               | ["/parent-200"]                                 || 1
            '2 unique nodes'                       | ["/parent-200", "/parent-201"]                  || 2
            '3 unique nodes'                       | ["/parent-200", "/parent-201", "/parent-202"]   || 3
            '1 unique node with duplicate xpath'   | ["/parent-200", "/parent-200"]                  || 1
            '2 unique nodes with duplicate xpath'  | ["/parent-200", "/parent-202", "/parent-200"]   || 2
            'list element with key (single quote)' | ["/parent-201/child-204[@key='A']"]             || 1
            'list element with key (double quote)' | ['/parent-201/child-204[@key="A"]']             || 1
            'non-existing xpath'                   | ["/NO-XPATH"]                                   || 0
            'existing and non-existing xpaths'     | ["/parent-200", "/NO-XPATH", "/parent-201"]     || 2
            'invalid xpath'                        | ["INVALID XPATH"]                               || 0
            'valid and invalid xpaths'             | ["/parent-200", "INVALID XPATH", "/parent-201"] || 2
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Get multiple data nodes error scenario: #scenario.'() {
        when: 'attempt to get data nodes with #scenario'
            objectUnderTest.getDataNodes(dataspaceName, anchorName, ['/not-relevant'], OMIT_DESCENDANTS)
        then: 'a #expectedException is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                 | dataspaceName  | anchorName     || expectedException
            'non-existing dataspace' | 'NO DATASPACE' | 'not relevant' || DataspaceNotFoundException
            'non-existing anchor'    | DATASPACE_NAME | 'NO ANCHOR'    || AnchorNotFoundException
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Update data node leaves.'() {
        when: 'update is performed for leaves'
            objectUnderTest.updateDataLeaves(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES,
                    '/parent-200/child-201', ['leaf-value': 'new'])
        then: 'leaves are updated for selected data node'
            def updatedFragment = fragmentRepository.getById(DATA_NODE_202_FRAGMENT_ID)
            def updatedLeaves = getLeavesMap(updatedFragment)
            assert updatedLeaves.size() == 1
            assert updatedLeaves.'leaf-value' == 'new'
        and: 'existing child entry remains as is'
            def childFragment = updatedFragment.childFragments.iterator().next()
            def childLeaves = getLeavesMap(childFragment)
            assert childFragment.id == CHILD_OF_DATA_NODE_202_FRAGMENT_ID
            assert childLeaves.'leaf-value' == 'original'
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Update data leaves error scenario: #scenario.'() {
        when: 'attempt to update data node for #scenario'
            objectUnderTest.updateDataLeaves(dataspaceName, anchorName, xpath, ['leaf-name': 'leaf-value'])
        then: 'a #expectedException is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                 | dataspaceName  | anchorName                        | xpath                 || expectedException
            'non-existing dataspace' | 'NO DATASPACE' | 'not relevant'                    | '/not relevant'       || DataspaceNotFoundException
            'non-existing anchor'    | DATASPACE_NAME | 'NO ANCHOR'                       | '/not relevant'       || AnchorNotFoundException
            'non-existing xpath'     | DATASPACE_NAME | ANCHOR_FOR_DATA_NODES_WITH_LEAVES | '/NON-EXISTING-XPATH' || DataNodeNotFoundException
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Update data node and descendants by removing descendants.'() {
        given: 'data node object with leaves updated, no children'
            def submittedDataNode = buildDataNode('/parent-200/child-201', ['leaf-value': 'new'], [])
        when: 'update data nodes and descendants is performed'
            objectUnderTest.updateDataNodeAndDescendants(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES, submittedDataNode)
        then: 'leaves have been updated for selected data node'
            def updatedFragment = fragmentRepository.getById(DATA_NODE_202_FRAGMENT_ID)
            def updatedLeaves = getLeavesMap(updatedFragment)
            assert updatedLeaves.size() == 1
            assert updatedLeaves.'leaf-value' == 'new'
        and: 'updated entry has no children'
            updatedFragment.childFragments.isEmpty()
        and: 'previously attached child entry is removed from database'
            fragmentRepository.findById(CHILD_OF_DATA_NODE_202_FRAGMENT_ID).isEmpty()
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Update data node and descendants with new descendants'() {
        given: 'data node object with leaves updated, having child with old content'
            def submittedDataNode = buildDataNode('/parent-200/child-201', ['leaf-value': 'new'], [
                  buildDataNode('/parent-200/child-201/grand-child', ['leaf-value': 'original'], [])
            ])
        when: 'update is performed including descendants'
            objectUnderTest.updateDataNodeAndDescendants(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES, submittedDataNode)
        then: 'leaves have been updated for selected data node'
            def updatedFragment = fragmentRepository.getById(DATA_NODE_202_FRAGMENT_ID)
            def updatedLeaves = getLeavesMap(updatedFragment)
            assert updatedLeaves.size() == 1
            assert updatedLeaves.'leaf-value' == 'new'
        and: 'existing child entry is not updated as content is same'
            def childFragment = updatedFragment.childFragments.iterator().next()
            childFragment.xpath == '/parent-200/child-201/grand-child'
            def childLeaves = getLeavesMap(childFragment)
            assert childLeaves.'leaf-value' == 'original'
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Update data node and descendants with same descendants but changed leaf value.'() {
        given: 'data node object with leaves updated, having child with old content'
            def submittedDataNode = buildDataNode('/parent-200/child-201', ['leaf-value': 'new'], [
                    buildDataNode('/parent-200/child-201/grand-child', ['leaf-value': 'new'], [])
            ])
        when: 'update is performed including descendants'
            objectUnderTest.updateDataNodeAndDescendants(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES, submittedDataNode)
        then: 'leaves have been updated for selected data node'
            def updatedFragment = fragmentRepository.getById(DATA_NODE_202_FRAGMENT_ID)
            def updatedLeaves = getLeavesMap(updatedFragment)
            assert updatedLeaves.size() == 1
            assert updatedLeaves.'leaf-value' == 'new'
        and: 'existing child entry is updated with the new content'
            def childFragment = updatedFragment.childFragments.iterator().next()
            childFragment.xpath == '/parent-200/child-201/grand-child'
            def childLeaves = getLeavesMap(childFragment)
            assert childLeaves.'leaf-value' == 'new'
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Update data node and descendants with different descendants xpath'() {
        given: 'data node object with leaves updated, having child with old content'
            def submittedDataNode = buildDataNode('/parent-200/child-201', ['leaf-value': 'new'], [
                    buildDataNode('/parent-200/child-201/grand-child-new', ['leaf-value': 'new'], [])
            ])
        when: 'update is performed including descendants'
            objectUnderTest.updateDataNodeAndDescendants(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES, submittedDataNode)
        then: 'leaves have been updated for selected data node'
            def updatedFragment = fragmentRepository.getById(DATA_NODE_202_FRAGMENT_ID)
            def updatedLeaves = getLeavesMap(updatedFragment)
            assert updatedLeaves.size() == 1
            assert updatedLeaves.'leaf-value' == 'new'
        and: 'previously attached child entry is removed from database'
            fragmentRepository.findById(CHILD_OF_DATA_NODE_202_FRAGMENT_ID).isEmpty()
        and: 'new child entry is persisted'
            def childFragment = updatedFragment.childFragments.iterator().next()
            childFragment.xpath == '/parent-200/child-201/grand-child-new'
            def childLeaves = getLeavesMap(childFragment)
            assert childLeaves.'leaf-value' == 'new'
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Update data node and descendants error scenario: #scenario.'() {
        given: 'data node object'
            def submittedDataNode = buildDataNode(xpath, ['leaf-name': 'leaf-value'], [])
        when: 'attempt to update data node for #scenario'
            objectUnderTest.updateDataNodeAndDescendants(dataspaceName, anchorName, submittedDataNode)
        then: 'a #expectedException is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                 | dataspaceName  | anchorName                        | xpath                 || expectedException
            'non-existing dataspace' | 'NO DATASPACE' | 'not relevant'                    | '/not relevant'       || DataspaceNotFoundException
            'non-existing anchor'    | DATASPACE_NAME | 'NO ANCHOR'                       | '/not relevant'       || AnchorNotFoundException
            'non-existing xpath'     | DATASPACE_NAME | ANCHOR_FOR_DATA_NODES_WITH_LEAVES | '/NON-EXISTING-XPATH' || DataNodeNotFoundException
            'invalid xpath'          | DATASPACE_NAME | ANCHOR_FOR_DATA_NODES_WITH_LEAVES | 'INVALID XPATH'       || CpsPathException
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Update existing list with #scenario.'() {
        given: 'a parent having a list of data nodes containing: #originalKeys (ech list element has a child too)'
            def parentXpath = '/parent-3'
            if (originalKeys.size() > 0) {
                def originalListEntriesAsDataNodes = createChildListAllHavingAttributeValue(parentXpath, 'original value', originalKeys, true)
                objectUnderTest.addListElements(DATASPACE_NAME, ANCHOR_NAME1, parentXpath, originalListEntriesAsDataNodes)
            }
        and: 'each original list element has one child'
            def originalParentFragment = fragmentRepository.getById(PARENT_3_FRAGMENT_ID)
            originalParentFragment.childFragments.each {assert it.childFragments.size() == 1 }
        when: 'it is updated with #scenario'
            def replacementListEntriesAsDataNodes = createChildListAllHavingAttributeValue(parentXpath, 'new value', replacementKeys, false)
            objectUnderTest.replaceListContent(DATASPACE_NAME, ANCHOR_NAME1, parentXpath, replacementListEntriesAsDataNodes)
        then: 'the result list ONLY contains the expected replacement elements'
            def parentFragment = fragmentRepository.getById(PARENT_3_FRAGMENT_ID)
            def allChildXpaths = parentFragment.childFragments.collect { it.xpath }
            def expectedListEntriesAfterUpdateAsXpaths = keysToXpaths(parentXpath, replacementKeys)
            assert allChildXpaths.size() == replacementKeys.size()
            assert allChildXpaths.containsAll(expectedListEntriesAfterUpdateAsXpaths)
        and: 'all the list elements have the new values'
            assert parentFragment.childFragments.stream().allMatch(childFragment -> childFragment.attributes.contains('new value'))
        and: 'there are no more grandchildren as none of the replacement list entries had a child'
            parentFragment.childFragments.each {assert it.childFragments.size() == 0 }
        where: 'the following replacement lists are applied'
            scenario                                            | originalKeys | replacementKeys
            'one existing entry only'                           | []           | ['NEW']
            'multiple new entries'                              | []           | ['NEW1', 'NEW2']
            'one new entry only (existing entries are deleted)' | ['A', 'B']   | ['NEW1', 'NEW2']
            'one existing on new entry'                         | ['A', 'B']   | ['A', 'NEW']
            'one existing entry only'                           | ['A', 'B']   | ['A']
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Replacing existing list element with attributes and (grand)child.'() {
        given: 'a parent with list elements A and B with attribute and grandchild tagged as "org"'
            def parentXpath = '/parent-3'
            def originalListEntriesAsDataNodes = createChildListAllHavingAttributeValue(parentXpath, 'org', ['A','B'], true)
            objectUnderTest.addListElements(DATASPACE_NAME, ANCHOR_NAME1, parentXpath, originalListEntriesAsDataNodes)
        when: 'A is replaced with an entry with attribute and grandchild tagged tagged as "new" (B is not in replacement list)'
            def replacementListEntriesAsDataNodes = createChildListAllHavingAttributeValue(parentXpath, 'new', ['A'], true)
            objectUnderTest.replaceListContent(DATASPACE_NAME, ANCHOR_NAME1, parentXpath, replacementListEntriesAsDataNodes)
        then: 'The updated fragment has a child-list with ONLY element "A"'
            def parentFragment = fragmentRepository.getById(PARENT_3_FRAGMENT_ID)
            parentFragment.childFragments.size() == 1
            def childListElementA = parentFragment.childFragments[0]
            childListElementA.xpath == "/parent-3/child-list[@key='A']"
        and: 'element "A" has an attribute with the "new" (tag) value'
            childListElementA.attributes == '{"attr1": "new"}'
        and: 'element "A" has a only one (grand)child'
            childListElementA.childFragments.size() == 1
        and: 'the grandchild is the new grandchild (tag)'
            def grandChild = childListElementA.childFragments[0]
            grandChild.xpath == "/parent-3/child-list[@key='A']/new-grand-child"
        and: 'the grandchild has an attribute with the "new" (tag) value'
            grandChild.attributes == '{"attr1": "new"}'
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Replace list element for a parent (parent-1) with existing one (non-list) child'() {
        when: 'a list element is added under the parent'
            def replacementListEntriesAsDataNodes = createChildListAllHavingAttributeValue(XPATH_DATA_NODE_WITH_DESCENDANTS, 'new', ['A','B'], false)
            objectUnderTest.replaceListContent(DATASPACE_NAME, ANCHOR_NAME1, XPATH_DATA_NODE_WITH_DESCENDANTS, replacementListEntriesAsDataNodes)
        then: 'the parent will have 3 children after the replacement'
            def parentFragment = fragmentRepository.getById(ID_DATA_NODE_WITH_DESCENDANTS)
            parentFragment.childFragments.size() == 3
            def xpaths = parentFragment.childFragments.collect {it.xpath}
        and: 'one of the children is the original child fragment'
            xpaths.contains('/parent-1/child-1')
        and: 'it has the two new list elements'
            xpaths.containsAll("/parent-1/child-list[@key='A']", "/parent-1/child-list[@key='B']")
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Replace list content using unknown parent'() {
        given: 'list element as a collection of data nodes'
            def listElementCollection = toDataNodes(['irrelevant'])
        when: 'attempt to replace list elements under unknown parent node'
            objectUnderTest.replaceListContent(DATASPACE_NAME, ANCHOR_NAME3, '/unknown', listElementCollection)
        then: 'a datanode not found exception is thrown'
            thrown(DataNodeNotFoundException)
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Replace list content with empty collection is not supported'() {
        when: 'attempt to replace list elements with empty collection'
            objectUnderTest.replaceListContent(DATASPACE_NAME, ANCHOR_NAME3, '/parent-203', [])
        then: 'a CPS admin exception is thrown'
            def thrown = thrown(CpsAdminException)
            assert thrown.message == 'Invalid list replacement'
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Delete list scenario: #scenario.'() {
        when: 'deleting list is executed for: #scenario.'
            objectUnderTest.deleteListDataNode(DATASPACE_NAME, ANCHOR_NAME3, targetXpaths)
        then: 'only the expected children remain'
            def parentFragment = fragmentRepository.getById(parentFragmentId)
            def remainingChildXpaths = parentFragment.childFragments.collect { it.xpath }
            assert remainingChildXpaths.size() == expectedRemainingChildXpaths.size()
            assert remainingChildXpaths.containsAll(expectedRemainingChildXpaths)
        where: 'following parameters were used'
            scenario                          | targetXpaths                                                 | parentFragmentId                     || expectedRemainingChildXpaths
            'list element with key'           | '/parent-203/child-204[@key="A"]'                            | LIST_DATA_NODE_PARENT203_FRAGMENT_ID || ["/parent-203/child-203", "/parent-203/child-204[@key='B']"]
            'list element with combined keys' | '/parent-202/child-205[@key="A" and @key2="B"]'              | LIST_DATA_NODE_PARENT202_FRAGMENT_ID || ["/parent-202/child-206[@key='A']"]
            'whole list'                      | '/parent-203/child-204'                                      | LIST_DATA_NODE_PARENT203_FRAGMENT_ID || ['/parent-203/child-203']
            'list element under list element' | '/parent-203/child-204[@key="B"]/grand-child-204[@key2="Y"]' | LIST_DATA_NODE_PARENT203_FRAGMENT_ID || ["/parent-203/child-203", "/parent-203/child-204[@key='A']", "/parent-203/child-204[@key='B']"]
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Delete data nodes with "/"-token in list key value: #scenario. (CPS-1409)'() {
        given: 'a data nodes with list-element child with "/" in index value (and grandchild)'
            def grandChild = new DataNodeBuilder().withXpath(deleteTestGrandChildXPath).build()
            def child = new DataNodeBuilder().withXpath(deleteTestChildXpath).withChildDataNodes([grandChild]).build()
            objectUnderTest.addChildDataNode(DATASPACE_NAME, ANCHOR_NAME3, deleteTestParentXPath, child)
        and: 'number of children before delete is stored'
            def numberOfChildrenBeforeDelete = objectUnderTest.getDataNode(DATASPACE_NAME, ANCHOR_NAME3, pathToParentOfDeletedNode, INCLUDE_ALL_DESCENDANTS).childDataNodes.size()
        when: 'target node is deleted'
            objectUnderTest.deleteDataNode(DATASPACE_NAME, ANCHOR_NAME3, deleteTarget)
        then: 'one child has been deleted'
            def numberOfChildrenAfterDelete = objectUnderTest.getDataNode(DATASPACE_NAME, ANCHOR_NAME3, pathToParentOfDeletedNode, INCLUDE_ALL_DESCENDANTS).childDataNodes.size()
            assert numberOfChildrenAfterDelete == numberOfChildrenBeforeDelete - 1
        where:
            scenario                | deleteTarget              | pathToParentOfDeletedNode
            'list element with /'   | deleteTestChildXpath      | deleteTestParentXPath
            'child of list element' | deleteTestGrandChildXPath | deleteTestChildXpath
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
    def 'Delete data node by xpath #scenario.'() {
        given: 'a valid data node'
            def dataNode
        and: 'data nodes are deleted'
            objectUnderTest.deleteDataNode(DATASPACE_NAME, ANCHOR_NAME3, xpathForDeletion)
        when: 'verify data nodes are removed'
            objectUnderTest.getDataNode(DATASPACE_NAME, ANCHOR_NAME3, xpathForDeletion, INCLUDE_ALL_DESCENDANTS)
        then:
            thrown(DataNodeNotFoundException)
        and: 'some related object is not deleted'
            if (xpathSurvivor!=null) {
                dataNode = objectUnderTest.getDataNode(DATASPACE_NAME, ANCHOR_NAME3, xpathSurvivor, INCLUDE_ALL_DESCENDANTS)
                assert dataNode.xpath == xpathSurvivor
            }
        where: 'following parameters were used'
            scenario                               | xpathForDeletion                                  || xpathSurvivor
            'child data node, parent still exists' | '/parent-206/child-206'                           || '/parent-206'
            'list element, sibling still exists'   | '/parent-206/child-206/grand-child-206[@key="A"]' || "/parent-206/child-206/grand-child-206[@key='X']"
            'container node'                       | '/parent-206'                                     || null
            'container list node'                  | '/parent-206[@key="A"]'                           || "/parent-206[@key='B']"
            'root node with xpath /'               | '/'                                               || null
            'root node with xpath passed as blank' | ''                                                || null
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Delete data node error scenario: #scenario.'() {
        when: 'data node is deleted'
            objectUnderTest.deleteDataNode(DATASPACE_NAME, ANCHOR_NAME3, datanodeXpath)
        then: 'a #expectedException is thrown'
            thrown(expectedException)
        where: 'the following parameters were used'
            scenario                                        | datanodeXpath                                    | expectedException
            'valid data node, non existent child node'      | '/parent-203/child-non-existent'                 | DataNodeNotFoundException
            'invalid list element'                          | '/parent-206/child-206/grand-child-206@key="A"]' | PathParsingException
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Delete data node for an anchor.'() {
        given: 'a data-node exists for an anchor'
            assert fragmentsExistInDB(DATASPACE_1001_ID, ANCHOR_3003_ID)
        when: 'data nodes are deleted '
            objectUnderTest.deleteDataNodes(DATASPACE_NAME, ANCHOR_NAME3)
        then: 'all data-nodes are deleted successfully'
            assert !fragmentsExistInDB(DATASPACE_1001_ID, ANCHOR_3003_ID)
    }

    def fragmentsExistInDB(dataSpaceId, anchorId) {
        !fragmentRepository.findRootsByDataspaceAndAnchor(dataSpaceId, anchorId).isEmpty()
    }

    static Collection<DataNode> toDataNodes(xpaths) {
        return xpaths.collect { new DataNodeBuilder().withXpath(it).build() }
    }


    static DataNode buildDataNode(xpath, leaves, childDataNodes) {
        return dataNodeBuilder.withXpath(xpath).withLeaves(leaves).withChildDataNodes(childDataNodes).build()
    }

    static Map<String, Object> getLeavesMap(FragmentEntity fragmentEntity) {
        return jsonObjectMapper.convertJsonString(fragmentEntity.attributes, Map<String, Object>.class)
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
        flatMap.put(dataNodeTree.xpath, dataNodeTree)
        dataNodeTree.childDataNodes
                .forEach(childDataNode -> treeToFlatMapByXpath(flatMap, childDataNode))
        return flatMap
    }

    def keysToXpaths(parent, Collection keys) {
        return keys.collect { "${parent}/child-list[@key='${it}']".toString() }
    }

    def static createDataNodeTree(String... xpaths) {
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


    def createChildListAllHavingAttributeValue(parentXpath, tag, Collection keys, boolean addGrandChild) {
        def listElementAsDataNodes = keysToXpaths(parentXpath, keys).collect {
                new DataNodeBuilder()
                    .withXpath(it)
                    .withLeaves([attr1: tag])
                    .build()
        }
        if (addGrandChild) {
            listElementAsDataNodes.each {it.childDataNodes = [createGrandChild(it.xpath, tag)]}
        }
        return listElementAsDataNodes
    }

    def createGrandChild(parentXPath, tag) {
        new DataNodeBuilder()
            .withXpath("${parentXPath}/${tag}-grand-child")
            .withLeaves([attr1: tag])
            .build()
    }

}
