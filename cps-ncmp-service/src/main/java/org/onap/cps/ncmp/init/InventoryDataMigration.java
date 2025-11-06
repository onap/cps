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
import org.onap.cps.ncmp.api.inventory.models.CmHandleRegistrationResponse;
import org.onap.cps.ncmp.api.inventory.models.CompositeState;
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistration;
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistrationResponse;
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle;
import org.onap.cps.ncmp.impl.inventory.CmHandleQueryService;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryDataMigration {
    private static final int BATCH_SIZE = 300;
    private final CmHandleQueryService cmHandleQueryService;
    private final NetworkCmProxyInventoryFacade networkCmProxyInventoryFacade;

    /**
     * Migration.
     */
    public void migrate() {
        log.info("Starting Inventory data migration...");

        final List<String> cmHandleIds = new ArrayList<>(cmHandleQueryService.getAllCmHandleReferences(false));
        log.info("CM handles to process {}", cmHandleIds.size());

        for (int i = 0; i < cmHandleIds.size(); i += BATCH_SIZE) {
            final int end = Math.min(i + BATCH_SIZE, cmHandleIds.size());
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
                if (!isValidHandle(handle, cmHandleId)) {
                    continue;
                }
                grouped.computeIfAbsent(handle.getDmiServiceName(), k -> new ArrayList<>()).add(handle);
            } catch (final Exception e) {
                log.error("Failed to process cmHandle {}", cmHandleId, e);
            }
        }

        // Process each DMI service group
        grouped.forEach((dmiServiceName, handles) -> {
            try {
                log.debug("Processing {} handles for DMI service {}", handles.size(), dmiServiceName);

                final DmiPluginRegistration registration = new DmiPluginRegistration();
                registration.setDmiPlugin(dmiServiceName);
                registration.setUpdatedCmHandles(handles);

                final DmiPluginRegistrationResponse response =
                        networkCmProxyInventoryFacade.updateDmiRegistration(registration);
                logUpdateResults(response, dmiServiceName);
            } catch (final Exception e) {
                log.error("Failed to update batch for DMI service {} with {} handles",
                        dmiServiceName, handles.size(), e);
            }
        });
    }

    private boolean isValidHandle(final NcmpServiceCmHandle handle, final String cmHandleId) {
        if (handle == null) {
            log.warn("Could not retrieve handle {}", cmHandleId);
            return false;
        }

        final CompositeState state = networkCmProxyInventoryFacade.getCmHandleCompositeState(cmHandleId);
        if (state == null || state.getCmHandleState() == null) {
            log.warn("No valid state found for handle {}", cmHandleId);
            return false;
        }

        handle.setCmHandleStatus(state.getCmHandleState().name());
        log.debug("Prepared handle {} for update with status {}", cmHandleId, handle.getCmHandleStatus());

        final String dmiServiceName = handle.getDmiServiceName();
        if (isBlank(dmiServiceName)) {
            log.warn("No DMI service name found for handle {}", cmHandleId);
            return false;
        }

        return true;
    }

    private boolean isBlank(final String str) {
        return str == null || str.trim().isEmpty();
    }

    private void logUpdateResults(final DmiPluginRegistrationResponse response, final String dmiServiceName) {
        if (response.getUpdatedCmHandles() != null) {
            final int successCount = (int) response.getUpdatedCmHandles().stream()
                    .filter(handle -> handle.getStatus() == CmHandleRegistrationResponse.Status.SUCCESS)
                    .count();
            final int failureCount = response.getUpdatedCmHandles().size() - successCount;

            if (failureCount > 0) {
                log.warn("Batch update for DMI service {} completed with {} successes and {} failures",
                        dmiServiceName, successCount, failureCount);
            } else {
                log.debug("Batch update completed successfully for DMI service {} with {} handles",
                        dmiServiceName, successCount);
            }
        }
    }
}
