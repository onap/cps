/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021 Bell Canada.
 *  Modifications Copyright (C) 2023 TechMahindra Ltd.
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
    def 'Cps Path query across anchors for leaf value(s) with : #scenario.'() {
        when: 'a query is executed to get a data node by the given cps path'
            def result = objectUnderTest.queryDataNodesAcrossAnchors(DATASPACE_NAME, cpsPath, includeDescendantsOption)
        then: 'the correct number of queried nodes are returned'
            assert result.size() == expectedNumberOfQueriedNodes
        and : 'correct anchors are queried'
            assert result.anchorName.containsAll(expectedAnchors)
        where: 'the following data is used'
            scenario                                    | cpsPath                                                      | includeDescendantsOption || expectedNumberOfQueriedNodes || expectedAnchors
            'String and no descendants'                 | '/shops/shop[@id=1]/categories[@code=1]/book[@title="Dune"]' | OMIT_DESCENDANTS         || 2                            || ['ANCHOR-004', 'ANCHOR-005']
            'Integer and descendants'                   | '/shops/shop[@id=1]/categories[@code=1]/book[@price=5]'      | INCLUDE_ALL_DESCENDANTS  || 3                            || ['ANCHOR-004', 'ANCHOR-005']
            'No condition no descendants'               | '/shops/shop[@id=1]/categories'                              | OMIT_DESCENDANTS         || 6                            || ['ANCHOR-004', 'ANCHOR-005']
            'multiple list-ancestors'                   | '//book/ancestor::categories'                                | INCLUDE_ALL_DESCENDANTS  || 4                            || ['ANCHOR-004', 'ANCHOR-005']
            'one ancestor with list value'              | '//book/ancestor::categories[@code=1]'                       | INCLUDE_ALL_DESCENDANTS  || 2                            || ['ANCHOR-004', 'ANCHOR-005']
            'list with index value in the xpath prefix' | '//categories[@code=1]/book/ancestor::shop[@id=1]'           | INCLUDE_ALL_DESCENDANTS  || 2                            || ['ANCHOR-004', 'ANCHOR-005']
            'ancestor with parent list'                 | '//book/ancestor::shop[@id=1]/categories[@code=2]'           | INCLUDE_ALL_DESCENDANTS  || 2                            || ['ANCHOR-004', 'ANCHOR-005']
            'ancestor with parent'                      | '//phonenumbers[@type="mob"]/ancestor::info/contact'         | INCLUDE_ALL_DESCENDANTS  || 5                            || ['ANCHOR-004', 'ANCHOR-005']
            'ancestor combined with text condition'     | '//book/title[text()="Dune"]/ancestor::shop'                 | INCLUDE_ALL_DESCENDANTS  || 10                           || ['ANCHOR-004', 'ANCHOR-005']
            'ancestor with parent that does not exist'  | '//book/ancestor::parentDoesNoExist/categories'              | INCLUDE_ALL_DESCENDANTS  || 0                            || []
            'ancestor does not exist'                   | '//book/ancestor::ancestorDoesNotExist'                      | INCLUDE_ALL_DESCENDANTS  || 0                            || []
    }

    def 'Cps Path query across anchors with syntax error throws a CPS Path Exception.'() {
        when: 'trying to execute a query with a syntax (parsing) error'
            objectUnderTest.queryDataNodesAcrossAnchors(DATASPACE_NAME, 'cpsPath that cannot be parsed' , OMIT_DESCENDANTS)
        then: 'a cps path exception is thrown'
            thrown(CpsPathException)
    }
}
