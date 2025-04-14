/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.init;

import static org.onap.cps.api.parameters.FetchDescendantsOption.DIRECT_CHILDREN_ONLY;
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DMI_REGISTRY_PARENT;

import com.hazelcast.map.IMap;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.ncmp.impl.inventory.CmHandleRegistrationService;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.utils.YangDataConverter;
import org.onap.cps.ncmp.utils.events.NcmpInventoryModelOnboardingFinishedEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlternateIdCacheDataLoader {

    private final InventoryPersistence inventoryPersistence;
    private final CmHandleRegistrationService cmHandleRegistrationService;

    @Qualifier("cmHandleIdPerAlternateId")
    private final IMap<String, String> cmHandleIdPerAlternateId;

    /**
     * Method to initialise the Alternate ID Cache by querying the current inventory.
     * This method is triggered by NcmpInventoryModelOnboardingFinishedEvent.
     *
     * @param event the event that triggers the initialization
     */
    @EventListener
    public void populateCmHandleIdPerAlternateIdMap(final NcmpInventoryModelOnboardingFinishedEvent event) {
        if (cmHandleIdPerAlternateId.isEmpty()) {
            log.info("Populating Alternate ID map from inventory");
            final Collection<DataNode> dataNodes = inventoryPersistence.getDataNode(
                    NCMP_DMI_REGISTRY_PARENT, DIRECT_CHILDREN_ONLY).iterator().next().getChildDataNodes();
            final Collection<YangModelCmHandle> yangModelCmHandles = dataNodes.stream()
                    .map(YangDataConverter::toYangModelCmHandle).toList();
            addAlternateIdsToCache(yangModelCmHandles);
        }
        log.info("Alternate ID map has {} entries", cmHandleIdPerAlternateId.size());
    }


    public void addAlternateIdsToCache(final Collection<YangModelCmHandle> yangModelCmHandles) {
        cmHandleRegistrationService.addAlternateIdsToCache(yangModelCmHandles);
    }

}
