/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
 *  Modifications Copyright (C) 2023 TechMahindra Ltd.
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

package org.onap.cps.integration.functional

import org.onap.cps.spi.exceptions.DataNodeNotFoundException

import java.time.OffsetDateTime
import org.onap.cps.api.CpsDataService
import org.onap.cps.integration.base.FunctionalSpecBase
import org.onap.cps.spi.FetchDescendantsOption

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS

class CpsDataServiceIntegrationSpec extends FunctionalSpecBase {

    CpsDataService objectUnderTest

    def setup() { objectUnderTest = cpsDataService }

    def 'Read bookstore top-level container(s) using #fetchDescendantsOption.'() {
        when: 'get data nodes for bookstore container'
            def result = objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore', fetchDescendantsOption)
        then: 'the tree consist ouf of #expectNumberOfDataNodes data nodes'
            assert countDataNodesInTree(result) == expectNumberOfDataNodes
        and: 'the top level data node has the expected attribute and value'
            assert result.leaves['bookstore-name'] == ['Easons']
        where: 'the following option is used'
            fetchDescendantsOption                         || expectNumberOfDataNodes
            FetchDescendantsOption.OMIT_DESCENDANTS        || 1
            FetchDescendantsOption.DIRECT_CHILDREN_ONLY    || 6
            FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS || 17
            new FetchDescendantsOption(2)                  || 17
    }

    def 'Read bookstore top-level container(s) has correct dataspace and anchor.'() {
        when: 'get data nodes for bookstore container'
            def result = objectUnderTest.getDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, '/bookstore', FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
        then: 'the correct dataspace was queried'
            assert result.dataspace.toSet() == [FUNCTIONAL_TEST_DATASPACE_1].toSet()
        and: 'the correct anchor was queried'
            assert result.anchorName.toSet() == [BOOKSTORE_ANCHOR_1].toSet()
    }

    def 'Multiple get limit exceeded: 32,764 (~ 2^15) xpaths.'() {
        given: 'more than 32,764 xpaths'
            def xpaths = (0..40_000).collect { "/size/of/this/path/does/not/matter/for/limit[@id='" + it + "']" }
        when: 'single operation is executed to get all datanodes with given xpaths'
            objectUnderTest.getDataNodesForMultipleXpaths(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, xpaths, INCLUDE_ALL_DESCENDANTS)
        then: 'a database exception is not thrown'
            noExceptionThrown()
    }

    def 'Delete multiple datanodes limit exceeded: 32,767 (~ 2^15) xpaths.'() {
        given: 'more than 32,767 xpaths'
            def xpaths = (0..40_000).collect { "/size/of/this/path/does/not/matter/for/limit[@id='" + it + "']" }
        when: 'single operation is executed to delete all datanodes with given xpaths'
            objectUnderTest.deleteDataNodes(FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, xpaths, OffsetDateTime.now())
        then: 'a database exception is not thrown (but a CPS DataNodeNotFoundException is thrown)'
            thrown(DataNodeNotFoundException.class)
    }

    def 'Delete datanodes from multiple anchors limit exceeded: 32,766 (~ 2^15) anchors.'() {
        given: 'more than 32,766 anchor names'
            def anchorNames = (0..40_000).collect { "size-of-this-name-does-not-matter-for-limit-" + it }
        when: 'single operation is executed to delete all datanodes in given anchors'
            objectUnderTest.deleteDataNodes(FUNCTIONAL_TEST_DATASPACE_1, anchorNames, OffsetDateTime.now())
        then: 'a database exception is not thrown'
            noExceptionThrown()
    }

}
