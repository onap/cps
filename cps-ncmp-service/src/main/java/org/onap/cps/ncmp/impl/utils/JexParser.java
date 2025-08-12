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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JexParser {

    private static final Pattern LOCATION_SEGMENT_PATTERN = Pattern.compile("(.*)\\[id=\\\"(.*)\\\"]");
    private static final String JEX_COMMENT_PREFIX = "&&";
    private static final String LINE_SEPARATOR_REGEX = "\\R";
    private static final String SEGMENT_SEPARATOR = "/";

    /**
     * Resolves alternate ids from a JEX basic expression with many paths.
     *
     * @param jsonExpression Multi-line JEX string with possible comments and relative xpaths.
     * @return List of unique alternate ids (FDNs) resolved from each valid path.
     */
    public static List<String> extractFdnsFromLocationPaths(final String jsonExpression) {
        if (jsonExpression == null) {
            return Collections.emptyList();
        }

        final String[] lines = jsonExpression.split(LINE_SEPARATOR_REGEX);

        final Stream<String> locationPaths = Arrays.stream(lines)
                .map(String::trim)
                .filter(locationPath -> !locationPath.startsWith(JEX_COMMENT_PREFIX));

        final Stream<String> fdns = locationPaths
                .map(JexParser::extractFdnPrefix)
                .flatMap(Optional::stream)
                .distinct();

        return fdns.collect(Collectors.toList());
    }

    /**
     * Returns FDN from a JSON expression as a java Optional.
     * Example: /SubNetwork[id="SN1"]/ManagedElement[id="ME1"]
     * returns: /SubNetwork=SN1/ManagedElement=ME1
     *
     * @param locationPath A single JEX path.
     * @return Optional containing resolved FDN if found; empty otherwise.
     */
    private static Optional<String> extractFdnPrefix(final String locationPath) {
        final List<String> locationPathSegments = splitIntoLocationPathsSegments(locationPath);
        final StringBuilder fdnBuilder = new StringBuilder();
        for (final String locationPathSegment : locationPathSegments) {

            final Matcher matcher = LOCATION_SEGMENT_PATTERN.matcher(locationPathSegment);
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

    private static List<String> splitIntoLocationPathsSegments(final String locationPath) {
        final String[] locationPathSegments = locationPath.split(SEGMENT_SEPARATOR);
        final List<String> locationPathSegmentsAsList = new ArrayList<>(Arrays.asList(locationPathSegments));
        if (!locationPathSegmentsAsList.isEmpty()) {
            locationPathSegmentsAsList.remove(0); // ignore root
        }
        return locationPathSegmentsAsList;
    }
}







