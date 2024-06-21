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

import org.onap.cps.api.CpsAnchorService
import org.onap.cps.integration.performance.base.CpsPerfTestBase

class CpsDataspaceServiceLimitsPerfTest extends CpsPerfTestBase {

    CpsAnchorService objectUnderTest

    def setup() { objectUnderTest = cpsAnchorService }

    def 'Get anchors from multiple schema set names limit exceeded: 32,766 (~ 2^15) schema set names.'() {
        given: 'more than 32,766 schema set names'
            def schemaSetNames = (0..40_000).collect { "size-of-this-name-does-not-matter-for-limit-" + it }
        when: 'single get is executed to get all the anchors'
            objectUnderTest.getAnchorsBySchemaSetNames(CPS_PERFORMANCE_TEST_DATASPACE, schemaSetNames)
        then: 'a database exception is not thrown'
            noExceptionThrown()
    }

    def 'Querying anchor names limit exceeded: 32,766 (~ 2^15) modules.'() {
        given: 'more than 32,766 module names'
            def moduleNames = (0..40_000).collect { "size-of-this-name-does-not-matter-for-limit-" + it }
        when: 'single query is executed to get all the anchors'
            objectUnderTest.queryAnchorNames(CPS_PERFORMANCE_TEST_DATASPACE, moduleNames)
        then: 'a database exception is not thrown'
            noExceptionThrown()
    }

}
