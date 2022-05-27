/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
 *  Modifications Copyright (C) 2022 Bell Canada
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

package org.onap.cps.ncmp.api.inventory.sync;

import java.security.SecureRandom;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.utils.YangDataConverter;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.inventory.CmHandleState;
import org.onap.cps.ncmp.api.inventory.CompositeState;
import org.onap.cps.ncmp.api.inventory.InventoryPersistence;
import org.onap.cps.ncmp.api.inventory.LockReasonCategory;
import org.onap.cps.spi.model.DataNode;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncUtils {

    private static final SecureRandom secureRandom = new SecureRandom();

    private final InventoryPersistence inventoryPersistence;

    private static final Pattern retryAttemptPattern = Pattern.compile("^Attempt #(\\d+) failed:");

    /**
     * Query data nodes for cm handles with an "ADVISED" cm handle state, and select a random entry for processing.
     *
     * @return a random yang model cm handle with an ADVISED state, return null if not found
     */
    public YangModelCmHandle getAnAdvisedCmHandle() {
        final List<DataNode> advisedCmHandles = inventoryPersistence.getCmHandlesByState(CmHandleState.ADVISED);
        if (advisedCmHandles.isEmpty()) {
            return null;
        }
        final int randomElementIndex = secureRandom.nextInt(advisedCmHandles.size());
        final String cmHandleId = advisedCmHandles.get(randomElementIndex).getLeaves()
            .get("id").toString();
        return inventoryPersistence.getYangModelCmHandle(cmHandleId);
    }


    /**
     * Query data nodes for cm handles with an "LOCKED" cm handle state with reason LOCKED_MISBEHAVING".
     *
     * @return a random yang model cm handle with an ADVISED state, return null if not found
     */
    public List<YangModelCmHandle> getLockedMisbehavingCmHandles() {
        final List<DataNode> lockedCmHandleAsDataNodeList = inventoryPersistence.getCmHandlesByCpsPath(
            "//lock-reason[@reason=\"LOCKED_MISBEHAVING\"]/ancestor::cm-handles");
        return lockedCmHandleAsDataNodeList.stream()
            .map(cmHandle -> YangDataConverter.convertCmHandleToYangModel(cmHandle,
                cmHandle.getLeaves().get("id").toString())).collect(Collectors.toList());
    }

    /**
     * Update Composite State attempts counter and set new lock reason and details.
     *
     * @param lockReasonCategory lock reason category
     * @param errorMessage       error message
     */
    public void updateLockReasonDetailsAndAttempts(final CompositeState compositeState,
                                                   final LockReasonCategory lockReasonCategory,
                                                   final String errorMessage) {
        int attempt = 1;
        if (compositeState.getLockReason() != null) {
            final Matcher matcher = retryAttemptPattern.matcher(compositeState.getLockReason().getDetails());
            if (matcher.find()) {
                attempt = 1 + Integer.parseInt(matcher.group(1));
            }
        }
        compositeState.setLockReason(CompositeState.LockReason.builder()
            .details(String.format("Attempt #%d failed: %s", attempt, errorMessage))
            .lockReasonCategory(lockReasonCategory).build());
    }

}
