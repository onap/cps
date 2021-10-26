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

package org.onap.cps.ncmp.api.impl;

import java.util.Map;
import lombok.Getter;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;

@Getter
public class DmiServiceNames {

    private String dmiServiceName;
    private String dataServiceName;
    private String modelsServiceName;
    private Map<String, String> servicesNames;
    private CpsDataService cpsDataService;

    private static final String NCMP_DMI_SERVICE_NAME = "dmi-service-name";
    private static final String NCMP_DMI_DATA_SERVICE_NAME = "dmi-data-service-name";
    private static final String NCMP_DMI_MODEL_SERVICE_NAME = "dmi-model-service-name";
    private static final String NCMP_DATASPACE_NAME = "NCMP-Admin";
    private static final String NCMP_DMI_REGISTRY_ANCHOR = "ncmp-dmi-registry";

    /**
     * Constructor for storing plugin service names.
     * @param cmHandle cm handle.
     * @param combinedServiceName combined dmi plugin.
     * @param dataServiceName data service name.
     * @param modelsServiceName model service name.
     */
    public DmiServiceNames(final String cmHandle, final String combinedServiceName, final String dataServiceName,
                           final String modelsServiceName) {

        final DataNode cmHandleDataNode = fetchDataNodeFromDmiRegistryForCmHandle(cmHandle);


        if (!combinedServiceName.isEmpty() && (dataServiceName.isEmpty() && modelsServiceName.isEmpty())) {
            this.dmiServiceName = String.valueOf(cmHandleDataNode.getLeaves().get(NCMP_DMI_SERVICE_NAME));
            servicesNames.put(NCMP_DMI_SERVICE_NAME, dmiServiceName);
        }

        if (combinedServiceName.isEmpty() && (!dataServiceName.isEmpty() && !modelsServiceName.isEmpty())) {
            this.dataServiceName = String.valueOf(cmHandleDataNode.getLeaves()
                .get(NCMP_DMI_DATA_SERVICE_NAME));
            this.modelsServiceName = String.valueOf(cmHandleDataNode.getLeaves()
                .get(NCMP_DMI_MODEL_SERVICE_NAME));
            servicesNames.put(NCMP_DMI_DATA_SERVICE_NAME, dataServiceName);
            servicesNames.put(NCMP_DMI_MODEL_SERVICE_NAME, modelsServiceName);
        }
    }

    private DataNode fetchDataNodeFromDmiRegistryForCmHandle(final String cmHandle) {
        final String xpathForDmiRegistryToFetchCmHandle = "/dmi-registry/cm-handles[@id='" + cmHandle + "']";
        return cpsDataService.getDataNode(NCMP_DATASPACE_NAME,
            NCMP_DMI_REGISTRY_ANCHOR,
            xpathForDmiRegistryToFetchCmHandle,
            FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS);
    }

}
