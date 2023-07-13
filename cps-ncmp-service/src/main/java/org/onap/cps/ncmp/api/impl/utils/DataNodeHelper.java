/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.utils;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.cps.spi.model.DataNode;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DataNodeHelper {

    /**
     * The nested DataNode object is being flattened.
     *
     * @param dataNode object.
     * @return DataNode as stream.
     */
    public static Stream<DataNode> flatten(final DataNode dataNode) {
        return Stream.concat(Stream.of(dataNode),
                dataNode.getChildDataNodes().stream().flatMap(DataNodeHelper::flatten));
    }

    /**
     * The leaves for each DataNode is listed as map.
     *
     * @param dataNodes as collection
     * @return list of map for the all leaves
     */
    public static List<Map<String, Serializable>> getDataNodeLeaves(final Collection<DataNode> dataNodes) {
        return dataNodes.stream()
                .flatMap(DataNodeHelper::flatten)
                .map(DataNode::getLeaves)
                .collect(Collectors.toList());
    }

    /**
     * Extracts the mapping of cm handle id to status with details from nodes leaves.
     *
     * @param dataNodeLeaves as a list of map
     * @return cm handle id to status and details mapping
     */
    public static Map<String, Map<String, String>> cmHandleIdToStatusAndDetailsAsMap(
            final List<Map<String, Serializable>> dataNodeLeaves) {
        return dataNodeLeaves.stream()
                .filter(entryset -> entryset.values().contains("PENDING")
                        || entryset.values().contains("ACCEPTED")
                        || entryset.values().contains("REJECTED"))
                .collect(
                        HashMap<String, Map<String, String>>::new,
                        (result, entry) -> {
                            final String cmHandleId = (String) entry.get("cmHandleId");
                            final String status = (String) entry.get("status");
                            final String details = (String) entry.get("details");

                            if (cmHandleId != null && status != null) {
                                result.put(cmHandleId, new HashMap<String, String>());
                                result.get(cmHandleId).put("status", status);
                                result.get(cmHandleId).put("details", details == null ? "" : details);
                            }
                        },
                        HashMap::putAll
                );
    }

    /**
     * Extracts the mapping of cm handle id to status with details from data node collection.
     *
     * @param dataNodes as a collection
     * @return cm handle id to status and details mapping
     */
    public static Map<String, Map<String, String>> cmHandleIdToStatusAndDetailsAsMapFromDataNode(
            final Collection<DataNode> dataNodes) {
        return cmHandleIdToStatusAndDetailsAsMap(getDataNodeLeaves(dataNodes));
    }
}
