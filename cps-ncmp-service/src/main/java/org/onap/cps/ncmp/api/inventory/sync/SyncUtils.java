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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.ncmp.api.impl.operations.YangModelCmHandleRetriever;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.inventory.CompositeState;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncUtils {

    private static final SecureRandom secureRandom = new SecureRandom();
    public static final String CM_HANDLE_ANCHOR = "ncmp-dmi-registry";
    private static final String CM_HANDLE_DATASPACE_NAME = "NCMP-Admin";

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
        final List<DataNode> advisedCmHandles = cpsDataPersistenceService.queryDataNodes(CM_HANDLE_DATASPACE_NAME,
            CM_HANDLE_ANCHOR, "//cm-handles[@state=\"ADVISED\"]",
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
     * Query data nodes for cm handles with an "LOCKED" cm handle state with reason LOCKED-MISBEHAVING".
     *
     * @return a random yang model cm handle with an ADVISED state, return null if not found
     */
    public List<YangModelCmHandle> getLockedMisbehavingCmHandles() {
        final List<DataNode> lockedCmHandleAsDataNodeList = cpsDataPersistenceService.queryDataNodes(
            CM_HANDLE_DATASPACE_NAME, CM_HANDLE_ANCHOR,
            "//lock-reason[@reason=\"LOCKED-MISBEHAVING\"]/ancestor::cm-handles",
            FetchDescendantsOption.OMIT_DESCENDANTS);
        final List<YangModelCmHandle> lockedCmHandleAsYangModelList = new ArrayList<>();
        for (final DataNode lockedCmHandleDataNode: lockedCmHandleAsDataNodeList) {
            final String cmHandleId = lockedCmHandleDataNode.getLeaves()
                .get("id").toString();
            lockedCmHandleAsYangModelList.add(yangModelCmHandleRetriever.getYangModelCmHandle(cmHandleId));
        }
        return lockedCmHandleAsYangModelList;
    }

    /**
     * Query UNSYNCHRONIZED cm Handles".
     *
     * @param cmHandlesAsList list of cm handles
     * @return a list of
     */
    public List<YangModelCmHandle> getUnSynchronizedCmHandles(final List<YangModelCmHandle> cmHandlesAsList) {
        return cmHandlesAsList.stream().filter(
            lockedCmHandleAsYangModel -> lockedCmHandleAsYangModel.getCompositeState().getDataStores()
                .getOperationalDataStore().getSyncState().equals("UNSYNCHRONIZED")).collect(Collectors.toList());
    }

    /**
     * Update the Cm Handle state to "READY".
     *
     * @param yangModelCmHandle yang model cm handle
     * @param compositeState cm handle state
     */
    public void updateCmHandleState(final YangModelCmHandle yangModelCmHandle, final CompositeState compositeState) {
        yangModelCmHandle.setCompositeState(compositeState);
        final String cmHandleJsonData = String.format("{\"cm-handles\":[%s]}",
            jsonObjectMapper.asJsonString(yangModelCmHandle));
        cpsDataService.updateNodeLeaves(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, NCMP_DMI_REGISTRY_PARENT,
            cmHandleJsonData, OffsetDateTime.now());
    }

}
