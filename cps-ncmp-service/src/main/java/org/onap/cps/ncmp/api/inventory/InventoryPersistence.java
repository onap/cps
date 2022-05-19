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

package org.onap.cps.ncmp.api.inventory;

import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NCMP_DATASPACE_NAME;
import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NCMP_DMI_REGISTRY_ANCHOR;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class InventoryPersistence {

    private final JsonObjectMapper jsonObjectMapper;

    private final CpsDataService cpsDataService;

    private static final CompositeStateBuilder compositeStateBuilder = new CompositeStateBuilder();

    /**
     * Get the Cm Handle Composite State from the data node.
     *
     * @param cmHandleId cm handle id
     * @return the cm handle composite state
     */
    public CompositeState getCmHandleState(final String cmHandleId) {
        final DataNode stateAsDataNode = cpsDataService.getDataNode(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
            "/dmi-registry/cm-handles[@id='" + cmHandleId + "']/state",
            FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS);
        return compositeStateBuilder.fromDataNode(stateAsDataNode).build();
    }

    /**
     * Update the cm handles state.
     *
     * @param cmHandleId    cm handle id
     * @param compositeState composite state
     */
    public void updateCmHandleState(final String cmHandleId, final CompositeState compositeState,
                                    final String lastUpdateTime) {
        compositeState.setLastUpdateTime(lastUpdateTime);
        final String cmHandleJsonData = String.format("{\"state\":%s}",
            jsonObjectMapper.asJsonString(compositeState));
        cpsDataService.replaceNodeTree(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
            "/dmi-registry/cm-handles[@id='" + cmHandleId + "']",
            cmHandleJsonData, OffsetDateTime.now());
    }

}
