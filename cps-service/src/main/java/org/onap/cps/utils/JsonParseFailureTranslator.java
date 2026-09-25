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

import lombok.experimental.UtilityClass;
import org.onap.cps.api.exceptions.CpsException;

@UtilityClass
class JsonParseFailureTranslator {

    /**
     * Translates the given failure context into a {@link CpsException}.
     * {@link JsonParseFailureRule} constants are evaluated in declaration order (most specific first).
     * The last rule ({@link JsonParseFailureRule#FALLBACK}) always matches, so a result is always produced.
     *
     * @param context the context captured at the point of failure
     * @return a CpsException with a user-friendly message
     */
    static CpsException translate(final JsonNodeFailureContext context) {
        for (final JsonParseFailureRule rule : JsonParseFailureRule.values()) {
            if (rule != JsonParseFailureRule.FALLBACK && rule.matches(context)) {
                return rule.toException(context);
            }
        }
        return JsonParseFailureRule.FALLBACK.toException(context);
    }
}