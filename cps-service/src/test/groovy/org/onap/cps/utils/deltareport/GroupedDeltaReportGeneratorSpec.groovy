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
import org.onap.cps.cpspath.parser.CpsPathUtil
import spock.lang.Specification

import java.util.concurrent.CompletionException

class GroupedDeltaReportGeneratorSpec extends Specification {

    def mockDeltaReportHelper = Mock(DeltaReportHelper)
    def objectUnderTest = new GroupedDeltaReportGenerator(mockDeltaReportHelper)

    def 'CompletionException with RuntimeException cause is unwrapped'() {
        given: 'source data nodes with an invalid xpath that causes a failure in the async removed report task'
            def source = [new DataNode(xpath: 'invalid[', leaves: ['key': 'value1'])]
            def target = []
        when: 'condensed delta reports are created'
            objectUnderTest.createCondensedDeltaReports(source, target)
        then: 'the RuntimeException is unwrapped from CompletionException and thrown directly'
            thrown(RuntimeException)
    }

    def 'CompletionException with non-RuntimeException cause is rethrown as CpsException'() {
        given: 'source data nodes that will be reported as removed'
            def source = [new DataNode(xpath: '/node', leaves: ['key': 'value1'])]
            def target = []
        and: 'CpsPathUtil is mocked globally to throw a CompletionException wrapping a checked exception'
            GroovyMock(CpsPathUtil, global: true)
            CpsPathUtil.getNormalizedParentXpath(_) >> { throw new CompletionException(new Exception('checked error')) }
        when: 'condensed delta reports are created'
            objectUnderTest.createCondensedDeltaReports(source, target)
        then: 'a CpsException is thrown with details about the operation'
            def thrownException = thrown(CpsException)
            thrownException.message == 'Failed to generate grouped delta report'
            thrownException.cause.message == 'checked error'
    }
}
