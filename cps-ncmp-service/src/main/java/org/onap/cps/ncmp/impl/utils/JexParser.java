/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JexParser {

    private static final Pattern XPATH_SEGMENT_PATTERN = Pattern.compile("(.*)\\[id=\\\"(.*)\\\"]");
    private static final String JEX_COMMENT_PREFIX = "&&";
    private static final String LINE_SEPARATOR_REGEX = "\\R";
    private static final String LINE_JOINER_DELIMITER = "\n";
    private static final String SEGMENT_SEPARATOR = "/";

    /**
     * Resolves a json expression into a list of xpaths.
     *
     * @param jsonExpressionsAsString Multi-line jex string with possible comments and relative xpaths.
     * @return List of xpaths
     */
    @SuppressWarnings("unused")
    public static List<String> toXpaths(final String jsonExpressionsAsString) {
        if (jsonExpressionsAsString == null) {
            return Collections.emptyList();
        } else {
            final String[] lines = jsonExpressionsAsString.split(LINE_SEPARATOR_REGEX);
            return Arrays.stream(lines)
                    .map(String::trim)
                    .filter(xpath -> !xpath.startsWith(JEX_COMMENT_PREFIX))
                    .distinct()
                    .toList();
        }
    }

    /**
     * Resolves alternate ids from a jex basic expression with many paths.
     *
     * @param jsonExpression Multi-line jex string with possible comments and relative xpaths.
     * @return List of unique alternate ids (fdns) resolved from each valid path.
     */
    public static List<String> extractFdnsFromXpaths(final String jsonExpression) {
        if (jsonExpression == null) {
            return Collections.emptyList();
        }

        final String[] lines = jsonExpression.split(LINE_SEPARATOR_REGEX);

        final Stream<String> xpaths = Arrays.stream(lines)
                .map(String::trim)
                .filter(xpath -> !xpath.startsWith(JEX_COMMENT_PREFIX));

        final Stream<String> fdns = xpaths
                .map(JexParser::extractFdnPrefix)
                .flatMap(Optional::stream)
                .distinct();

        return fdns.collect(Collectors.toList());
    }

    /**
     * Returns fdn from a json expression as a java Optional.
     * Example: /SubNetwork[id="SN1"]/ManagedElement[id="ME1"]
     * returns: /SubNetwork=SN1/ManagedElement=ME1
     *
     * @param xpath A single jex path.
     * @return Optional containing resolved fdn if found; empty otherwise.
     */
    public static Optional<String> extractFdnPrefix(final String xpath) {
        final List<String> xpathSegments = splitIntoXpaths(xpath);
        final StringBuilder fdnBuilder = new StringBuilder();
        for (final String xpathSegment : xpathSegments) {

            final Matcher matcher = XPATH_SEGMENT_PATTERN.matcher(xpathSegment);
            if (matcher.find()) {
                final String managedObjectName = matcher.group(1);
                final String managedObjectId = matcher.group(2);
                fdnBuilder.append(SEGMENT_SEPARATOR)
                        .append(managedObjectName)
                        .append("=")
                        .append(managedObjectId);
            } else {
                break;
            }
        }

        final String fdn = fdnBuilder.toString();
        return fdn.isEmpty() ? Optional.empty() : Optional.of(fdn);
    }

    /**
     * Concatenates the given list of xpaths into a single json expression string.
     * Each path separated by the {@code LINE_JOINER_DELIMITER}.
     *
     * @param xpaths List of xpath strings to be joined.
     * @return A string representing the concatenated json expression; empty otherwise.
     */
    @SuppressWarnings("unused")
    public static String toJsonExpressionsAsString(final Collection<String> xpaths) {
        return String.join(LINE_JOINER_DELIMITER, xpaths);
    }

    private static List<String> splitIntoXpaths(final String xpath) {
        final String[] xpathSegments = xpath.split(SEGMENT_SEPARATOR);
        final List<String> xpathSegmentsAsList = new ArrayList<>(Arrays.asList(xpathSegments));
        if (!xpathSegmentsAsList.isEmpty()) {
            xpathSegmentsAsList.remove(0); // ignore root
        }
        return xpathSegmentsAsList;
    }
}







