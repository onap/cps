/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Bell Canada. All rights reserved.
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2022-2025 OpenInfra Foundation Europe.
 *  Modifications Copyright (C) 2022-2023 Deutsche Telekom AG
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

package org.onap.cps.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.exceptions.DataValidationException;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.utils.XmlParsingContext;
import org.onap.cps.utils.XmlParsingContextHolder;
import org.onap.cps.utils.YangUtils;
import org.opendaylight.yangtools.yang.common.Ordering;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.NodeIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ChoiceNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.LeafSetNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapNode;
import org.opendaylight.yangtools.yang.data.api.schema.MixinNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.ValueNode;
import org.opendaylight.yangtools.yang.data.api.schema.builder.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.spi.node.ImmutableNodes;

@Slf4j
public class DataNodeBuilder {

    private ContainerNode containerNode;
    private String xpath;
    private String moduleNamePrefix;
    private String parentNodeXpath = "";
    private Map<String, Serializable> leaves = Collections.emptyMap();
    private Collection<DataNode> childDataNodes = Collections.emptySet();
    private String dataspaceName;
    private String anchorName;

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
     * To use {@link Collection} of Normalized Nodes for creating {@link DataNode}.
     *
     * @param containerNode used for creating the Data Node
     * @return this {@link DataNodeBuilder} object
     */
    public DataNodeBuilder withContainerNode(final ContainerNode containerNode) {
        this.containerNode = containerNode;
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
     * To use dataspace name for creating {@link DataNode}.
     *
     * @param dataspaceName dataspace name for the data node
     * @return DataNodeBuilder
     */
    public DataNodeBuilder withDataspace(final String dataspaceName) {
        this.dataspaceName = dataspaceName;
        return this;
    }

    /**
     * To use anchor name for creating {@link DataNode}.
     *
     * @param anchorName anchor name for the data node
     * @return DataNodeBuilder
     */
    public DataNodeBuilder withAnchor(final String anchorName) {
        this.anchorName = anchorName;
        return this;
    }

    /**
     * To use module name for prefix for creating {@link DataNode}.
     *
     * @param moduleNamePrefix module name as prefix
     * @return DataNodeBuilder
     */
    public DataNodeBuilder withModuleNamePrefix(final String moduleNamePrefix) {
        this.moduleNamePrefix = moduleNamePrefix;
        return this;
    }

    /**
     * To use attributes for creating {@link DataNode}.
     *
     * @param leaves for the data node
     * @return DataNodeBuilder
     */
    public DataNodeBuilder withLeaves(final Map<String, Serializable> leaves) {
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
        if (containerNode != null) {
            return buildFromContainerNode();
        }
        return buildFromAttributes();
    }

    /**
     * To build a {@link Collection} of {@link DataNode} objects.
     *
     * @return {@link DataNode} {@link Collection}
     */
    public Collection<DataNode> buildCollection() {
        if (containerNode != null) {
            return buildCollectionFromContainerNode();
        }
        return Collections.emptySet();
    }

    private DataNode buildFromAttributes() {
        final var dataNode = new DataNode();
        dataNode.setXpath(xpath);
        dataNode.setModuleNamePrefix(moduleNamePrefix);
        dataNode.setLeaves(leaves);
        dataNode.setChildDataNodes(childDataNodes);
        dataNode.setDataspace(dataspaceName);
        dataNode.setAnchorName(anchorName);
        return dataNode;
    }

    private DataNode buildFromContainerNode() {
        final Collection<DataNode> dataNodeCollection = buildCollectionFromContainerNode();
        if (dataNodeCollection.isEmpty()) {
            throw new DataValidationException("Unsupported Normalized Node", "No valid node found");
        }
        return dataNodeCollection.iterator().next();
    }

    private Collection<DataNode> buildCollectionFromContainerNode() {
        final DataNode parentDataNode = new DataNodeBuilder().withXpath(parentNodeXpath).build();
        final XmlParsingContext xmlContext = XmlParsingContextHolder.get();
        ContainerNode processedContainer = containerNode;
        if (xmlContext != null && xmlContext.isWrappedByXmlUtils()) {
            if (xmlContext.isHasParentXpath()) {
                if (isArtificialWrapper(containerNode, xmlContext)) {
                    processedContainer = unwrapContainer(containerNode);
                }
            } else {
                processedContainer = skipModuleNameWrapper(
                        containerNode, xmlContext.getParentXpath());
            }
        }
        for (final NormalizedNode normalizedNode : processedContainer.body()) {
            addDataNodeFromNormalizedNode(parentDataNode, normalizedNode);
        }
        if (xmlContext != null) {
            XmlParsingContextHolder.clear();
        }
        return parentDataNode.getChildDataNodes();
    }

    private static ContainerNode skipModuleNameWrapper(
            final ContainerNode containerNode, final String moduleName) {
        for (final NormalizedNode child : containerNode.body()) {
            if (child instanceof DataContainerNode dataContainerNode) {
                final String childLocalName =
                        dataContainerNode.name().getNodeType().getLocalName();
                if ("data".equals(childLocalName)) {
                    return skipModuleNameWrapper(
                            (ContainerNode) child, moduleName);
                }
                if (childLocalName.equals(moduleName)) {
                    final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> builder =
                            ImmutableNodes.builderFactory().newContainerBuilder()
                                    .withNodeIdentifier(containerNode.name());
                    for (final DataContainerChild grandChild
                            : dataContainerNode.body()) {
                        builder.withChild(grandChild);
                    }
                    return builder.build();
                }
            }
        }
        return containerNode;
    }

    private static boolean isArtificialWrapper(final ContainerNode node,
                                               final org.onap.cps.utils.XmlParsingContext context) {
        final Collection<?> nodeBody = node.body();
        if (nodeBody.isEmpty()) {
            return false;
        }
        if (nodeBody.size() != 1) {
            return false;
        }
        final Object firstChild = nodeBody.iterator().next();
        if (firstChild instanceof MapNode) {
            final MapNode mapNode = (MapNode) firstChild;
            final String mapNodeName = mapNode.name().getNodeType().getLocalName();
            final String expectedWrapperName = extractLastXpathSegment(context.getParentXpath());
            if (!mapNodeName.equals(expectedWrapperName)) {
                return false;
            }
            final Collection<MapEntryNode> mapEntries = mapNode.body();
            if (mapEntries.size() != 1) {
                return false;
            }
            final MapEntryNode mapEntryNode = mapEntries.iterator().next();
            return !mapEntryNode.body().isEmpty();
        }
        if (!(firstChild instanceof DataContainerNode)) {
            return false;
        }
        final DataContainerNode childContainer = (DataContainerNode) firstChild;
        final String childName = childContainer.name().getNodeType().getLocalName();
        final String expectedWrapperName = extractLastXpathSegment(context.getParentXpath());
        if (!childName.equals(expectedWrapperName)) {
            return false;
        }
        return !childContainer.body().isEmpty();
    }

    private static ContainerNode unwrapContainer(final ContainerNode wrappedContainer) {
        final Object firstChild = wrappedContainer.body().iterator().next();
        if (firstChild instanceof MapNode) {
            final MapNode mapNode = (MapNode) firstChild;
            final Collection<MapEntryNode> mapEntries = mapNode.body();
            if (mapEntries.size() == 1) {
                final MapEntryNode mapEntryNode = mapEntries.iterator().next();
                final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> unwrappedBuilder =
                       ImmutableNodes.builderFactory().newContainerBuilder()
                               .withNodeIdentifier(wrappedContainer.name());
                for (final DataContainerChild child : mapEntryNode.body()) {
                    unwrappedBuilder.withChild(child);
                }
                return unwrappedBuilder.build();
            }
        }
        if (firstChild instanceof DataContainerNode) {
            final DataContainerNode wrapperNode = (DataContainerNode) firstChild;
            final DataContainerNodeBuilder<NodeIdentifier, ContainerNode> unwrappedBuilder =
                    ImmutableNodes.builderFactory().newContainerBuilder().withNodeIdentifier(wrappedContainer.name());
            for (final DataContainerChild child : wrapperNode.body()) {
                unwrappedBuilder.withChild(child);
            }
            return unwrappedBuilder.build();
        }
        return wrappedContainer;
    }

    private static String extractLastXpathSegment(final String xpath) {
        if (xpath == null || xpath.isEmpty()) {
            return "";
        }
        final int lastSlash = xpath.lastIndexOf('/');
        if (lastSlash < 0) {
            return xpath;
        }
        final String lastSegment = xpath.substring(lastSlash + 1);
        final int bracketIndex = lastSegment.indexOf('[');
        if (bracketIndex > 0) {
            return lastSegment.substring(0, bracketIndex);
        }
        return lastSegment;
    }

    private static void addDataNodeFromNormalizedNode(final DataNode currentDataNode,
        final NormalizedNode normalizedNode) {

        if (normalizedNode instanceof ChoiceNode choiceNode) {
            addChoiceNode(currentDataNode, choiceNode);
        } else if (normalizedNode instanceof DataContainerNode dataContainerNode) {
            addYangContainer(currentDataNode, dataContainerNode);
        } else if (normalizedNode instanceof MapNode mapNode) {
            addDataNodeForEachListElement(currentDataNode, mapNode);
        } else if (normalizedNode instanceof ValueNode<?> valueNode) {
            addYangLeaf(currentDataNode, valueNode.name().getNodeType().getLocalName(),
                    (Serializable) valueNode.body());
        } else if (normalizedNode instanceof LeafSetNode<?> leafSetNode) {
            addYangLeafList(currentDataNode, leafSetNode);
        } else {
            log.warn("Unsupported NormalizedNode type detected: {}", normalizedNode.getClass());
        }
    }

    private static void addYangContainer(final DataNode currentDataNode, final DataContainerNode dataContainerNode) {
        final DataNode dataContainerDataNode =
            (dataContainerNode instanceof MixinNode)
                ? currentDataNode
                : createAndAddChildDataNode(currentDataNode, YangUtils.buildXpath(dataContainerNode.name()));
        final Collection<DataContainerChild> normalizedChildNodes = dataContainerNode.body();
        for (final NormalizedNode normalizedNode : normalizedChildNodes) {
            addDataNodeFromNormalizedNode(dataContainerDataNode, normalizedNode);
        }
    }

    private static void addYangLeaf(final DataNode currentDataNode, final String leafName,
                                    final Serializable leafValue) {
        final Map<String, Serializable> leaves = new ImmutableMap.Builder<String, Serializable>()
            .putAll(currentDataNode.getLeaves())
            .put(leafName, leafValue)
            .build();
        currentDataNode.setLeaves(leaves);
    }

    private static void addYangLeafList(final DataNode currentDataNode, final LeafSetNode<?> leafSetNode) {
        final String leafListName = leafSetNode.name().getNodeType().getLocalName();
        List<?> leafListValues = (leafSetNode.body())
                .stream()
                .map(NormalizedNode::body)
                .collect(Collectors.toList());
        if (leafSetNode.ordering() == Ordering.SYSTEM) {
            leafListValues.sort(null);
        } else {
            log.trace("Maintaining user order");
        }
        leafListValues = Collections.unmodifiableList(leafListValues);
        addYangLeaf(currentDataNode, leafListName, (Serializable) leafListValues);
    }

    private static void addDataNodeForEachListElement(final DataNode currentDataNode, final MapNode mapNode) {
        final Collection<MapEntryNode> mapEntryNodes = mapNode.body();
        for (final MapEntryNode mapEntryNode : mapEntryNodes) {
            addDataNodeFromNormalizedNode(currentDataNode, mapEntryNode);
        }
    }

    private static DataNode createAndAddChildDataNode(final DataNode parentDataNode, final String childXpath) {

        final var newChildDataNode = new DataNodeBuilder()
            .withXpath(parentDataNode.getXpath() + childXpath)
            .build();
        final Set<DataNode> allChildDataNodes = new ImmutableSet.Builder<DataNode>()
            .addAll(parentDataNode.getChildDataNodes())
            .add(newChildDataNode)
            .build();
        parentDataNode.setChildDataNodes(allChildDataNodes);
        return newChildDataNode;
    }

    private static void addChoiceNode(final DataNode currentDataNode, final ChoiceNode choiceNode) {

        final Collection<DataContainerChild> normalizedChildNodes = choiceNode.body();
        for (final NormalizedNode normalizedNode : normalizedChildNodes) {
            addDataNodeFromNormalizedNode(currentDataNode, normalizedNode);
        }
    }

}
