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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.inventory.NetworkCmProxyInventoryFacade;
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle;
import org.onap.cps.ncmp.impl.inventory.CmHandleQueryService;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.impl.models.CmHandleStateUpdate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataMigration {

    public final InventoryPersistence inventoryPersistence;
    protected int batchSize = 300;
    private final CmHandleQueryService cmHandleQueryService;
    private final NetworkCmProxyInventoryFacade networkCmProxyInventoryFacade;


    /**
     * Migration of CompositeState CmHandleState into a new top level attribute.
     * One off migration job.
     */
    public void migrateInventoryToR20250722() {
        log.info("Inventory data migration started");
        final List<String> cmHandleIds = new ArrayList<>(cmHandleQueryService.getAllCmHandleReferences(false));
        log.info("Number of cm handles to process {}", cmHandleIds.size());
        final int totalCmHandleIds = cmHandleIds.size();
        for (int batchStart = 0; batchStart < totalCmHandleIds; batchStart += batchSize) {
            final int batchEnd = Math.min(batchStart + batchSize, cmHandleIds.size());
            final List<String> batchIds = cmHandleIds.subList(batchStart, batchEnd);
            try {
                migrateBatch(batchIds);
            } catch (final Exception e) {
                log.error("Failed to process batch starting at index {}", batchStart, e);
            }
        }
        log.info("Inventory Cm Handle data migration completed.");
    }

    private void migrateBatch(final List<String> cmHandleIds) {
        log.debug("Processing batch of {} Cm Handles", cmHandleIds.size());

        final Map<String, List<NcmpServiceCmHandle>> cmHandlesPerDmi = new HashMap<>();
        final List<CmHandleStateUpdate> stateUpdates = new ArrayList<>();

        for (final String cmHandleId : cmHandleIds) {
            try {
                final NcmpServiceCmHandle ncmpServiceCmHandle =
                        networkCmProxyInventoryFacade.getNcmpServiceCmHandle(cmHandleId);
                cmHandlesPerDmi.computeIfAbsent(ncmpServiceCmHandle.getDmiServiceName(),
                        dmiServiceName -> new ArrayList<>()).add(ncmpServiceCmHandle);
            } catch (final Exception e) {
                log.error("Failed to get CM handle {}", cmHandleId, e);
            }
        }

        cmHandlesPerDmi.forEach((dmiServiceName, ncmpServiceCmHandles) ->
            ncmpServiceCmHandles.forEach(cmHandle -> {
                final String valueFromOldModel = cmHandle.getCompositeState().getCmHandleState().name();
                try {
                    stateUpdates.add(new CmHandleStateUpdate(
                            cmHandle.getCmHandleId(),
                            valueFromOldModel
                    ));
                } catch (final Exception e) {
                    log.error("Failed to process CM handle {} state", cmHandle.getCmHandleId(), e);
                }
            })
        );
        try {
            if (!stateUpdates.isEmpty()) {
                inventoryPersistence.bulkUpdateCmHandleStates(stateUpdates);
                log.debug("Successfully updated {} Cm Handles", stateUpdates.size());
            }
        } catch (final Exception e) {
            log.error("Failed to perform bulk update for batch", e);
        }
    }
}

