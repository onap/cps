/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Bell Canada. All rights reserved.
 *  Modifications Copyright (C) 2021 Pantheon.tech
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

package org.onap.cps.spi.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.utils.YangUtils;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.ValueNode;

@Slf4j
public class DataNodeBuilder {

    private NormalizedNode<?, ?> normalizedNodeTree;
    private String xpath;
    private String parentNodeXpath = "";
    private Map<String, Object> leaves = Collections.emptyMap();
    private Collection<DataNode> childDataNodes = Collections.emptySet();

    /**
     * To use parent node xpath for creating {@link DataNode}.
     *
     * @param parentNodeXpath xpath of a parent node
     * @return this {@link DataNodeBuilder} object
     */
    public DataNodeBuilder withParentNodeXpath(final String parentNodeXpath) {
        this.parentNodeXpath = parentNodeXpath;
        return this;
    }


    /**
     * To use {@link NormalizedNode} for creating {@link DataNode}.
     *
     * @param normalizedNodeTree used for creating the Data Node
     * @return this {@link DataNodeBuilder} object
     */
    public DataNodeBuilder withNormalizedNodeTree(final NormalizedNode<?, ?> normalizedNodeTree) {
        this.normalizedNodeTree = normalizedNodeTree;
        return this;
    }

    /**
     * To use xpath for creating {@link DataNode}.
     *
     * @param xpath for the data node
     * @return DataNodeBuilder
     */
    public DataNodeBuilder withXpath(final String xpath) {
        this.xpath = xpath;
        return this;
    }

    /**
     * To use attributes for creating {@link DataNode}.
     *
     * @param leaves for the data node
     * @return DataNodeBuilder
     */
    public DataNodeBuilder withLeaves(final Map<String, Object> leaves) {
        this.leaves = leaves;
        return this;
    }

    /**
     * To specify child nodes needs to be used while creating {@link DataNode}.
     *
     * @param childDataNodes to be added to the dataNode
     * @return DataNodeBuilder
     */
    public DataNodeBuilder withChildDataNodes(final Collection<DataNode> childDataNodes) {
        // Added as this is being set from test cases .
        // Open for suggestions
        this.childDataNodes = childDataNodes;
        return this;
    }

    /**
     * To create the {@link DataNode}.
     *
     * @return {@link DataNode}
     */
    public DataNode build() {
        if (normalizedNodeTree != null) {
            return buildFromNormalizedNodeTree();
        } else {
            return buildFromAttributes();
        }
    }

    private DataNode buildFromAttributes() {
        final DataNode dataNode = new DataNode();
        dataNode.setXpath(xpath);
        dataNode.setLeaves(leaves);
        dataNode.setChildDataNodes(childDataNodes);
        return dataNode;
    }

    private DataNode buildFromNormalizedNodeTree() {
        final DataNode parentDataNode = new DataNodeBuilder().withXpath(parentNodeXpath).build();
        addDataNodeFromNormalizedNode(parentDataNode, normalizedNodeTree);
        return parentDataNode.getChildDataNodes().iterator().next();
    }

    private static void addDataNodeFromNormalizedNode(final DataNode currentDataNode,
        final NormalizedNode<?, ?> normalizedNode) {

        if (normalizedNode instanceof DataContainerNode) {
            addYangContainer(currentDataNode, (DataContainerNode<?>) normalizedNode);
        } else if (normalizedNode instanceof MapNode) {
            addDataNodeForEachListElement(currentDataNode, (MapNode) normalizedNode);
        } else if (normalizedNode instanceof ValueNode) {
            final ValueNode<?, ?> valuesNode = (ValueNode<?, ?>) normalizedNode;
            addYangLeaf(currentDataNode, valuesNode.getNodeType().getLocalName(), valuesNode.getValue());
        } else if (normalizedNode instanceof LeafSetNode) {
            addYangLeafList(currentDataNode, (LeafSetNode<?>) normalizedNode);
        } else {
            log.warn("Unsupported NormalizedNode type detected: {}", normalizedNode.getClass());
        }
    }

    private static void addYangContainer(final DataNode currentDataNode, final DataContainerNode<?> dataContainerNode) {
        final DataNode dataContainerDataNode =
            (dataContainerNode.getIdentifier() instanceof YangInstanceIdentifier.AugmentationIdentifier)
                ? currentDataNode
                : createAndAddChildDataNode(currentDataNode, YangUtils.buildXpath(dataContainerNode.getIdentifier()));
        final Collection<DataContainerChild<?, ?>> normalizedChildNodes = dataContainerNode.getValue();
        for (final NormalizedNode<?, ?> normalizedNode : normalizedChildNodes) {
            addDataNodeFromNormalizedNode(dataContainerDataNode, normalizedNode);
        }
    }

    private static void addYangLeaf(final DataNode currentDataNode, final String leafName, final Object leafValue) {
        final Map<String, Object> leaves = new ImmutableMap.Builder<String, Object>()
            .putAll(currentDataNode.getLeaves())
            .put(leafName, leafValue)
            .build();
        currentDataNode.setLeaves(leaves);
    }

    private static void addYangLeafList(final DataNode currentDataNode, final LeafSetNode<?> leafSetNode) {
        final String leafListName = leafSetNode.getNodeType().getLocalName();
        final List<?> leafListValues = ((Collection<? extends NormalizedNode<?, ?>>) leafSetNode.getValue())
            .stream()
            .map(normalizedNode -> ((ValueNode<?, ?>) normalizedNode).getValue())
            .collect(Collectors.toUnmodifiableList());
        addYangLeaf(currentDataNode, leafListName, leafListValues);
    }

    private static void addDataNodeForEachListElement(final DataNode currentDataNode, final MapNode mapNode) {
        final Collection<MapEntryNode> mapEntryNodes = mapNode.getValue();
        for (final MapEntryNode mapEntryNode : mapEntryNodes) {
            addDataNodeFromNormalizedNode(currentDataNode, mapEntryNode);
        }
    }

    private static DataNode createAndAddChildDataNode(final DataNode parentDataNode, final String childXpath) {

        final DataNode newChildDataNode = new DataNodeBuilder()
            .withXpath(parentDataNode.getXpath() + childXpath)
            .build();
        final Set<DataNode> allChildDataNodes = new ImmutableSet.Builder<DataNode>()
            .addAll(parentDataNode.getChildDataNodes())
            .add(newChildDataNode)
            .build();
        parentDataNode.setChildDataNodes(allChildDataNodes);
        return newChildDataNode;
    }

}
