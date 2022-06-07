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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
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

    private static final String XPATH_TO_CM_HANDLE = "/dmi-registry/cm-handles[@id='" + "%s" + "']";

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
                String.format(XPATH_TO_CM_HANDLE, cmHandleId) + "/state",
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
            String.format(XPATH_TO_CM_HANDLE, cmHandleId),
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
     * This method retrieves DMI service name and DMI properties for a given cm handle.
     * @param cmHandleId the id of the cm handle
     * @return yang model cm handle
     */
    public YangModelCmHandle getYangModelCmHandle(final String cmHandleId) {
        CpsValidator.validateNameCharacters(cmHandleId);
        final DataNode cmHandleDataNode = getCmHandleDataNode(cmHandleId);
        final NcmpServiceCmHandle ncmpServiceCmHandle = new NcmpServiceCmHandle();
        ncmpServiceCmHandle.setCmHandleId(cmHandleId);
        populateCmHandleDetails(cmHandleDataNode, ncmpServiceCmHandle);
        return YangModelCmHandle.toYangModelCmHandle(
            (String) cmHandleDataNode.getLeaves().get("dmi-service-name"),
            (String) cmHandleDataNode.getLeaves().get("dmi-data-service-name"),
            (String) cmHandleDataNode.getLeaves().get("dmi-model-service-name"),
            ncmpServiceCmHandle
        );
    }

    private DataNode getCmHandleDataNode(final String cmHandle) {
        return cpsDataService.getDataNode(NCMP_DATASPACE_NAME,
            NCMP_DMI_REGISTRY_ANCHOR,
            String.format(XPATH_TO_CM_HANDLE, cmHandle),
            FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS);
    }

    private static void populateCmHandleDetails(final DataNode cmHandleDataNode,
                                                final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final Map<String, String> dmiProperties = new LinkedHashMap<>();
        final Map<String, String> publicProperties = new LinkedHashMap<>();
        final CompositeStateBuilder compositeStateBuilder = new CompositeStateBuilder();
        CompositeState compositeState = compositeStateBuilder.build();
        for (final DataNode childDataNode: cmHandleDataNode.getChildDataNodes()) {
            if (childDataNode.getXpath().contains("/additional-properties[@name=")) {
                addProperty(childDataNode, dmiProperties);
            } else if (childDataNode.getXpath().contains("/public-properties[@name=")) {
                addProperty(childDataNode, publicProperties);
            } else if (childDataNode.getXpath().endsWith("/state")) {
                compositeState = compositeStateBuilder.fromDataNode(childDataNode).build();
            }
        }
        ncmpServiceCmHandle.setDmiProperties(dmiProperties);
        ncmpServiceCmHandle.setPublicProperties(publicProperties);
        ncmpServiceCmHandle.setCompositeState(compositeState);
    }

    private static void addProperty(final DataNode propertyDataNode, final Map<String, String> propertiesAsMap) {
        propertiesAsMap.put(String.valueOf(propertyDataNode.getLeaves().get("name")),
            String.valueOf(propertyDataNode.getLeaves().get("value")));
    }

}
