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
import org.onap.cps.spi.exceptions.CpsPathException
import org.onap.cps.spi.model.DataNode
import org.onap.cps.spi.model.DataNodeBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import spock.lang.Unroll

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS

class CpsDataPersistenceQueryDataNodeSpec extends CpsPersistenceSpecBase {

    @Autowired
    CpsDataPersistenceService objectUnderTest

    static final Gson GSON = new GsonBuilder().create()

    static final String SET_DATA = '/data/fragment.sql'
    static final String XPATH_DATA_NODE_WITH_DESCENDANTS = '/parent-1'

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

    static def createDataNodeTree(String... xpaths) {
        def dataNodeBuilder = new DataNodeBuilder().withXpath(xpaths[0])
        if (xpaths.length > 1) {
            def xPathsDescendant = Arrays.copyOfRange(xpaths, 1, xpaths.length)
            def childDataNode = createDataNodeTree(xPathsDescendant)
            dataNodeBuilder.withChildDataNodes(ImmutableSet.of(childDataNode))
        }
        dataNodeBuilder.build()
    }

    def static treeToFlatMapByXpath(Map<String, DataNode> flatMap, DataNode dataNodeTree) {
        flatMap.put(dataNodeTree.getXpath(), dataNodeTree)
        dataNodeTree.getChildDataNodes()
                .forEach(childDataNode -> treeToFlatMapByXpath(flatMap, childDataNode))
        return flatMap
    }


    @Unroll
    @Sql([CLEAR_DATA, SET_DATA])
    def 'Cps Path query for single leaf value with type: #type.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodes(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES, cpsPath, includeDescendantsOption)
        then: 'the correct data is returned'
            def leaves = '[common-leaf-name:common-leaf value, common-leaf-name-int:5.0]'
            DataNode dataNode = result.stream().findFirst().get()
            dataNode.getLeaves().toString() == leaves
            dataNode.getChildDataNodes().size() == expectedNumberOfChidlNodes
        where: 'the following data is used'
            type                        | cpsPath                                                          | includeDescendantsOption || expectedNumberOfChidlNodes
            'String and no descendants' | '/parent-200/child-202[@common-leaf-name=\'common-leaf value\']' | OMIT_DESCENDANTS         || 0
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
        then: 'the correct number of data nodes are retrieved'
            result.size() == expectedXPaths.size()
        and: 'xpaths of the retrieved data nodes are as expected'
            for(int i = 0; i<result.size(); i++) {
                result[i].getXpath() == expectedXPaths[i]
            }
        where: 'the following data is used'
            scenario                                  | cpsPath             || expectedXPaths
            'fully unique descendant name'            | '//grand-child-202' || ['/parent-200/child-202/grand-child-202']
            'descendant name match end of other node' | '//child-202'       || ['/parent-200/child-202','/parent-201/child-202']
    }

    @Unroll
    @Sql([CLEAR_DATA, SET_DATA])
    def 'Cps Path query using descendant anywhere ends with yang list containing %scenario '() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodes(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES, cpsPath, OMIT_DESCENDANTS)
        then: 'the correct number of data nodes are retrieved'
            result.size() == expectedXPaths.size()
        and: 'xpaths of the retrieved data nodes are as expected'
            for(int i = 0; i<result.size(); i++) {
                result[i].getXpath() == expectedXPaths[i]
            }
        where: 'the following data is used'
            scenario                       | cpsPath                                                                          || expectedXPaths
            'one attribute'                | '//child-202[@common-leaf-name-int=5]'                                           || ['/parent-200/child-202','/parent-201/child-202']
            'trailing "and" is ignored'    | '//child-202[@common-leaf-name-int=5 and]'                                       || ['/parent-200/child-202','/parent-201/child-202']
            'more than one attribute'      | '//child-202[@common-leaf-name-int=5 and @common-leaf-name="common-leaf value"]' || ['/parent-200/child-202']
            'attributes reversed in order' | '//child-202[@common-leaf-name="common-leaf value" and @common-leaf-name-int=5]' || ['/parent-200/child-202']
    }

    @Unroll
    @Sql([CLEAR_DATA, SET_DATA])
    def 'Cps Path query error scenario using descendant anywhere ends with yang list containing %scenario '() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodes(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES, cpsPath, OMIT_DESCENDANTS)
        then: 'exception is thrown'
            thrown(CpsPathException)
        where: 'the following data is used'
            scenario                                  | cpsPath
            'one of the attributes without value'     | '//child-202[@common-leaf-name-int=5 and @another-attribute"]'
            'more than one attribute separated by or' | '//child-202[@common-leaf-name-int=5 or @common-leaf-name="common-leaf value"]'
    }
}
