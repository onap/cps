/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
 *  Modifications Copyright (C) 2021 Bell Canada. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.spi.query;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.onap.cps.spi.exceptions.CpsPathException;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CpsPathQuery {

    private String xpathPrefix;
    private String leafName;
    private Object leafValue;

    public static final Pattern QUERY_CPS_PATH_WITH_SINGLE_LEAF_PATTERN =
        Pattern.compile("(.*)\\[\\s*@(.*?)\\s*=\\s*(.*?)\\s*]");

    public static final Pattern LEAF_STRING_VALUE_PATTERN = Pattern.compile("['\"](.*)['\"]");

    public static final Pattern LEAF_INTEGER_VALUE_PATTERN = Pattern.compile("[-+]?\\d+");


    /**
     * Returns a xpath prefix, leaf name and leaf value for the given cps path.
     *
     * @param cpsPath cps path
     * @return a CpsPath object containing the xpath prefix, leaf name and leaf value.
     */
    public static CpsPathQuery createFrom(final String cpsPath) {
        final Matcher matcher = QUERY_CPS_PATH_WITH_SINGLE_LEAF_PATTERN.matcher(cpsPath);
        if (matcher.matches()) {
            final String xpathPrefix = matcher.group(1);
            final String leafName = matcher.group(2);
            final Object leafValue = convertLeafValueToCorrectType(matcher.group(3));
            return new CpsPathQuery(xpathPrefix, leafName, leafValue);
        }
        throw new CpsPathException("Invalid cps path.",
            String.format("Cannot interpret or parse cps path %s.", cpsPath));
    }

    private static Object convertLeafValueToCorrectType(final String leafValueString) {
        final Matcher stringValueWithQuotesMatcher = LEAF_STRING_VALUE_PATTERN.matcher(leafValueString);
        if (stringValueWithQuotesMatcher.matches()) {
            return stringValueWithQuotesMatcher.group(1);
        }
        final Matcher integerValueMatcher = LEAF_INTEGER_VALUE_PATTERN.matcher(leafValueString);
        if (integerValueMatcher.matches()) {
            return Integer.valueOf(leafValueString);
        }
        throw new CpsPathException("Unsupported leaf value.",
            String.format("Unsupported leaf value %s in cps path.", leafValueString));
    }
}
