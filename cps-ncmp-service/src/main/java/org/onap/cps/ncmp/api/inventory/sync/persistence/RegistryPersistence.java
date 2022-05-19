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

package org.onap.cps.ncmp.api.inventory.sync.persistence;

import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NCMP_DATASPACE_NAME;
import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NCMP_DMI_REGISTRY_ANCHOR;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.ncmp.api.inventory.CmHandleState;
import org.onap.cps.ncmp.api.inventory.CompositeState;
import org.onap.cps.ncmp.api.inventory.LockReason;
import org.onap.cps.utils.JsonObjectMapper;

@RequiredArgsConstructor
public class RegistryPersistence {

    private final JsonObjectMapper jsonObjectMapper;

    private final CpsDataService cpsDataService;

    /**
     * update the cm handles state.
     *
     * @param cmHandleState cm handle state
     * @param cmHandleId cm handle id
     */
    public void updateCmHandleState(final CmHandleState cmHandleState, final String cmHandleId) {
        final String cmHandleJsonData = String.format("{\"state\":{\"cm-handle-state\":%s}}",
            jsonObjectMapper.asJsonString(cmHandleState));
        cpsDataService.updateNodeLeaves(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
            "/dmi-registry/cm-handles[@id='" + cmHandleId + "']",
            cmHandleJsonData, OffsetDateTime.now());

    }

    /**
     * Persist the lock reason and details to the database.
     *
     * @param cmHandleId cm handle id
     * @param reason lock reason
     * @param details lock details
     */
    public void saveLockReasonAndDetails(final String cmHandleId, final LockReason reason, final String details) {
        final CompositeState.LockReason lockReason =
            CompositeState.LockReason.builder().reason(reason).details(details).build();
        final String cmHandleLockReasonJsonData =  String.format("{\"lock-reason\":%s}",
            jsonObjectMapper.asJsonString(lockReason));
        cpsDataService.saveData(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
            "/dmi-registry/cm-handles[@id='" + cmHandleId + "']/state",
            cmHandleLockReasonJsonData, OffsetDateTime.now());
    }

}
