/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.ncmp.api.inventory;

import static org.onap.cps.ncmp.api.impl.utils.YangDataConverter.convertYangModelCmHandleToNcmpServiceCmHandle;
import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.impl.utils.YangDataConverter;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.model.DataNode;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class InventoryQuery {

    private final CpsDataPersistenceService cpsDataPersistenceService;
    private static final Map<String, NcmpServiceCmHandle> NO_QUERY_TO_EXECUTE = null;
    private static final String NCMP_DATASPACE_NAME = "NCMP-Admin";
    private static final String NCMP_DMI_REGISTRY_ANCHOR = "ncmp-dmi-registry";

    /**
     * Query CmHandles based on PublicProperties.
     *
     * @param publicPropertyQueryPairs public properties for query
     * @return CmHandles which have these public properties
     */
    public Map<String, NcmpServiceCmHandle> queryCmHandlePublicProperties(
        final Map<String, String> publicPropertyQueryPairs) {
        if (publicPropertyQueryPairs.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, NcmpServiceCmHandle> cmHandleIdToNcmpServiceCmHandles = null;
        for (final Map.Entry<String, String> entry : publicPropertyQueryPairs.entrySet()) {
            final String cpsPath = "//public-properties[@name='" + entry.getKey() + "' and @value='"
                + entry.getValue() + "']/ancestor::cm-handles";

            final Collection<DataNode> dataNodes = queryDataNodes(cpsPath);
            if (cmHandleIdToNcmpServiceCmHandles == null) {
                cmHandleIdToNcmpServiceCmHandles = collectDataNodesToNcmpServiceCmHandles(dataNodes);
            } else {
                final Collection<String> cmHandleIdsToRetain = dataNodes.parallelStream()
                    .map(dataNode -> dataNode.getLeaves().get("id").toString()).collect(Collectors.toSet());
                cmHandleIdToNcmpServiceCmHandles.keySet().retainAll(cmHandleIdsToRetain);
            }
            if (cmHandleIdToNcmpServiceCmHandles.isEmpty()) {
                break;
            }
        }
        return cmHandleIdToNcmpServiceCmHandles;
    }

    /**
     * Combine Maps of CmHandles.
     *
     * @param firstQuery first CmHandles Map
     * @param secondQuery second CmHandles Map
     * @return combined Map of CmHandles
     */
    public Map<String, NcmpServiceCmHandle> combineCmHandleQueries(
        final Map<String, NcmpServiceCmHandle> firstQuery,
        final Map<String, NcmpServiceCmHandle> secondQuery) {
        if (firstQuery == NO_QUERY_TO_EXECUTE && secondQuery == NO_QUERY_TO_EXECUTE) {
            return new HashMap<>();
        } else if (firstQuery == NO_QUERY_TO_EXECUTE) {
            return secondQuery;
        } else if (secondQuery == NO_QUERY_TO_EXECUTE) {
            return firstQuery;
        } else {
            firstQuery.keySet().retainAll(secondQuery.keySet());
            return firstQuery;
        }
    }


    private List<DataNode> queryDataNodes(final String cpsPath) {
        return cpsDataPersistenceService.queryDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
            cpsPath, INCLUDE_ALL_DESCENDANTS);
    }

    private Map<String, NcmpServiceCmHandle> collectDataNodesToNcmpServiceCmHandles(
        final Collection<DataNode> dataNodes) {
        final Map<String, NcmpServiceCmHandle> cmHandleIdToNcmpServiceCmHandle = new HashMap<>();
        dataNodes.forEach(dataNode -> {
            final NcmpServiceCmHandle ncmpServiceCmHandle = createNcmpServiceCmHandle(dataNode);
            cmHandleIdToNcmpServiceCmHandle.put(ncmpServiceCmHandle.getCmHandleId(), ncmpServiceCmHandle);
        });
        return cmHandleIdToNcmpServiceCmHandle;
    }

    private NcmpServiceCmHandle createNcmpServiceCmHandle(final DataNode dataNode) {
        return convertYangModelCmHandleToNcmpServiceCmHandle(YangDataConverter
            .convertCmHandleToYangModel(dataNode, dataNode.getLeaves().get("id").toString()));
    }
}


