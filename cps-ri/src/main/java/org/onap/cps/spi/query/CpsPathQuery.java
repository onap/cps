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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.onap.cps.spi.exceptions.CpsPathException;

@Getter
@Setter(AccessLevel.PRIVATE)
public class CpsPathQuery {

    private CpsPathQueryType cpsPathQueryType;
    private String xpathPrefix;
    private String leafName;
    private Object leafValue;
    private String endsWith;

    private static final String NON_CAPTURING_GROUP_1_TO_99_YANG_CONTAINERS = "((?:\\/[^\\/]+){1,99})";

    private static final String YANG_LEAF_VALUE_EQUALS_CONDITION = "\\[\\s*@(\\S+?)\\s*=\\s*(.*?)\\s*\\]";

    private static final Pattern QUERY_CPS_PATH_WITH_SINGLE_LEAF_PATTERN =
        Pattern.compile(NON_CAPTURING_GROUP_1_TO_99_YANG_CONTAINERS + YANG_LEAF_VALUE_EQUALS_CONDITION);

    private static final Pattern QUERY_CPS_PATH_ENDS_WITH_PATTERN = Pattern.compile("\\/\\/(.+)");

    private static final Pattern LEAF_INTEGER_VALUE_PATTERN = Pattern.compile("[-+]?\\d+");

    private static final Pattern LEAF_STRING_VALUE_PATTERN = Pattern.compile("['\"](.*)['\"]");

    /**
     * Returns a cps path query.
     *
     * @param cpsPath cps path
     * @return a CpsPath object.
     */
    public static CpsPathQuery createFrom(final String cpsPath) {
        Matcher matcher = QUERY_CPS_PATH_WITH_SINGLE_LEAF_PATTERN.matcher(cpsPath);
        final CpsPathQuery cpsPathQuery = new CpsPathQuery();
        if (matcher.matches()) {
            cpsPathQuery.setCpsPathQueryType(CpsPathQueryType.XPATH_LEAF_VALUE);
            cpsPathQuery.setXpathPrefix(matcher.group(1));
            cpsPathQuery.setLeafName(matcher.group(2));
            cpsPathQuery.setLeafValue(convertLeafValueToCorrectType(matcher.group(3)));
            return cpsPathQuery;
        }
        matcher = QUERY_CPS_PATH_ENDS_WITH_PATTERN.matcher(cpsPath);
        if (matcher.matches()) {
            cpsPathQuery.setCpsPathQueryType(CpsPathQueryType.XPATH_ENDS_WITH);
            cpsPathQuery.setEndsWith(matcher.group(1));
            return cpsPathQuery;
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
