/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Nordix Foundation.
 *  Modifications Copyright (C) 2025 Deutsche Telekom AG
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

package org.onap.cps.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.model.Anchor;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.impl.DataNodeBuilder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DataMapper {

    private final CpsAnchorService cpsAnchorService;
    private final PrefixResolver prefixResolver;

    /**
     * Convert a data node to a data map.
     *
     * @param dataspaceName the name of the dataspace
     * @param anchorName    the name of the anchor
     * @param dataNode      the data node to convert
     * @return the data node represented as a map of key value pairs
     */
    public Map<String, Object> toDataMap(final String dataspaceName, final String anchorName, final DataNode dataNode) {
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        final String prefix = prefixResolver.getPrefix(anchor, dataNode.getXpath());
        return DataMapUtils.toDataMapWithIdentifier(dataNode, prefix);
    }

    /**
     * Convert a collection of data nodes to a list of data maps.
     *
     * @param dataspaceName the name dataspace name
     * @param anchorName    the name of the anchor
     * @param dataNodes     the data nodes to convert
     * @return a list of maps representing the data nodes
     */
    public List<Map<String, Object>> toDataMaps(final String dataspaceName, final String anchorName,
                                                final Collection<DataNode> dataNodes) {
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        return toDataMaps(anchor, dataNodes);
    }

    /**
     * Convert a collection of data nodes to a list of data maps.
     *
     * @param anchor        the anchor
     * @param dataNodes     the data nodes to convert
     * @return a list of maps representing the data nodes
     */
    public List<Map<String, Object>> toDataMaps(final Anchor anchor, final Collection<DataNode> dataNodes) {
        final List<Map<String, Object>> dataMaps = new ArrayList<>(dataNodes.size());
        for (final DataNode dataNode : dataNodes) {
            final String prefix = prefixResolver.getPrefix(anchor, dataNode.getXpath());
            final Map<String, Object> dataMap = DataMapUtils.toDataMapWithIdentifier(dataNode, prefix);
            dataMaps.add(dataMap);
        }
        return dataMaps;
    }

    /**
     * Convert a collection of data nodes (belonging to multiple anchors) to a list of data maps.
     *
     * @param dataspaceName the name dataspace name
     * @param dataNodes     the data nodes to convert
     * @return a list of maps representing the data nodes
     */
    public List<Map<String, Object>> toDataMaps(final String dataspaceName, final Collection<DataNode> dataNodes) {
        final List<Map<String, Object>> dataNodesAsMaps = new ArrayList<>(dataNodes.size());
        final Map<String, List<DataNode>> dataNodesPerAnchor = groupDataNodesPerAnchor(dataNodes);
        for (final Map.Entry<String, List<DataNode>> dataNodesPerAnchorEntry : dataNodesPerAnchor.entrySet()) {
            final String anchorName = dataNodesPerAnchorEntry.getKey();
            final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
            final DataNode dataNode = dataNodesPerAnchorEntry.getValue().get(0);
            final String prefix = prefixResolver.getPrefix(anchor, dataNode.getXpath());
            final Map<String, Object> dataNodeAsMap = DataMapUtils.toDataMapWithIdentifierAndAnchor(
                dataNodesPerAnchorEntry.getValue(), anchorName, prefix);
            dataNodesAsMaps.add(dataNodeAsMap);
        }
        return dataNodesAsMaps;
    }

    /**
     * Convert a collection of data nodes to a list of data maps.
     * List nodes are returned as a map entry where the key is the list node name and the value is
     * a list of maps, each representing an individual list item. Container nodes are returned as
     * nested maps under their respective parent node names.
     *
     * @param dataspaceName the name dataspace name
     * @param anchorName    the name of the anchor
     * @param dataNodes     the data nodes to convert
     * @return a map reflecting the complete data node structure, where:
     *                - leaf values are returned as key-value pairs,
     *                - containers are returned as nested maps,
     *                - and list nodes are grouped under a single key as a list of map entries.
     */

    public Map<String, Object> toDataMapForApiV3(final String dataspaceName, final String anchorName,
                                                 final Collection<DataNode> dataNodes) {
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        dataNodes.forEach(dataNode ->
            dataNode.setModuleNamePrefix(prefixResolver.getPrefix(anchor, dataNode.getXpath())));
        final DataNode containerNode = new DataNodeBuilder().withChildDataNodes(dataNodes).build();
        return DataMapUtils.toDataMap(containerNode);
    }

    /**
     * Converts list of attributes values to a list of data maps.
     * @param attributeName   attribute name
     * @param attributeValues attribute values
     * @return a list of maps representing the attribute values
     */
    public List<Map<String, Object>> toAttributeMaps(final String attributeName,
                                                     final Collection<Object> attributeValues) {
        return attributeValues.stream().map(attributeValue -> Map.of(attributeName, attributeValue)).toList();
    }

    /**
     * Convert a collection of data nodes to a data map.
     *
     * @param anchor        the anchor
     * @param dataNodes     the data nodes to convert
     * @return a map representing the data nodes
     */
    public Map<String, Object> toFlatDataMap(final Anchor anchor, final Collection<DataNode> dataNodes) {
        final List<Map<String, Object>> dataNodesAsMaps = toDataMaps(anchor, dataNodes);
        return flattenDataNodesMaps(dataNodesAsMaps);
    }

    private Map<String, Object> flattenDataNodesMaps(final List<Map<String, Object>> dataNodesAsMaps) {
        final Map<String, Object> dataNodesAsFlatMap = new HashMap<>();
        for (final Map<String, Object> dataNodeAsMap : dataNodesAsMaps) {
            dataNodesAsFlatMap.putAll(dataNodeAsMap);
        }
        return dataNodesAsFlatMap;
    }

    private static Map<String, List<DataNode>> groupDataNodesPerAnchor(final Collection<DataNode> dataNodes) {
        return dataNodes.stream().collect(Collectors.groupingBy(DataNode::getAnchorName));
    }

}
