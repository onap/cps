/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl;

public class JsonUtils {

    private static final String BACK_SLASH = "\\";
    private static final String NEW_LINE = "\n";
    private static final String QUOTE = "\"";

    private JsonUtils() {
        throw new IllegalStateException();
    }

    /**
     * Remove redundant beginning and end characters.
     * @param input string to format
     * @return formatted string
     */
    public static String removeWrappingTokens(final String input) {
        return input.substring(1, input.length() - 1);
    }

    /**
     * Remove redundant escape characters.
     * @param input string to format
     * @return formatted string
     */
    public static String removeRedundantEscapeCharacters(final String input) {
        return input.replace(BACK_SLASH + "n", NEW_LINE)
            .replace(BACK_SLASH + QUOTE, QUOTE)
            .replace(BACK_SLASH + BACK_SLASH + QUOTE, BACK_SLASH + QUOTE);
    }
}
