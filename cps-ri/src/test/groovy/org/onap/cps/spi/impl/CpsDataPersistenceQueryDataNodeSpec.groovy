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

import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.exceptions.CpsPathException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS

class CpsDataPersistenceQueryDataNodeSpec extends CpsPersistenceSpecBase {

    @Autowired
    CpsDataPersistenceService objectUnderTest

    static final String SET_DATA = '/data/cps-path-query.sql'

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Cps Path query for leaf value(s) with : #scenario.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodes(DATASPACE_NAME, ANCHOR_FOR_SHOP_EXAMPLE, cpsPath, includeDescendantsOption)
        then: 'the correct number of parent nodes are returned'
            result.size() == expectedNumberOfParentNodes
        then: 'the correct data is returned'
            result.each {
                assert it.getChildDataNodes().size() == expectedNumberOfChildNodes
            }
        where: 'the following data is used'
            scenario                      | cpsPath                                                      | includeDescendantsOption || expectedNumberOfParentNodes | expectedNumberOfChildNodes
            'String and no descendants'   | '/shops/shop[@id=1]/categories[@code=1]/book[@title="Dune"]' | OMIT_DESCENDANTS         || 1                           | 0
            'Integer and descendants'     | '/shops/shop[@id=1]/categories[@code=1]/book[@price=5]'      | INCLUDE_ALL_DESCENDANTS  || 1                           | 1
            'No condition no descendants' | '/shops/shop[@id=1]/categories'                              | OMIT_DESCENDANTS         || 2                           | 0
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Query for attribute by cps path with cps paths that return no data because of #scenario.'() {
        when: 'a query is executed to get data nodes for the given cps path'
            def result = objectUnderTest.queryDataNodes(DATASPACE_NAME, ANCHOR_FOR_SHOP_EXAMPLE, cpsPath, OMIT_DESCENDANTS)
        then: 'no data is returned'
            result.isEmpty()
        where: 'following cps queries are performed'
            scenario                         | cpsPath
            'cps path is incomplete'         | '/shops[@title="Dune"]'
            'leaf value does not exist'      | '/shops/shop[@id=1]/categories[@code=1]/book[@title=\'does not exist\']'
            'incomplete end of xpath prefix' | '/shops/shop[@id=1]/categories/book[@price=15]'
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Cps Path query using descendant anywhere and #type (further) descendants.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def cpsPath = '//categories[@code=1]'
            def result = objectUnderTest.queryDataNodes(DATASPACE_NAME, ANCHOR_FOR_SHOP_EXAMPLE, cpsPath, includeDescendantsOption)
        then: 'the data node has the correct number of children'
            def dataNode = result.stream().findFirst().get()
            dataNode.getChildDataNodes().size() == expectedNumberOfChildNodes
        where: 'the following data is used'
            type      | includeDescendantsOption || expectedNumberOfChildNodes
            'omit'    | OMIT_DESCENDANTS         || 0
            'include' | INCLUDE_ALL_DESCENDANTS  || 1
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Cps Path query using descendant anywhere with #scenario '() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodes(DATASPACE_NAME, ANCHOR_FOR_SHOP_EXAMPLE, cpsPath, OMIT_DESCENDANTS)
        then: 'the correct number of data nodes are retrieved'
            result.size() == expectedXPaths.size()
        and: 'xpaths of the retrieved data nodes are as expected'
            for (int i = 0; i < result.size(); i++) {
                assert result[i].getXpath() == expectedXPaths[i]
            }
        where: 'the following data is used'
            scenario                                                 | cpsPath                                 || expectedXPaths
            'fully unique descendant name'                           | '//categories[@code=2]'                 || ["/shops/shop[@id='1']/categories[@code='2']", "/shops/shop[@id='2']/categories[@code='1']", "/shops/shop[@id='2']/categories[@code='2']"]
            'descendant name match end of other node'                | '//book'                                || ["/shops/shop[@id='1']/categories[@code='1']/book", "/shops/shop[@id='1']/categories[@code='2']/book"]
            'descendant with text condition on leaf'                 | '//book/title[text()="Chapters"]'       || ["/shops/shop[@id='1']/categories[@code='2']/book"]
            'descendant with text condition case mismatch'           | '//book/title[text()="chapters"]'       || []
            'descendant with text condition on int leaf'             | '//book/price[text()="5"]'              || ["/shops/shop[@id='1']/categories[@code='1']/book"]
            'descendant with text condition on leaf-list'            | '//book/labels[text()="special offer"]' || ["/shops/shop[@id='1']/categories[@code='1']/book"]
            'descendant with text condition partial match'           | '//book/labels[text()="special"]'       || []
            'descendant with text condition (existing) empty string' | '//book/labels[text()=""]'              || ["/shops/shop[@id='1']/categories[@code='1']/book"]
            'descendant with text condition on int leaf-list'        | '//book/editions[text()="2000"]'        || ["/shops/shop[@id='1']/categories[@code='2']/book"]
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Cps Path query using descendant anywhere with #scenario condition(s) for a container element.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodes(DATASPACE_NAME, ANCHOR_FOR_SHOP_EXAMPLE, cpsPath, OMIT_DESCENDANTS)
        then: 'the correct number of data nodes are retrieved'
            result.size() == expectedXPaths.size()
        and: 'xpaths of the retrieved data nodes are as expected'
            for (int i = 0; i < result.size(); i++) {
                assert result[i].getXpath() == expectedXPaths[i]
            }
        where: 'the following data is used'
            scenario                   | cpsPath                                               || expectedXPaths
            'one leaf'                 | '//author[@FirstName="Joe"]'                          || ["/shops/shop[@id='1']/categories[@code='1']/book/author[@FirstName='Joe' and @Surname='Bloggs']", "/shops/shop[@id='1']/categories[@code='2']/book/author[@FirstName='Joe' and @Surname='Smith']"]
            'more than one leaf'       | '//author[@FirstName="Joe" and @Surname="Bloggs"]'    || ["/shops/shop[@id='1']/categories[@code='1']/book/author[@FirstName='Joe' and @Surname='Bloggs']"]
            'leaves reversed in order' | '//author[@Surname="Bloggs" and @FirstName="Joe"]'    || ["/shops/shop[@id='1']/categories[@code='1']/book/author[@FirstName='Joe' and @Surname='Bloggs']"]
            'leaf and text condition'  | '//author[@FirstName="Joe"]/Surname[text()="Bloggs"]' || ["/shops/shop[@id='1']/categories[@code='1']/book/author[@FirstName='Joe' and @Surname='Bloggs']"]
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Cps Path query using descendant anywhere with #scenario condition(s) for a list element.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodes(DATASPACE_NAME, ANCHOR_FOR_SHOP_EXAMPLE, cpsPath, OMIT_DESCENDANTS)
        then: 'the correct number of data nodes are retrieved'
            result.size() == expectedXPaths.size()
        and: 'xpaths of the retrieved data nodes are as expected'
            for (int i = 0; i < result.size(); i++) {
                assert result[i].getXpath() == expectedXPaths[i]
            }
        where: 'the following data is used'
            scenario                              | cpsPath                                        || expectedXPaths
            'one partial key leaf'                | '//author[@FirstName="Joe"]'                   || ["/shops/shop[@id='1']/categories[@code='1']/book/author[@FirstName='Joe' and @Surname='Bloggs']", "/shops/shop[@id='1']/categories[@code='2']/book/author[@FirstName='Joe' and @Surname='Smith']"]
            'one non key leaf'                    | '//author[@title="Dune"]'                      || ["/shops/shop[@id='1']/categories[@code='1']/book/author[@FirstName='Joe' and @Surname='Bloggs']"]
            'mix of partial key and non key leaf' | '//author[@FirstName="Joe" and @title="Dune"]' || ["/shops/shop[@id='1']/categories[@code='1']/book/author[@FirstName='Joe' and @Surname='Bloggs']"]
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Query for attribute by cps path of type ancestor with #scenario.'() {
        when: 'the given cps path is parsed'
            def result = objectUnderTest.queryDataNodes(DATASPACE_NAME, ANCHOR_FOR_SHOP_EXAMPLE, cpsPath, INCLUDE_ALL_DESCENDANTS)
        then: 'the xpaths of the retrieved data nodes are as expected'
            result.size() == expectedXPaths.size()
            for (int i = 0; i < result.size(); i++) {
                assert result[i].getXpath() == expectedXPaths[i]
            }
        where: 'the following data is used'
            scenario                                    | cpsPath                                              || expectedXPaths
            'multiple list-ancestors'                   | '//book/ancestor::categories'                        || ["/shops/shop[@id='1']/categories[@code='1']", "/shops/shop[@id='1']/categories[@code='2']"]
            'one ancestor with list value'              | '//book/ancestor::categories[@code=1]'               || ["/shops/shop[@id='1']/categories[@code='1']"]
            'top ancestor'                              | '//shop[@id=1]/ancestor::shops'                      || ['/shops']
            'list with index value in the xpath prefix' | '//categories[@code=1]/book/ancestor::shop[@id=1]'   || ["/shops/shop[@id='1']"]
            'ancestor with parent list'                 | '//book/ancestor::shop[@id=1]/categories[@code=2]'   || ["/shops/shop[@id='1']/categories[@code='2']"]
            'ancestor with parent'                      | '//phonenumbers[@type="mob"]/ancestor::info/contact' || ["/shops/shop[@id='3']/info/contact"]
            'ancestor combined with text condition'     | '//book/title[text()="Dune"]/ancestor::shop'         || ["/shops/shop[@id='1']"]
            'ancestor with parent that does not exist'  | '//book/ancestor::parentDoesNoExist/categories'      || []
            'ancestor does not exist'                   | '//book/ancestor::ancestorDoesNotExist'              || []
    }

    def 'Cps Path query with syntax error throws a CPS Path Exception.'() {
        when: 'trying to execute a query with a syntax (parsing) error'
            objectUnderTest.queryDataNodes(DATASPACE_NAME, ANCHOR_FOR_SHOP_EXAMPLE, 'cpsPath that cannot be parsed' , OMIT_DESCENDANTS)
        then: 'exception is thrown'
            thrown(CpsPathException)
    }

}
