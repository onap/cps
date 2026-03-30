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

package org.onap.cps.utils.deltareport;

import java.util.concurrent.CompletionException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.cps.api.exceptions.CpsException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CompletionExceptionConverter {

    /**
     * Handles a {@link CompletionException} by unwrapping it if the cause is a {@link RuntimeException},
     * or wrapping it in a {@link CpsException} otherwise.
     *
     * <p>Always throws — declared to return {@link RuntimeException} so callers can write
     * {@code throw CompletionExceptionUtils.handleCompletionException(...)} to satisfy the compiler.
     *
     * @param completionException the completion exception to handle
     * @param errorMessage        short error message for the {@link CpsException}
     * @param errorDetails        detail text for the {@link CpsException}
     * @return never returns — always throws
     */
    public static RuntimeException convertCompletionException(final CompletionException completionException,
                                                             final String errorMessage,
                                                             final String errorDetails) {
        if (completionException.getCause() instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new CpsException(errorMessage, errorDetails, completionException.getCause());
    }
}

