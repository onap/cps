/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.operations;

import java.util.LinkedHashMap;
import java.util.Map;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.ncmp.api.models.CmHandle;
import org.onap.cps.ncmp.api.models.PersistenceCmHandle;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
import org.springframework.stereotype.Component;

/**
 * Retrieves PersistenceCmHandles & properties.
 */
@Component
public class PersistenceCmHandleRetriever {

    private static final String NCMP_DATASPACE_NAME = "NCMP-Admin";
    private static final String NCMP_DMI_REGISTRY_ANCHOR = "ncmp-dmi-registry";

    private CpsDataService cpsDataService;

    /**
     * Constructor for PersistenceCmHandleRetriever.
     * 
     * @param cpsDataService the cps data service.
     */
    public PersistenceCmHandleRetriever(final CpsDataService cpsDataService) {
        this.cpsDataService = cpsDataService;
    }

    /**
     * This method retieves dmi service name and properties for a given cm handle.
     * @param cmHandleId the id of the cm handle
     * @return persistence cm handle
     */
    public PersistenceCmHandle retrieveCmHandleDmiServiceNameAndProperties(final String cmHandleId) {
        final DataNode cmHandleDataNode = getCmHandleDataNode(cmHandleId);
        final CmHandle cmHandle = new CmHandle();
        cmHandle.setCmHandleID(cmHandleId);
        populateCmHandleProperties(cmHandleDataNode, cmHandle);
        return PersistenceCmHandle.toPersistenceCmHandle(
            String.valueOf(cmHandleDataNode.getLeaves().get("dmi-service-name")),
            String.valueOf(cmHandleDataNode.getLeaves().get("dmi-data-service-name")),
            String.valueOf(cmHandleDataNode.getLeaves().get("dmi-model-service-name")),
            cmHandle
        );
    }

    private DataNode getCmHandleDataNode(final String cmHandle) {
        final String xpathForDmiRegistryToFetchCmHandle = "/dmi-registry/cm-handles[@id='" + cmHandle + "']";
        return cpsDataService.getDataNode(NCMP_DATASPACE_NAME,
            NCMP_DMI_REGISTRY_ANCHOR,
            xpathForDmiRegistryToFetchCmHandle,
            FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS);
    }

    private static void populateCmHandleProperties(final DataNode cmHandleDataNode, final CmHandle cmHandle) {
        final Map<String, String> dmiProperties = new LinkedHashMap<>();
        final Map<String, String> publicProperties = new LinkedHashMap<>();
        for (final DataNode childDataNode: cmHandleDataNode.getChildDataNodes()) {
            if (childDataNode.getXpath().contains("/additional-properties[@name=")) {
                addProperty(childDataNode, dmiProperties);
            }
            if (childDataNode.getXpath().contains("/public-properties[name@=")) {
                addProperty(childDataNode, publicProperties);
            }
        }
        cmHandle.setDmiProperties(dmiProperties);
        cmHandle.setPublicProperties(publicProperties);
    }

    private static void addProperty(final DataNode propertyDataNode, final Map<String, String> propertiesAsMap) {
        propertiesAsMap.put(String.valueOf(propertyDataNode.getLeaves().get("name")),
            String.valueOf(propertyDataNode.getLeaves().get("value")));
    }

}
