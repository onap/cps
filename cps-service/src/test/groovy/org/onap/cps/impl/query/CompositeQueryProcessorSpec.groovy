/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 Deutsche Telekom AG
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

package org.onap.cps.impl.query

import org.onap.cps.api.model.CompositeQuery
import org.onap.cps.api.parameters.FetchDescendantsOption
import spock.lang.Specification

class CompositeQueryProcessorSpec extends Specification {

    def objectUnderTest = new CompositeQueryProcessor()

    def 'Process composite query.'() {
        given: 'a dataspace name, anchor name, composite query and fetch descendants option'
            def dataspaceName = 'some-dataspace'
            def anchorName = 'some-anchor'
            def compositeQuery = new CompositeQuery(cpsPath: '/some-path', operator: 'and', conditions: [])
            def fetchDescendantsOption = FetchDescendantsOption.OMIT_DESCENDANTS
        when: 'processCompositeQuery is invoked'
            objectUnderTest.processCompositeQuery(dataspaceName, anchorName, compositeQuery, fetchDescendantsOption)
        then: 'an UnsupportedOperationException is thrown'
            thrown(UnsupportedOperationException)
    }
}

