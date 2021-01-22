/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
 *  Modifications Copyright (C) 2020 Bell Canada. All rights reserved.
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

package org.onap.cps.utils;

import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsDataNodeBuilder;
import org.onap.cps.spi.model.DataNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.ValueNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

@Slf4j
public class YangUtils {

    private YangUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Parse a string containing json data for a certain model (schemaContext).
     *
     * @param jsonData      a string containing json data for the given model
     * @param schemaContext the SchemaContext for the given data
     * @return the NormalizedNode representing the json data
     */
    public static NormalizedNode<?, ?> parseJsonData(final String jsonData, final SchemaContext schemaContext)
            throws IOException {
        final JSONCodecFactory jsonCodecFactory = JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02
                .getShared(schemaContext);
        final NormalizedNodeResult normalizedNodeResult = new NormalizedNodeResult();
        final NormalizedNodeStreamWriter normalizedNodeStreamWriter = ImmutableNormalizedNodeStreamWriter
                .from(normalizedNodeResult);
        try (final JsonParserStream jsonParserStream = JsonParserStream
                .create(normalizedNodeStreamWriter, jsonCodecFactory)) {
            final JsonReader jsonReader = new JsonReader(new StringReader(jsonData));
            jsonParserStream.parse(jsonReader);
        }
        return normalizedNodeResult.getResult();
    }

    /**
     * Break a Normalized Node tree into fragments that can be stored by the persistence service.
     *
     * @param tree   the normalized node tree
     * @return the 'parent' DataNode for the tree contain all relevant children etc.
     */
    public static DataNode createDataNodeTreeFromNormalizedNode(final NormalizedNode<? extends PathArgument, ?> tree) {
        final String xpath = buildXpathId(tree.getIdentifier());
        final DataNode parentDataNode = CpsDataNodeBuilder.createParentDataNode(xpath);
        createDataNodeTreeFromNormalizedNode(parentDataNode, tree);
        return parentDataNode;
    }

    private static void createDataNodeTreeFromNormalizedNode(final DataNode currentDataNode,
        final NormalizedNode normalizedNode) {
        if (normalizedNode instanceof DataContainerNode) {
            inspectContainer(currentDataNode, (DataContainerNode) normalizedNode);
        } else if (normalizedNode instanceof MapNode) {
            inspectKeyedList(currentDataNode, (MapNode) normalizedNode);
        } else if (normalizedNode instanceof ValueNode) {
            inspectLeaf(currentDataNode, (ValueNode) normalizedNode);
        } else if (normalizedNode instanceof LeafSetNode) {
            inspectLeafList(currentDataNode, (LeafSetNode) normalizedNode);
        } else {
            log.warn("Cannot normalize {}", normalizedNode.getClass());
        }
    }

    private static void inspectLeaf(final DataNode currentDataNode, final ValueNode valueNode) {
        final Object value = valueNode.getValue();
        currentDataNode.addLeafValue(valueNode.getNodeType().getLocalName(), value);
    }

    private static void inspectLeafList(final DataNode currentDataNode, final LeafSetNode leafSetNode) {
        currentDataNode.addLeafListName(leafSetNode.getNodeType().getLocalName());
        for (final NormalizedNode value : (Collection<NormalizedNode>) leafSetNode.getValue()) {
            createDataNodeTreeFromNormalizedNode(currentDataNode, value);
        }
    }

    private static void inspectContainer(final DataNode currentDataNode, final DataContainerNode dataContainerNode) {
        final Collection<NormalizedNode> leaves = (Collection) dataContainerNode.getValue();
        for (final NormalizedNode leaf : leaves) {
            createDataNodeTreeFromNormalizedNode(currentDataNode, leaf);
        }
    }

    private static void inspectKeyedList(final DataNode currentDataNode, final MapNode mapNode) {
        createNodeForEachListElement(currentDataNode, mapNode);
    }

    /**
     * Create a node for each list element.
     *
     * @param currentDataNode   the current data Node
     * @param mapNode the map node
     */
    private static void createNodeForEachListElement(final DataNode currentDataNode, final MapNode mapNode) {
        final Collection<MapEntryNode> mapEntryNodes = mapNode.getValue();
        for (final MapEntryNode mapEntryNode : mapEntryNodes) {
            final String xpathId = buildXpathId(mapEntryNode.getIdentifier());
            final DataNode listElementNodes =
                CpsDataNodeBuilder.createChildNode(currentDataNode, xpathId);
            createDataNodeTreeFromNormalizedNode(listElementNodes, mapEntryNode);
        }
    }

    /**
     * Build xpathId from the YangInstanceIdentifier.
     *
     * @param nodeIdentifier   te identifier of each node
     * @return the generated xpathId for each node
     */
    private static String buildXpathId(final YangInstanceIdentifier.PathArgument nodeIdentifier) {
        final StringBuilder xpathIdBuilder = new StringBuilder();
        xpathIdBuilder.append("/").append(nodeIdentifier.getNodeType().getLocalName());

        if (nodeIdentifier instanceof NodeIdentifierWithPredicates) {
            xpathIdBuilder.append(getKeyAttributesStatement((NodeIdentifierWithPredicates) nodeIdentifier));
        }
        return xpathIdBuilder.toString();
    }

    private static String getKeyAttributesStatement(final NodeIdentifierWithPredicates nodeIdentifier) {
        final List<String> keyAttributes = nodeIdentifier.entrySet().stream().map(
            entry -> {
                final String name = entry.getKey().getLocalName();
                final String value = String.valueOf(entry.getValue()).replace("'", "\\'");
                return String.format("@%s='%s'", name, value);
            }
        ).collect(Collectors.toList());

        if (keyAttributes.isEmpty()) {
            return "";
        } else {
            Collections.sort(keyAttributes);
            return "[" + String.join(" and ", keyAttributes) + "]";
        }
    }
}
