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

package org.onap.cps.ncmp.api.impl;

import static org.onap.cps.ncmp.api.impl.NetworkCmProxyDataServiceImpl.NCMP_DATASPACE_NAME;
import static org.onap.cps.ncmp.api.impl.NetworkCmProxyDataServiceImpl.NCMP_DMI_REGISTRY_ANCHOR;
import static org.onap.cps.ncmp.api.impl.NetworkCmProxyDataServiceImpl.NCMP_DMI_REGISTRY_PARENT;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.ncmp.api.models.CmHandle;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.exceptions.DataNodeNotFoundException;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.DataNodeBuilder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NetworkCmProxyDataServicePropertyHandler {

    private final CpsDataService cpsDataService;

    /**
     * Returns collection of updated dataNodes with updated leaves based on input updated CmHandle DMI and Public
     * property updates.
     *
     * @param cmHandles collection of cmHandles
     * @return Collection of updated dataNodes
     */
    public Collection<DataNode> updateDataNodeLeaves(final Collection<CmHandle> cmHandles) {

        final Collection<DataNode> updatedDataNodes = new ArrayList<>();

        cmHandles.forEach(cmHandle -> {
            try {
                final String targetXpath =
                        NCMP_DMI_REGISTRY_PARENT + "/cm-handles[@id='" + cmHandle.getCmHandleID() + "']";
                final DataNode existingDataNode =
                        cpsDataService.getDataNode(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, targetXpath,
                                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS);
                final DataNode updatedDataNode =
                        updateLeaves(existingDataNode, cmHandle.getDmiProperties(), cmHandle.getPublicProperties());
                updatedDataNodes.add(updatedDataNode);
            } catch (final DataNodeNotFoundException e) {
                log.error("Unable to find dataNode for cmHandleId {} with cause {}", cmHandle.getCmHandleID(),
                        e.getMessage());
                throw e;
            }
        });
        return updatedDataNodes;
    }

    private DataNode updateLeaves(final DataNode dataNode, final Map<String, String> updatedDmiProperties,
            final Map<String, String> updatedPublicProperties) {

        final Map<String, Object> updatedLeaves = new HashMap<>(dataNode.getLeaves());
        updatedDmiProperties.forEach((updatedAttributeKey, updatedAttributeValue) -> {
            updateLeaf(updatedLeaves, updatedAttributeKey, updatedAttributeValue);
        });

        updatedPublicProperties.forEach((updatedAttributeKey, updatedAttributeValue) -> {
            updateLeaf(updatedLeaves, updatedAttributeKey, updatedAttributeValue);
        });

        return buildDataNodeWithUpdatedLeaves(dataNode, updatedLeaves);
    }

    private void updateLeaf(final Map<String, Object> leaves, final String updatedAttributeKey,
            final String updatedAttributeValue) {
        if (leaves.containsKey(updatedAttributeKey)) {
            if (updatedAttributeValue == null) {
                log.info("Removing the attribute with ( key : {} , existingValue : {} )", updatedAttributeKey,
                        leaves.get(updatedAttributeKey));
                leaves.remove(updatedAttributeKey);
            } else {
                log.info("Updating the attribute with ( key : {} , existingValue : {} to newValue : {} )",
                        updatedAttributeKey, leaves.get(updatedAttributeKey), updatedAttributeValue);
                leaves.put(updatedAttributeKey, updatedAttributeValue);
            }
        } else {
            if (updatedAttributeValue == null) {
                log.info("Ignoring the attribute with ( key : {} ) as its value is null", updatedAttributeKey);
            } else {
                log.info("Adding the attribute with ( key : {} , value : {} )", updatedAttributeKey,
                        updatedAttributeValue);
                leaves.put(updatedAttributeKey, updatedAttributeValue);
            }
        }
    }

    private DataNode buildDataNodeWithUpdatedLeaves(final DataNode existingDataNode,
            final Map<String, Object> updatedLeaves) {
        return new DataNodeBuilder().withXpath(existingDataNode.getXpath())
                .withChildDataNodes(existingDataNode.getChildDataNodes()).withLeaves(ImmutableMap.copyOf(updatedLeaves))
                .build();
    }
}
