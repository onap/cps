/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
 *  Modifications Copyright (C) 2021 Bell Canada.
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

import static org.springframework.util.StringUtils.isEmpty;

import java.util.HashMap;
import java.util.Map;
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
    private String descendantName;
    private Map<String, Object> leavesData;
    private String ancestorSchemaNodeIdentifier;

    private static final String NON_CAPTURING_GROUP_1_TO_99_YANG_CONTAINERS = "((?:\\/[^\\/]+){1,99})";

    private static final String YANG_LEAF_VALUE_EQUALS_CONDITION =
        "\\[\\s{0,9}@(\\S+?)\\s{0,9}=\\s{0,9}(.*?)\\s{0,9}\\]";

    private static final Pattern QUERY_CPS_PATH_WITH_SINGLE_LEAF_PATTERN =
        Pattern.compile(NON_CAPTURING_GROUP_1_TO_99_YANG_CONTAINERS + YANG_LEAF_VALUE_EQUALS_CONDITION);

    private static final Pattern DESCENDANT_ANYWHERE_PATTERN = Pattern.compile("\\/\\/([^\\/].+)");

    private static final Pattern LEAF_INTEGER_VALUE_PATTERN = Pattern.compile("[-+]?\\d+");

    private static final Pattern LEAF_STRING_VALUE_IN_SINGLE_QUOTES_PATTERN = Pattern.compile("'(.*)'");
    private static final Pattern LEAF_STRING_VALUE_IN_DOUBLE_QUOTES_PATTERN = Pattern.compile("\"(.*)\"");

    private static final String YANG_MULTIPLE_LEAF_VALUE_EQUALS_CONDITION = "\\[(.*?)\\s{0,9}]";

    private static final Pattern DESCENDANT_ANYWHERE_PATTERN_WITH_MULTIPLE_LEAF_PATTERN =
        Pattern.compile(DESCENDANT_ANYWHERE_PATTERN + YANG_MULTIPLE_LEAF_VALUE_EQUALS_CONDITION);

    private static final String INDIVIDUAL_LEAF_DETAIL_PATTERN = ("\\s{0,9}and\\s{0,9}");

    private static final Pattern LEAF_VALUE_PATTERN = Pattern.compile("@(\\S+?)=(.*)");

    private static final Pattern ANCESTOR_AXIS_PATTERN = Pattern.compile("(\\S+)\\/ancestor::(\\S+)");

    /**
     * Returns a cps path query.
     *
     * @param cpsPathSource cps path
     * @return a CpsPath object.
     */
    public static CpsPathQuery createFrom(final String cpsPathSource) {
        var cpsPath = cpsPathSource;
        final CpsPathQuery cpsPathQuery = new CpsPathQuery();
        var matcher = ANCESTOR_AXIS_PATTERN.matcher(cpsPath);
        if (matcher.matches()) {
            cpsPath = matcher.group(1);
            cpsPathQuery.setAncestorSchemaNodeIdentifier(matcher.group(2));
        }
        matcher = QUERY_CPS_PATH_WITH_SINGLE_LEAF_PATTERN.matcher(cpsPath);
        if (matcher.matches()) {
            cpsPathQuery.setParametersForSingleLeafValue(cpsPath, matcher);
            return cpsPathQuery;
        }
        matcher = DESCENDANT_ANYWHERE_PATTERN_WITH_MULTIPLE_LEAF_PATTERN.matcher(cpsPath);
        if (matcher.matches()) {
            cpsPathQuery.setParametersForDescendantWithLeafValues(cpsPath, matcher);
            return cpsPathQuery;
        }
        matcher = DESCENDANT_ANYWHERE_PATTERN.matcher(cpsPath);
        if (matcher.matches()) {
            cpsPathQuery.setParametersForDescendantAnywhere(matcher);
            return cpsPathQuery;
        }
        throw new CpsPathException("Invalid cps path.",
            String.format("Cannot interpret or parse cps path '%s'.", cpsPath));
    }

    /**
     * Has ancestor axis been populated.
     *
     * @return boolean value.
     */
    public boolean hasAncestorAxis() {
        return !(isEmpty(ancestorSchemaNodeIdentifier));
    }

    private void setParametersForSingleLeafValue(final String cpsPath, final Matcher matcher) {
        setCpsPathQueryType(CpsPathQueryType.XPATH_LEAF_VALUE);
        setXpathPrefix(matcher.group(1));
        setLeafName(matcher.group(2));
        setLeafValue(convertLeafValueToCorrectType(matcher.group(3), cpsPath));
    }

    private void setParametersForDescendantWithLeafValues(final String cpsPath, final Matcher matcher) {
        setCpsPathQueryType(CpsPathQueryType.XPATH_HAS_DESCENDANT_WITH_LEAF_VALUES);
        setDescendantName(matcher.group(1));
        final Map<String, Object> leafData = new HashMap<>();
        for (final String leafValuePair : matcher.group(2).split(INDIVIDUAL_LEAF_DETAIL_PATTERN)) {
            final var descendentMatcher = LEAF_VALUE_PATTERN.matcher(leafValuePair);
            if (descendentMatcher.matches()) {
                leafData.put(descendentMatcher.group(1),
                    convertLeafValueToCorrectType(descendentMatcher.group(2), cpsPath));
            } else {
                throw new CpsPathException("Invalid cps path.",
                    String.format("Cannot interpret or parse attributes in cps path '%s'.", cpsPath));
            }
        }
        setLeavesData(leafData);
    }

    private void setParametersForDescendantAnywhere(final Matcher matcher) {
        setCpsPathQueryType(CpsPathQueryType.XPATH_HAS_DESCENDANT_ANYWHERE);
        setDescendantName(matcher.group(1));
    }

    private static Object convertLeafValueToCorrectType(final String leafValueString, final String cpsPath) {
        final var stringValueWithSingleQuotesMatcher =
                LEAF_STRING_VALUE_IN_SINGLE_QUOTES_PATTERN.matcher(leafValueString);
        if (stringValueWithSingleQuotesMatcher.matches()) {
            return stringValueWithSingleQuotesMatcher.group(1);
        }
        final var stringValueWithDoubleQuotesMatcher =
                LEAF_STRING_VALUE_IN_DOUBLE_QUOTES_PATTERN.matcher(leafValueString);
        if (stringValueWithDoubleQuotesMatcher.matches()) {
            return stringValueWithDoubleQuotesMatcher.group(1);
        }
        final var integerValueMatcher = LEAF_INTEGER_VALUE_PATTERN.matcher(leafValueString);
        if (integerValueMatcher.matches()) {
            return Integer.valueOf(leafValueString);
        }
        throw new CpsPathException("Unsupported leaf value.",
            String.format("Unsupported leaf value '%s' in cps path '%s'.", leafValueString, cpsPath));
    }
}
