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

import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.exceptions.CpsPathException
import org.onap.cps.spi.model.DataNode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS

class CpsDataPersistenceQueryDataNodeSpec extends CpsPersistenceSpecBase {

    @Autowired
    CpsDataPersistenceService objectUnderTest

    static final String SET_DATA = '/data/fragment.sql'

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

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Query for attribute by cps path with cps paths that return no data because of #scenario.'() {
        when: 'a query is executed to get datanodes for the given cps path'
            def result = objectUnderTest.queryDataNodes(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES, cpsPath, FetchDescendantsOption.OMIT_DESCENDANTS)
        then: 'no data is returned'
            result.isEmpty()
        where: 'following cps queries are performed'
            scenario                         | cpsPath
            'cps path is incomplete'         | '/parent-200[@common-leaf-name-int=5]'
            'leaf value does not exist'      | '/parent-200/child-202[@common-leaf-name=\'does not exist\']'
            'incomplete end of xpath prefix' | '/parent-200/child-20[@common-leaf-name-int=5]'
    }

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

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Cps Path query using descendant anywhere with #scenario '() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodes(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES, cpsPath, OMIT_DESCENDANTS)
        then: 'the correct number of data nodes are retrieved'
            result.size() == expectedXPaths.size()
        and: 'xpaths of the retrieved data nodes are as expected'
            for (int i = 0; i < result.size(); i++) {
                assert result[i].getXpath() == expectedXPaths[i]
            }
        where: 'the following data is used'
            scenario                                  | cpsPath             || expectedXPaths
            'fully unique descendant name'            | '//grand-child-202' || ['/parent-200/child-202/grand-child-202']
            'descendant name match end of other node' | '//child-202'       || ['/parent-200/child-202', '/parent-201/child-202']
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Cps Path query using descendant anywhere with #scenario condition(s) for a container element.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodes(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES, cpsPath, OMIT_DESCENDANTS)
        then: 'the correct number of data nodes are retrieved'
            result.size() == expectedXPaths.size()
        and: 'xpaths of the retrieved data nodes are as expected'
            for (int i = 0; i < result.size(); i++) {
                assert result[i].getXpath() == expectedXPaths[i]
            }
        where: 'the following data is used'
            scenario                    | cpsPath                                                                          || expectedXPaths
            'one leaf'                  | '//child-202[@common-leaf-name-int=5]'                                           || ['/parent-200/child-202','/parent-201/child-202']
            'trailing "and" is ignored' | '//child-202[@common-leaf-name-int=5 and]'                                       || ['/parent-200/child-202','/parent-201/child-202']
            'more than one leaf'        | '//child-202[@common-leaf-name-int=5 and @common-leaf-name="common-leaf value"]' || ['/parent-200/child-202']
            'leaves reversed in order'  | '//child-202[@common-leaf-name="common-leaf value" and @common-leaf-name-int=5]' || ['/parent-200/child-202']
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Cps Path query using descendant anywhere with #scenario condition(s) for a list element.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodes(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES, cpsPath, OMIT_DESCENDANTS)
        then: 'the correct number of data nodes are retrieved'
            result.size() == expectedXPaths.size()
        and: 'xpaths of the retrieved data nodes are as expected'
            for(int i = 0; i<result.size(); i++) {
                assert result[i].getXpath() == expectedXPaths[i]
            }
        where: 'the following data is used'
            scenario                               | cpsPath                                                || expectedXPaths
            'one partial key leaf'                 | '//child-203[@key1="A"]'                               || ['/parent-201/child-203[@key1="A" and @key2=1]','/parent-201/child-203[@key1="A" and @key2=2]']
            'one non key leaf'                     | '//child-203[@other-leaf="other value"]'               || ['/parent-201/child-203[@key1="A" and @key2=2]']
            'mix of partial key and non key leaf'  | '//child-203[@key1="A" and @other-leaf="leaf value"]'  || ['/parent-201/child-203[@key1="A" and @key2=1]']
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Cps Path query error scenario using descendant anywhere ends with yang list containing %scenario '() {
        when: 'a query is executed to get a data node by the given cps path'
            objectUnderTest.queryDataNodes(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES, cpsPath, OMIT_DESCENDANTS)
        then: 'exception is thrown'
            thrown(CpsPathException)
        where: 'the following data is used'
            scenario                             | cpsPath
            'one of the leaf without value'      | '//child-202[@common-leaf-name-int=5 and @another-attribute"]'
            'more than one leaf separated by or' | '//child-202[@common-leaf-name-int=5 or @common-leaf-name="common-leaf value"]'
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Query for attribute by cps path of type ancestor with #scenario.'() {
        when: 'the given cps path is parsed'
            def result = objectUnderTest.queryDataNodes(DATASPACE_NAME, ANCHOR_NAME1, cpsPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the xpaths of the retrieved data nodes are as expected'
            result.size() == expectedXPaths.size()
            for (int i = 0; i < result.size(); i++) {
                assert result[i].getXpath() == expectedXPaths[i]
            }
        where: 'the following data is used'
            scenario                                  | cpsPath                                                || expectedXPaths
            'multiple list-ancestors'                   | '//books/ancestor::categories'                         || ['/bookstore/books/categories[@name="SciFi"]', '/bookstore/magazines/categories[@name="kids"]']
            'one ancestor value'                        | '//books/ancestor::books'                              || ['/bookstore/books']
            'top ancestor'                              | '//books/ancestor::bookstore'                          || ['/bookstore']
            'list with index value in the xpath prefix' | '//categories[@name="kids"]/books/ancestor::bookstore' || ['/bookstore']
            'ancestor with parent'                      | '//books/ancestor::/bookstore/magazines'               || ['/bookstore/magazines']
            'ancestor with parent that does not exist'  | '//books/ancestor::/parentDoesNoExist/magazines'       || []
            'ancestor does not exist'                   | '//books/ancestor::ancestorDoesNotExist'               || []
    }
}
