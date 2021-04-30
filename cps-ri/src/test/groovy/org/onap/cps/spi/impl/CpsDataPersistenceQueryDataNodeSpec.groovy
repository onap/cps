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

    static final String SET_DATA = '/data/CpsPathQuery.sql'

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Cps Path query for single leaf value with type: #type.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodes(DATASPACE_NAME, ANCHOR_NAME3, cpsPath, includeDescendantsOption)
        then: 'the correct data is returned'
            def leaves = '[id:1.0, volume:6]'
            DataNode dataNode = result.stream().findFirst().get()
            dataNode.getLeaves().toString() == leaves
            dataNode.getChildDataNodes().size() == expectedNumberOfChidlNodes
        where: 'the following data is used'
            type                        | cpsPath                            | includeDescendantsOption || expectedNumberOfChidlNodes
            'String and no descendants' | '/bookstore/bookID[@volume=\'6\']' | OMIT_DESCENDANTS         || 0
            'Integer and descendants'   | '/bookstore/bookID[@id=1]'         | INCLUDE_ALL_DESCENDANTS  || 2
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Query for attribute by cps path with cps paths that return no data because of #scenario.'() {
        when: 'a query is executed to get datanodes for the given cps path'
            def result = objectUnderTest.queryDataNodes(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES, cpsPath, FetchDescendantsOption.OMIT_DESCENDANTS)
        then: 'no data is returned'
            result.isEmpty()
        where: 'following cps queries are performed'
            scenario                         | cpsPath
            'cps path is incomplete'         | '/bookstore[@volume=\'6\']'
            'leaf value does not exist'      | '/bookstore/bookID[@volume=\'does not exist\']'
            'incomplete end of xpath prefix' | '/bookstore/book[@id=1]'
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Cps Path query using descendant anywhere and #type (further) descendants.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def cpsPath = '//bookID'
            def result = objectUnderTest.queryDataNodes(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES, cpsPath, includeDescendantsOption)
        then: 'the data node has the correct number of children'
            DataNode dataNode = result.stream().findFirst().get()
            dataNode.getChildDataNodes().size() == expectedNumberOfChildNodes
        where: 'the following data is used'
            type      | includeDescendantsOption || expectedNumberOfChildNodes
            'omit'    | OMIT_DESCENDANTS         || 0
            'include' | INCLUDE_ALL_DESCENDANTS  || 2
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
            scenario                                  | cpsPath    || expectedXPaths
            'fully unique descendant name'            | '//bookID' || ['/bookstore/bookID']
            'descendant name match end of other node' | '//book'   || ['/bookstore/bookID/categories[@genre="Kids"]/book', '/bookstore/bookID/categories[@genre="SciFi"]/book']
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Cps Path query using descendant anywhere with #scenario condition(s) for a container element.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodes(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES, cpsPath, OMIT_DESCENDANTS)
        then: 'the correct number of data nodes are retrieved'
            result.size() == 1
        and: 'xpaths of the retrieved data nodes are as expected'
            assert result[0].getXpath() == '/bookstore/bookID/categories[@genre="Kids"]/book'
        where: 'the following data is used'
            scenario                    | cpsPath
            'one leaf'                  | '//book[@genre="Kids"]'
            'trailing "and" is ignored' | '//book[@genre="Kids" and]'
            'more than one leaf'        | '//book[@genre="Kids" and @id=1]'
            'leaves reversed in order'  | '//book[@id=1 and @genre="Kids"]'
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Cps Path query using descendant anywhere with #scenario condition(s) for a list element.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodes(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES, cpsPath, OMIT_DESCENDANTS)
        then: 'the correct number of data nodes are retrieved'
            def expectedXpaths = '/bookstore/bookID/categories[@genre="Kids"]/book/info[@title="The Golden Compass" and @price=15]'
            result.size() == 1
        and: 'xpaths of the retrieved data nodes are as expected'
            assert result[0].getXpath() == expectedXpaths
        where: 'the following data is used'
            scenario                              | cpsPath
            'one partial key leaf'                | '//info[@title="The Golden Compass"]'
            'one non key leaf'                    | '//info[@price=15]'
            'mix of partial key and non key leaf' | '//info[@title="The Golden Compass" and @price=15]'
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Cps Path query error scenario using descendant anywhere ends with yang list containing %scenario '() {
        when: 'a query is executed to get a data node by the given cps path'
            objectUnderTest.queryDataNodes(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES, cpsPath, OMIT_DESCENDANTS)
        then: 'exception is thrown'
            thrown(CpsPathException)
        where: 'the following data is used'
            scenario                             | cpsPath
            'one of the leaf without value'      | '//info[@title="The Golden Compass" and @price=]'
            'more than one leaf separated by or' | '//info[@title="The Golden Compass" or @price=15]'
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Query for attribute by cps path of type ancestor with #scenario.'() {
        when: 'the given cps path is parsed'
            def result = objectUnderTest.queryDataNodes(DATASPACE_NAME, ANCHOR_FOR_DATA_NODES_WITH_LEAVES, cpsPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the xpaths of the retrieved data nodes are as expected'
            result.size() == expectedXPaths.size()
            for (int i = 0; i < result.size(); i++) {
                assert result[i].getXpath() == expectedXPaths[i]
            }
        where: 'the following data is used'
            scenario                                    | cpsPath                                                 || expectedXPaths
            'multiple list-ancestors'                   | '//book/ancestor::categories'                           || ['/bookstore/bookID/categories[@genre="Kids"]', '/bookstore/bookID/categories[@genre="SciFi"]']
            'one ancestor value'                        | '//book/ancestor::bookID'                               || ['/bookstore/bookID']
            'top ancestor'                              | '//book/ancestor::bookstore'                            || ['/bookstore']
            'list with index value in the xpath prefix' | '//categories[@genre="SciFi"]/book/ancestor::bookstore' || ['/bookstore']
            'ancestor with parent'                      | '//book/ancestor::bookID/categories'                    || ['/bookstore/bookID/categories[@genre="Kids"]', '/bookstore/bookID/categories[@genre="SciFi"]']
            'ancestor with parent that does not exist'  | '//book/ancestor::/parentDoesNoExist/bookID'            || []
            'ancestor does not exist'                   | '//book/ancestor::ancestorDoesNotExist'                 || []
    }
}
