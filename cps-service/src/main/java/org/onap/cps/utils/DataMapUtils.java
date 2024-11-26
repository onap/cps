/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
 *  Modifications (C) 2021-2023 Nordix Foundation
 *  Modifications Copyright (C) 2022 Bell Canada
 *  Modifications Copyright (C) 2022-2023 TechMahindra Ltd.
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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toUnmodifiableList;
import static java.util.stream.Collectors.toUnmodifiableMap;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.cps.cpspath.parser.CpsPathQuery;
import org.onap.cps.cpspath.parser.CpsPathUtil;
import org.onap.cps.spi.api.model.DataNode;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DataMapUtils {

    /**
     * Converts DataNode structure into a map including the root node identifier for a JSON response.
     *
     * @param dataNode data node object
     * @return a map representing same data with the root node identifier
     */
    public static Map<String, Object> toDataMapWithIdentifier(final DataNode dataNode, final String prefix) {
        final String nodeIdentifierWithPrefix = getNodeIdentifierWithPrefix(dataNode.getXpath(), prefix);
        return ImmutableMap.<String, Object>builder().put(nodeIdentifierWithPrefix, toDataMap(dataNode)).build();
    }

    /**
     * Converts list of DataNode structure into a map including the root node identifier for a JSON response.
     * @param dataNodeList list of data nodes for a given anchor name
     * @param anchorName anchor name
     * @param prefix prefix
     * @return a map representing same list of data for given anchor with the root node identifier
     */
    public static Map<String, Object> toDataMapWithIdentifierAndAnchor(final List<DataNode> dataNodeList,
                                                                       final String anchorName, final String prefix) {
        final List<Map<String, Object>> dataMaps = toDataNodesWithIdentifier(dataNodeList, prefix);
        return ImmutableMap.<String, Object>builder().put("anchorName", anchorName)
                .put("dataNodes", dataMaps).build();
    }

    private static List<Map<String, Object>> toDataNodesWithIdentifier(final List<DataNode> dataNodeList,
                                                                       final String prefix) {
        final List<Map<String, Object>> dataMaps = new ArrayList<>(dataNodeList.size());
        for (final DataNode dataNode: dataNodeList) {
            final String nodeIdentifierWithPrefix = getNodeIdentifierWithPrefix(dataNode.getXpath(), prefix);
            final Map<String, Object> dataMap = ImmutableMap.<String, Object>builder()
                    .put(nodeIdentifierWithPrefix, toDataMap(dataNode)).build();
            dataMaps.add(dataMap);
        }
        return dataMaps;
    }

    /**
     * Converts DataNode structure into a map for a JSON response.
     *
     * @param dataNode data node object
     * @return a map representing same data
     */
    public static Map<String, Object> toDataMap(final DataNode dataNode) {
        return ImmutableMap.<String, Object>builder()
            .putAll(dataNode.getLeaves())
            .putAll(listElementsAsMap(dataNode.getChildDataNodes()))
            .putAll(containerElementsAsMap(dataNode.getChildDataNodes()))
            .build();
    }

    private static Map<String, Object> listElementsAsMap(final Collection<DataNode> dataNodes) {
        if (dataNodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return ImmutableMap.<String, Object>builder()
            .putAll(
                dataNodes.stream()
                    .filter(dataNode -> isListElement(dataNode.getXpath()))
                    .collect(groupingBy(
                        dataNode -> getNodeIdentifier(dataNode.getXpath()),
                        mapping(DataMapUtils::toDataMap, toUnmodifiableList())
                    ))
            ).build();
    }

    private static Map<String, Object> containerElementsAsMap(final Collection<DataNode> dataNodes) {
        if (dataNodes.isEmpty()) {
            return Collections.emptyMap();
        }
        return dataNodes.stream()
            .filter(dataNode -> isContainerNode(dataNode.getXpath()))
            .collect(
                toUnmodifiableMap(
                    dataNode -> getNodeIdentifier(dataNode.getXpath()),
                    DataMapUtils::toDataMap
                ));
    }

    private static String getNodeIdentifier(String xpath) {
        final CpsPathQuery cpsPathQuery = CpsPathUtil.getCpsPathQuery(xpath);
        if (cpsPathQuery.isPathToListElement()) {
            xpath = cpsPathQuery.getXpathPrefix();
        }
        final int fromIndex = xpath.lastIndexOf('/') + 1;
        return xpath.substring(fromIndex);
    }

    private static String getNodeIdentifierWithPrefix(final String xpath, final String moduleNamePrefix) {
        if (moduleNamePrefix != null) {
            return moduleNamePrefix + ":" + getNodeIdentifier(xpath);
        }
        return getNodeIdentifier(xpath);
    }

    private static boolean isContainerNode(final String xpath) {
        return !isListElement(xpath);
    }

    private static boolean isListElement(final String xpath) {
        return xpath.endsWith("]");
    }
}
