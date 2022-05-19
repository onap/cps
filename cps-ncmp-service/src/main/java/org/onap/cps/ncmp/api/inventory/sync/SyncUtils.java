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

package org.onap.cps.ncmp.api.inventory.sync;

import java.security.SecureRandom;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.operations.YangModelCmHandleRetriever;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.inventory.CmHandleState;
import org.onap.cps.ncmp.api.inventory.CompositeState;
import org.onap.cps.ncmp.api.inventory.LockReason;
import org.onap.cps.ncmp.api.inventory.sync.persistence.RegistryPersistence;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncUtils {

    private static final SecureRandom secureRandom = new SecureRandom();

    private final CpsDataPersistenceService cpsDataPersistenceService;

    private final RegistryPersistence registryPersistence;

    private final YangModelCmHandleRetriever yangModelCmHandleRetriever;

    /**
     * Query data nodes for cm handles with an "ADVISED" cm handle state, and select a random entry for processing.
     *
     * @return a random yang model cm handle with an ADVISED state, return null if not found
     */
    public YangModelCmHandle getAnAdvisedCmHandle() {
        final List<DataNode> advisedCmHandles = cpsDataPersistenceService.queryDataNodes("NCMP-Admin",
            "ncmp-dmi-registry", "//state[@cm-handle-state=\"ADVISED\"]/ancestor::cm-handles",
            FetchDescendantsOption.OMIT_DESCENDANTS);
        if (advisedCmHandles.isEmpty()) {
            return null;
        }
        final int randomElementIndex = secureRandom.nextInt(advisedCmHandles.size());
        final String cmHandleId = advisedCmHandles.get(randomElementIndex).getLeaves()
            .get("id").toString();
        return yangModelCmHandleRetriever.getYangModelCmHandle(cmHandleId);
    }

    /**
     * Update the Cm Handle state to "READY".
     *
     * @param yangModelCmHandle yang model cm handle
     */
    public void setCmHandleReadyState(final YangModelCmHandle yangModelCmHandle) {
        final CompositeState compositeState = new CompositeState();
        yangModelCmHandle.setCompositeState(compositeState);
        compositeState.setCmhandleState(CmHandleState.READY);
        registryPersistence.updateCmHandleState(compositeState.getCmhandleState(), yangModelCmHandle.getId());
    }

    /**
     * Set the Cm Handle state to "LOCKED".
     *
     * @param yangModelCmHandle yang model cm handle
     * @param reason lock reason enum
     * @param details lock reason details
     */
    public void lockCmHandleState(final YangModelCmHandle yangModelCmHandle,
                                  final LockReason reason,
                                  final String details) {
        final CompositeState compositeState = new CompositeState();
        yangModelCmHandle.setCompositeState(compositeState);
        compositeState.setCmhandleState(CmHandleState.LOCKED);
        registryPersistence.updateCmHandleState(compositeState.getCmhandleState(), yangModelCmHandle.getId());
        registryPersistence.saveLockReasonAndDetails(yangModelCmHandle.getId(), reason, details);
    }

}
