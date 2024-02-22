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

package org.onap.cps.ncmp.api.impl.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.onap.cps.ncmp.api.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.cps.spi.exceptions.DataNodeNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlternateIdChecker {

    private final InventoryPersistence inventoryPersistence;

    private static final String NO_CURRENT_ALTERNATE_ID = "";

    /**
     * Check if the alternate can be applied to the given cm handle (id).
     * Conditions:
     * - proposed alternate is blank (it wil be ignored)
     * - proposed alternate is same as current (no change)
     * - proposed alternate is not in use for a different cm handle (in the DB)
     *
     * @param cmHandleId cm handle id
     * @param proposedAlternateId proposed alternate id
     * @return true if the new alternate id not in use or equal to current alternate id, false otherwise
     */
    public boolean canApplyAlternateId(final String cmHandleId, final String proposedAlternateId) {
        String currentAlternateId = "";
        try {
            final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(cmHandleId);
            currentAlternateId = yangModelCmHandle.getAlternateId();
        } catch (final DataNodeNotFoundException dataNodeNotFoundException) {
            // work with blank current alternate id
        }
        return this.canApplyAlternateId(cmHandleId, currentAlternateId, proposedAlternateId);
    }

    /**
     * Check if the alternate can be applied to the given cm handle.
     * Conditions:
     * - proposed alternate is blank (it wil be ignored)
     * - proposed alternate is same as current (no change)
     * - proposed alternate is not in use for a different cm handle (in the DB)
     *
     * @param cmHandleId   cm handle id
     * @param currentAlternateId current alternate id
     * @param proposedAlternateId new alternate id
     * @return true if the new alternate id not in use or equal to current alternate id, false otherwise
     */
    public boolean canApplyAlternateId(final String cmHandleId,
                                       final String currentAlternateId,
                                       final String proposedAlternateId) {
        if (StringUtils.isBlank(currentAlternateId)) {
            if (alternateIdAlreadyInDb(proposedAlternateId)) {
                log.warn("Alternate id update ignored, cannot update cm handle {}, alternate id is already "
                    + "assigned to a different cm handle", cmHandleId);
                return false;
            }
            return true;
        }
        if (currentAlternateId.equals(proposedAlternateId)) {
            return true;
        }
        log.warn("Alternate id update ignored, cannot update cm handle {}, already has an alternate id of {}",
            cmHandleId, currentAlternateId);
        return false;
    }

    /**
     * Check all alternate ids of a batch of NEW cm handles.
     * Includes cross-checks in the batch itself for duplicates. Only the first entry encountered wil be accepted.
     * This method can only be used for NEW cm handle registrations NOT for updating existing ones
     *
     * @param newNcmpServiceCmHandles the proposed new cm handles
     * @return collection of cm handles ids which are acceptable
     */
    public Collection<String> getIdsOfCmHandlesWithRejectedAlternateId(
                                    final Collection<NcmpServiceCmHandle> newNcmpServiceCmHandles) {
        final Set<String> acceptedAlternateIds = new HashSet<>(newNcmpServiceCmHandles.size());
        final Collection<String> rejectedCmHandleIds = new ArrayList<>();
        for (final NcmpServiceCmHandle ncmpServiceCmHandle : newNcmpServiceCmHandles) {
            final String cmHandleId = ncmpServiceCmHandle.getCmHandleId();
            final String proposedAlternateId = ncmpServiceCmHandle.getAlternateId();
            final boolean isAcceptable;
            if (StringUtils.isEmpty(proposedAlternateId)) {
                isAcceptable = true;
            } else {
                if (acceptedAlternateIds.contains(proposedAlternateId)) {
                    isAcceptable = false;
                    log.warn("Alternate id update ignored, cannot update cm handle {}, alternate id is already "
                        + "assigned to a different cm handle (in this batch)", cmHandleId);
                } else {
                    isAcceptable = canApplyAlternateId(cmHandleId, NO_CURRENT_ALTERNATE_ID, proposedAlternateId);
                }
            }
            if (isAcceptable) {
                acceptedAlternateIds.add(proposedAlternateId);
            } else {
                rejectedCmHandleIds.add(cmHandleId);
            }
        }
        return rejectedCmHandleIds;
    }

    private boolean alternateIdAlreadyInDb(final String alternateId) {
        try {
            inventoryPersistence.getCmHandleDataNodeByAlternateId(alternateId);
        } catch (final DataNodeNotFoundException dataNodeNotFoundException) {
            return false;
        }
        return true;
    }

}
