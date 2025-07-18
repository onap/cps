/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2025 OpenInfra Foundation Europe. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.utils;

import java.util.Collection;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RestConfStylePathToCpsPathUtil {

    /**
     * Convert RESTCONF style path to CpsPath.
     *
     * @param restConfStylePath restconf style path
     * @param schemaContext     schema context
     * @return CpsPath
     */
    public static String convertToCpsPath(final String restConfStylePath,
            final SchemaContext schemaContext) {
        if (restConfStylePath == null || restConfStylePath.trim().isEmpty()) {
            return "";
        }

        final StringBuilder cpsPathBuilder = new StringBuilder();
        final String[] segments = restConfStylePath.split("/");
        Collection<? extends DataSchemaNode> currentSchemaNodes = schemaContext.getChildNodes();

        for (final String segment : segments) {
            if (segment.isEmpty()) {
                continue;
            }

            final PathSegment pathSegment = parsePathSegment(segment);
            final DataSchemaNode schemaNode = findMatchingSchemaNode(pathSegment.nodeName, currentSchemaNodes);
            buildCpsPath(pathSegment, schemaNode, cpsPathBuilder);
            currentSchemaNodes =
                    (schemaNode instanceof DataNodeContainer container) ? container.getChildNodes() : List.of();
        }

        return cpsPathBuilder.toString();
    }

    private static void buildCpsPath(final PathSegment pathSegment, final DataSchemaNode schemaNode,
            final StringBuilder cpsPathBuilder) {
        cpsPathBuilder.append("/").append(pathSegment.nodeName);

        if (pathSegment.keyValue != null && schemaNode instanceof ListSchemaNode listNode) {
            final String keyFilter = buildKeyFilter(listNode, pathSegment.keyValue);
            cpsPathBuilder.append(keyFilter);
        }
    }

    private static PathSegment parsePathSegment(final String segment) {
        // Strip module prefix (e.g., "stores:bookstore" -> "bookstore")
        final String rawSegment = segment.contains(":") ? segment.substring(segment.indexOf(":") + 1) : segment;

        if (rawSegment.contains("=")) {
            final String[] parts = rawSegment.split("=", 2);
            return new PathSegment(parts[0], parts[1]);
        }

        return new PathSegment(rawSegment, null);
    }

    private static DataSchemaNode findMatchingSchemaNode(final String nodeName,
            final Collection<? extends DataSchemaNode> schemaNodes) {
        return schemaNodes.stream()
                       .filter(schemaNode -> nodeName.equals(schemaNode.getQName().getLocalName()))
                       .findFirst()
                       .orElseThrow(() -> new IllegalArgumentException("Schema node not found: " + nodeName));
    }

    private static String buildKeyFilter(final ListSchemaNode listNode, final String keyValue) {
        final List<QName> keyQNames = listNode.getKeyDefinition();
        if (keyQNames.isEmpty()) {
            throw new IllegalArgumentException("No key defined for list node: " + listNode.getQName().getLocalName());
        }

        final String keyName = keyQNames.get(0).getLocalName(); // Only first key supported for now
        return "[@" + keyName + "='" + keyValue + "']";
    }

    private record PathSegment(String nodeName, String keyValue) { }

}
