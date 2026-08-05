/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 Deutsche Telekom AG. All rights reserved.
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

package org.onap.cps.utils;

import org.onap.cps.api.exceptions.CpsException;

interface JsonParseFailureRule {

    /**
     * Returns {@code true} if this rule applies to the given failure context.
     *
     * @param context the context captured at the point of failure
     * @return {@code true} if the rule matches
     */
    boolean matches(JsonNodeFailureContext context);

    /**
     * Converts the failure context into a {@link CpsException}.
     * <p>
     * This method must only be invoked after {@link #matches(JsonNodeFailureContext)}
     * has returned {@code true} for the same context. Callers must not invoke {@code toException} independently
     * because a rule may assume its preconditions are satisfied and behave incorrectly otherwise.
     * </p>
     *
     * @param context the context captured at the point of failure
     * @return a {@link CpsException} (or a more specific subtype) with a user-friendly message
     */
    CpsException toException(JsonNodeFailureContext context);
}

