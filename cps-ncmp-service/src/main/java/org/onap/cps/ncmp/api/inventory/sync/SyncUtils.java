/*
 * ============LICENSE_START=======================================================
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

import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NCMP_DATASPACE_NAME;
import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NCMP_DMI_REGISTRY_ANCHOR;
import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NCMP_DMI_REGISTRY_PARENT;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.ncmp.api.impl.operations.YangModelCmHandleRetriever;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.inventory.CmHandleState;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncUtils {

    private static final String UNSYNCHRONIZED_CM_HANDLES = "//state/datastores"
            + "/operational[@sync-state=\"UNSYNCHRONIZED\"]/ancestor::cm-handles";
    private static final String READY_CM_HANDLES = "//cm-handles[@id='%s']/state[@cm-handle-state=\"READY\"]";

    private static final SecureRandom secureRandom = new SecureRandom();
    private final CpsDataService cpsDataService;

    private final CpsDataPersistenceService cpsDataPersistenceService;

    private final JsonObjectMapper jsonObjectMapper;

    private final YangModelCmHandleRetriever yangModelCmHandleRetriever;

    /**
     * Query data nodes for cm handles with an "ADVISED" cm handle state, and select a random entry for processing.
     *
     * @return a random yang model cm handle with an ADVISED state, return null if not found
     */
    public YangModelCmHandle getAnAdvisedCmHandle() {
        final List<DataNode> advisedCmHandles = cpsDataPersistenceService.queryDataNodes("NCMP-Admin",
            "ncmp-dmi-registry", "//cm-handles[@state=\"ADVISED\"]",
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
     * First query data nodes for cm handles with CM Handle Operational Sync State in "UNSYNCHRONIZED" and
     * randomly select a CM Handle and query the data nodes for CM Handle State in "READY".
     *
     * @return a random yang model cm handle with State in READY and Operation Sync State in "UNSYNCHRONIZED",
     *         return null if not found
     */
    public YangModelCmHandle getUnSynchronizedReadyCmHandle() {
        final List<DataNode> unSynchronizedCmHandles = executeCpsQuery(UNSYNCHRONIZED_CM_HANDLES);
        if (unSynchronizedCmHandles.isEmpty()) {
            return null;
        }
        final int randomElementIndex = secureRandom.nextInt(unSynchronizedCmHandles.size());
        final String cmHandleId = unSynchronizedCmHandles.get(randomElementIndex).getLeaves()
                .get("id").toString();
        final List<DataNode> readyCmHandles = executeCpsQuery(String.format(READY_CM_HANDLES, cmHandleId));
        if (readyCmHandles.isEmpty()) {
            return null;
        }
        return yangModelCmHandleRetriever.getYangModelCmHandle(cmHandleId);
    }

    private List<DataNode> executeCpsQuery(final String cpsQuery) {
        return cpsDataPersistenceService.queryDataNodes(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                cpsQuery, FetchDescendantsOption.OMIT_DESCENDANTS);
    }

    /**
     * Update the Cm Handle state to "READY".
     *
     * @param yangModelCmHandle yang model cm handle
     * @param cmHandleState cm handle state
     */
    public void updateCmHandleState(final YangModelCmHandle yangModelCmHandle, final CmHandleState cmHandleState) {
        yangModelCmHandle.getCompositeState().setCmhandleState(cmHandleState);
        final String cmHandleJsonData = String.format("{\"cm-handles\":[%s]}",
            jsonObjectMapper.asJsonString(yangModelCmHandle));
        cpsDataService.updateNodeLeaves(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, NCMP_DMI_REGISTRY_PARENT,
                cmHandleJsonData, OffsetDateTime.now());
    }

    /**
     * Update the Cm Handle Operational datastore Syncstate to "SYNCHRONIZED".
     *
     * @param yangModelCmHandle yang model cm handle
     */
    public void updateCmHandleStateWithNodeLeaves(final YangModelCmHandle yangModelCmHandle) {

        final String cmHandleJsonData = String.format("{\"cm-handles\":[%s]}",
                jsonObjectMapper.asJsonString(yangModelCmHandle));
        cpsDataService.updateNodeLeavesAndExistingDescendantLeaves(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                NCMP_DMI_REGISTRY_PARENT, cmHandleJsonData, OffsetDateTime.now());
    }
}
