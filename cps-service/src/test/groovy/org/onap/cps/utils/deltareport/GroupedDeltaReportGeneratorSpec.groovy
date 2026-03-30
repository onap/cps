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

    def 'Condensed delta report generation with an unexpected checked exception in an async task'() {
        given: 'a future pre-failed with a checked exception (not a RuntimeException)'
            def failedFuture = new CompletableFuture<List>()
            failedFuture.completeExceptionally(new Exception('my checked error'))
        and: 'a future that completes successfully with an empty list'
            def successFuture = CompletableFuture.completedFuture([])
        when: 'condensed delta report collection is triggered with the pre-failed future'
            objectUnderTest.invokeMethod('collectDeltaReports', [failedFuture, [], successFuture] as Object[])
        then: 'a CpsException is thrown with the expected message'
            def thrownException = thrown(CpsException)
            assert thrownException.message == 'Failed to generate grouped delta report'
        and: 'the details describe the failure'
            assert thrownException.details == 'Unexpected error during condensed delta report generation'
        and: 'the original checked exception is preserved as the cause'
            assert thrownException.cause.message == 'my checked error'
    }
}
