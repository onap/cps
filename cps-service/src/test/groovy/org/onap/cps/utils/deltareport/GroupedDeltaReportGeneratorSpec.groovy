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

import java.util.concurrent.CompletableFuture

class GroupedDeltaReportGeneratorSpec extends Specification {

    def mockDeltaReportHelper = Mock(DeltaReportHelper)
    def objectUnderTest = new GroupedDeltaReportGenerator(mockDeltaReportHelper)

    def 'CompletionException with RuntimeException cause is unwrapped'() {
        given: 'source data nodes with an invalid xpath that causes a failure in the async removed report task'
            def source = [new DataNode(xpath: 'invalid[', leaves: ['key': 'value1'])]
            def target = []
        when: 'condensed delta reports are created'
            objectUnderTest.createCondensedDeltaReports(source, target)
        then: 'expected exception is thrown'
            thrown(RuntimeException)
    }

    def 'CompletionException with non-RuntimeException cause is rethrown as CpsException'() {
        given: 'a future that fails with a checked exception wrapped in CompletionException'
            def failedFuture = new CompletableFuture()
            failedFuture.completeExceptionally(new Exception('checked error'))
            def successFuture = CompletableFuture.completedFuture([])
        when: 'collectDeltaReports is called with the failed future'
            objectUnderTest.collectDeltaReports(failedFuture, [], successFuture)
        then: 'expected exception is thrown'
            def thrownException = thrown(CpsException)
            thrownException.message == 'Failed to generate grouped delta report'
            thrownException.cause.message == 'checked error'
    }
}
