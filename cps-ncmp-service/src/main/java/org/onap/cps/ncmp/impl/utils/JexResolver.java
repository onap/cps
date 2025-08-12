/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class JexResolver {


    /**
     * Resolves alternate IDs from a multi-line dataNodeSelector JEX string.
     *
     * @param dataNodeSelector Multi-line JEX string with possible comments and relative xpaths.
     * @return List of unique alternate IDs (FDNs) resolved from each selector line.
     */
    public static List<String> resolveAlternateIds(final String dataNodeSelector) {
        if (dataNodeSelector == null || dataNodeSelector.isBlank()) {
            return Collections.emptyList();
        }

        return Arrays.stream(dataNodeSelector.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("&&"))
                .map(JexResolver::resolveFdnFromSingleSelector)
                .flatMap(Optional::stream)
                .map(JexResolver::toAlternateId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Resolves the longest valid prefix path (FDN) from a single selector line.
     * Valid if all path segments up to the point have explicit [id="X"] attributes.
     *
     * @param nodeSelector A single JEX selector string.
     * @return Optional containing resolved FDN if found; empty otherwise.
     */
    private static Optional<String> resolveFdnFromSingleSelector(final String nodeSelector) {
        if (!nodeSelector.startsWith("/")) {
            return Optional.empty();
        }
        final String[] parts = nodeSelector.split("/");
        final StringBuilder fdnBuilder = new StringBuilder();
        for (final String part : parts) {
            if (part.isEmpty()) {
                continue;
            }

            final Matcher matcher = Pattern.compile("\\[id\\s*=\\s*['\"]?([^'\"]+)['\"]?\\]").matcher(part);
            if (matcher.find()) {
                final String idValue = matcher.group(1);
                final String normalized = part.replaceAll("\\[id\\s*=\\s*['\"]?([^'\"]+)['\"]?\\]",
                        "[id=\"" + idValue + "\"]");
                fdnBuilder.append("/").append(normalized);
            } else {
                break;
            }
        }

        final String resolvedFdn = fdnBuilder.toString();
        return resolvedFdn.isEmpty() ? Optional.empty() : Optional.of(resolvedFdn);
    }

    /**
     * Converts normalized FDN to alternate ID format.
     * Example: /SubNetwork[id="SN1"]/ManagedElement[id="ME1"]
     * becomes: /SubNetwork=SN1/ManagedElement=ME1
     *
     * @param fdn Normalized FDN string.
     * @return Alternate ID string.
     */
    private static String toAlternateId(final String fdn) {
        return fdn.replaceAll("\\[id=\"([^\"]+)\"\\]", "=$1");
    }

}







