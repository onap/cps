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
import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NO_TIMESTAMP;

import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.ncmp.api.impl.operations.YangModelCmHandleRetriever;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableScheduling
public class SyncUtils {

    private final CpsDataService cpsDataService;

    private final CpsDataPersistenceService cpsDataPersistenceService;

    private final JsonObjectMapper jsonObjectMapper;

    private final YangModelCmHandleRetriever yangModelCmHandleRetriever;

    /**
     * Schedule Job which syncs cm-handle and changes the state from 'ADVISED' to 'READY'.
     */
    @Scheduled(fixedDelay = 30000)
    public void scheduleCmHandleSync() {
        YangModelCmHandle newAdvisedCmHandle = getAdvisedCmHandle();
        while (newAdvisedCmHandle != null) {
            // ToDo When Cm-Handle in the 'ADVISED' state is Retrieved, Set CM-Handle state to 'LOCKED'
            //  and give lock reason
            // ToDo if lock fails, move to next cm handle.
            // ToDo Update last update time with a timestamp everytime Cm-handle state is changed
            final String cmHandleJsonData = updatedCmHandlesAsJson(newAdvisedCmHandle);
            updateCmHandleState(cmHandleJsonData);
            log.debug("{} is now in READY state", newAdvisedCmHandle.getId());
            newAdvisedCmHandle = getAdvisedCmHandle();
        }
        log.debug("No Cm-Handles currently found in an ADVISED state");
    }

    private YangModelCmHandle getAdvisedCmHandle() {
        final Collection<DataNode> advisedCmHandles = cpsDataPersistenceService.queryDataNodes("NCMP-Admin",
            "ncmp-dmi-registry", "//cm-handles[@state=\"ADVISED\"]",
            FetchDescendantsOption.OMIT_DESCENDANTS);
        for (final DataNode advisedCmHandle: advisedCmHandles) {
            if (advisedCmHandle != null) {
                return yangModelCmHandleRetriever.getYangModelCmHandle(advisedCmHandle.getLeaves()
                    .get("id").toString());
            }
        }
        return null;
    }

    private String updatedCmHandlesAsJson(final YangModelCmHandle newAdvisedCmHandle) {
        newAdvisedCmHandle.setCmHandleState("READY");
        return String.format("{\"cm-handles\":[%s]}",
            jsonObjectMapper.asJsonString(newAdvisedCmHandle));
    }

    private void updateCmHandleState(final String cmHandleJsonData) {
        cpsDataService.updateNodeLeaves(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, NCMP_DMI_REGISTRY_PARENT,
            cmHandleJsonData, NO_TIMESTAMP);
    }
}
