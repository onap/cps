/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the 'License');
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.integration.performance.cps

import java.time.OffsetDateTime
import org.onap.cps.api.CpsDataService
import org.onap.cps.integration.performance.base.CpsPerfTestBase
import org.onap.cps.spi.exceptions.DataNodeNotFoundException

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS

class CpsDataServiceLimitsPerfTest extends CpsPerfTestBase {

    CpsDataService objectUnderTest

    def setup() { objectUnderTest = cpsDataService }

    def 'Multiple get limit exceeded: 32,764 (~ 2^15) xpaths.'() {
        given: 'more than 32,764 xpaths'
            def xpaths = (0..40_000).collect { "/size/of/this/path/does/not/matter/for/limit[@id='" + it + "']" }
        when: 'single operation is executed to get all datanodes with given xpaths'
            objectUnderTest.getDataNodesForMultipleXpaths(CPS_PERFORMANCE_TEST_DATASPACE, 'bookstore1', xpaths, INCLUDE_ALL_DESCENDANTS)
        then: 'a database exception is not thrown'
            noExceptionThrown()
    }

    def 'Delete multiple datanodes limit exceeded: 32,767 (~ 2^15) xpaths.'() {
        given: 'more than 32,767 xpaths'
            def xpaths = (0..40_000).collect { "/size/of/this/path/does/not/matter/for/limit[@id='" + it + "']" }
        when: 'single operation is executed to delete all datanodes with given xpaths'
            objectUnderTest.deleteDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, 'bookstore1', xpaths, OffsetDateTime.now())
        then: 'a database exception is not thrown (but a CPS DataNodeNotFoundException is thrown)'
            thrown(DataNodeNotFoundException.class)
    }

    def 'Delete datanodes from multiple anchors limit exceeded: 32,766 (~ 2^15) anchors.'() {
        given: 'more than 32,766 anchor names'
            def anchorNames = (0..40_000).collect { "size-of-this-name-does-not-matter-for-limit-" + it }
        when: 'single operation is executed to delete all datanodes in given anchors'
            objectUnderTest.deleteDataNodes(CPS_PERFORMANCE_TEST_DATASPACE, anchorNames, OffsetDateTime.now())
        then: 'a database exception is not thrown'
            noExceptionThrown()
    }

}
