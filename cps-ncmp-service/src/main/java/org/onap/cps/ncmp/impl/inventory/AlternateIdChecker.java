/*
 * ============LICENSE_START========================================================
 * Copyright (c) 2024 Nordix Foundation.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.impl.inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.spi.exceptions.DataNodeNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlternateIdChecker {

    public enum Operation {
        CREATE, UPDATE
    }

    private final InventoryPersistence inventoryPersistence;

    private static final String NO_CURRENT_ALTERNATE_ID = "";

    /**
     * Check all alternate ids of a batch of cm handles.
     * Includes cross-checks in the batch itself for duplicates. Only the first entry encountered wil be accepted.
     *
     * @param newNcmpServiceCmHandles the proposed new cm handles
     * @param operation type of operation being executed
     * @return collection of cm handles ids which are acceptable
     */
    public Collection<String> getIdsOfCmHandlesWithRejectedAlternateId(
                                    final Collection<NcmpServiceCmHandle> newNcmpServiceCmHandles,
                                    final Operation operation) {
        final Set<String> assignedAlternateIds = getAlternateIdsAlreadyInDb(newNcmpServiceCmHandles);
        final Collection<String> rejectedCmHandleIds = new ArrayList<>();
        for (final NcmpServiceCmHandle ncmpServiceCmHandle : newNcmpServiceCmHandles) {
            final String cmHandleId = ncmpServiceCmHandle.getCmHandleId();
            final String proposedAlternateId = ncmpServiceCmHandle.getAlternateId();
            if (isProposedAlternateIdAcceptable(proposedAlternateId, operation, assignedAlternateIds, cmHandleId)) {
                assignedAlternateIds.add(proposedAlternateId);
            } else {
                rejectedCmHandleIds.add(cmHandleId);
            }
        }
        return rejectedCmHandleIds;
    }

    private boolean isProposedAlternateIdAcceptable(final String proposedAlternateId, final Operation operation,
                                                    final Set<String> assignedAlternateIds, final String cmHandleId) {
        if (StringUtils.isBlank(proposedAlternateId)) {
            return true;
        }
        final String currentAlternateId = getCurrentAlternateId(operation, cmHandleId);
        if (currentAlternateId.equals(proposedAlternateId)) {
            return true;
        }
        if (StringUtils.isNotBlank(currentAlternateId)) {
            log.warn("Alternate id update ignored, cannot update cm handle {}, already has an alternate id of {}",
                    cmHandleId, currentAlternateId);
            return false;
        }
        if (assignedAlternateIds.contains(proposedAlternateId)) {
            log.warn("Alternate id update ignored, cannot update cm handle {}, alternate id is already "
                    + "assigned to a different cm handle", cmHandleId);
            return false;
        }
        return true;
    }

    private Set<String> getAlternateIdsAlreadyInDb(final Collection<NcmpServiceCmHandle> ncmpServiceCmHandles) {
        final Set<String> alternateIdsToCheck = ncmpServiceCmHandles.stream()
                .map(NcmpServiceCmHandle::getAlternateId)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        return inventoryPersistence.getYangModelCmHandleByAlternateIds(alternateIdsToCheck).stream()
                .map(YangModelCmHandle::getAlternateId)
                .collect(Collectors.toSet());
    }

    private String getCurrentAlternateId(final Operation operation, final String cmHandleId) {
        if (operation == Operation.UPDATE) {
            try {
                return inventoryPersistence.getYangModelCmHandle(cmHandleId).getAlternateId();
            } catch (final DataNodeNotFoundException dataNodeNotFoundException) {
                // work with blank current alternate id
            }
        }
        return NO_CURRENT_ALTERNATE_ID;
    }

}
