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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionStatus;
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
     * @param dataNodes as collection.
     * @return list of map for the all leaves.
     */
    public static List<Map<String, Serializable>> getDataNodeLeaves(final Collection<DataNode> dataNodes) {
        return dataNodes.stream()
                .flatMap(DataNodeHelper::flatten)
                .map(DataNode::getLeaves)
                .collect(Collectors.toList());
    }

    /**
     * The cm handle and status is listed as a collection.
     *
     * @param dataNodeLeaves as a list of map.
     * @return list of collection containing cm handle id and statuses.
     */
    public static List<Collection<Serializable>> getCmHandleIdToStatus(
            final List<Map<String, Serializable>> dataNodeLeaves) {
        return dataNodeLeaves.stream()
                .map(Map::values)
                .filter(col -> col.contains("PENDING")
                        || col.contains("ACCEPTED")
                        || col.contains("REJECTED"))
                .collect(Collectors.toList());
    }

    /**
     * The cm handle and status is returned as a map.
     *
     * @param cmHandleIdToStatus as a list of collection
     * @return a map of cm handle id to status
     */
    public static Map<String, SubscriptionStatus> getCmHandleIdToStatusMap(
            final List<Collection<Serializable>> cmHandleIdToStatus) {
        final Map<String, SubscriptionStatus> resultMap = new HashMap<>();
        for (final Collection<Serializable> cmHandleToStatusBucket: cmHandleIdToStatus) {
            final Iterator<Serializable> bucketIterator = cmHandleToStatusBucket.iterator();
            while (bucketIterator.hasNext()) {
                SubscriptionStatus.populateCmHandleToSubscriptionStatusMap(resultMap, bucketIterator);
            }
        }
        return resultMap;
    }
}
