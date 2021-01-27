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

class CpsDataPersistenceServiceSpec extends CpsPersistenceSpecBase {

    @Autowired
    CpsDataPersistenceService objectUnderTest

    static final String SET_DATA = '/data/fragment.sql'
    static final long PARENT_ID_4001 = 4001;
    static final String PARENT_XPATH1 = '/parent-1'

    static final DataNode newDataNode = new DataNodeBuilder().build()
    static DataNode existingDataNode
    static DataNode existingChildDataNode

    static {
        existingDataNode = createDataNodeTree(PARENT_XPATH1)
        existingChildDataNode = createDataNodeTree('/parent-1/child-1')
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Get fragment with descendants.'() {
        /*
        TODO: This test is not really testing the object under test! Needs to be updated as part of CPS-71
        Actually I think this test will become redundant once th store data node tests is asserted using
        a new getByXpath() method in the service (object under test)
        A lot of preloaded dat will become redundant then too
         */
        //
        when: 'a fragment is retrieved from the repository'
            def fragment = fragmentRepository.findById(PARENT_ID_4001).orElseThrow();
        then: 'it has the correct xpath'
            fragment.xpath == '/parent-1'
        and: 'it contains the children'
            fragment.childFragments.size() == 1
            def childFragment = fragment.childFragments[0]
            childFragment.xpath == '/parent-1/child-1'
        and: "and its children's children"
            childFragment.childFragments.size() == 1
            def grandchildFragment = childFragment.childFragments[0]
            grandchildFragment.xpath == '/parent-1/child-1/grandchild-1'
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'StoreDataNode with descendants.'() {
        when: 'a fragment with descendants is stored'
            def parentXpath = "/parent-new";
            def childXpath = "/parent-new/child-new";
            def grandChildXpath = "/parent-new/child-new/grandchild-new";
            objectUnderTest.storeDataNode(DATASPACE_NAME, ANCHOR_NAME1,
                createDataNodeTree(parentXpath, childXpath, grandChildXpath));
        then: 'it can be retrieved by its xpath'
            def parentFragment = getFragmentByXpath(parentXpath);
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
    def  'Store datanode error scenario: #scenario.'() {
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
            def newChild = createDataNodeTree('xpath for new child');
        when: 'the child is added to an existing parent with 1 child'
            objectUnderTest.addChildDataNode(DATASPACE_NAME, ANCHOR_NAME1, PARENT_XPATH1, newChild);
        then: 'the parent is now has to 2 children'
            def expectedExistingChildPath = '/parent-1/child-1'
            def parentFragment = fragmentRepository.findById(PARENT_ID_4001).orElseThrow()
            parentFragment.getChildFragments().size() == 2
        and : 'it still has the old child'
            parentFragment.getChildFragments().find( {it.xpath == expectedExistingChildPath})
        and : 'it has the new child'
            parentFragment.getChildFragments().find( {it.xpath == newChild.xpath})
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def  'Add child error scenario: #scenario.'() {
        when: 'attempt to add a child data node with #scenario'
            objectUnderTest.addChildDataNode(DATASPACE_NAME, ANCHOR_NAME1, parentXpath, dataNode)
        then: 'a #expectedException is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                 | parentXpath   | dataNode              || expectedException
            'parent does not exist'  | 'unknown'     | newDataNode           || DataNodeNotFoundException
            'already existing child' | PARENT_XPATH1 | existingChildDataNode || DataIntegrityViolationException
    }

    static def createDataNodeTree(String... xpaths) {
        def dataNodeBuilder = new DataNodeBuilder().withXpath(xpaths[0])
        if (xpaths.length > 1) {
            def xPathsDescendant = Arrays.copyOfRange(xpaths, 1, xpaths.length)
            def childDataNode = createDataNodeTree(xPathsDescendant)
            dataNodeBuilder.withChildDataNodes(ImmutableSet.of(childDataNode))
        }
        dataNodeBuilder.build();
    }

    def getFragmentByXpath = xpath -> {
        //TODO: Remove this method when CPS-71 gets implemented
        fragmentRepository.findAll().stream()
          .filter(fragment -> fragment.getXpath().contains(xpath)).findAny().orElseThrow();
    }

}
