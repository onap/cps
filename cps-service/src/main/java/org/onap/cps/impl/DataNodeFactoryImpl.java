/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Deutsche Telekom AG
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

import static org.onap.cps.cpspath.parser.CpsPathUtil.NO_PARENT_PATH;
import static org.onap.cps.cpspath.parser.CpsPathUtil.ROOT_NODE_XPATH;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.DataNodeFactory;
import org.onap.cps.api.exceptions.DataValidationException;
import org.onap.cps.api.model.Anchor;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.cpspath.parser.CpsPathUtil;
import org.onap.cps.utils.ContentType;
import org.onap.cps.utils.YangParser;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DataNodeFactoryImpl implements DataNodeFactory {

    private final YangParser yangParser;

    @Override
    public Collection<DataNode> createDataNodesWithAnchorAndXpathToNodeData(final Anchor anchor,
                                                               final Map<String, String> nodesDataPerParentNodeXpath,
                                                               final ContentType contentType) {
        final Collection<DataNode> dataNodes = new ArrayList<>();
        for (final Map.Entry<String, String> nodeDataToParentNodeXpath : nodesDataPerParentNodeXpath.entrySet()) {
            dataNodes.addAll(createDataNodesWithAnchorParentXpathAndNodeData(anchor, nodeDataToParentNodeXpath.getKey(),
                nodeDataToParentNodeXpath.getValue(), contentType));
        }
        return dataNodes;
    }

    @Override
    public Collection<DataNode> createDataNodesWithAnchorXpathAndNodeData(final Anchor anchor, final String xpath,
                                                                          final String nodeData,
                                                                          final ContentType contentType) {
        final String xpathToBuildNodes = isRootNodeXpath(xpath) ? NO_PARENT_PATH :
            CpsPathUtil.getNormalizedParentXpath(xpath);
        final ContainerNode containerNode = yangParser.parseData(contentType, nodeData, anchor, xpathToBuildNodes);
        return convertToDataNodes(xpathToBuildNodes, containerNode);
    }

    @Override
    public Collection<DataNode> createDataNodesWithAnchorParentXpathAndNodeData(final Anchor anchor,
                                                                                final String parentNodeXpath,
                                                                                final String nodeData,
                                                                                final ContentType contentType) {

        final String normalizedParentNodeXpath = CpsPathUtil.getNormalizedXpath(parentNodeXpath);
        final ContainerNode containerNode =
            yangParser.parseData(contentType, nodeData, anchor, normalizedParentNodeXpath);
        return convertToDataNodes(normalizedParentNodeXpath, containerNode);
    }

    @Override
    public Collection<DataNode> createDataNodesWithYangResourceXpathAndNodeData(
                                                                final Map<String, String> yangResourceContentPerName,
                                                                final String xpath, final String nodeData,
                                                                final ContentType contentType) {
        final String normalizedParentNodeXpath = isRootNodeXpath(xpath) ? NO_PARENT_PATH :
            CpsPathUtil.getNormalizedParentXpath(xpath);
        final ContainerNode containerNode =
            yangParser.parseData(contentType, nodeData, yangResourceContentPerName, normalizedParentNodeXpath);
        return convertToDataNodes(normalizedParentNodeXpath, containerNode);
    }

    private static Collection<DataNode> convertToDataNodes(final String normalizedParentNodeXpath,
                                                           final ContainerNode containerNode) {
        final Collection<DataNode> dataNodes = new DataNodeBuilder()
            .withParentNodeXpath(normalizedParentNodeXpath)
            .withContainerNode(containerNode)
            .buildCollection();
        if (dataNodes.isEmpty()) {
            throw new DataValidationException("No Data Nodes", "The request did not return any data nodes for xpath "
                + normalizedParentNodeXpath);
        }
        return dataNodes;
    }

    private static boolean isRootNodeXpath(final String xpath) {
        return ROOT_NODE_XPATH.equals(xpath);
    }
}
