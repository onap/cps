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

package org.onap.cps.utils.deltareport

import org.onap.cps.api.exceptions.CpsException
import spock.lang.Specification

import java.util.concurrent.CompletionException

class CompletionExceptionConverterSpec extends Specification {

    def 'Handling a completion exception with a runtime exception cause'() {
        given: 'a CompletionException wrapping a RuntimeException'
            def runtimeException = new IllegalStateException('runtime error')
            def completionException = new CompletionException(runtimeException)
        when: 'the completion exception is handled'
            throw CompletionExceptionConverter.convertCompletionException(completionException, 'some message', 'some details')
        then: 'the original RuntimeException is thrown unwrapped'
            def thrownException = thrown(IllegalStateException)
            assert thrownException.message == 'runtime error'
    }

    def 'Handling a completion exception with an unexpected checked exception cause'() {
        given: 'a CompletionException wrapping a checked exception'
            def checkedException = new Exception('checked error')
            def completionException = new CompletionException(checkedException)
        when: 'the completion exception is handled'
            throw CompletionExceptionConverter.convertCompletionException(completionException, 'my message', 'my details')
        then: 'a CpsException is thrown with the provided message and details'
            def thrownException = thrown(CpsException)
            assert thrownException.message == 'my message'
            assert thrownException.details == 'my details'
            assert thrownException.cause == checkedException
    }
}

