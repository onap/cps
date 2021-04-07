/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
 *  Modifications (C) 2021 Nordix Foundation
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
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.cps.spi.model.DataNode;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DataMapUtils {

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
                    .filter(dataNode -> isListNode(dataNode.getXpath()))
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

    private static String getNodeIdentifier(final String xpath) {
        final int fromIndex = xpath.lastIndexOf("/") + 1;
        final int toIndex = xpath.indexOf("[", fromIndex);
        return toIndex > 0 ? xpath.substring(fromIndex, toIndex) : xpath.substring(fromIndex);
    }

    private static boolean isContainerNode(final String xpath) {
        return !isListNode(xpath);
    }

    private static boolean isListNode(final String xpath) {
        return xpath.endsWith("]");
    }
}
