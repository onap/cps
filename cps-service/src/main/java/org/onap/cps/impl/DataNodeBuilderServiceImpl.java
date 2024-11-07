/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 TechMahindra Ltd.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.DataNodeBuilderService;
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
public class DataNodeBuilderServiceImpl implements DataNodeBuilderService {

    private static final String ROOT_NODE_XPATH = "/";
    private static final String PARENT_NODE_XPATH_FOR_ROOT_NODE_XPATH = "";
    private static final String NO_DATA_NODES = "No data nodes.";

    private final YangParser yangParser;

    /**
     * Build data nodes using a map of xpath to JSON/XML data, and anchor name.
     *
     * @param anchor        name of Anchor sharing same schema structure as the JSON/XML string
     * @param nodesData     map of xpath and node JSON/XML data
     * @param contentType   JSON or XML content type
     * @return              a collection of {@link DataNode}
     */
    @Override
    public Collection<DataNode> buildDataNodesWithAnchorAndXpathToNodeData(final Anchor anchor,
                                                                  final Map<String, String> nodesData,
                                                                  final ContentType contentType) {
        final Collection<DataNode> dataNodes = new ArrayList<>();
        for (final Map.Entry<String, String> nodeData : nodesData.entrySet()) {
            dataNodes.addAll(buildDataNodesWithAnchorParentXpathAndNodeData(anchor, nodeData.getKey(),
                    nodeData.getValue(), contentType));
        }
        return dataNodes;
    }

    /**
     * Build data nodes from a JSON or XML string using the associated anchor name and parent node xpath.
     *
     * @param anchor            name of Anchor sharing same schema structure as the JSON/XML string
     * @param parentNodeXpath   xpath to parent node of the first node in nodeData
     * @param nodeData          JSON or XML data string
     * @param contentType       JSON or XML content type
     * @return                  a collection of {@link DataNode}
     */
    @Override
    public Collection<DataNode> buildDataNodesWithAnchorParentXpathAndNodeData(final Anchor anchor,
                                                                               final String parentNodeXpath,
                                                                               final String nodeData,
                                                                               final ContentType contentType) {

        if (ROOT_NODE_XPATH.equals(parentNodeXpath)) {
            final ContainerNode containerNode = yangParser.parseData(contentType, nodeData,
                    anchor, PARENT_NODE_XPATH_FOR_ROOT_NODE_XPATH);
            final Collection<DataNode> dataNodes = new DataNodeBuilder()
                    .withContainerNode(containerNode)
                    .buildCollection();
            if (dataNodes.isEmpty()) {
                throw new DataValidationException(NO_DATA_NODES, "No data nodes provided");
            }
            return dataNodes;
        }
        final String normalizedParentNodeXpath = CpsPathUtil.getNormalizedXpath(parentNodeXpath);
        final ContainerNode containerNode =
                yangParser.parseData(contentType, nodeData, anchor, normalizedParentNodeXpath);
        final Collection<DataNode> dataNodes = new DataNodeBuilder()
                .withParentNodeXpath(normalizedParentNodeXpath)
                .withContainerNode(containerNode)
                .buildCollection();
        if (dataNodes.isEmpty()) {
            throw new DataValidationException(NO_DATA_NODES, "No data nodes provided");
        }
        return dataNodes;
    }

    /**
     * Build data nodes from a JSON or XML string using the associated anchor name and xpath.
     *
     * @param anchor        name of Anchor sharing same schema structure as the JSON/XML string
     * @param xpath         xpath
     * @param nodeData      JSON or XML data string
     * @param contentType   JSON or XML content type
     * @return              a collection of {@link DataNode}
     */
    @Override
    public Collection<DataNode> buildDataNodesWithAnchorXpathAndNodeData(final Anchor anchor, final String xpath,
                                                                 final String nodeData, final ContentType contentType) {

        if (!isRootNodeXpath(xpath)) {
            final String parentNodeXpath = CpsPathUtil.getNormalizedParentXpath(xpath).isEmpty() ? ROOT_NODE_XPATH
                    : CpsPathUtil.getNormalizedParentXpath(xpath);
            return buildDataNodesWithAnchorParentXpathAndNodeData(anchor, parentNodeXpath, nodeData, contentType);
        }
        return buildDataNodesWithAnchorParentXpathAndNodeData(anchor, xpath, nodeData, contentType);
    }

    /**
     * Build data nodes from a JSON or XML string using the associated schema context and xpath.
     *
     * @param yangResourcesNameToContentMap YANG resources (files) map where key is a name and value is content
     * @param xpath                         xpath
     * @param nodeData                      JSON or XML data string
     * @param contentType                   JSON or XML content type
     * @return                              a collection of {@link DataNode}
     */
    @Override
    public Collection<DataNode> buildDataNodesWithYangResourceXpathAndNodeData(
            final Map<String, String> yangResourcesNameToContentMap, final String xpath,
            final String nodeData, final ContentType contentType) {
        if (!isRootNodeXpath(xpath)) {
            final String parentNodeXpath = CpsPathUtil.getNormalizedParentXpath(xpath).isEmpty() ? ROOT_NODE_XPATH
                    : CpsPathUtil.getNormalizedParentXpath(xpath);
            return buildDataNodesWithYangResourceParentNodeXpathAndNodeData(yangResourcesNameToContentMap,
                    parentNodeXpath, nodeData, contentType);
        }
        return buildDataNodesWithYangResourceParentNodeXpathAndNodeData(yangResourcesNameToContentMap, xpath, nodeData,
                contentType);
    }

    private Collection<DataNode> buildDataNodesWithYangResourceParentNodeXpathAndNodeData(
        final Map<String, String> yangResourcesNameToContentMap, final String parentNodeXpath,
        final String nodeData, final ContentType contentType) {

        if (isRootNodeXpath(parentNodeXpath)) {
            final ContainerNode containerNode = yangParser.parseData(contentType, nodeData,
                yangResourcesNameToContentMap, PARENT_NODE_XPATH_FOR_ROOT_NODE_XPATH);
            final Collection<DataNode> dataNodes = new DataNodeBuilder()
                .withContainerNode(containerNode)
                .buildCollection();
            if (dataNodes.isEmpty()) {
                throw new DataValidationException(NO_DATA_NODES,
                    "Data nodes were not found under the xpath " + parentNodeXpath);
            }
            return dataNodes;
        }
        final String normalizedParentNodeXpath = CpsPathUtil.getNormalizedXpath(parentNodeXpath);
        final ContainerNode containerNode =
            yangParser.parseData(contentType, nodeData, yangResourcesNameToContentMap, normalizedParentNodeXpath);
        final Collection<DataNode> dataNodes = new DataNodeBuilder()
            .withParentNodeXpath(normalizedParentNodeXpath)
            .withContainerNode(containerNode)
            .buildCollection();
        if (dataNodes.isEmpty()) {
            throw new DataValidationException(NO_DATA_NODES,
                "Data nodes were not found under the xpath " + parentNodeXpath);
        }
        return dataNodes;
    }

    private static boolean isRootNodeXpath(final String xpath) {
        return ROOT_NODE_XPATH.equals(xpath);
    }
}
