/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 Deutsche Telekom AG
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

package org.onap.cps.utils.deltareport

import org.onap.cps.api.exceptions.CpsException
import org.onap.cps.api.model.DataNode
import spock.lang.Specification

import java.util.concurrent.CompletionException

class DeltaReportGeneratorSpec extends Specification {

    def mockDeltaReportHelper = Mock(DeltaReportHelper)
    def objectUnderTest = new DeltaReportGenerator(mockDeltaReportHelper)

    def 'Delta report generation with a failing async task'() {
        given: 'a mocked helper that throws a RuntimeException'
            mockDeltaReportHelper.createDeltaReportsForUpdates(_, _, _) >> { throw new IllegalStateException('test error') }
        and: 'source and target data nodes with the same xpath'
            def source = [new DataNode(xpath: '/node', leaves: ['key': 'value1'])]
            def target = [new DataNode(xpath: '/node', leaves: ['key': 'value2'])]
        when: 'delta reports are created'
            objectUnderTest.createDeltaReports(source, target)
        then: 'the original RuntimeException is thrown unwrapped from CompletionException'
            def thrownException = thrown(IllegalStateException)
            assert thrownException.message == 'test error'
    }
}
