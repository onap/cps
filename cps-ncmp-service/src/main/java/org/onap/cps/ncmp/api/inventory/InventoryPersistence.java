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

import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.ncmp.api.impl.utils.YangDataConverter;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.utils.CpsValidator;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class InventoryPersistence {

    private static final String NCMP_DATASPACE_NAME = "NCMP-Admin";

    private static final String NCMP_DMI_REGISTRY_ANCHOR = "ncmp-dmi-registry";

    private final JsonObjectMapper jsonObjectMapper;

    private final CpsDataService cpsDataService;

    private final CpsDataPersistenceService cpsDataPersistenceService;

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
     * Save the cm handles state.
     *
     * @param cmHandleId    cm handle id
     * @param compositeState composite state
     */
    public void saveCmHandleState(final String cmHandleId, final CompositeState compositeState) {
        final String cmHandleJsonData = String.format("{\"state\":%s}",
            jsonObjectMapper.asJsonString(compositeState));
        cpsDataService.replaceNodeTree(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
            "/dmi-registry/cm-handles[@id='" + cmHandleId + "']",
            cmHandleJsonData, OffsetDateTime.now());
    }

    /**
     * Method which returns cm handles by the cm handles state.
     *
     * @param cmHandleState cm handle state
     * @return a list of cm handles
     */
    public List<DataNode> getCmHandlesByState(final CmHandleState cmHandleState) {
        return cpsDataPersistenceService.queryDataNodes(NCMP_DATASPACE_NAME,
            NCMP_DMI_REGISTRY_ANCHOR, "//state[@cm-handle-state=\""
                + cmHandleState + "\"]/ancestor::cm-handles",
            FetchDescendantsOption.OMIT_DESCENDANTS);
    }

    /**
     * This method retrieves DMI service name, DMI properties and the state for a given cm handle.
     * @param cmHandleId the id of the cm handle
     * @return yang model cm handle
     */
    public YangModelCmHandle getYangModelCmHandle(final String cmHandleId) {
        CpsValidator.validateNameCharacters(cmHandleId);
        return YangDataConverter.convertCmHandleToYangModel(getCmHandleDataNode(cmHandleId), cmHandleId);
    }

    private DataNode getCmHandleDataNode(final String cmHandle) {
        final String xpathForDmiRegistryToFetchCmHandle = "/dmi-registry/cm-handles[@id='" + cmHandle + "']";
        return cpsDataService.getDataNode(NCMP_DATASPACE_NAME,
            NCMP_DMI_REGISTRY_ANCHOR,
            xpathForDmiRegistryToFetchCmHandle,
            FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS);
    }

}
