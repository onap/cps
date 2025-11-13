/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2025 OpenInfra Foundation Europe. All rights reserved.
 *  Modifications Copyright (C) 2023 TechMahindra Ltd.
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
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryDataMigration {

    public final InventoryPersistence inventoryPersistence;
    protected int batchSize = 300;
    private final CmHandleQueryService cmHandleQueryService;
    private final NetworkCmProxyInventoryFacade networkCmProxyInventoryFacade;


    /**
     * Migration.
     */
    public void migrateData() {
        log.info("Inventory data migration started");

        final List<String> cmHandleIds = new ArrayList<>(cmHandleQueryService.getAllCmHandleReferences(false));
        log.info("CM handles to process {}", cmHandleIds.size());

        for (int i = 0; i < cmHandleIds.size(); i += batchSize) {
            final int end = Math.min(i + batchSize, cmHandleIds.size());
            final List<String> batchIds = cmHandleIds.subList(i, end);
            try {
                migrateBatch(batchIds);
            } catch (final Exception e) {
                log.error("Failed to process batch starting at index {}", i, e);
            }
        }

        log.info("Inventory Cm Handle data migration completed.");
    }

    private void migrateBatch(final List<String> cmHandleIds) {
        log.debug("Processing batch of {} Cm Handles", cmHandleIds.size());

        final Map<String, List<NcmpServiceCmHandle>> grouped = new HashMap<>();

        for (final String cmHandleId : cmHandleIds) {
            try {
                final NcmpServiceCmHandle handle = networkCmProxyInventoryFacade.getNcmpServiceCmHandle(cmHandleId);
                grouped.computeIfAbsent(handle.getDmiServiceName(), k -> new ArrayList<>()).add(handle);
            } catch (final Exception e) {
                log.error("Failed to get CM handle {}", cmHandleId, e);
            }
        }

        grouped.forEach((dmiServiceName, handles) -> handles.forEach(handle -> {
            try {
                inventoryPersistence.setAndUpdateCmHandleField(
                        handle.getCmHandleId(),
                        "cm-handle-state",
                        handle.getCompositeState().getCmHandleState().name());
            } catch (final Exception e) {
                log.error("Failed to persist CM handle {}", handle.getCmHandleId(), e);
            }
        }));
    }
}

