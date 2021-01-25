/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Bell Canada. All rights reserved.
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.spi.model.DataNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.ValueNode;

@Slf4j
public class CpsDataNodeBuilder {

    /**
     * Break a Normalized Node tree into a DataNode object tree that can be stored by the persistence service.
     *
     * @param normalizedNodeTree   the normalized node tree
     * @return the DataNode (tree) containing the same descendants as the normalizedNodeTree
     */
    public static DataNode createDataNodeTreeFromNormalizedNode(
            final NormalizedNode<? extends YangInstanceIdentifier.PathArgument, ?> normalizedNodeTree) {
        final String xpath = buildXpath(normalizedNodeTree.getIdentifier());
        final DataNode dataNode = new DataNode();
        dataNode.setXpath(xpath);
        addDataNodeFromNormalizedNode(dataNode, normalizedNodeTree);
        return dataNode;
    }

    private static void addDataNodeFromNormalizedNode(final DataNode currentDataNode,
                                                      final NormalizedNode normalizedNode) {
        if (normalizedNode instanceof DataContainerNode) {
            addYangContainer(currentDataNode, (DataContainerNode) normalizedNode);
        } else if (normalizedNode instanceof MapNode) {
            addYangKeyedList(currentDataNode, (MapNode) normalizedNode);
        } else if (normalizedNode instanceof ValueNode) {
            addYangLeaf(currentDataNode, (ValueNode) normalizedNode);
        } else if (normalizedNode instanceof LeafSetNode) {
            addYangLeafList(currentDataNode, (LeafSetNode) normalizedNode);
        } else {
            log.warn("Cannot normalize {}", normalizedNode.getClass());
        }
    }

    private static void addYangContainer(final DataNode currentDataNode, final DataContainerNode dataContainerNode) {
        final Collection<NormalizedNode> normalizedChildNodes = (Collection) dataContainerNode.getValue();
        for (final NormalizedNode normalizedNode : normalizedChildNodes) {
            addDataNodeFromNormalizedNode(currentDataNode, normalizedNode);
        }
    }

    private static void addYangKeyedList(final DataNode currentDataNode, final MapNode mapNode) {
        addDataNodeForEachListElement(currentDataNode, mapNode);
    }

    private static void addYangLeaf(final DataNode currentDataNode, final ValueNode valueNode) {
        final String leafName = valueNode.getNodeType().getLocalName();
        /**
         * TODO Toine Siebelink, not sure if this is the best solution. But is was the quickest way
         * to get around updating an immutable map that kas a key value pair where thr value is an
         * immutable list to which we recursively keep adding objects...
         */
        final Map<String, Object> mutableLeavesMap = new HashMap<>(currentDataNode.getLeaves());
        Object leafValue;
        if (currentDataNode.getOptionalLeafListNames().isPresent()
                && currentDataNode.getOptionalLeafListNames().get().contains(leafName)) {
            leafValue = getUpdatedLeafListValue(currentDataNode, leafName, valueNode.getValue());
            mutableLeavesMap.remove(leafName);
        } else {
            leafValue = valueNode.getValue();
        }
        final Map leaves = (new ImmutableMap.Builder<String, Object>())
                .putAll(mutableLeavesMap)
                .put(leafName, leafValue)
                .build();
        currentDataNode.setLeaves(leaves);
    }

    private static ImmutableList getUpdatedLeafListValue(final DataNode currentDataNode, final String name, final Object value) {
        ImmutableList.Builder builder = new ImmutableList.Builder<String>();
        if (currentDataNode.getLeaves().containsKey(name)) {
            builder.addAll((Iterable<Object>) currentDataNode.getLeaves().get(name));
        }
        builder.add(value);
        return builder.build();
    }

    private static void addYangLeafList(final DataNode currentDataNode, final LeafSetNode leafSetNode) {
        final ImmutableSet.Builder builder = new ImmutableSet.Builder<String>();
        if (currentDataNode.getOptionalLeafListNames().isPresent()) {
            builder.addAll(currentDataNode.getOptionalLeafListNames().get());
        }
        builder.add(leafSetNode.getNodeType().getLocalName());
        ImmutableSet leafListNames = builder.build();
        currentDataNode.setOptionalLeafListNames(Optional.of(leafListNames));
        for (final NormalizedNode normalizedNode : (Collection<NormalizedNode>) leafSetNode.getValue()) {
            addDataNodeFromNormalizedNode(currentDataNode, normalizedNode);
        }
    }

    private static void addDataNodeForEachListElement(final DataNode currentDataNode, final MapNode mapNode) {
        final Collection<MapEntryNode> mapEntryNodes = mapNode.getValue();
        for (final MapEntryNode mapEntryNode : mapEntryNodes) {
            final String xpathChild = buildXpath(mapEntryNode.getIdentifier());
            final DataNode childDataNode = createAndAddChildDataNode(currentDataNode, xpathChild);
            addDataNodeFromNormalizedNode(childDataNode, mapEntryNode);
        }
    }

    private static DataNode createAndAddChildDataNode(final DataNode parentDataNode, final String childXpath) {
        final DataNode newChildDataNode = new DataNode();
        newChildDataNode.setXpath(parentDataNode.getXpath() + childXpath);
        final Set<DataNode> allChildDataNodes = new ImmutableSet.Builder<DataNode>()
                .addAll(parentDataNode.getChildDataNodes())
                .add(newChildDataNode)
                .build();
        parentDataNode.setChildDataNodes(allChildDataNodes);
        return newChildDataNode;
    }

    private static String buildXpath(final YangInstanceIdentifier.PathArgument nodeIdentifier) {
        final StringBuilder xpathBuilder = new StringBuilder();
        xpathBuilder.append("/").append(nodeIdentifier.getNodeType().getLocalName());

        if (nodeIdentifier instanceof YangInstanceIdentifier.NodeIdentifierWithPredicates) {
            xpathBuilder.append(getKeyAttributesStatement(
                    (YangInstanceIdentifier.NodeIdentifierWithPredicates) nodeIdentifier));
        }
        return xpathBuilder.toString();
    }

    private static String getKeyAttributesStatement(
            final YangInstanceIdentifier.NodeIdentifierWithPredicates nodeIdentifier) {
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
